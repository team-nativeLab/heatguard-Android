package com.nativelap.heartguard.data.upload

internal object PresignedUploadHeaderValidator {
    fun hasMatchingContentType(
        requiredHeaders: Map<String, String>,
        expectedContentType: String,
    ): Boolean {
        val contentTypeHeaders = requiredHeaders.valuesForHeader(CONTENT_TYPE_HEADER)
        return contentTypeHeaders.size == 1 && contentTypeHeaders.single() == expectedContentType
    }

    fun hasMatchingContentLength(
        requiredHeaders: Map<String, String>,
        expectedSizeBytes: Long,
    ): Boolean {
        val contentLengthHeaders = requiredHeaders.valuesForHeader(CONTENT_LENGTH_HEADER)
        return contentLengthHeaders.size == 1 && contentLengthHeaders.single().toLongOrNull() == expectedSizeBytes
    }

    fun hasMatchingContentLengthIfPresent(
        requiredHeaders: Map<String, String>,
        expectedSizeBytes: Long,
    ): Boolean {
        val contentLengthHeaders = requiredHeaders.valuesForHeader(CONTENT_LENGTH_HEADER)
        return contentLengthHeaders.isEmpty() ||
            (contentLengthHeaders.size == 1 && contentLengthHeaders.single().toLongOrNull() == expectedSizeBytes)
    }

    fun matchesUpload(
        requiredHeaders: Map<String, String>,
        expectedContentType: String,
        expectedSizeBytes: Long,
        requiresContentLength: Boolean,
    ): Boolean {
        if (!hasMatchingContentType(requiredHeaders, expectedContentType)) {
            return false
        }

        if (requiresContentLength) {
            return hasMatchingContentLength(requiredHeaders, expectedSizeBytes)
        }

        return hasMatchingContentLengthIfPresent(requiredHeaders, expectedSizeBytes)
    }

    private fun Map<String, String>.valuesForHeader(headerName: String): List<String> =
        entries
            .filter { (actualHeaderName, _) -> actualHeaderName.equals(headerName, ignoreCase = true) }
            .map { (_, headerValue) -> headerValue }

    private const val CONTENT_TYPE_HEADER = "Content-Type"
    private const val CONTENT_LENGTH_HEADER = "Content-Length"
}
