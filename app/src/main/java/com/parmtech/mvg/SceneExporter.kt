package com.parmtech.mvg

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import java.io.File
import java.util.UUID

/**
 * Local, owner-controlled MP4 export of finished clips in project storyboard order.
 * Google Cloud video generation is NOT invoked: this operation uses the phone's codecs
 * and may be slow or fail for 4K/HDR/mixed-codec source clips.
 *
 * In v0.5 this exporter is Activity-scoped: do not assume export survives process death.
 */
@OptIn(UnstableApi::class)
class SceneExporter(private val context: Context) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var engine: Transformer? = null
    private var running = false
    private var destination: File? = null
    private var progressTask: Runnable? = null

    fun isRunning(): Boolean = running

    fun export(
        projectId: String,
        clipsInStoryboardOrder: List<FilmClip>,
        onProgress: (Int) -> Unit,
        onComplete: (File?, String?) -> Unit
    ) {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "Media3 export must start on Android main thread"
        }
        require(!running) { "Another movie export is in progress" }
        require(clipsInStoryboardOrder.size >= 2) { "Choose at least two finished scenes" }
        require(clipsInStoryboardOrder.all {
            it.projectId == projectId && it.status == "Complete"
        }) { "Only downloaded, complete project scenes can be exported" }
        val inputFiles = clipsInStoryboardOrder.map { clip ->
            File(clip.localPath).also { file ->
                require(file.isFile && file.length() > 100) {
                    "Missing local MP4 for scene: " + clip.title
                }
            }
        }
        // Prevent obvious 'storage full' failures, without promising exact re-encode size.
        val inputBytes = inputFiles.sumOf { it.length() }
        val minimumFree = inputBytes / 2L + (80L * 1024L * 1024L)
        require(context.filesDir.usableSpace > minimumFree) {
            "Free up phone storage before exporting these scenes"
        }
        val folder = File(context.filesDir, "media/exports").apply { mkdirs() }
        val output = File(folder,
            "MVG_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().take(8) + ".mp4")
        destination = output

        val sequence = EditedMediaItemSequence.withAudioAndVideoFrom(
            inputFiles.map { file ->
                EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(file))).build()
            }
        )
        val composition = Composition.Builder(sequence).build()

        running = true
        val listener = object : Transformer.Listener {
            override fun onCompleted(
                composition: Composition,
                exportResult: ExportResult
            ) {
                if (!running) return
                finish()
                if (output.isFile && output.length() > 100L) {
                    onProgress(100)
                    onComplete(output, null)
                } else {
                    output.delete()
                    onComplete(null, "Export completed without a valid MP4 file")
                }
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException
            ) {
                finish()
                output.delete()
                onComplete(null, "MP4 export failed: " +
                    (exportException.localizedMessage ?: "Device may not support these video codecs"))
            }
        }
        try {
            val transformer = Transformer.Builder(context.applicationContext)
                .addListener(listener).build()
            engine = transformer
            transformer.start(composition, output.absolutePath)
            val holder = ProgressHolder()
            val task = object : Runnable {
                override fun run() {
                    if (!running) return
                    try {
                        if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                            onProgress(holder.progress.coerceIn(0, 99))
                        }
                    } catch (_: Exception) {
                        // Progress is best effort, but a transient progress issue should not
                        // cancel media work already started.
                    }
                    if (running) mainHandler.postDelayed(this, 600L)
                }
            }
            progressTask = task
            mainHandler.postDelayed(task, 600L)
        } catch (error: Exception) {
            finish()
            output.delete()
            onComplete(null, "Cannot start export: " +
                (error.localizedMessage ?: "Unsupported source video"))
        }
    }

    fun cancel() {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (!running) return
        running = false
        stopProgress()
        try { engine?.cancel() } finally {
            engine = null
            destination?.delete()
            destination = null
        }
    }

    private fun finish() {
        running = false
        stopProgress()
        engine = null
        destination = null
    }

    private fun stopProgress() {
        progressTask?.let { mainHandler.removeCallbacks(it) }
        progressTask = null
    }

    companion object {
        /**
         * Copies an exported movie to the phone's Movies/MVG album. Call only after
         * user taps Save to Movies, not automatically, and do this off the UI thread.
         */
        fun copyToMovies(context: Context, source: File): Uri {
            require(VideoOutputGuard.isMp4File(source)) {
                "Local MP4 missing or failed video header validation"
            }
            require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "On Android 9 or earlier use Share movie to save the MP4"
            }
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, source.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/MVG")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = requireNotNull(
                resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ) { "Android could not create a Movies file" }
            try {
                requireNotNull(resolver.openOutputStream(uri)) { "Cannot write destination file" }
                    .use { sink ->
                        source.inputStream().use { input -> input.copyTo(sink) }
                    }
                resolver.update(uri, ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }, null, null)
                return uri
            } catch (error: Exception) {
                resolver.delete(uri, null, null)
                throw error
            }
        }
    }
}
