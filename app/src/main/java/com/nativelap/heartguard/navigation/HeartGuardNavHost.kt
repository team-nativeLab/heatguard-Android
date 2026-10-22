Warning: truncated output (original token count: 9000)
Total output lines: 711

package com.nativelap.heartguard.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateStatus
import com.nativelap.heartguard.view.component.RecordType
import com.nativelap.heartguard.view.route.account.HeartGuardWithdrawConfirmRoute
import com.nativelap.heartguard.view.route.account.HeartGuardWithdrawDoneRoute
import com.nativelap.heartguard.view.route.account.HeartGuardWithdrawNoticeRoute
import com.nativelap.heartguard.view.route.auth.HeartGuardLoginRoute
import com.nativelap.heartguard.view.route.auth.HeartGuardPhoneNumberRoute
import com.nativelap.heartguard.view.route.auth.HeartGuardSignUpRoute
import com.nativelap.heartguard.view.route.checklist.ChecklistRoute
import com.nativelap.heartguard.view.route.emergency.HeartGuardCallingRoute
import com.nativelap.heartguard.view.route.emergency.HeartGuardEmergencyRoute
import com.nativelap.heartguard.view.route.feedback.HeartGuardSaveFailureRoute
import com.nativelap.heartguard.view.route.feedback.HeartGuardSaveSuccessRoute
import com.nativelap.heartguard.view.route.history.HeartGuardRecordDetailRoute
import com.nativelap.heartguard.view.route.history.HeartGuardRecordHistoryRoute
import com.nativelap.heartguard.view.route.home.HeartGuardHomeRoute
import com.nativelap.heartguard.view.route.inquiry.HeartGuardInquiryRoute
import com.nativelap.heartguard.view.route.menu.MenuDrawerHost
import com.nativelap.heartguard.view.route.notification.HeartGuardTeamNotificationsRoute
import com.nativelap.heartguard.view.route.password.HeartGuardPasswordChangeRoute
import com.nativelap.heartguard.view.route.photo.HeartGuardFieldPhotoRoute
import com.nativelap.heartguard.view.route.photo.HeartGuardPhotoCameraRoute
import com.nativelap.heartguard.view.route.photo.HeartGuardRestPhotoRoute
import com.nativelap.heartguard.view.route.photo.HeartGuardWorkPhotoRoute
import com.nativelap.heartguard.view.route.profile.HeartGuardProfileEditRoute
import com.nativelap.heartguard.view.route.record.HeartGuardRecordTypeSelectionRoute
import com.nativelap.heartguard.view.route.record.HeartGuardTemperatureRecordRoute
import com.nativelap.heartguard.view.route.record.RecordCancelConfirmRoute
import com.nativelap.heartguard.view.screen.intro.IntroScreen
import com.nativelap.heartguard.viewmodel.account.WithdrawViewModel
import com.nativelap.heartguard.viewmodel.checklist.ChecklistViewModel
import com.nativelap.heartguard.viewmodel.emergency.EmergencyViewModel
import com.nativelap.heartguard.viewmodel.home.HomeUiState
import com.nativelap.heartguard.viewmodel.home.HomeViewModel
import com.nativelap.heartguard.viewmodel.notification.NotificationOpenTarget
import com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel
import com.nativelap.heartguard.viewmodel.record.RecordSubmissionState

/** 인증 상태에 따라 인증 흐름과 메인 흐름 중 하나를 구성하는 앱 진입점이다.
 * SessionManager가 공개하는 인증 상태를 단일 진입점으로 구독해, 세션이 만료되면
 * (BearerTokenAuthenticator가 expireSession()을 호출한 경우 포함) 자동으로 로그인 화면으로 돌아간다. */
@Composable
internal fun HeartGuardNavHost(sessionViewModel: HeartGuardSessionViewModel = hiltViewModel()) {
    val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()
    val sessionGeneration by sessionViewModel.sessionGeneration.collectAsStateWithLifecycle()

    when (sessionState) {
        // 앱 시작 직후 저장된 토큰 확인이 끝나기 전까지는 인트로 로고를 보여 준다.
        SessionState.Initializing -> IntroScreen()

        SessionState.Authenticated -> HeartGuardMainNavDisplay(sessionGeneration = sessionGeneration)

        SessionState.Unauthenticated -> HeartGuardAuthNavDisplay()
    }
}

