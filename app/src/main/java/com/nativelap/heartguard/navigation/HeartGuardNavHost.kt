package com.nativelap.heartguard.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.nativelap.heartguard.view.route.auth.HeartGuardLoginRoute
import com.nativelap.heartguard.view.route.auth.HeartGuardSignUpRoute
import com.nativelap.heartguard.view.route.emergency.HeartGuardCallingRoute
import com.nativelap.heartguard.view.route.emergency.HeartGuardEmergencyRoute
import com.nativelap.heartguard.view.route.feedback.HeartGuardSaveConfirmationRoute
import com.nativelap.heartguard.view.route.feedback.HeartGuardSaveFailureRoute
import com.nativelap.heartguard.view.route.feedback.HeartGuardSaveSuccessRoute
import com.nativelap.heartguard.view.route.home.HeartGuardHomeRoute
import com.nativelap.heartguard.view.route.photo.HeartGuardFieldPhotoRoute
import com.nativelap.heartguard.view.route.photo.HeartGuardRestPhotoRoute
import com.nativelap.heartguard.view.route.photo.HeartGuardWorkPhotoRoute
import com.nativelap.heartguard.view.route.record.HeartGuardRecordTypeSelectionRoute
import com.nativelap.heartguard.view.route.record.HeartGuardTemperatureRecordRoute

/** 인증 상태에 따라 인증 흐름과 메인 흐름 중 하나를 구성하는 앱 진입점이다. */
@Composable
internal fun HeartGuardNavHost() {
    var isAuthenticated by rememberSaveable {
        mutableStateOf(false)
    }

    if (isAuthenticated) {
        HeartGuardMainNavDisplay()
    } else {
        HeartGuardAuthNavDisplay(
            onAuthenticated = { isAuthenticated = true },
        )
    }
}

/** 서버 인증이 연결되기 전까지 로그인·회원가입 화면 전환을 담당하는 임시 인증 흐름이다. */
@Composable
private fun HeartGuardAuthNavDisplay(
    onAuthenticated: () -> Unit,
) {
    val backStack = rememberNavBackStack(HeartGuardDestination.Login)

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        entryProvider = entryProvider {
            entry<HeartGuardDestination.Login> {
                HeartGuardLoginRoute(
                    onLoginClick = onAuthenticated,
                    onSignUpClick = {
                        backStack.add(HeartGuardDestination.SignUp)
                    },
                )
            }
            entry<HeartGuardDestination.SignUp> {
                HeartGuardSignUpRoute(
                    onSignUpClick = {
                        backStack.removeLastOrNull()
                    },
                    onLoginClick = {
                        backStack.removeLastOrNull()
                    },
                )
            }
        },
    )
}

/** 로그인 이후의 홈·기록·긴급 호출 흐름을 단일 Navigation 3 back stack으로 연결한다. */
@Composable
private fun HeartGuardMainNavDisplay() {
    val backStack = rememberNavBackStack(HeartGuardDestination.Home)

    fun goBack() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    fun goHome() {
        while (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    val sceneStrategies: List<SceneStrategy<NavKey>> = listOf(
        DialogSceneStrategy(),
        HeartGuardBottomSheetSceneStrategy(),
        SinglePaneSceneStrategy(),
    )

    NavDisplay(
        backStack = backStack,
        onBack = ::goBack,
        sceneStrategies = sceneStrategies,
        entryProvider = entryProvider {
            entry<HeartGuardDestination.Home> {
                HeartGuardHomeRoute(
                    onEmergencyClick = {
                        backStack.add(HeartGuardDestination.Emergency)
                    },
                    onTemperatureRecordClick = {
                        backStack.add(HeartGuardDestination.RecordTypeSelection)
                    },
                    onWorkPhotoClick = {
                        backStack.add(HeartGuardDestination.WorkPhoto)
                    },
                    onRestPhotoClick = {
                        backStack.add(HeartGuardDestination.RestPhoto)
                    },
                )
            }
            entry<HeartGuardDestination.Emergency> {
                HeartGuardEmergencyRoute(
                    onCallClick = {
                        backStack.add(HeartGuardDestination.Calling)
                    },
                )
            }
            entry<HeartGuardDestination.Calling> {
                HeartGuardCallingRoute(
                    onCancelClick = ::goBack,
                    onEndClick = ::goHome,
                )
            }
            entry<HeartGuardDestination.RecordTypeSelection>(
                metadata = HeartGuardBottomSheetSceneStrategy.bottomSheet(),
            ) {
                HeartGuardRecordTypeSelectionRoute(
                    onConfirm = { recordType ->
                        val nextDestination = when (recordType) {
                            "temperature" -> HeartGuardDestination.TemperatureRecord
                            "work" -> HeartGuardDestination.WorkPhoto
                            "rest" -> HeartGuardDestination.RestPhoto
                            else -> null
                        }

                        if (nextDestination != null) {
                            backStack.removeLastOrNull()
                            backStack.add(nextDestination)
                        }
                    },
                )
            }
            entry<HeartGuardDestination.TemperatureRecord> {
                HeartGuardTemperatureRecordRoute(
                    onSaveClick = {
                        backStack.add(HeartGuardDestination.SaveConfirmation)
                    },
                )
            }
            entry<HeartGuardDestination.FieldPhoto> {
                HeartGuardFieldPhotoRoute()
            }
            entry<HeartGuardDestination.WorkPhoto> {
                HeartGuardWorkPhotoRoute(
                    onUploadClick = {
                        backStack.add(HeartGuardDestination.SaveConfirmation)
                    },
                )
            }
            entry<HeartGuardDestination.RestPhoto> {
                HeartGuardRestPhotoRoute(
                    onUploadClick = {
                        backStack.add(HeartGuardDestination.SaveConfirmation)
                    },
                )
            }
            entry<HeartGuardDestination.SaveConfirmation>(
                metadata = DialogSceneStrategy.dialog(),
            ) {
                HeartGuardSaveConfirmationRoute(
                    onDismissClick = ::goBack,
                    onConfirmClick = {
                        backStack.removeLastOrNull()
                        backStack.add(HeartGuardDestination.SaveSuccess)
                    },
                )
            }
            entry<HeartGuardDestination.SaveSuccess> {
                HeartGuardSaveSuccessRoute(
                    onCompleteClick = ::goHome,
                )
            }
            entry<HeartGuardDestination.SaveFailure> {
                HeartGuardSaveFailureRoute(
                    onRetryClick = ::goBack,
                )
            }
        },
    )
}
