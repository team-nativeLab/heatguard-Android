package com.nativelap.heartguard.data.record.repository

import android.net.Uri
import com.nativelap.heartguard.core.di.IoDispatcher
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.data.record.PhotoFileReader
import com.nativelap.heartguard.data.record.PhotoUploadPayload
import com.nativelap.heartguard.data.record.dto.RecordRequestDto
import com.nativelap.heartguard.data.record.dto.UploadFileRequestDto
import com.nativelap.heartguard.data.record.mapper.toApiValue
import com.nativelap.heartguard.data.record.mapper.toDomain
import com.nativelap.heartguard.data.record.remote.RecordRemoteDataSource
import com.nativelap.heartguard.domain.record.model.FieldRecord
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.domain.record.repository.RecordRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class RecordRepositoryImpl @Inject constructor(
    private val recordRemoteDataSource: RecordRemoteDataSource,
    private val photoFileReader: PhotoFileReader,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RecordRepository {

    override suspend fun uploadFieldPhotos(photoUris: List<Uri>): ApiResult<List<String>> {
        if (photoUris.isEmpty()) {
            return ApiResult.Success(emptyList())
        }

        val payloads = withContext(ioDispatcher) {
            photoUris.map { uri -> photoFileReader.read(uri) }
        }

        val issueResult = recordRemoteDataSource.issueUploadUrls(
            payloads.mapIndexed { index, payload -> payload.toUploadFileRequestDto(slot = index + 1) },
        )
        val uploadSlots = when (issueResult) {
            is ApiResult.Success -> issueResult.value
            is ApiResult.Failure -> return issueResult
        }

        // 응답 예시(uploads[])에 slot을 되돌려주는 필드가 없어, 요청 files[]와 같은 순서로 대응한다고
        // 가정한다 — 문서에 명시적으로 보장된 내용은 아니므로 실제 연동 시 재확인이 필요하다.
        uploadSlots.forEachIndexed { index, slot ->
            val payload = payloads.getOrNull(index) ?: return@forEachIndexed
            val uploadResult = recordRemoteDataSource.uploadToPresignedUrl(
                uploadUrl = slot.uploadUrl,
                requiredHeaders = slot.requiredHeaders,
                contentType = payload.contentType,
                bytes = payload.bytes,
            )
            if (uploadResult is ApiResult.Failure) {
                return uploadResult
            }
        }

        return ApiResult.Success(uploadSlots.map { slot -> slot.objectKey })
    }

    override suspend fun submitFieldRecord(
        type: FieldRecordType,
        photoKeys: List<String>,
        temperature: Double?,
        humidity: Double?,
        noThermometer: Boolean,
        memo: String?,
    ): ApiResult<FieldRecord> {
        val request = RecordRequestDto(
            type = type.toApiValue(),
            photoKeys = photoKeys,
            temperature = temperature,
            humidity = humidity,
            noThermometer = noThermometer,
            memo = memo,
            measuredAt = Instant.now().toString(),
        )

        return when (val result = recordRemoteDataSource.submitRecord(request)) {
            is ApiResult.Success -> ApiResult.Success(result.value.toDomain())
            is ApiResult.Failure -> result
        }
    }

    private fun PhotoUploadPayload.toUploadFileRequestDto(slot: Int) = UploadFileRequestDto(
        slot = slot,
        contentType = contentType,
        size = sizeBytes,
        sha256 = sha256,
    )
}
