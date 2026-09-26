package com.nativelap.heartguard.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.nativelap.heartguard.view.route.photo.HeartGuardPhotoCameraRoute
import com.nativelap.heartguard.view.route.photo.HeartGuardFieldPhotoRoute
import com.nativelap.heartguard.view.route.photo.HeartGuardRestPhotoRoute
import com.nativelap.heartguard.view.route.photo.HeartGuardWorkPhotoRoute
import com.nativelap.heartguard.view.route.record.HeartGuardRecordTypeSelectionRoute
import com.nativelap.heartguard.view.route.record.HeartGuardTemperatureRecordRoute
import com.nativelap.heartguard.viewmodel.emergency.EmergencyViewModel
import com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel

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

    // 기록유형선택→온도기록/사진촬영→저장전확인까지 여러 NavKey가 RecordDraftViewModel 하나를
    // 공유해야 한다(android-navigation SKILL '화면 간 ViewModel 공유' 참고). android-navigation
    // SKILL이 예시로 든 "수동 ViewModelStoreOwner"는 이 프로젝트가 쓰는 androidx.hilt-navigation-compose
    // 1.2.0에서 실제로 동작하지 않는다 — createHiltViewModelFactory()는 대상 owner가
    // NavBackStackEntry일 때만 Hilt 팩토리를 만들고, 그 외의 일반 ViewModelStoreOwner는
    // HasDefaultViewModelProviderFactory를 구현하지 않는 한 NewInstanceFactory(no-arg 리플렉션)로
    // 폴백해 생성자 의존성이 있는 ViewModel 생성 시 크래시한다. 대신 인자 없는 hiltViewModel()을 써서
    // 컴포지션 상위의 기본 ViewModelStoreOwner(Activity, @AndroidEntryPoint 필요)를 그대로 따르고,
    // 흐름별 초기화는 ViewModelStore를 새로 만드는 대신 Route가 명시적으로 호출하는
    // recordDraftViewModel.reset()으로 대체한다.
    val recordDraftViewModel: RecordDraftViewModel = hiltViewModel()

    // recordDraftViewModel은 이제 Activity 스코프라 화면 흐름을 벗어나는 것만으로는 정리되지 않는다.
    // 로그아웃·세션 만료로 이 Composable 자체가 컴포지션에서 사라질 때도(기록 도중이었더라도) 임시
    // 사진 파일이 남지 않도록 여기서 한 번 더 reset()을 보장한다.
    DisposableEffect(Unit) {
        onDispose { recordDraftViewModel.reset() }
    }

    // Emergency·Calling 화면이 공유하는 EmergencyViewModel도 같은 이유로 수동 ViewModelStoreOwner
    // 없이 인자 없는 hiltViewModel()을 쓴다. 흐름 종료 시 정리는 emergencyViewModel.reset()으로 한다.
    val emergencyViewModel: EmergencyViewModel = hiltViewModel()

    // emergencyViewModel도 같은 이유로, 로그아웃·세션 만료 시 3초 폴링이 남지 않도록 정리한다.
    DisposableEffect(Unit) {
        onDispose { emergencyViewModel.reset() }
    }

    fun goToSaveConfirmation() {
        backStack.add(HeartGuardDestination.SaveConfirmation)
    }

    fun goBack() {
        if (backStack.size > 1) {
            val poppedDestination = backStack.removeLastOrNull()
            // 화면 안의 "취소"/"종료" 버튼 콜백뿐 아니라 시스템/제스처 뒤로가기로 Emergency·Calling을
            // 벗어날 때도 폴링을 멈춰야 한다 — onBack은 이 함수 하나로 모아져 있어 여기서만 처리하면 된다.
            if (poppedDestination is HeartGuardDestination.Emergency ||
                poppedDestination is HeartGuardDestination.Calling
            ) {
                emergencyViewModel.reset()
            }
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
                    // 관리자 전화는 HeartGuardHomeRoute 내부에서 바로 다이얼러로 연결하므로
                    // 여기서는 긴급호출 흐름으로 이동하는 콜백만 전달한다.
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
                    emergencyViewModel = emergencyViewModel,
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
                    emergencyViewModel = emergencyViewModel,
                    onCancelClick = ::goBack,
                    onEndClick = ::goHome,
                )
            }
            entry<HeartGuardDestination.RecordTypeSelection>(
                metadata = HeartGuardBottomSheetSceneStrategy.bottomSheet(),
            ) {
                HeartGuardRecordTypeSelectionRoute(
                    recordDraftViewModel = recordDraftViewModel,
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
                    recordDraftViewModel = recordDraftViewModel,
                    onFieldPhotoClick = {
                        backStack.add(HeartGuardDestination.FieldPhoto)
                    },
                    onSaveClick = ::goToSaveConfirmation,
                )
            }
            entry<HeartGuardDestination.FieldPhoto> {
                HeartGuardFieldPhotoRoute(
                    recordDraftViewModel = recordDraftViewModel,
                    onSaveClick = ::goToSaveConfirmation,
                    onCameraClick = { recordType ->
                        backStack.add(HeartGuardDestination.PhotoCamera(recordType))
                    },
                )
            }
            entry<HeartGuardDestination.WorkPhoto> {
                HeartGuardWorkPhotoRoute(
                    recordDraftViewModel = recordDraftViewModel,
                    onUploadClick = ::goToSaveConfirmation,
                    onCameraClick = { recordType ->
                        backStack.add(HeartGuardDestination.PhotoCamera(recordType))
                    },
                )
            }
            entry<HeartGuardDestination.RestPhoto> {
                HeartGuardRestPhotoRoute(
                    recordDraftViewModel = recordDraftViewModel,
                    onUploadClick = ::goToSaveConfirmation,
                    onCameraClick = { recordType ->
                        backStack.add(HeartGuardDestination.PhotoCamera(recordType))
                    },
                )
            }
            entry<HeartGuardDestination.PhotoCamera> { key ->
                HeartGuardPhotoCameraRoute(
                    recordDraftViewModel = recordDraftViewModel,
                    recordType = key.recordType,
                    onBackClick = ::goBack,
                )
            }
            entry<HeartGuardDestination.SaveConfirmation> {
                HeartGuardSaveConfirmationRoute(
                    recordDraftViewModel = recordDraftViewModel,
                    onCaptureClick = ::goBack,
                    onSaveSuccess = {
                        backStack.add(HeartGuardDestination.SaveSuccess)
                    },
                    onSaveFailure = {
                        backStack.add(HeartGuardDestination.SaveFailure)
                    },
                )
            }
            entry<HeartGuardDestination.SaveSuccess> {
                HeartGuardSaveSuccessRoute(
                    recordDraftViewModel = recordDraftViewModel,
                    onCompleteClick = ::goHome,
                )
            }
            entry<HeartGuardDestination.SaveFailure> {
                HeartGuardSaveFailureRoute(
                    recordDraftViewModel = recordDraftViewModel,
                    onRetryClick = ::goBack,
                    // "임시저장 후 나가기"는 draft를 보존한 채 홈으로 돌아가는 동작을 의도하므로,
                    // SaveSuccess와 달리 여기서는 recordDraftViewModel.reset()을 호출하지 않는다.
                    onSaveDraftAndExitClick = ::goHome,
                )
            }
        },
    )
}