private fun shouldConfirmRecordExit(backStack: List<NavKey>): Boolean {
    val currentDestination = backStack.lastOrNull() as? HeartGuardDestination
    val previousDestination = backStack.getOrNull(backStack.lastIndex - 1) as? HeartGuardDestination

    return currentDestination.isRecordEditingDestination() &&
        !previousDestination.isRecordFlowDestination()
}

private fun HeartGuardDestination?.isRecordEditingDestination(): Boolean =
    when (this) {
        HeartGuardDestination.TemperatureRecord,
        HeartGuardDestination.FieldPhoto,
        HeartGuardDestination.WorkPhoto,
        HeartGuardDestination.RestPhoto,
        -> true

        else -> false
    }

private fun HeartGuardDestination?.isRecordFlowDestination(): Boolean =
    when (this) {
        HeartGuardDestination.TemperatureRecord,
        HeartGuardDestination.FieldPhoto,
        HeartGuardDestination.WorkPhoto,
        HeartGuardDestination.RestPhoto,
        is HeartGuardDestination.PhotoCamera,
        -> true

        else -> false
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
        entryProvider =
            entryProvider {
                entry<HeartGuardDestination.Login> {
                    HeartGuardLoginRoute(
                        onNavigateToSignUp = {
                            backStack.add(HeartGuardDestination.SignUpForm)
                        },
                        onNavigateToPhoneNumber = {
                            backStack.add(HeartGuardDestination.PhoneNumberLink)
                        },
                    )
                }
                entry<HeartGuardDestination.SignUp> {
                    // 이전 버전에서 저장된 가입 NavKey가 복원돼도 가입 화면은 다시 노출하지 않는다.
                    HeartGuardLoginRoute()
                }
                entry<HeartGuardDestination.SignUpForm> {
                    HeartGuardSignUpRoute(
                        onNavigateToLogin = {
                            if (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                        },
                    )
                }
                entry<HeartGuardDestination.PhoneNumberLink> {
                    HeartGuardPhoneNumberRoute(
                        onNavigateBack = { backStack.removeLastOrNull() },
                    )
                }
            },
    )
}

