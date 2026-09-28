package com.nativelap.heartguard.viewmodel.record

/** 기록 저장 요청이 끝났음을 저장 화면에 한 번만 알린다. 화면은 이 결과로 성공·실패 화면으로 이동한다. */
sealed interface RecordSubmissionEffect {
    data object Succeeded : RecordSubmissionEffect

    data object Failed : RecordSubmissionEffect
}
