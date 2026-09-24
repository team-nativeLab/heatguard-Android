package com.nativelap.heartguard.domain.record.usecase

import android.net.Uri
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.record.repository.RecordRepository
import javax.inject.Inject

/** 사진 업로드 URL 발급과 실제 업로드를 한 번에 묶는다. ViewModel은 이 순서를 직접 조립하지 않는다. */
class UploadFieldPhotosUseCase @Inject constructor(
    private val recordRepository: RecordRepository,
) {
    suspend operator fun invoke(photoUris: List<Uri>): ApiResult<List<String>> =
        recordRepository.uploadFieldPhotos(photoUris)
}
