package com.parmtech.mvg

import android.net.Uri
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

/**
 * Checks Android 10+ MediaStore copy on a running emulator. Does NOT pretend
 * to test decoding/playing actual Veo video or device-specific 4K codecs.
 */
@RunWith(AndroidJUnit4::class)
class ClipGalleryDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun completedLocalMp4CanBeCopiedToMoviesWithoutChangingTheOriginal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val directory = File(context.filesDir, "media").apply { mkdirs() }
        val source = File(directory, "clip_gallery_test_" + UUID.randomUUID() + ".mp4")
        val original = ByteArray(8192) { (it % 197).toByte() }
        // Minimal structural MP4 signature for storage transfer testing.
        original[0] = 0
        original[1] = 0
        original[2] = 0
        original[3] = 24
        "ftyp".toByteArray().copyInto(original, 4)
        "isom".toByteArray().copyInto(original, 8)
        var saved: Uri? = null
        try {
            source.writeBytes(original)
            assertTrue(VideoOutputGuard.isMp4File(source))
            saved = SceneExporter.copyToMovies(context, source)
            val uri = requireNotNull(saved)
            val copied = requireNotNull(context.contentResolver.openInputStream(uri)).use {
                it.readBytes()
            }
            assertArrayEquals(original, copied)
            assertArrayEquals(original, source.readBytes())
        } finally {
            saved?.let { context.contentResolver.delete(it, null, null) }
            source.delete()
        }
    }

    @Test fun invalidMp4CannotPolluteGallery() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val directory = File(context.filesDir, "media").apply { mkdirs() }
        val source = File(directory, "bad_gallery_" + UUID.randomUUID() + ".mp4")
        try {
            source.writeBytes(ByteArray(512) { 0x5a.toByte() })
            try {
                SceneExporter.copyToMovies(context, source)
                fail("Invalid MP4 bytes should not be inserted in MediaStore")
            } catch (expected: IllegalArgumentException) {
                assertTrue(expected.message.orEmpty().contains("MP4"))
            }
        } finally {
            source.delete()
        }
    }
}
