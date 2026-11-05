package com.parmtech.mvg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoSafetyTest {
    private fun clip(status: String, operation: String = "projects/p/locations/us-central1/operations/op") =
        FilmClip(
            projectId = "project-1", title = "Scene", prompt = "Golden sunset",
            modelId = "veo-3.1-fast-generate-001", duration = 8, ratio = "16:9",
            resolution = "720p", status = status, operation = operation
        )

    @Test fun onlyPreviouslySubmittedVideoOperationsResume() {
        assertTrue(VideoRecoveryPolicy.canResume(clip("Running")))
        assertTrue(VideoRecoveryPolicy.canResume(clip("Auth required")))
        assertTrue(VideoRecoveryPolicy.canResume(clip("Check again")))
        assertTrue(VideoRecoveryPolicy.canResume(clip("OutputReady")))
        assertFalse(VideoRecoveryPolicy.canResume(clip("Submitting", "")))
        assertFalse(VideoRecoveryPolicy.canResume(clip("Needs review", "")))
        assertFalse(VideoRecoveryPolicy.canResume(clip("Complete")))
        assertFalse(VideoRecoveryPolicy.canResume(clip("Failed")))
        assertEquals(2, VideoRecoveryPolicy.pending(listOf(
            clip("Failed"), clip("Check again"), clip("Submitting", ""), clip("Auth required")
        )).size)
    }

    @Test fun rejectsNonVideoResponsesAndShortData() {
        val html = "<html><body>Login required</body></html>".toByteArray()
        assertFalse(VideoOutputGuard.isMp4Header(html))
        assertFalse(VideoOutputGuard.isMp4Header(byteArrayOf(0, 0, 0, 8)))
        assertFalse(VideoOutputGuard.isMp4Header(byteArrayOf(
            0, 0, 0, 0, 102, 116, 121, 112, 105, 115, 111, 109
        )))
    }

    @Test fun acceptsMp4AndFragmentedMp4Signatures() {
        for (marker in listOf("ftyp", "styp")) {
            val head = byteArrayOf(0, 0, 0, 24) + marker.toByteArray() +
                byteArrayOf(105, 115, 111, 109)
            assertTrue(VideoOutputGuard.isMp4Header(head))
        }
    }
}
