package com.nativelap.heartguard.data.record.remote

internal data class ExternalUploadResult(
    val statusCode: Int,
    val errorCode: String? = null,
)
