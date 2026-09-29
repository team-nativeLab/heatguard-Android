package com.nativelap.heartguard.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.nativelap.heartguard.core.session.SessionState
import com.nativelap.heartguard.view.component.RecordType
import com.nativelap.heartguard.view.route.account.HeartGuardWithdrawConfirmRoute
import com.nativelap.heartguard.view.route.account.HeartGuardWithdrawDoneRoute
import com.nativelap.heartguard.view.route.account.HeartGuardWithdrawNoticeRoute
import com.nativelap.heartguard.view.route.auth.HeartGuardLoginRoute
import com.nativelap.heartguard.view.route.emergency.HeartGuardCallingRoute
import com.nativelap.heartguard.view.route.emergency.HeartGuardEmergencyRoute
import com.nativelap.heartguard.view.route.feedback.HeartGuardSaveFailureRoute
import com.nativelap.heartguard.view.route.feedback.HeartGuardSaveSuccessRoute
import com.nativelap.heartguard.view.route.home.HeartGuardHomeRoute
import com.nativelap.heartguard.view.route.history.HeartGuardRecordDetailRoute
import com.nativelap.heartguard.view.route.history.HeartGuardRecordHistoryRoute
import com.nativelap.heartguard.view.route.inquiry.HeartGuardInquiryRoute
import com.nativelap.heartguard.view.route.password.HeartGuardPasswordChangeRoute
import com.nativelap.heartguard.view.route.profile.HeartGuardProfileEditRoute
import com.nativelap.heartguard.view.route.photo.HeartGuardPhotoCameraRoute
import com.nativelap.heartguard.view.route.photo.HeartGuardFieldPhotoRoute
import com.nativelap.heartguard.view.route.photo.HeartGuardRestPhotoRoute
import com.nativelap.heartguard.view.route.photo.HeartGuardWorkPhotoRoute
import com.nativelap.heartguard.view.route.record.HeartGuardRecordTypeSelectionRoute
import com.nativelap.heartguard.view.route.record.HeartGuardTemperatureRecordRoute
import com.nativelap.heartguard.viewmodel.account.WithdrawViewModel
import com.nativelap.heartguard.viewmodel.emergency.EmergencyViewModel
import com.nativelap.heartguard.viewmodel.home.HomeUiState
import com.nativelap.heartguard.viewmodel.home.HomeViewModel
import com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel

/** 인증 상태에 따라 인증 흐름과 메인 흐름 중 하나를 구성하는 앱 진입점이다.
 * SessionManager가 공개하는 인증 상태를 단일 진입점으로 구독해, 세션이 만료되면
 * (BearerTokenAuthenticator가 expireSession()을 호출한 경우 포함) 자동으로 로그인 화면으로 돌아간다. */
@Composable
internal fun HeartGuardNavHost(
    sessionViewModel: HeartGuardSessionViewModel = hiltViewModel(),
) {
    val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()

    when (sessionState) {
        // 앱 시작 직후 저장된 토큰 확인이 끝나기 전까지는 어느 화면도 그리지 않는다.
        // TODO: 스플래시 화면이 추가되면 빈 화면 대신 그 화면을 보여준다.
        SessionState.Initializing -> Unit
        SessionState.Authenticated -> HeartGuardMainNavDisplay()
        SessionState.Unauthenticated -> HeartGuardAuthNavDisplay()
    }
}

/** 사전 발급 계정 로그인과 인증 전 Navigation 3 back stack을 담당한다. */
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
            entry<HeartGuardDestination.SignUp> {
                // 이전 버전에서 저장된 가입 NavKey가 복원돼도 가입 화면은 다시 노출하지 않는다.
                HeartGuardLoginRoute()
            }
        },
    )
}

