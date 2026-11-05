package com.parmtech.mvg

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ProjectBackupTest {
    private val id = UUID.randomUUID().toString()

    @Test
    fun onlyExpectedPrivateMediaEntriesAreAllowed() {
        assertTrue(ProjectBackup.entryIsValid("media/clips/" + id + ".mp4"))
        assertTrue(ProjectBackup.entryIsValid("media/movies/" + id + ".mp4"))
        assertTrue(ProjectBackup.entryIsValid("media/avatars/" + id + ".jpg"))
        assertTrue(ProjectBackup.entryIsValid("media/avatars/" + id + ".png"))
    }

    @Test
    fun rejectsZipSlipAbsolutePathsAndNonMediaFiles() {
        val malicious = listOf(
            "../media/clips/" + id + ".mp4",
            "media/../../secrets/token.txt",
            "/media/clips/" + id + ".mp4",
            "media/clips/" + id + ".json",
            "media/movies/" + id + ".png",
            "media/avatars/" + id + ".mp4",
            "media/clips/abc.mp4",
            "media/clips/" + id + ".mp4/../../other",
            "media/clips/" + id + ".mp4\n"
        )
        malicious.forEach { assertFalse(it, ProjectBackup.entryIsValid(it)) }
    }
}
