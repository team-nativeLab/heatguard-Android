package com.nativelap.heartguard.view.route.record

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel
import com.nativelap.heartguard.viewmodel.record.RecordSubmissionEffect

/** 기록 저장 버튼에 연결할 동작을 만든다. 저장은 [RecordDraftViewModel.submit]이 viewModelScope에서 처리하고,
 * 끝나면 오는 일회성 결과를 화면이 보이는 동안만 수집해 [onSaveSuccess] 또는 [onSaveFailure]로 이동한다.
 * 여러 저장 화면이 이 함수를 쓰지만 화면에 올라온 한 곳만 결과를 받는다. 실패 후 다시 돌아와도 이전 결과로 다시 이동하지 않는다. */
@Composable
internal fun rememberRecordSubmitter(
    recordDraftViewModel: RecordDraftViewModel,
    onSaveSuccess: () -> Unit,
    onSaveFailure: () -> Unit,
): () -> Unit {
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnSaveSuccess by rememberUpdatedState(onSaveSuccess)
    val latestOnSaveFailure by rememberUpdatedState(onSaveFailure)

    LaunchedEffect(recordDraftViewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            recordDraftViewModel.submissionEffects.collect { submissionEffect ->
                when (submissionEffect) {
                    RecordSubmissionEffect.Succeeded -> latestOnSaveSuccess()
                    RecordSubmissionEffect.Failed -> latestOnSaveFailure()
                }
            }
        }
    }

    return recordDraftViewModel::submit
}
