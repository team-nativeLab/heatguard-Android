package com.nativelap.heartguard.data.record.repository

import android.net.Uri
import com.nativelap.heartguard.core.di.IoDispatcher
import com.nativelap.heartguard.core.network.ApiError
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
import java.io.IOException
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class RecordRepositoryImpl @Inject constructor(
    private val recordRemoteDataSource: RecordRemoteDataSource,
    private val photoFileReader: PhotoFileReader,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RecordRepository {

    /** 사진 업로드 계약: 업로드 URL 발급(파일은 보내지 않고 slot·contentType·size·sha256만) → 각 uploadUrl로 PUT →
     * 받은 objectKey를 기록 저장의 photoKeys로 쓴다. 서버는 files[i]와 같은 순서로 uploads[i]를 돌려주므로
     * 요청 순서로 사진과 키를 묶는다(업로드가 끝나는 순서와 무관). slot은 기록 안에서의 사진 위치(1·2)라
     * 이미 올린 사진을 건너뛰고 재시도해도 원래 위치 번호를 그대로 보낸다.
     * 이미 올린 사진([alreadyUploadedPhotoKeys])은 다시 올리지 않고, 올릴 때마다 [onPhotoUploaded]로 알려
     * 부분 실패 후 재시도에서 재사용할 수 있게 한다. */
    override suspend fun uploadFieldPhotos(
        photoUris: List<Uri>,
        alreadyUploadedPhotoKeys: Map<Uri, String>,
        onPhotoUploaded: (Uri, String) -> Unit,
    ): ApiResult<List<String>> {
        if (photoUris.isEmpty()) {
            return ApiResult.Success(emptyList())
        }

        val uploadedObjectKeysByUri = alreadyUploadedPhotoKeys
            .filterKeys { photoUri -> photoUri in photoUris }
            .toMutableMap()
        val pendingPhotoUris = photoUris.filterNot { photoUri -> photoUri in uploadedObjectKeysByUri }
        if (pendingPhotoUris.isEmpty()) {
            return ApiResult.Success(photoUris.mapNotNull(uploadedObjectKeysByUri::get))
        }

        val pendingPayloads = readPhotoPayloads(pendingPhotoUris)
            ?: return ApiResult.Failure(ApiError.Unknown)

        val issueResult = recordRemoteDataSource.issueUploadUrls(
            pendingPayloads.map { photoPayload ->
                photoPayload.toUploadFileRequestDto(
                    slot = photoUris.indexOf(photoPayload.photoUri) + 1,
                )
            },
        )
        val uploadSlots = when (issueResult) {
            is ApiResult.Success -> issueResult.value
            is ApiResult.Failure -> return issueResult
        }
        if (uploadSlots.size != pendingPayloads.size) {
            return ApiResult.Failure(ApiError.Unknown)
        }

        pendingPayloads.zip(uploadSlots).forEach { (photoPayload, uploadSlot) ->
            val uploadResult = recordRemoteDataSource.uploadToPresignedUrl(
                uploadUrl = uploadSlot.uploadUrl,
                requiredHeaders = uploadSlot.requiredHeaders.withDefaultContentType(photoPayload.contentType),
                photoContent = photoFileReader.createUploadBody(photoPayload),
            )
            if (uploadResult is ApiResult.Failure) {
                return uploadResult
            }
            uploadedObjectKeysByUri[photoPayload.photoUri] = uploadSlot.objectKey
            onPhotoUploaded(photoPayload.photoUri, uploadSlot.objectKey)
        }

        return ApiResult.Success(photoUris.mapNotNull(uploadedObjectKeysByUri::get))
    }

    override suspend fun submitFieldRecord(
        type: FieldRecordType,
        photoKeys: List<String>,
        measuredAt: OffsetDateTime,
        temperature: Double?,
        humidity: Double?,
        memo: String?,
    ): ApiResult<FieldRecord> {
        val request = RecordRequestDto(
            type = type.toApiValue(),
            photoKeys = photoKeys,
            temperature = temperature,
            humidity = humidity,
            memo = memo,
            // 초가 0이면 초를 생략하는 toString() 대신 초 단위로 잘라 항상 같은 ISO-8601 형식으로 보낸다.
            measuredAt = measuredAt
                .truncatedTo(ChronoUnit.SECONDS)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        )

        return recordRemoteDataSource.submitRecord(request).map { recordResponse -> recordResponse.toDomain() }
    }

    // 서버가 Content-Type을 요구 헤더로 주지 않았으면 발급 요청에 보낸 사진 형식을 붙인다(대소문자 구분 없이 확인).
    private fun Map<String, String>.withDefaultContentType(photoContentType: String): Map<String, String> {
        val hasContentType = keys.any { headerName -> headerName.equals(CONTENT_TYPE_HEADER, ignoreCase = true) }
        return if (hasContentType) {
            this
        } else {
            this + (CONTENT_TYPE_HEADER to photoContentType)
        }
    }

    // 로컬 사진 파일을 읽는다. 캐시 파일이 지워졌거나 앨범 권한이 사라진 경우 null이며, 네트워크 오류와 섞지 않는다.
    private suspend fun readPhotoPayloads(photoUris: List<Uri>): List<PhotoUploadPayload>? {
        return withContext(ioDispatcher) {
            try {
                photoUris.map { photoUri -> photoFileReader.read(photoUri) }
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            }
        }
    }

    private fun PhotoUploadPayload.toUploadFileRequestDto(slot: Int) = UploadFileRequestDto(
        slot = slot,
        contentType = contentType,
        size = sizeBytes,
        sha256 = sha256,
    )

    private companion object {
        const val CONTENT_TYPE_HEADER = "Content-Type"
    }
}
