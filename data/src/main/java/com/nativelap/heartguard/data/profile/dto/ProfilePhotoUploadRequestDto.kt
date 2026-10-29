package com.nativelap.heartguard.data.profile.dto

import com.nativelap.heartguard.data.upload.PresignedUploadHeaderValidator
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfilePhotoUploadRequestDto(
    @SerialName("contentType")
    val contentType: String,
    @SerialName("size")
    val size: Long,
)

@Serializable
data class ProfilePhotoUploadResponseDto(
    @SerialName("uploadId")
    val uploadId: String,
    @SerialName("uploadUrl")
    val uploadUrl: String,
    @SerialName("expiresAt")
    val expiresAt: String,
    @SerialName("requiredHeaders")
    val requiredHeaders: Map<String, String> = emptyMap(),
)

@Serializable
data class ProfilePhotoConfirmRequestDto(
    @SerialName("uploadId")
    val uploadId: String,
)

@Serializable
data class ProfilePhotoResponseDto(
    @SerialName("photoUrl")
    val photoUrl: String? = null,
    @SerialName("expiresAt")
    val expiresAt: String? = null,
)

/** Profile upload URLs must carry the same byte length that the streaming request body will send. */
internal fun ProfilePhotoUploadResponseDto.hasMatchingContentLength(sizeBytes: Long): Boolean =
    PresignedUploadHeaderValidator.hasMatchingContentLength(requiredHeaders, sizeBytes)
