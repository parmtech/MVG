package com.parmtech.mvg

import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Runs on a real Android runtime / emulator rather than just JVM mocks.
 * Exercises app ContentResolver + FileProvider, Keystore-adjacent StudioStore,
 * Android private media files, AES-GCM backup, read-only verification and
 * add-only ZIP restoration. No Google account, paid API call or network is used.
 */
@RunWith(AndroidJUnit4::class)
class PortableBackupDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun temporaryArchive(extension: String): Pair<File, android.net.Uri> {
        val file = File(context.filesDir, "media/test_backup_" + UUID.randomUUID() + extension)
        check(file.parentFile?.mkdirs() == true || file.parentFile?.isDirectory == true)
        return file to FileProvider.getUriForFile(
            context, context.packageName + ".files", file
        )
    }

    @Test fun encryptedBackupThroughFileProviderVerifiesAndRestoresAsNewProject() {
        val store = StudioStore(context)
        val project = store.createProject("Emulator backup test " + UUID.randomUUID())
        val originalBytes = ByteArray(180_000) { ((it * 13 + 17) % 251).toByte() }
        val sourceFile = store.videoOutputFile(UUID.randomUUID().toString())
            .apply { writeBytes(originalBytes) }
        val clip = FilmClip(
            projectId = project.id,
            title = "Complete existing film",
            prompt = "A safe storyboard backup device test",
            modelId = "veo-3.1-fast-generate-001",
            duration = 8,
            ratio = "16:9",
            resolution = "720p",
            status = "Complete",
            localPath = sourceFile.absolutePath
        )
        store.addClip(clip)
        store.saveDraft(project.id, "My recovered draft")
        val (archive, uri) = temporaryArchive(".mvgbackup")
        val passphrase = "device-test-passphrase-unique".toCharArray()
        var restored: BackupImport? = null
        try {
            val originalSnapshot = store.portableBackupSnapshot()
            assertTrue(originalSnapshot.assets.any { it.entryName.endsWith(".mp4") })
            val created = ProjectBackup.create(context, uri, originalSnapshot, passphrase)
            assertTrue(archive.exists())
            assertTrue(archive.length() > 0)
            assertTrue(created.files >= 1)
            assertEquals(0, created.missingMedia)

            val verified = ProjectBackup.verify(context, uri, passphrase)
            assertTrue(verified.projects >= 1)
            assertTrue(verified.clips >= 1)
            assertEquals(0, verified.missingMedia)

            val beforeCount = store.projects.size
            val plan = ProjectBackup.read(context, uri, passphrase)
            restored = plan
            val importedProject = plan.projects.single { it.title.startsWith(project.title) }
            assertNotEquals(project.id, importedProject.id)
            val importedClip = plan.clips.single { it.projectId == importedProject.id }
            assertNotEquals(clip.id, importedClip.id)
            assertTrue(File(importedClip.localPath).exists())
            assertArrayEquals(originalBytes, File(importedClip.localPath).readBytes())
            assertEquals("My recovered draft", plan.drafts[importedProject.id])

            store.commitBackupImport(plan)
            assertEquals(beforeCount + plan.projects.size, store.projects.size)
            val reloaded = StudioStore(context)
            assertTrue(reloaded.projects.any { it.id == importedProject.id })
            assertTrue(reloaded.clips.any { it.id == importedClip.id })
            assertArrayEquals(originalBytes, sourceFile.readBytes())
        } finally {
            restored?.projects?.forEach { store.removeProject(it.id) }
            restored?.createdFiles?.forEach { it.delete() }
            store.removeProject(project.id)
            sourceFile.delete()
            archive.delete()
            passphrase.fill('\u0000')
        }
    }

    @Test fun wrongPasswordCannotImportOrModifyOriginalProject() {
        val store = StudioStore(context)
        val beforeProjects = store.projects.map { it.id }.toSet()
        val (archive, uri) = temporaryArchive(".mvgbackup")
        val pass = "proper-device-backup-password".toCharArray()
        try {
            val snapshot = BackupSnapshot(JSONObject()
                .put("projects", JSONArray())
                .put("clips", JSONArray())
                .put("movies", JSONArray())
                .put("avatars", JSONArray())
                .put("messages", JSONArray())
                .put("drafts", JSONObject()), emptyList())
            ProjectBackup.create(context, uri, snapshot, pass)
            try {
                ProjectBackup.read(context, uri, "incorrect-password-here".toCharArray())
                fail("Must not import using an incorrect password")
            } catch (_: Exception) {
                assertEquals(beforeProjects, StudioStore(context).projects.map { it.id }.toSet())
            }
        } finally {
            archive.delete()
            pass.fill('\u0000')
        }
    }

    @Test fun legacyZipIsReadableWithoutPasswordAndDoesNotAutoGenerateVideos() {
        val (archive, uri) = temporaryArchive(".zip")
        val manifest = JSONObject()
            .put("format", "MVG_PORTABLE_PROJECT_BACKUP")
            .put("schemaVersion", 1)
            .put("projects", JSONArray())
            .put("clips", JSONArray())
            .put("movies", JSONArray())
            .put("avatars", JSONArray())
            .put("messages", JSONArray())
            .put("drafts", JSONObject())
            .put("files", JSONArray())
        try {
            ZipOutputStream(archive.outputStream()).use { out ->
                out.putNextEntry(ZipEntry("manifest.json"))
                out.write(manifest.toString().toByteArray())
                out.closeEntry()
            }
            assertFalse(BackupCrypt.isEncrypted(archive.inputStream().buffered()))
            val result = ProjectBackup.verify(context, uri, CharArray(0))
            assertEquals(0, result.projects)
            val plan = ProjectBackup.read(context, uri, CharArray(0))
            assertTrue(plan.projects.isEmpty())
            assertTrue(plan.createdFiles.isEmpty())
        } finally {
            archive.delete()
        }
    }
}
