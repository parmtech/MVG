package com.parmtech.mvg

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Resumes a *known* provider operation. Never submits a paid video-generation POST.
 * Android OS decides exact background scheduling; backoff and network constraints apply.
 */
class VideoJobWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString(VideoJobs.CLIP_ID) ?: return@withContext Result.failure()
        val store = StudioStore(applicationContext)
        val clip = store.getClipById(id) ?: return@withContext Result.success()
        if (clip.status == "Complete" || clip.status == "Failed") return@withContext Result.success()
        if (clip.operation.isBlank()) return@withContext Result.failure()
        // Never poll with a token from a different auth mode or an expired Google grant.
        // This only rechecks an existing operation; no paid generation is resubmitted.
        if (store.config.authMode != "GOOGLE" || !store.isGoogleAuthorizationFresh()) {
            store.updateClip(clip.copy(status = "Auth required",
                error = "Google authorization expired or switched. Continue with Google, then Check / Download."))
            return@withContext Result.failure()
        }
        val token = store.activeCredential()
        if (token.isBlank()) {
            store.updateClip(clip.copy(status = "Auth required",
                error = "Reconnect Google Cloud in Settings, then Check / Download."))
            return@withContext Result.failure()
        }
        try {
            val state = VertexClient.pollVideo(store.config, token, clip)
            when {
                !state.done -> {
                    if (runAttemptCount >= 12) {
                        store.updateClip(clip.copy(status = "Check again",
                            error = "Background check limit reached. Tap Check / Download to continue."))
                        Result.success()
                    } else {
                        store.updateClip(clip.copy(status = "Running", error = ""))
                        Result.retry()
                    }
                }
                state.error.isNotBlank() -> {
                    store.updateClip(clip.copy(status = "Failed", error = state.error))
                    Result.success()
                }
                else -> {
                    try {
                        val output = store.videoOutputFile(clip.id)
                        VertexClient.writeVideo(token, state, output)
                        store.updateClip(clip.copy(status = "Complete",
                            remoteUri = state.remoteUri, localPath = output.absolutePath, error = ""))
                        Result.success()
                    } catch (error: Exception) {
                        // Provider operation succeeded. Only output retrieval is retried;
                        // never regenerate the billable video.
                        store.updateClip(clip.copy(status = "OutputReady", remoteUri = state.remoteUri,
                            error = error.message?.take(240) ?: "Download not complete"))
                        if (runAttemptCount >= 12) Result.success() else Result.retry()
                    }
                }
            }
        } catch (error: Exception) {
            val reason = error.message?.take(240) ?: "Unknown provider error"
            if (reason.contains("HTTP 401") || reason.contains("HTTP 403") ||
                reason.contains("OAuth access token", ignoreCase = true)) {
                store.updateClip(clip.copy(status = "Auth required",
                    error = reason + " — reconnect in Settings, then check again"))
                Result.failure()
            } else if (runAttemptCount >= 12) {
                store.updateClip(clip.copy(status = "Check again", error = reason))
                Result.success()
            } else {
                store.updateClip(clip.copy(status = "Waiting for network", error = reason))
                Result.retry()
            }
        }
    }
}

/**
 * Pure policy for existing Cloud operations; never choose to submit a paid job here.
 * The operation ID is required, so an unconfirmed "Submitting" clip cannot regenerate.
 */
internal object VideoRecoveryPolicy {
    fun canResume(clip: FilmClip): Boolean =
        clip.operation.isNotBlank() &&
            clip.status != "Complete" &&
            clip.status != "Failed"

    fun pending(clips: List<FilmClip>): List<FilmClip> = clips.filter(::canResume)
}

object VideoJobs {
    const val CLIP_ID = "clip_id"

    /** Re-enqueue only *known* operations after successful Google reauthorization. */
    fun resumeKnown(context: Context, clips: List<FilmClip>, authorized: Boolean): Int {
        if (!authorized) return 0
        val pending = VideoRecoveryPolicy.pending(clips)
        pending.forEach { enqueue(context, it) }
        return pending.size
    }

    fun enqueue(context: Context, clip: FilmClip) {
        if (!VideoRecoveryPolicy.canResume(clip)) return
        val request = OneTimeWorkRequestBuilder<VideoJobWorker>()
            .setInputData(workDataOf(CLIP_ID to clip.id))
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30L, TimeUnit.SECONDS)
            .addTag("MVG-video-" + clip.id)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "MVG-video-" + clip.id, ExistingWorkPolicy.KEEP, request
        )
    }

    fun cancelChecks(context: Context, clipId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("MVG-video-" + clipId)
    }
}
