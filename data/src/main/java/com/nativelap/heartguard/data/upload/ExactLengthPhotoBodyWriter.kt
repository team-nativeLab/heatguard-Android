package com.nativelap.heartguard.data.upload

import okio.BufferedSink
import java.io.IOException
import java.io.InputStream

internal object ExactLengthPhotoBodyWriter {
    fun write(
        inputStream: InputStream,
        sink: BufferedSink,
        expectedSizeBytes: Long,
    ) {
        val readBuffer = ByteArray(READ_BUFFER_SIZE)
        var writtenBytes = 0L

        while (true) {
            val readCount = inputStream.read(readBuffer)
            if (readCount < 0) {
                break
            }
            if (writtenBytes + readCount > expectedSizeBytes) {
                throw IOException(PHOTO_SIZE_CHANGED_MESSAGE)
            }

            sink.write(readBuffer, 0, readCount)
            writtenBytes += readCount
        }

        if (writtenBytes != expectedSizeBytes) {
            throw IOException(PHOTO_SIZE_CHANGED_MESSAGE)
        }
    }

    private const val READ_BUFFER_SIZE = 64 * 1024
    private const val PHOTO_SIZE_CHANGED_MESSAGE = "Photo size changed during upload"
}
