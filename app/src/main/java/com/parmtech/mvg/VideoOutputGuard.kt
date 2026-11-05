package com.parmtech.mvg

import java.io.File

/** Small structural signature check before claiming provider output is a playable MP4. */
internal object VideoOutputGuard {
    fun isMp4Header(header: ByteArray): Boolean {
        if (header.size < 12) return false
        val boxSize = ((header[0].toLong() and 255) shl 24) or
            ((header[1].toLong() and 255) shl 16) or
            ((header[2].toLong() and 255) shl 8) or
            (header[3].toLong() and 255)
        val marker = String(header, 4, 4, Charsets.US_ASCII)
        return boxSize >= 8 && (marker == "ftyp" || marker == "styp")
    }

    fun isMp4File(file: File): Boolean =
        file.isFile && file.length() > 100L && file.inputStream().use { stream ->
            val head = ByteArray(12)
            var count = 0
            while (count < head.size) {
                val n = stream.read(head, count, head.size - count)
                if (n < 0) break
                count += n
            }
            count >= head.size && isMp4Header(head)
        }
}
