package com.nativelap.heartguard.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 폭염가드에서 사용하는 모든 화면과 오버레이를 타입 안전한 Navigation 3 목적지로 정의한다. */
@Serializable
internal sealed interface HeartGuardDestination : NavKey {
    @Serializable
    data object Login : HeartGuardDestination

    @Serializable
    data object SignUp : HeartGuardDestination

    @Serializable
    data object Home : HeartGuardDestination

    @Serializable
    data object Emergency : HeartGuardDestination

    @Serializable
    data object Calling : HeartGuardDestination

    @Serializable
    data object RecordTypeSelection : HeartGuardDestination

    @Serializable
    data object TemperatureRecord : HeartGuardDestination

    @Serializable
    data object FieldPhoto : HeartGuardDestination

    @Serializable
    data object WorkPhoto : HeartGuardDestination

    @Serializable
    data object RestPhoto : HeartGuardDestination

    @Serializable
    data object SaveConfirmation : HeartGuardDestination

    @Serializable
    data object SaveSuccess : HeartGuardDestination

    @Serializable
    data object SaveFailure : HeartGuardDestination

    @Serializable
    data object WithdrawNotice : HeartGuardDestination

    @Serializable
    data object WithdrawConfirm : HeartGuardDestination

    @Serializable
    data object WithdrawDone : HeartGuardDestination
}
