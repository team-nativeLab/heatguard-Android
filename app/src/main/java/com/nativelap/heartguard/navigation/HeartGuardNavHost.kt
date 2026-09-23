package com.nativelap.heartguard.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.nativelap.heartguard.view.component.RecordType
import com.nativelap.heartguard.view.route.auth.HeartGuardLoginRoute
import com.nativelap.heartguard.view.route.emergency.HeartGuardCallingRoute
import com.nativelap.heartguard.view.route.emergency.HeartGuardEmergencyRoute
import com.nativelap.heartguard.view.route.feedback.HeartGuardSaveFailureRoute
import com.nativelap.heartguard.view.route.feedback.HeartGuardSaveConfirmationRoute
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
    HeartGuardAuthNavDisplay()
}

/** 팀 로그인 계약이 연결될 때까지 인증 화면만 제공한다. */
@Composable
private fun HeartGuardAuthNavDisplay() {
    val backStack = rememberNavBackStack(HeartGuardDestination.Login)

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        transitionSpec = {
            ContentTransform(
                targetContentEnter = EnterTransition.None,
                initialContentExit = ExitTransition.None,
                sizeTransform = null,
            )
        },
        popTransitionSpec = {
            ContentTransform(
                targetContentEnter = EnterTransition.None,
                initialContentExit = ExitTransition.None,
                sizeTransform = null,
            )
        },
        predictivePopTransitionSpec = {
            ContentTransform(
                targetContentEnter = EnterTransition.None,
                initialContentExit = ExitTransition.None,
                sizeTransform = null,
            )
        },
        entryProvider = entryProvider {
            entry<HeartGuardDestination.Login> {
                HeartGuardLoginRoute()
            }
        },
    )
}

/** 로그인 이후의 홈·기록·긴급 호출 흐름을 단일 Navigation 3 back stack으로 연결한다. */
@Composable
private fun HeartGuardMainNavDisplay() {
    val backStack = rememberNavBackStack(HeartGuardDestination.Home)

    // TODO: ViewModel·Repository 연동 전까지 저장 성공/실패를 구분할 실제 로직이 없다.
    // 실패 화면(SaveFailure)이 실제로 도달 가능함을 보장하기 위해, 매 저장 시도마다
    // 성공/실패를 번갈아 시뮬레이션하는 임시 상태다. 서버 연동 이슈에서 실제 결과값으로 교체해야 한다.
    var isNextSaveAttemptSuccessful by rememberSaveable {
        mutableStateOf(true)
    }

    fun goToSaveResult() {
        val resultDestination = if (isNextSaveAttemptSuccessful) {
            HeartGuardDestination.SaveSuccess
        } else {
            HeartGuardDestination.SaveFailure
        }
        isNextSaveAttemptSuccessful = !isNextSaveAttemptSuccessful
        backStack.add(resultDestination)
    }

    fun goToSaveConfirmation() {
        backStack.add(HeartGuardDestination.SaveConfirmation)
    }

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
        HeartGuardBottomSheetSceneStrategy(),
        SinglePaneSceneStrategy(),
    )

    NavDisplay(
        backStack = backStack,
        onBack = ::goBack,
        sceneStrategies = sceneStrategies,
        transitionSpec = {
            ContentTransform(
                targetContentEnter = EnterTransition.None,
                initialContentExit = ExitTransition.None,
                sizeTransform = null,
            )
        },
        popTransitionSpec = {
            ContentTransform(
                targetContentEnter = EnterTransition.None,
                initialContentExit = ExitTransition.None,
                sizeTransform = null,
            )
        },
        predictivePopTransitionSpec = {
            ContentTransform(
                targetContentEnter = EnterTransition.None,
                initialContentExit = ExitTransition.None,
                sizeTransform = null,
            )
        },
        entryProvider = entryProvider {
            entry<HeartGuardDestination.Home> {
                HeartGuardHomeRoute(
                    onManagerCallClick = {
                        backStack.add(HeartGuardDestination.Emergency)
                    },
                    onEmergencyClick = {
                        backStack.add(HeartGuardDestination.Emergency)
                    },
                    onFieldPhotoClick = {
                        backStack.add(HeartGuardDestination.FieldPhoto)
                    },
                    onRecordHistoryClick = {},
                    onRecordClick = {
                        backStack.add(HeartGuardDestination.RecordTypeSelection)
                    },
                )
            }
            entry<HeartGuardDestination.Emergency> {
                HeartGuardEmergencyRoute(
                    // Figma 03_긴급상황 화면은 이미 관리자 호출 알림이 진행 중인 상태를 전제로 하며,
                    // "호출 취소" 버튼은 이 알림을 취소하고 이전 화면(Home)으로 돌아가는 동작이다.
                    // (04_호출중 화면으로 진행하는 것이 아니다 — 그 화면은 Emergency 진입 전 별도 트리거로 도달한다.)
                    onCallClick = {
                        backStack.add(HeartGuardDestination.Calling)
                    },
                    onCancelClick = ::goHome,
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
                        // RecordType이 enum이라 when이 모든 분기를 강제하므로 else/null 분기가 필요 없다.
                        val nextDestination = when (recordType) {
                            RecordType.TEMPERATURE -> HeartGuardDestination.TemperatureRecord
                            RecordType.WORK -> HeartGuardDestination.WorkPhoto
                            RecordType.REST -> HeartGuardDestination.RestPhoto
                        }

                        backStack.removeLastOrNull()
                        backStack.add(nextDestination)
                    },
                )
            }
            entry<HeartGuardDestination.TemperatureRecord> {
                HeartGuardTemperatureRecordRoute(
                    onFieldPhotoClick = {
                        backStack.add(HeartGuardDestination.FieldPhoto)
                    },
                    onSaveClick = ::goToSaveConfirmation,
                )
            }
            entry<HeartGuardDestination.FieldPhoto> {
                HeartGuardFieldPhotoRoute(
                    onSaveClick = ::goToSaveConfirmation,
                )
            }
            entry<HeartGuardDestination.WorkPhoto> {
                HeartGuardWorkPhotoRoute(
                    onUploadClick = ::goToSaveConfirmation,
                )
            }
            entry<HeartGuardDestination.RestPhoto> {
                HeartGuardRestPhotoRoute(
                    onUploadClick = ::goToSaveConfirmation,
                )
            }
            entry<HeartGuardDestination.SaveConfirmation> {
                HeartGuardSaveConfirmationRoute(
                    onCaptureClick = ::goBack,
                    onSaveClick = ::goToSaveResult,
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
                    onSaveDraftAndExitClick = ::goHome,
                )
            }
        },
    )
}
