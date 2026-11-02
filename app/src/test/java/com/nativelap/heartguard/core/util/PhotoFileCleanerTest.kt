package com.nativelap.heartguard.core.util

import java.io.File
import java.nio.file.Files
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhotoFileCleanerTest {
    private val validFileName = "$PHOTO_CAPTURE_FILE_PREFIX${UUID.randomUUID()}.jpg"

    @Test
    fun `accepts file name in the format created by photo capture target`() {
        assertTrue(isCaptureFileName(validFileName))
    }

    @Test
    fun `rejects file name containing parent traversal`() {
        assertFalse(isCaptureFileName("$PHOTO_CAPTURE_FILE_PREFIX../../databases/app.db"))
    }

    @Test
    fun `rejects file name containing a path separator after the uuid`() {
        assertFalse(isCaptureFileName("$PHOTO_CAPTURE_FILE_PREFIX${UUID.randomUUID()}.jpg/../../x"))
    }

    @Test
    fun `rejects uppercase uuid`() {
        assertFalse(isCaptureFileName("$PHOTO_CAPTURE_FILE_PREFIX${UUID.randomUUID().toString().uppercase()}.jpg"))
    }

    @Test
    fun `rejects other extension`() {
        assertFalse(isCaptureFileName("$PHOTO_CAPTURE_FILE_PREFIX${UUID.randomUUID()}.png"))
    }

    @Test
    fun `rejects other prefix`() {
        assertFalse(isCaptureFileName("other_capture_${UUID.randomUUID()}.jpg"))
    }

    @Test
    fun `resolves file name from photo-captures root with exactly one segment`() {
        assertEquals(validFileName, captureFileNameFrom(listOf("photo-captures", validFileName)))
    }

    @Test
    fun `rejects segments with another root`() {
        assertNull(captureFileNameFrom(listOf("other-root", validFileName)))
    }

    @Test
    fun `rejects segments with extra depth`() {
        assertNull(captureFileNameFrom(listOf("photo-captures", "nested", validFileName)))
    }

    @Test
    fun `rejects segments without a file name`() {
        assertNull(captureFileNameFrom(listOf("photo-captures")))
        assertNull(captureFileNameFrom(emptyList()))
    }

    @Test
    fun `rejects a decoded single segment that contains a path separator`() {
        assertNull(captureFileNameFrom(listOf("photo-captures", "$PHOTO_CAPTURE_FILE_PREFIX../../x.jpg")))
    }

    @Test
    fun `accepts a file directly inside the photo directory`() {
        withTemporaryDirectory { photoDirectory ->
            val captureFile = File(photoDirectory, validFileName)

            assertTrue(isDirectChildOf(photoDirectory, captureFile))
        }
    }

    @Test
    fun `rejects a parent traversal path outside the photo directory`() {
        withTemporaryDirectory { photoDirectory ->
            val escapingFile = File(photoDirectory, "../$validFileName")

            assertFalse(isDirectChildOf(photoDirectory, escapingFile))
        }
    }

    @Test
    fun `rejects a symbolic link that points outside the photo directory`() {
        withTemporaryDirectory { root ->
            val photoDirectory = File(root, "photos").apply { mkdirs() }
            val outsideFile = File(root, "outside.jpg").apply { writeText("capture") }
            val linkFile = File(photoDirectory, validFileName)
            Files.createSymbolicLink(linkFile.toPath(), outsideFile.toPath())

            assertFalse(isDirectChildOf(photoDirectory, linkFile))
        }
    }

    private fun withTemporaryDirectory(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("photo-cleaner-test").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
