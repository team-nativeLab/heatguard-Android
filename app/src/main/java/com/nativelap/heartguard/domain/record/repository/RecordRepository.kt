package com.nativelap.heartguard.domain.record.repository

import android.net.Uri
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.record.model.FieldRecord
import com.nativelap.heartguard.domain.record.model.FieldRecordType

interface RecordRepository {

    /** 사진을 presigned URL 발급 → 실제 업로드 순서로 처리하고, 기록 등록에 쓸 objectKey 목록을 돌려준다.
     * [photoUris]가 비어 있으면(예: 온도계만 기록) 빈 목록을 그대로 성공으로 반환한다. */
    suspend fun uploadFieldPhotos(photoUris: List<Uri>): ApiResult<List<String>>

    suspend fun submitFieldRecord(
        type: FieldRecordType,
        photoKeys: List<String>,
        temperature: Double?,
        humidity: Double?,
        noThermometer: Boolean,
        memo: String?,
    ): ApiResult<FieldRecord>
}
