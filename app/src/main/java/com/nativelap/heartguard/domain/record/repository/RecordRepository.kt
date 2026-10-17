package com.nativelap.heartguard.domain.record.repository

import android.net.Uri
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.record.model.FieldRecord
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import java.time.OffsetDateTime

interface RecordRepository {
    /** 사진을 presigned URL 발급 → 실제 업로드 순서로 처리하고, 기록 등록에 쓸 objectKey 목록을 돌려준다. */
    suspend fun uploadFieldPhotos(
        photoUris: List<Uri>,
        alreadyUploadedPhotoKeys: Map<Uri, String> = emptyMap(),
        onPhotoUploaded: (Uri, String) -> Unit = { _, _ -> },
    ): ApiResult<List<String>>

    suspend fun submitFieldRecord(
        expectedSessionGeneration: Long,
        type: FieldRecordType,
        photoKeys: List<String>,
        measuredAt: OffsetDateTime,
        temperature: Double?,
        humidity: Double?,
        memo: String?,
        restStartedAt: OffsetDateTime?,
        restEndedAt: OffsetDateTime?,
    ): ApiResult<FieldRecord>
}
