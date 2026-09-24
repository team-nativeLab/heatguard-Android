package com.nativelap.heartguard.data.record.repository

import android.net.Uri
import com.nativelap.heartguard.core.di.IoDispatcher
import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.map
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
    private val apiExecutor: ApiExecutor,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RecordRepository {

    override suspend fun uploadFieldPhotos(photoUris: List<Uri>): ApiResult<List<String>> {
        if (photoUris.isEmpty()) {
            return ApiResult.Success(emptyList())
        }

        // photoFileReader.read()는 파일을 못 읽으면(캐시 파일이 이미 삭제된 경우 등) 예외를 던진다.
        // ApiExecutor로 감싸 그 예외도 다른 네트워크 실패와 동일하게 ApiResult.Failure로 변환되게 한다.
        val readResult = apiExecutor.execute {
            withContext(ioDispatcher) {
                photoUris.map { uri -> photoFileReader.read(uri) }
            }
        }
        val payloads = when (readResult) {
            is ApiResult.Success -> readResult.value
            is ApiResult.Failure -> return readResult
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
        // 이 가정을 벗어나 uploadSlots가 payloads보다 많이 오는 예외적인 응답이라도, 실제로 업로드를
        // 마친 slot의 objectKey만 기록 등록에 넘긴다(업로드하지 않은 objectKey가 섞여 들어가지 않도록).
        val uploadedObjectKeys = mutableListOf<String>()
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
            uploadedObjectKeys += slot.objectKey
        }

        return ApiResult.Success(uploadedObjectKeys)
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

        return recordRemoteDataSource.submitRecord(request).map { it.toDomain() }
    }

    private fun PhotoUploadPayload.toUploadFileRequestDto(slot: Int) = UploadFileRequestDto(
        slot = slot,
        contentType = contentType,
        size = sizeBytes,
        sha256 = sha256,
    )
}
