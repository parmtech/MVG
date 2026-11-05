package com.parmtech.mvg

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCryptTest {
    private val password = "local-owner-backup-passphrase".toCharArray()

    private fun encrypt(payload: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        BackupCrypt.encrypt(output, password).use { it.write(payload) }
        return output.toByteArray()
    }

    @Test fun encryptionIsRoundTripAuthenticatedAcrossMultipleChunks() {
        val plain = ByteArray(200_000) { ((it * 17 + 5) % 251).toByte() }
        val cipher = encrypt(plain)
        assertFalse(cipher.contentEquals(plain))
        assertTrue(BackupCrypt.isEncrypted(BufferedInputStream(ByteArrayInputStream(cipher))))
        val stream = BackupCrypt.decrypt(ByteArrayInputStream(cipher), password)
        val recovered = stream.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(18000)
            while (true) {
                val n = input.read(buffer)
                if (n == -1) break
                out.write(buffer, 0, n)
            }
            input.verifyComplete()
            out.toByteArray()
        }
        assertArrayEquals(plain, recovered)
    }

    @Test fun badPasswordOrTamperingNeverAuthenticates() {
        val data = encrypt(ByteArray(70_000) { 0x43 })
        val wrong = BackupCrypt.decrypt(
            ByteArrayInputStream(data), "another-owner-password".toCharArray())
        try {
            wrong.use { it.verifyComplete() }
            fail("Wrong password must be rejected")
        } catch (expected: IOException) {
            assertTrue(expected.message.orEmpty().contains("Wrong backup password"))
        }
        val corrupted = data.clone()
        corrupted[corrupted.size / 2] = (corrupted[corrupted.size / 2].toInt() xor 1).toByte()
        val stream = BackupCrypt.decrypt(ByteArrayInputStream(corrupted), password)
        try {
            stream.use { it.verifyComplete() }
            fail("Corrupt archive must not authenticate")
        } catch (expected: IOException) {
            assertTrue(expected.message.orEmpty().contains("damaged encrypted file"))
        }
    }

    @Test fun truncatedArchiveCannotPassFinalTagCheck() {
        val cipher = encrypt(ByteArray(50_000) { 0x5a })
        val shorter = cipher.copyOf(cipher.size - 14)
        val reader = BackupCrypt.decrypt(ByteArrayInputStream(shorter), password)
        try {
            reader.use { it.verifyComplete() }
            fail("Truncation must be rejected")
        } catch (expected: Exception) {
            assertTrue(expected is IOException || expected is IllegalArgumentException)
        }
    }

    @Test fun trailingDataAfterAuthenticatedEndIsRejected() {
        val cipher = encrypt(ByteArray(55) { 0x12 }) + byteArrayOf(1, 2, 3)
        val reader = BackupCrypt.decrypt(ByteArrayInputStream(cipher), password)
        try {
            reader.use { it.verifyComplete() }
            fail("Trailing data must be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("trailing"))
        }
    }

    @Test fun passwordStrengthMinimumIsEnforced() {
        try {
            BackupCrypt.encrypt(ByteArrayOutputStream(), "short".toCharArray())
            fail("Weak password should be rejected")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("12"))
        }
    }
}
