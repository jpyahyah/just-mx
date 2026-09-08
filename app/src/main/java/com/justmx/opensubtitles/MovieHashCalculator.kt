package com.justmx.opensubtitles

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * OpenSubtitles hash (moviehash) calculator.
 * Reads the first 64KB and last 64KB of an input stream to compute the hash.
 *
 * Algorithm:
 * - size = total content length
 * - Read first 64KB and last 64KB (or fewer if file is smaller)
 * - Sum all uint64 values (little-endian) from both chunks
 * - hash = size + sum, wrapped to 64-bit
 * - Return as 16-character hex string
 */
object MovieHashCalculator {

    private const val CHUNK_SIZE = 65536 // 64KB

    /**
     * Compute hash from two input streams: one at start, one at end.
     * @param size Total file size (Content-Length)
     * @param headStream Stream for first 64KB of file
     * @param tailStream Stream for last 64KB of file
     * @return 16-char hex hash string
     */
    fun computeHash(size: Long, headStream: InputStream, tailStream: InputStream): String {
        var hash = size

        hash += processChunk(headStream)
        hash += processChunk(tailStream)

        // Wrap to unsigned 64-bit
        return String.format("%016x", hash)
    }

    /**
     * Process an InputStream reading up to 64KB of uint64 little-endian values.
     * Handles streams that are shorter than 64KB gracefully.
     */
    private fun processChunk(stream: InputStream): Long {
        val buffer = ByteArray(CHUNK_SIZE)
        var totalBytesRead = 0

        try {
            while (totalBytesRead < CHUNK_SIZE) {
                val read = stream.read(buffer, totalBytesRead, CHUNK_SIZE - totalBytesRead)
                if (read <= 0) break
                totalBytesRead += read
            }
        } catch (e: Exception) {
            // If we can't read exactly 64KB, use what we got
        }

        if (totalBytesRead == 0) return 0L

        // Ensure we only process complete uint64 chunks (multiples of 8 bytes)
        val usableBytes = (totalBytesRead / 8) * 8
        val byteBuffer = ByteBuffer.wrap(buffer, 0, usableBytes).order(ByteOrder.LITTLE_ENDIAN)

        var sum = 0L
        while (byteBuffer.remaining() >= 8) {
            sum = sum + byteBuffer.long
        }

        return sum
    }
}