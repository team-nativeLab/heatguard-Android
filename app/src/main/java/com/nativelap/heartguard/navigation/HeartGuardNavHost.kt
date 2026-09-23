package com.nativelap.heartguard.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.nativelap.heartguard.core.session.SessionState
import com.nativelap.heartguard.view.component.RecordType
import com.nativelap.heartguard.view.route.auth.HeartGuardLoginRoute
import com.nativelap.heartguard.view.route.auth.HeartGuardSignUpRoute
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

/** 인증 상태에 따라 인증 흐름과 메인 흐름 중 하나를 구성하는 앱 진입점이다.
 * SessionManager가 공개하는 인증 상태를 단일 진입점으로 구독해, 세션이 만료되면
 * (BearerTokenAuthenticator가 expireSession()을 호출한 경우 포함) 자동으로 로그인 화면으로 돌아간다. */
@Composable
internal fun HeartGuardNavHost(
    sessionViewModel: HeartGuardSessionViewModel = hiltViewModel(),
) {
    // TODO: androidx.lifecycle:lifecycle-runtime-compose를 새 dependency로 추가하는 것에 대한
    // 사용자 확인을 받으면 collectAsState를 collectAsStateWithLifecycle로 교체한다.
    val sessionState by sessionViewModel.sessionState.collectAsState()

    when (sessionState) {
        // 앱 시작 직후 저장된 토큰 확인이 끝나기 전까지는 어느 화면도 그리지 않는다.
        // TODO: 스플래시 화면이 추가되면 빈 화면 대신 그 화면을 보여준다.
        SessionState.Initializing -> Unit
        SessionState.Authenticated -> HeartGuardMainNavDisplay()
        SessionState.Unauthenticated -> HeartGuardAuthNavDisplay(
            onAuthenticated = sessionViewModel::onLoginSucceeded,
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
