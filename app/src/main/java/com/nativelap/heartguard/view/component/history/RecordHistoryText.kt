package com.nativelap.heartguard.view.component.history

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.record.model.FieldRecordType
import com.nativelap.heartguard.viewmodel.history.RecordHistoryFilter
import java.time.format.DateTimeFormatter
import java.util.Locale

// 기록 내역·상세 화면이 공통으로 쓰는 기록 유형 표시 규칙이다. 앱이 모르는 유형(null)은 "기타 기록"으로 보여준다.

@Composable
@ReadOnlyComposable
fun recordTypeTitleText(recordType: FieldRecordType?): String {
    return when (recordType) {
        FieldRecordType.THERMOMETER -> stringResource(R.string.history_type_thermometer)
        FieldRecordType.WORK -> stringResource(R.string.history_type_work)
        FieldRecordType.REST -> stringResource(R.string.history_type_rest)
        null -> stringResource(R.string.history_type_unknown)
    }
}

@Composable
@ReadOnlyComposable
fun recordHistoryFilterText(filter: RecordHistoryFilter): String {
    return when (filter) {
        RecordHistoryFilter.ALL -> stringResource(R.string.history_filter_all)
        RecordHistoryFilter.THERMOMETER -> stringResource(R.string.history_filter_temperature)
        RecordHistoryFilter.WORK -> stringResource(R.string.history_filter_work_photo)
        RecordHistoryFilter.REST -> stringResource(R.string.history_filter_rest_photo)
    }
}

@DrawableRes
fun recordTypeIconRes(recordType: FieldRecordType?): Int {
    return when (recordType) {
        FieldRecordType.WORK -> R.drawable.record_work_photo
        FieldRecordType.REST -> R.drawable.record_rest_photo
        FieldRecordType.THERMOMETER,
        null,
        -> R.drawable.record_type_thermometer
    }
}

/** 앱 문자열이 한국어 하나뿐이라, 요일 이름이 기기 언어(예: 영어 "Mon")로 섞이지 않도록 한국어 로캘로 날짜를 포맷한다. */
fun koreanDateFormatter(pattern: String): DateTimeFormatter = DateTimeFormatter.ofPattern(pattern, Locale.KOREAN)