/** 로그인 이후의 홈·기록·긴급 호출 흐름을 단일 Navigation 3 back stack으로 연결한다. */
@Composable
private fun HeartGuardMainNavDisplay() {
    val backStack = rememberNavBackStack(HeartGuardDestination.Home)

    // 팀 현장페이지(현재 온도·습도·체감온도·폭염 단계·관리자 번호·체크 시각)는 홈뿐 아니라 온도계 기록·
    // 긴급 호출 화면도 함께 보여준다. 아래 공유 ViewModel들과 같은 이유로 여기서 한 번 만들어 필요한 Route에 명시적으로 넘긴다.
    val homeViewModel: HomeViewModel = hiltViewModel()

    DisposableEffect(homeViewModel) {
        homeViewModel.loadTeamSiteOverview()
        onDispose { homeViewModel.reset() }
    }

    // 기록유형선택→온도기록/사진촬영→저장까지 여러 NavKey가 RecordDraftViewModel 하나를
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

    // 입력 초안은 SavedStateHandle로 복원하고 세션 로그아웃·만료 시 ViewModel이 직접 사진과 값을 정리한다.
    // Emergency·Calling 화면이 공유하는 EmergencyViewModel도 같은 이유로 수동 ViewModelStoreOwner
    // 없이 인자 없는 hiltViewModel()을 쓴다. 흐름 종료 시 정리는 emergencyViewModel.reset()으로 한다.
    val emergencyViewModel: EmergencyViewModel = hiltViewModel()

    // emergencyViewModel도 같은 이유로, 로그아웃·세션 만료 시 3초 폴링이 남지 않도록 정리한다.
    DisposableEffect(Unit) {
        onDispose { emergencyViewModel.reset() }
    }

    // 앱을 다시 열었을 때 서버에 진행 중인 긴급호출이 있으면 이어받아, 홈의 "긴급 전화"가 새 호출 대신 그 호출로 이어지게 한다.
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val hasServerActiveEmergencyCall = (homeUiState as? HomeUiState.Success)
        ?.overview
        ?.hasActiveEmergencyCall == true
    LaunchedEffect(hasServerActiveEmergencyCall) {
        if (hasServerActiveEmergencyCall) {
            emergencyViewModel.resumeActiveCall()
        }
    }

    // 회원탈퇴 안내·최종 확인·완료 화면이 입력값(비밀번호 포함)과 요청 상태를 공유한다. 같은 이유로 Activity 스코프이며,
    // 로그아웃·세션 만료로 이 Composable이 사라질 때 비밀번호가 메모리에 남지 않도록 여기서도 reset()한다.
    val withdrawViewModel: WithdrawViewModel = hiltViewModel()

    DisposableEffect(Unit) {
        onDispose { withdrawViewModel.reset() }
    }

    fun goToSaveSuccess() {
        backStack.add(HeartGuardDestination.SaveSuccess)
    }

    fun goToSaveFailure() {
        backStack.add(HeartGuardDestination.SaveFailure)
    }

    fun goBack() {
        if (backStack.size > 1) {
            val poppedDestination = backStack.removeLastOrNull()
            // 호출 중 화면에서 뒤로 가도 서버의 긴급호출은 계속 진행 중이므로 상태·폴링을 지우지 않는다.
            // 홈의 "긴급 전화"로 다시 들어오면 같은 호출의 호출 중 화면으로 돌아간다.
            // 안내 화면에서 뒤로 나가면 회원탈퇴 흐름을 벗어난 것이므로 입력한 비밀번호와 진행 중인 요청을 정리한다.
            if (poppedDestination is HeartGuardDestination.WithdrawNotice) {
                withdrawViewModel.reset()
            }
        }
    }

    // 탈퇴에 성공하면 확인 다이얼로그와 안내 화면을 back stack에서 걷어내고 완료 화면으로 교체한다.
    // 그래야 완료 화면에서 탈퇴 전 화면으로 되돌아갈 수 없다.
    fun goToWithdrawDone() {
        while (
            backStack.lastOrNull() is HeartGuardDestination.WithdrawConfirm ||
            backStack.lastOrNull() is HeartGuardDestination.WithdrawNotice
        ) {
            backStack.removeLastOrNull()
        }
        backStack.add(HeartGuardDestination.WithdrawDone)
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
                    homeViewModel = homeViewModel,
                    // 관리자 전화는 현장관리자 긴급호출 흐름으로 이동한다(본사 긴급 전화는 Route가 바로 다이얼한다).
                    // 이미 진행 중인 긴급호출이 있으면 새로 호출하지 않도록 바로 호출 중 화면으로 보낸다.
                    onManagerCallClick = {
                        val hasActiveEmergencyCall = emergencyViewModel.uiState.value.callId != null
                        if (hasActiveEmergencyCall) {
                            backStack.add(HeartGuardDestination.Calling)
                        } else {
                            backStack.add(HeartGuardDestination.Emergency)
                        }
                    },
                    // 홈의 현장 사진은 온도계 기록(THERMOMETER)의 사진이다. 기록유형 선택을 거치지 않으므로
                    // 여기서 새 온도계 기록을 시작해, 저장 시 기록 종류가 비어 실패하지 않게 한다.
                    onFieldPhotoClick = {
                        recordDraftViewModel.startRecord(RecordType.TEMPERATURE)
                        backStack.add(HeartGuardDestination.FieldPhoto)
                    },
                    onRecordClick = {
                        backStack.add(HeartGuardDestination.RecordTypeSelection)
                    },
                    onProfileEditClick = {
                        backStack.add(HeartGuardDestination.ProfileEdit)
                    },
                    onInquiryClick = {
                        backStack.add(HeartGuardDestination.Inquiry)
                    },
                    onRecordHistoryClick = {
                        backStack.add(HeartGuardDestination.RecordHistory)
                    },
                    onWithdrawClick = {
                        backStack.add(HeartGuardDestination.WithdrawNotice)
                    },
                )
            }
            entry<HeartGuardDestination.ProfileEdit> {
                HeartGuardProfileEditRoute(
                    onBackClick = ::goBack,
                    onPasswordChangeClick = {
                        backStack.add(HeartGuardDestination.PasswordChange)
                    },
                )
            }
            entry<HeartGuardDestination.PasswordChange> {
                HeartGuardPasswordChangeRoute(onBackClick = ::goBack)
            }
            entry<HeartGuardDestination.Inquiry> {
                HeartGuardInquiryRoute(onBackClick = ::goBack)
            }
            entry<HeartGuardDestination.RecordHistory> {
                HeartGuardRecordHistoryRoute(
                    onBackClick = ::goBack,
                    onRecordClick = { recordId ->
                        backStack.add(HeartGuardDestination.RecordHistoryDetail(recordId))
                    },
                    // Figma 23 빈 상태의 "기록하기"는 07 기록유형선택 시트를 연다.
                    onCreateRecordClick = {
                        backStack.add(HeartGuardDestination.RecordTypeSelection)
                    },
                )
            }
            entry<HeartGuardDestination.RecordHistoryDetail> { destination ->
                HeartGuardRecordDetailRoute(
                    recordId = destination.recordId,
                    onBackClick = ::goBack,
                )
            }
            entry<HeartGuardDestination.Emergency> {
                HeartGuardEmergencyRoute(
                    emergencyViewModel = emergencyViewModel,
                    homeViewModel = homeViewModel,
                    // 호출이 등록되면 03을 04로 바꿔, 04에서 뒤로 가도 호출 전 화면(03)이 다시 보이지 않게 한다.
                    onCallStarted = {
                        backStack.removeLastOrNull()
                        backStack.add(HeartGuardDestination.Calling)
                    },
                    onCancelClick = ::goHome,
                )
            }
            entry<HeartGuardDestination.Calling> {
                HeartGuardCallingRoute(
                    emergencyViewModel = emergencyViewModel,
                    homeViewModel = homeViewModel,
                    onCallClosed = ::goHome,
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
                    homeViewModel = homeViewModel,
                    onFieldPhotoClick = {
                        backStack.add(HeartGuardDestination.FieldPhoto)
                    },
                    onSaveSuccess = ::goToSaveSuccess,
                    onSaveFailure = ::goToSaveFailure,
                )
            }
            entry<HeartGuardDestination.FieldPhoto> {
                HeartGuardFieldPhotoRoute(
                    recordDraftViewModel = recordDraftViewModel,
                    onCameraClick = { recordType ->
                        backStack.add(HeartGuardDestination.PhotoCamera(recordType))
                    },
                    onSaveSuccess = ::goToSaveSuccess,
                    onSaveFailure = ::goToSaveFailure,
                )
            }
            entry<HeartGuardDestination.WorkPhoto> {
                HeartGuardWorkPhotoRoute(
                    recordDraftViewModel = recordDraftViewModel,
                    onCameraClick = { recordType ->
                        backStack.add(HeartGuardDestination.PhotoCamera(recordType))
                    },
                    onSaveSuccess = ::goToSaveSuccess,
                    onSaveFailure = ::goToSaveFailure,
                )
            }
            entry<HeartGuardDestination.RestPhoto> {
                HeartGuardRestPhotoRoute(
                    recordDraftViewModel = recordDraftViewModel,
                    onCameraClick = { recordType ->
                        backStack.add(HeartGuardDestination.PhotoCamera(recordType))
                    },
                    onSaveSuccess = ::goToSaveSuccess,
                    onSaveFailure = ::goToSaveFailure,
                )
            }
            entry<HeartGuardDestination.PhotoCamera> { key ->
                HeartGuardPhotoCameraRoute(
                    recordDraftViewModel = recordDraftViewModel,
                    recordType = key.recordType,
                    onBackClick = ::goBack,
                )
            }
            entry<HeartGuardDestination.SaveSuccess> {
                HeartGuardSaveSuccessRoute(
                    recordDraftViewModel = recordDraftViewModel,
                    onCompleteClick = ::goHome,
                )
            }
            entry<HeartGuardDestination.WithdrawNotice> {
                HeartGuardWithdrawNoticeRoute(
                    withdrawViewModel = withdrawViewModel,
                    onBackClick = ::goBack,
                    onWithdrawClick = {
                        backStack.add(HeartGuardDestination.WithdrawConfirm)
                    },
                    onWithdrawSucceeded = ::goToWithdrawDone,
                )
            }
            entry<HeartGuardDestination.WithdrawConfirm>(
                metadata = DialogSceneStrategy.dialog(
                    DialogProperties(usePlatformDefaultWidth = false),
                ),
            ) {
                HeartGuardWithdrawConfirmRoute(
                    withdrawViewModel = withdrawViewModel,
                    onDismiss = ::goBack,
                )
            }
            entry<HeartGuardDestination.WithdrawDone> {
                HeartGuardWithdrawDoneRoute(
                    withdrawViewModel = withdrawViewModel,
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
