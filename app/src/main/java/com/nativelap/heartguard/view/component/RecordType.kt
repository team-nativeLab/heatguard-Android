package com.nativelap.heartguard.view.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.nativelap.heartguard.R

/**
 * 기록 유형 선택 화면(07/08_기록유형선택)에서 사용하는 기록 유형.
 * 이전에는 "temperature"/"work"/"rest" 문자열 리터럴이 여러 파일에 중복 하드코딩되어 있었는데,
 * enum으로 정리해 오타로 인한 분기 누락을 컴파일 타임에 방지한다.
 */
enum class RecordType {
    TEMPERATURE,
    WORK,
    REST,
}

/**
 * 07/08_기록유형선택 화면과 RecordRoutes에서 공통으로 사용하는 기록 유형 옵션 목록.
 * 이전에는 동일한 옵션 리스트가 화면·Route 두 곳에 복붙되어 있었는데, 여기로 통합해 중복을 제거한다.
 */
@Composable
fun recordTypeOptions(): List<RecordTypeOptionUiModel> {
    return listOf(
        RecordTypeOptionUiModel(
            key = RecordType.TEMPERATURE,
            title = stringResource(R.string.record_temperature_title),
            description = stringResource(R.string.record_temperature_description),
            iconPainter = painterResource(R.drawable.record_temperature),
        ),
        RecordTypeOptionUiModel(
            key = RecordType.WORK,
            title = stringResource(R.string.record_work_photo_title),
            description = stringResource(R.string.record_work_photo_description),
            iconPainter = painterResource(R.drawable.record_work_photo),
        ),
        RecordTypeOptionUiModel(
            key = RecordType.REST,
            title = stringResource(R.string.record_rest_photo_title),
            description = stringResource(R.string.record_rest_photo_description),
            iconPainter = painterResource(R.drawable.record_rest_photo),
        ),
    )
}
