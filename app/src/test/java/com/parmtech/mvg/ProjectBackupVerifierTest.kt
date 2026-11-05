package com.parmtech.mvg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ProjectBackupVerifierTest {
    private val password = "strong-unique-project-passphrase".toCharArray()
    private val projectId = UUID.randomUUID().toString()
    private val clipId = UUID.randomUUID().toString()
    private val fileName = "media/clips/$clipId.mp4"
    private val media = ByteArray(150_000) { ((it * 79 + 11) % 251).toByte() }

    private fun manifest(hasMedia: Boolean = true, correctHash: Boolean = true): JSONObject {
        val hash = MessageDigest.getInstance("SHA-256").digest(media)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        val entry = JSONObject().put("name", fileName).put("size", media.size)
            .put("sha256", if (correctHash) hash else "0".repeat(64))
        val project = JSONObject().put("id", projectId).put("title", "Storyboard")
        val clip = JSONObject().put("id", clipId).put("projectId", projectId)
            .put("status", "Complete")
            .put("backupMedia", if (hasMedia) fileName else "")
        return JSONObject()
            .put("format", "MVG_PORTABLE_PROJECT_BACKUP")
            .put("schemaVersion", 1)
            .put("projects", JSONArray().put(project))
            .put("clips", JSONArray().put(clip))
            .put("movies", JSONArray())
            .put("avatars", JSONArray())
            .put("messages", JSONArray())
            .put("drafts", JSONObject().put(projectId, "Opening scene"))
            .put("files", if (hasMedia) JSONArray().put(entry) else JSONArray())
    }

    private fun zip(encrypted: Boolean, manifest: JSONObject, includeMedia: Boolean = true): ByteArray {
        val sink = ByteArrayOutputStream()
        val output = if (encrypted) BackupCrypt.encrypt(sink, password) else sink
        ZipOutputStream(output).use { zip ->
            if (includeMedia) {
                zip.putNextEntry(ZipEntry(fileName))
                zip.write(media)
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return sink.toByteArray()
    }

    @Test fun encryptedBackupVerifiesEveryAssetWithoutImporting() {
        val bytes = zip(true, manifest())
        val report = ProjectBackup.verifyStream(ByteArrayInputStream(bytes), password)
        assertEquals(1, report.projects)
        assertEquals(1, report.clips)
        assertEquals(1, report.files)
        assertEquals(media.size.toLong(), report.bytes)
    }

    @Test fun legacyPlainZipCanStillBeVerified() {
        val report = ProjectBackup.verifyStream(
            ByteArrayInputStream(zip(false, manifest())), CharArray(0))
        assertEquals(1, report.projects)
        assertEquals(1, report.files)
    }

    @Test fun malformedMediaDigestIsRejected() {
        val bytes = zip(true, manifest(correctHash = false))
        try {
            ProjectBackup.verifyStream(ByteArrayInputStream(bytes), password)
            fail("A corrupt media digest must fail verification")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("SHA-256"))
        }
    }

    @Test fun missingMediaReferencedByManifestIsRejected() {
        val bytes = zip(true, manifest(), includeMedia = false)
        try {
            ProjectBackup.verifyStream(ByteArrayInputStream(bytes), password)
            fail("Missing video must fail verification")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("count") ||
                expected.message.orEmpty().contains("missing"))
        }
    }

    @Test fun validBackupCanStillWarnAboutMissingOriginalLocalMedia() {
        val archive = zip(true, manifest(hasMedia = false), includeMedia = false)
        val result = ProjectBackup.verifyStream(ByteArrayInputStream(archive), password)
        assertEquals(1, result.projects)
        assertEquals(0, result.files)
        assertEquals(1, result.missingMedia)
    }

    @Test fun wrongPasswordFailsBeforeAnySuccessReport() {
        val bytes = zip(true, manifest())
        try {
            ProjectBackup.verifyStream(ByteArrayInputStream(bytes), "wrong-password-xyz".toCharArray())
            fail("Invalid password must fail verification")
        } catch (expected: Exception) {
            assertTrue(expected.message.orEmpty().contains("password") ||
                expected.message.orEmpty().contains("damaged"))
        }
    }

    @Test fun unreferencedMediaInManifestIsRejected() {
        val bytes = zip(true, manifest(hasMedia = false), includeMedia = true)
        try {
            ProjectBackup.verifyStream(ByteArrayInputStream(bytes), password)
            fail("Unlisted video must fail verification")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("count") ||
                expected.message.orEmpty().contains("Unreferenced"))
        }
    }
}
