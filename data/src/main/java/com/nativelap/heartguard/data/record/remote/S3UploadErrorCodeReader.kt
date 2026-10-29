package com.nativelap.heartguard.data.record.remote

internal object S3UploadErrorCodeReader {
    private val codePattern = Regex("""<Code>\s*([A-Za-z][A-Za-z0-9]+)\s*</Code>""")

    private val safeErrorCodes =
        mapOf(
            "AccessDenied" to "S3_ACCESS_DENIED",
            "AuthorizationHeaderMalformed" to "S3_AUTHORIZATION_HEADER_MALFORMED",
            "EntityTooLarge" to "S3_ENTITY_TOO_LARGE",
            "ExpiredToken" to "S3_EXPIRED_TOKEN",
            "InvalidAccessKeyId" to "S3_INVALID_ACCESS_KEY_ID",
            "InvalidRequest" to "S3_INVALID_REQUEST",
            "RequestExpired" to "S3_REQUEST_EXPIRED",
            "RequestTimeTooSkewed" to "S3_REQUEST_TIME_TOO_SKEWED",
            "SignatureDoesNotMatch" to "S3_SIGNATURE_DOES_NOT_MATCH",
            "XAmzContentSHA256Mismatch" to "S3_CONTENT_SHA256_MISMATCH",
        )

    fun read(errorBody: String): String? {
        if (errorBody.isBlank() || errorBody.length > MAX_ERROR_BODY_CHARACTERS) {
            return null
        }

        val code = codePattern.find(errorBody)?.groupValues?.getOrNull(CODE_CAPTURE_GROUP) ?: return null
        return safeErrorCodes[code]
    }

    private const val CODE_CAPTURE_GROUP = 1
    private const val MAX_ERROR_BODY_CHARACTERS = 16 * 1024
}
