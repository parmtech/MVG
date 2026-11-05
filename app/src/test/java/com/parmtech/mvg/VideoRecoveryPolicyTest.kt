package com.parmtech.mvg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoRecoveryPolicyTest {
    private fun clip(status: String, operation: String = "projects/example/locations/us-central1/operations/123") =
        FilmClip(
            projectId = "project-1", title = "Scene", prompt = "A filmed scene",
            modelId = "veo-3.1-fast-generate-001", duration = 8,
            ratio = "16:9", resolution = "720p", status = status, operation = operation
        )

    @Test fun onlyKnownProviderOperationsAreRecoverable() {
        assertTrue(VideoRecoveryPolicy.canResume(clip("Running")))
        assertTrue(VideoRecoveryPolicy.canResume(clip("Auth required")))
        assertTrue(VideoRecoveryPolicy.canResume(clip("OutputReady")))
        assertTrue(VideoRecoveryPolicy.canResume(clip("Check again")))
        assertTrue(VideoRecoveryPolicy.canResume(clip("Waiting for network")))
        assertFalse(VideoRecoveryPolicy.canResume(clip("Submitting", operation = "")))
        assertFalse(VideoRecoveryPolicy.canResume(clip("Needs review", operation = "")))
        assertFalse(VideoRecoveryPolicy.canResume(clip("Complete")))
        assertFalse(VideoRecoveryPolicy.canResume(clip("Failed")))
    }

    @Test fun pendingOnlyIncludesSafeExistingOperationIds() {
        val jobs = listOf(
            clip("Running"), clip("Auth required"), clip("Complete"),
            clip("Submitting", operation = ""), clip("Failed"), clip("Check again")
        )
        assertEquals(listOf("Running", "Auth required", "Check again"),
            VideoRecoveryPolicy.pending(jobs).map { it.status })
    }
}
