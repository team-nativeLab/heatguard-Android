package com.nativelap.heartguard.view.route.record

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel
import com.nativelap.heartguard.viewmodel.record.RecordSubmissionState
import kotlinx.coroutines.launch

/** 기록 저장 버튼에 연결할 동작을 만든다. [RecordDraftViewModel.submit] 결과를 직접 받아
 * 성공이면 [onSaveSuccess], 실패면 [onSaveFailure]로 이동한다. 이미 저장 중이면 연타를 무시한다.
 * 상태 변화를 구독해 이동하지 않으므로, 실패 후 "다시 시도하기"로 돌아와도 이전 결과로 다시 이동하지 않는다. */
@Composable
internal fun rememberRecordSubmitter(
    recordDraftViewModel: RecordDraftViewModel,
    onSaveSuccess: () -> Unit,
    onSaveFailure: () -> Unit,
): () -> Unit {
    val coroutineScope = rememberCoroutineScope()
    val submissionState by recordDraftViewModel.submissionState.collectAsStateWithLifecycle()
    val latestOnSaveSuccess by rememberUpdatedState(onSaveSuccess)
    val latestOnSaveFailure by rememberUpdatedState(onSaveFailure)

    return {
        if (submissionState !is RecordSubmissionState.Submitting) {
            coroutineScope.launch {
                val submitResult = recordDraftViewModel.submit()
                if (submitResult is ApiResult.Success) {
                    latestOnSaveSuccess()
                } else {
                    latestOnSaveFailure()
                }
            }
        }
    }
}
