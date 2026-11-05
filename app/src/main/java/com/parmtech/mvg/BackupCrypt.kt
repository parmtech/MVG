package com.parmtech.mvg

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FilterInputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * MVGSEC01: password-encrypted stream container for private ZIP backups.
 *
 * Version 1 format: MAGIC (8 ASCII bytes), random salt (16), random nonce-prefix (8),
 * then records: int32 plaintextLength + AES-256-GCM(plaintext + 16-byte tag).
 * Each record authenticates MAGIC, salt, nonce prefix, record counter and length as AAD.
 * Records are at most 64 KiB. A final authenticated zero-length record MUST be present,
 * followed immediately by EOF. No unauthenticated plaintext is exposed by the reader.
 *
 * PBKDF2-HMAC-SHA256 (250,000 iterations) is portable to Android 8+. For offline
 * high-value backups prefer a strong unique passphrase; there is no password recovery.
 */
object BackupCrypt {
    private val MAGIC = "MVGSEC01".toByteArray(Charsets.US_ASCII)
    private const val SALT_SIZE = 16
    private const val PREFIX_SIZE = 8
    private const val GCM_TAG = 16
    private const val CHUNK = 64 * 1024
    private const val PBKDF2_ITERATIONS = 250_000
    private const val MAX_RECORDS = 65_536

    fun isEncrypted(input: BufferedInputStream): Boolean {
        input.mark(MAGIC.size)
        val prefix = ByteArray(MAGIC.size)
        val n = input.read(prefix)
        input.reset()
        return n == MAGIC.size && prefix.contentEquals(MAGIC)
    }

    fun encrypt(destination: OutputStream, password: CharArray): OutputStream {
        require(password.size >= 12) { "Use a backup password with at least 12 characters" }
        val random = SecureRandom()
        val salt = ByteArray(SALT_SIZE).also(random::nextBytes)
        val prefix = ByteArray(PREFIX_SIZE).also(random::nextBytes)
        val key = derive(password, salt)
        val output = DataOutputStream(destination)
        output.write(MAGIC)
        output.write(salt)
        output.write(prefix)
        return SealedOutput(output, salt, prefix, key)
    }

    fun decrypt(source: InputStream, password: CharArray): VerifiedInput {
        val input = DataInputStream(source)
        val magic = ByteArray(MAGIC.size)
        input.readFully(magic)
        require(magic.contentEquals(MAGIC)) { "This is not an encrypted MVG backup" }
        val salt = ByteArray(SALT_SIZE).also(input::readFully)
        val prefix = ByteArray(PREFIX_SIZE).also(input::readFully)
        require(password.isNotEmpty()) { "Enter the password used when creating this backup" }
        return VerifiedInput(input, salt, prefix, derive(password, salt))
    }

    private fun derive(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, 256)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
            try { SecretKeySpec(bytes, "AES") } finally { bytes.fill(0) }
        } finally { spec.clearPassword() }
    }

    private fun cryptCipher(
        mode: Int, key: SecretKeySpec, salt: ByteArray, prefix: ByteArray,
        counter: Int, length: Int
    ): Cipher {
        val iv = ByteArray(12)
        prefix.copyInto(iv, 0)
        iv[8] = (counter ushr 24).toByte()
        iv[9] = (counter ushr 16).toByte()
        iv[10] = (counter ushr 8).toByte()
        iv[11] = counter.toByte()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(mode, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(MAGIC)
        cipher.updateAAD(salt)
        cipher.updateAAD(prefix)
        cipher.updateAAD(iv, 8, 4)
        cipher.updateAAD(byteArrayOf(
            (length ushr 24).toByte(), (length ushr 16).toByte(),
            (length ushr 8).toByte(), length.toByte()
        ))
        return cipher
    }

    private class SealedOutput(
        private val sink: DataOutputStream,
        private val salt: ByteArray,
        private val prefix: ByteArray,
        private val key: SecretKeySpec
    ) : OutputStream() {
        private val buffer = ByteArray(CHUNK)
        private var size = 0
        private var count = 0
        private var closed = false

        override fun write(b: Int) {
            ensureOpen()
            buffer[size++] = b.toByte()
            if (size == CHUNK) emit(size)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            ensureOpen()
            require(off >= 0 && len >= 0 && off <= b.size - len)
            var index = off
            var remain = len
            while (remain > 0) {
                val amount = minOf(remain, CHUNK - size)
                b.copyInto(buffer, size, index, index + amount)
                size += amount
                index += amount
                remain -= amount
                if (size == CHUNK) emit(size)
            }
        }

        private fun ensureOpen() { check(!closed) { "Encrypted backup already closed" } }

        private fun emit(length: Int) {
            require(count < MAX_RECORDS) { "Encrypted backup exceeds maximum supported size" }
            val encrypted = cryptCipher(Cipher.ENCRYPT_MODE, key, salt, prefix, count, length)
                .doFinal(buffer, 0, length)
            sink.writeInt(length)
            sink.write(encrypted)
            size = 0
            count++
        }

        override fun flush() { sink.flush() }

        override fun close() {
            if (closed) return
            closed = true
            try {
                if (size != 0) emit(size)
                emit(0) // authenticated final record prevents silent truncation.
                sink.flush()
            } finally {
                buffer.fill(0)
                sink.close()
            }
        }
    }

    class VerifiedInput internal constructor(
        source: DataInputStream,
        private val salt: ByteArray,
        private val prefix: ByteArray,
        private val key: SecretKeySpec
    ) : FilterInputStream(source) {
        private val stream = source
        private var buffer = ByteArray(0)
        private var pos = 0
        private var count = 0
        private var finished = false

        private fun nextRecord(): Boolean {
            check(!finished)
            require(count < MAX_RECORDS) { "Encrypted archive exceeds chunk limit" }
            val length = try { stream.readInt() }
                catch (_: java.io.EOFException) {
                    throw java.io.IOException("Encrypted backup is truncated (missing final tag)")
                }
            require(length in 0..CHUNK) { "Corrupt encrypted backup block length" }
            val ciphertext = ByteArray(length + GCM_TAG)
            stream.readFully(ciphertext)
            val plain = try {
                cryptCipher(Cipher.DECRYPT_MODE, key, salt, prefix, count, length)
                    .doFinal(ciphertext)
            } catch (_: Exception) {
                throw java.io.IOException("Wrong backup password or damaged encrypted file")
            }
            count++
            if (length == 0) {
                require(plain.isEmpty()) { "Invalid encrypted final block" }
                require(stream.read() == -1) { "Encrypted archive has unexpected trailing data" }
                finished = true
                buffer = ByteArray(0)
                pos = 0
                return false
            }
            buffer = plain
            pos = 0
            return true
        }

        override fun read(): Int {
            if (finished) return -1
            if (pos >= buffer.size && !nextRecord()) return -1
            return buffer[pos++].toInt() and 255
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            require(off >= 0 && len >= 0 && off <= b.size - len)
            if (len == 0) return 0
            if (finished) return -1
            if (pos >= buffer.size && !nextRecord()) return -1
            val amount = minOf(len, buffer.size - pos)
            buffer.copyInto(b, off, pos, pos + amount)
            pos += amount
            return amount
        }

        fun verifyComplete() {
            val discard = ByteArray(64 * 1024)
            while (read(discard) != -1) { /* verify every record, including final tag */ }
            check(finished) { "Encrypted archive is incomplete" }
        }

        override fun close() {
            buffer.fill(0)
            super.close()
        }
    }
}