/** 로그인 이후의 홈·기록·긴급 호출 흐름을 단일 Navigation 3 back stack으로 연결한다. */
@Composable
private fun HeartGuardMainNavDisplay(sessionGeneration: Long) {
    val backStack = rememberNavBackStack(HeartGuardDestination.Home)

    // 로그아웃→재로그인이 한 프레임 안에 지나가 이 화면이 유지돼도, 세대가 바뀌면 이전 계정의 화면 이동 기록을 홈으로 되돌린다.
    // 세대는 프로세스마다 다시 시작하므로 저장하지 않는다. 프로세스 복원 시에는 복원된 back stack을 그대로 둔다.
    var displayedSessionGeneration by remember { mutableLongStateOf(sessionGeneration) }
    LaunchedEffect(sessionGeneration) {
        if (sessionGeneration != displayedSessionGeneration) {
            displayedSessionGeneration = sessionGeneration
            backStack.clear()
            backStack.add(HeartGuardDestination.Home)
        }
    }

    // 팀 현장페이지(현재 온도·습도·체감온도·폭염 단계·관리자 번호·체크 시각)는 홈뿐 아니라 온도계 기록·
    // 긴급 호출 화면도 함께 보여준다. 아래 공유 ViewModel들과 같은 이유로 여기서 한 번 만들어 필요한 Route에 명시적으로 넘긴다.
    val homeViewModel: HomeViewModel = hiltViewModel()

    // 공유 ViewModel은 Activity 수명이라 회전·다크모드 전환으로 이 Composable이 다시 만들어져도 살아 있다.
    // 그때 reset()하면 진행 중인 긴급호출·탈퇴 요청과 홈 값이 사라지므로, 설정 변경이 아닐 때(로그아웃·세션 만료·앱 종료)만 정리한다.
    val hostActivity = LocalActivity.current

    fun isLeavingMainFlow(): Boolean = hostActivity?.isChangingConfigurations != true

    DisposableEffect(homeViewModel) {
        if (homeViewModel.uiState.value !is HomeUiState.Success) {
            homeViewModel.loadTeamSiteOverview()
        }
        onDispose {
            if (isLeavingMainFlow()) {
                homeViewModel.reset()
            }
        }
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
    val recordSubmissionState by recordDraftViewModel.submissionState.collectAsStateWithLifecycle()
    val checklistViewModel: ChecklistViewModel = hiltViewModel()

    // 입력 초안은 SavedStateHandle로 복원하고 세션 로그아웃·만료 시 ViewModel이 직접 사진과 값을 정리한다.
    // Emergency·Calling 화면이 공유하는 EmergencyViewModel도 …3000 tokens truncated…         recordId = destination.recordId,
                        onBackClick = ::goBack,
                    )
                }
                // 등록 중인 03은 닫기 제스처를 막고, 취소 버튼으로만 홈에 돌아간다.
                entry<HeartGuardDestination.Emergency>(
                    metadata = HeartGuardBottomSheetSceneStrategy.bottomSheet(isDismissible = false),
                ) {
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
                entry<HeartGuardDestination.Calling>(
                    // 04의 Back·스와이프 닫기 시도는 서버 종료 요청을 보내되 승인될 때까지 시트를 유지한다.
                    metadata =
                        HeartGuardBottomSheetSceneStrategy.bottomSheet(
                            isDismissible = false,
                            onDismissAttempt = {
                                emergencyViewModel.updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
                            },
                        ),
                ) {
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
                            val nextDestination =
                                when (recordType) {
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
                    MenuDrawerHost(
                        onProfileEditClick = { backStack.add(HeartGuardDestination.ProfileEdit) },
                        onInquiryClick = { backStack.add(HeartGuardDestination.Inquiry) },
                        onWithdrawClick = { backStack.add(HeartGuardDestination.WithdrawNotice) },
                        onNotificationsClick = { backStack.add(HeartGuardDestination.TeamNotifications) },
                    ) { onMenuClick, onNotificationClick ->
                        HeartGuardTemperatureRecordRoute(
                            onMenuClick = onMenuClick,
                            onNotificationClick = onNotificationClick,
                            recordDraftViewModel = recordDraftViewModel,
                            homeViewModel = homeViewModel,
                            onFieldPhotoClick = {
                                backStack.add(HeartGuardDestination.FieldPhoto)
                            },
                            onSaveSuccess = ::goToSaveSuccess,
                            onSaveFailure = ::goToSaveFailure,
                        )
                    }
                }
                entry<HeartGuardDestination.FieldPhoto> {
                    MenuDrawerHost(
                        onProfileEditClick = { backStack.add(HeartGuardDestination.ProfileEdit) },
                        onInquiryClick = { backStack.add(HeartGuardDestination.Inquiry) },
                        onWithdrawClick = { backStack.add(HeartGuardDestination.WithdrawNotice) },
                        onNotificationsClick = { backStack.add(HeartGuardDestination.TeamNotifications) },
                    ) { onMenuClick, onNotificationClick ->
                        HeartGuardFieldPhotoRoute(
                            onMenuClick = onMenuClick,
                            onNotificationClick = onNotificationClick,
                            recordDraftViewModel = recordDraftViewModel,
                            onCameraClick = { recordType ->
                                backStack.add(HeartGuardDestination.PhotoCamera(recordType))
                            },
                            onSaveSuccess = ::goToSaveSuccess,
                            onSaveFailure = ::goToSaveFailure,
                        )
                    }
                }
                entry<HeartGuardDestination.WorkPhoto> {
                    MenuDrawerHost(
                        onProfileEditClick = { backStack.add(HeartGuardDestination.ProfileEdit) },
                        onInquiryClick = { backStack.add(HeartGuardDestination.Inquiry) },
                        onWithdrawClick = { backStack.add(HeartGuardDestination.WithdrawNotice) },
                        onNotificationsClick = { backStack.add(HeartGuardDestination.TeamNotifications) },
                    ) { onMenuClick, onNotificationClick ->
                        HeartGuardWorkPhotoRoute(
                            onMenuClick = onMenuClick,
                            onNotificationClick = onNotificationClick,
                            recordDraftViewModel = recordDraftViewModel,
                            onCameraClick = { recordType ->
                                backStack.add(HeartGuardDestination.PhotoCamera(recordType))
                            },
                            onSaveSuccess = ::goToSaveSuccess,
                            onSaveFailure = ::goToSaveFailure,
                        )
                    }
                }
                entry<HeartGuardDestination.RestPhoto> {
                    MenuDrawerHost(
                        onProfileEditClick = { backStack.add(HeartGuardDestination.ProfileEdit) },
                        onInquiryClick = { backStack.add(HeartGuardDestination.Inquiry) },
                        onWithdrawClick = { backStack.add(HeartGuardDestination.WithdrawNotice) },
                        onNotificationsClick = { backStack.add(HeartGuardDestination.TeamNotifications) },
                    ) { onMenuClick, onNotificationClick ->
                        HeartGuardRestPhotoRoute(
                            onMenuClick = onMenuClick,
                            onNotificationClick = onNotificationClick,
                            recordDraftViewModel = recordDraftViewModel,
                            onCameraClick = { recordType ->
                                backStack.add(HeartGuardDestination.PhotoCamera(recordType))
                            },
                            onSaveSuccess = ::goToSaveSuccess,
                            onSaveFailure = ::goToSaveFailure,
                        )
                    }
                }
                entry<HeartGuardDestination.PhotoCamera> { key ->
                    HeartGuardPhotoCameraRoute(
                        recordDraftViewModel = recordDraftViewModel,
                        recordType = key.recordType,
                        onBackClick = ::goBack,
                    )
                }
                // 저장 성공 시트는 스와이프로 닫혀 입력 화면으로 돌아가 같은 기록을 다시 저장하지 않도록 버튼으로만 닫는다.
                entry<HeartGuardDestination.SaveSuccess>(
                    metadata = HeartGuardBottomSheetSceneStrategy.bottomSheet(isDismissible = false),
                ) {
                    HeartGuardSaveSuccessRoute(
                        recordDraftViewModel = recordDraftViewModel,
                        onCompleteClick = ::goHome,
                        onRecordDetailClick = { recordId ->
                            goHome()
                            backStack.add(HeartGuardDestination.RecordHistoryDetail(recordId))
                        },
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
                    metadata =
                        DialogSceneStrategy.dialog(
                            DialogProperties(usePlatformDefaultWidth = false),
                        ),
                ) {
                    HeartGuardWithdrawConfirmRoute(
                        withdrawViewModel = withdrawViewModel,
                        onDismiss = ::goBack,
                    )
                }
                entry<HeartGuardDestination.RecordCancelConfirm>(
                    metadata =
                        DialogSceneStrategy.dialog(
                            DialogProperties(usePlatformDefaultWidth = false),
                        ),
                ) {
                    RecordCancelConfirmRoute(
                        recordDraftViewModel = recordDraftViewModel,
                        onContinueClick = ::goBack,
                        onGoHomeClick = ::leaveRecordFlow,
                    )
                }
                entry<HeartGuardDestination.WithdrawDone> {
                    HeartGuardWithdrawDoneRoute(
                        withdrawViewModel = withdrawViewModel,
                    )
                }
                // 실패 시트를 내리면 "다시 시도하기"와 같이 입력 화면으로 돌아간다.
                entry<HeartGuardDestination.SaveFailure>(
                    metadata =
                        HeartGuardBottomSheetSceneStrategy.bottomSheet(
                            isDismissible = recordSubmissionState !is RecordSubmissionState.Unknown,
                        ),
                ) {
                    HeartGuardSaveFailureRoute(
                        recordDraftViewModel = recordDraftViewModel,
                        onRetryClick = ::goBack,
                        onCheckHistoryClick = {
                            recordDraftViewModel.reset()
                            goHome()
                            backStack.add(HeartGuardDestination.RecordHistory)
                        },
                        onUnknownExitClick = {
                            recordDraftViewModel.reset()
                            goHome()
                        },
                        // "임시저장 후 나가기"는 draft를 보존한 채 홈으로 돌아가는 동작을 의도하므로,
                        // SaveSuccess와 달리 여기서는 recordDraftViewModel.reset()을 호출하지 않는다.
                        onSaveDraftAndExitClick = {
                            recordDraftViewModel.markDraftTemporarilySaved()
                            goHome()
                        },
                    )
                }
            },
    )
}
