package com.nativelap.heartguard.viewmodel.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.clearStateWhenSessionEnds
import com.nativelap.heartguard.domain.record.usecase.GetRecordDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 기록 상세 화면의 기록 한 건 조회를 담당한다. 사진 URL은 짧게 유효한 서명 URL이라 화면에 들어올 때마다 새로 받는다. */
@HiltViewModel
class RecordDetailViewModel @Inject constructor(
    private val getRecordDetailUseCase: GetRecordDetailUseCase,
    sessionManager: SessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow<RecordDetailUiState>(RecordDetailUiState.Loading)
    val uiState: StateFlow<RecordDetailUiState> = _uiState.asStateFlow()

    private var loadDetailJob: Job? = null

    init {
        clearStateWhenSessionEnds(sessionManager) {
            loadDetailJob?.cancel()
            loadDetailJob = null
            _uiState.value = RecordDetailUiState.Loading
        }
    }

    /** [recordId] 기록을 조회한다. 다른 기록의 이전 결과가 잠깐이라도 보이지 않도록 먼저 Loading으로 바꾼다. */
    fun loadRecordDetail(recordId: String) {
        loadDetailJob?.cancel()
        _uiState.value = RecordDetailUiState.Loading
        loadDetailJob = viewModelScope.launch {
            _uiState.value = when (val detailResult = getRecordDetailUseCase(recordId)) {
                is ApiResult.Success -> RecordDetailUiState.Loaded(detailResult.value)
                is ApiResult.Failure -> RecordDetailUiState.Failed
            }
        }
    }
}
