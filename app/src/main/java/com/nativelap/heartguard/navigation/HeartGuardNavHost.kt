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
import androidx.compose.runtime.mutableStateOf
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
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.core.session.SessionState
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateStatus
import com.nativelap.heartguard.view.component.RecordType
import com.nativelap.heartguard.view.route.account.HeartGuardWithdrawConfirmRoute
import com.nativelap.heartguard.view.route.account.HeartGuardWithdrawDoneRoute
import com.nativelap.heartguard.view.route.account.HeartGuardWithdrawNoticeRoute
import com.nativelap.heartguard.view.route.auth.HeartGuardLoginRoute
import com.nativelap.heartguard.view.route.auth.HeartGuardPhoneNumberRoute
import com.nativelap.heartguard.view.route.auth.HeartGuardSignUpRoute
import com.nativelap.heartguard.view.route.checklist.ChecklistOverviewRoute
import com.nativelap.heartguard.view.route.checklist.ChecklistRoute
import com.nativelap.heartguard.view.route.emergency.HeartGuardCallingRoute
import com.nativelap.heartguard.view.route.emergency.HeartGuardEmergencyRoute
import com.nativelap.heartguard.view.route.feedback.HeartGuardSaveFailureRoute
import com.nativelap.heartguard.view.route.feedback.HeartGuardSaveSuccessRoute
import com.nativelap.heartguard.view.route.history.HeartGuardRecordDetailRoute
import com.nativelap.heartguard.view.route.history.HeartGuardRecordHistoryRoute
import com.nativelap.heartguard.view.route.home.HeartGuardHomeRoute
import com.nativelap.heartguard.view.route.home.HomeHeadquartersCallConfirmationRoute
import com.nativelap.heartguard.view.route.home.HomeHeadquartersContactRoute
import com.nativelap.heartguard.view.route.inquiry.HeartGuardInquiryRegistrationRoute
import com.nativelap.heartguard.view.route.inquiry.HeartGuardInquiryRoute
import com.nativelap.heartguard.view.route.menu.MenuDrawerHost
import com.nativelap.heartguard.view.route.notification.HeartGuardTeamNotificationsRoute
import com.nativelap.heartguard.view.route.password.HeartGuardPasswordChangeRoute
import com.nativelap.heartguard.view.route.photo.HeartGuardFieldPhotoRoute
import com.nativelap.heartguard.view.route.photo.HeartGuardRestPhotoRoute
import com.nativelap.heartguard.view.route.photo.HeartGuardWorkPhotoRoute
import com.nativelap.heartguard.view.route.profile.HeartGuardProfileEditRoute
import com.nativelap.heartguard.view.route.record.HeartGuardRecordTypeSelectionRoute
import com.nativelap.heartguard.view.route.record.HeartGuardTemperatureRecordRoute
import com.nativelap.heartguard.view.route.record.RecordCancelConfirmRoute
import com.nativelap.heartguard.view.route.site.AppAccountSitesRoute
import com.nativelap.heartguard.view.route.site.SiteUnlinkConfirmationRoute
import com.nativelap.heartguard.view.screen.intro.IntroScreen
import com.nativelap.heartguard.viewmodel.account.WithdrawViewModel
import com.nativelap.heartguard.viewmodel.checklist.ChecklistViewModel
import com.nativelap.heartguard.viewmodel.emergency.EmergencyViewModel
import com.nativelap.heartguard.viewmodel.home.HomeUiState
import com.nativelap.heartguard.viewmodel.home.HomeViewModel
import com.nativelap.heartguard.viewmodel.inquiry.InquiryViewModel
import com.nativelap.heartguard.viewmodel.notification.NotificationOpenTarget
import com.nativelap.heartguard.viewmodel.record.RecordDraftViewModel
import com.nativelap.heartguard.viewmodel.record.RecordSubmissionState
import com.nativelap.heartguard.viewmodel.site.AppAccountSitesViewModel

/** 인증 상태에 따라 인증 흐름과 메인 흐름 중 하나를 구성하는 앱 진입점이다.
 * SessionManager가 공개하는 인증 상태를 단일 진입점으로 구독해, 세션이 만료되면
 * (BearerTokenAuthenticator가 expireSession()을 호출한 경우 포함) 자동으로 로그인 화면으로 돌아간다. */
@Composable
internal fun HeartGuardNavHost(
    sessionViewModel: HeartGuardSessionViewModel = hiltViewModel(),
    appLinkTokenViewModel: AppLinkTokenViewModel = hiltViewModel(),
) {
    val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()
    val sessionRole by sessionViewModel.sessionRole.collectAsStateWithLifecycle()
    val sessionGeneration by sessionViewModel.sessionGeneration.collectAsStateWithLifecycle()
    val pendingAppLink by appLinkTokenViewModel.pendingLink.collectAsStateWithLifecycle()

    when (sessionState) {
        // 앱 시작 직후 저장된 토큰 확인이 끝나기 전까지는 인트로 로고를 보여 준다.
        SessionState.Initializing -> {
            IntroScreen()
        }

        SessionState.Authenticated -> {
            when (sessionRole) {
                SessionRole.APP_ACCOUNT -> {
                    LaunchedEffect(sessionGeneration) {
                        sessionViewModel.expireLegacyAppAccountSession()
                    }
                    IntroScreen()
                }

                SessionRole.TEAM_MEMBER,
                -> {
                    if (pendingAppLink == null) {
                        HeartGuardMainNavDisplay(
                            sessionGeneration = sessionGeneration,
                            appLinkTokenViewModel = appLinkTokenViewModel,
                        )
                    } else {
                        HeartGuardPhoneNumberRoute(
                            pendingAppLink = requireNotNull(pendingAppLink),
                            onClaimSuccess = { completedLink ->
                                appLinkTokenViewModel.consume(completedLink)
                            },
                            onCancelClaim = { canceledLink ->
                                appLinkTokenViewModel.consume(canceledLink)
                            },
                            onSessionRequired = sessionViewModel::expireTeamMemberSession,
                            isAppAccountSession = false,
                        )
                    }
                }

                null -> {
                    IntroScreen()
                }
            }
        }

        SessionState.Unauthenticated -> {
            HeartGuardAuthNavDisplay()
        }
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
        -> true

        else -> false
    }

/** 사전 발급 계정 로그인과 인증 전 Navigation 3 back stack을 담당한다. */
@Composable
private fun HeartGuardAuthNavDisplay() {
    val backStack = rememberNavBackStack(HeartGuardDestination.Login)
    var isBackStackReady by remember { mutableStateOf(false) }

    LaunchedEffect(backStack) {
        if (
            HeartGuardDestination.Login in backStack ||
            HeartGuardDestination.SignUp in backStack
        ) {
            backStack.clear()
            backStack.add(HeartGuardDestination.Login)
        }
        isBackStackReady = true
    }

    if (!isBackStackReady) {
        IntroScreen()
        return
    }

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
                    )
                }
                entry<HeartGuardDestination.AppAccountLogin> {
                    HeartGuardLoginRoute(
                        onNavigateToSignUp = {
                            backStack.add(HeartGuardDestination.SignUpForm)
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
            },
    )
}

/** 로그인 이후의 홈·기록·긴급 호출 흐름을 단일 Navigation 3 back stack으로 연결한다. */
@Composable
private fun HeartGuardMainNavDisplay(
    sessionGeneration: Long,
    appLinkTokenViewModel: AppLinkTokenViewModel,
) {
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

    // ViewModel이 세션 변경을 관찰해 요청과 화면 상태를 정리하므로 세션 세대별 인스턴스를 만들지 않는다.
    val appAccountSitesViewModel: AppAccountSitesViewModel =
        hiltViewModel(key = "team-sites")

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
    val inquiryViewModel: InquiryViewModel = hiltViewModel()
    val recordSubmissionState by recordDraftViewModel.submissionState.collectAsStateWithLifecycle()
    val checklistViewModel: ChecklistViewModel = hiltViewModel()

    // 입력 초안은 SavedStateHandle로 복원하고 세션 로그아웃·만료 시 ViewModel이 직접 사진과 값을 정리한다.
    // Emergency·Calling 화면이 공유하는 EmergencyViewModel도 같은 이유로 수동 ViewModelStoreOwner
    // 없이 인자 없는 hiltViewModel()을 쓴다. 흐름 종료 시 정리는 emergencyViewModel.reset()으로 한다.
    val emergencyViewModel: EmergencyViewModel = hiltViewModel()

    // emergencyViewModel도 같은 이유로, 로그아웃·세션 만료 시 3초 폴링이 남지 않도록 정리한다.
    DisposableEffect(Unit) {
        onDispose {
            if (isLeavingMainFlow()) {
                emergencyViewModel.reset()
            }
        }
    }

    // 앱을 다시 열었을 때 서버에 진행 중인 긴급호출이 있으면 이어받아, 홈의 "긴급 전화"가 새 호출 대신 그 호출로 이어지게 한다.
    // 홈 조회가 끝날 때마다 다시 확인해, 이어받기가 한 번 실패해도 응답 내용과 상관없이 다음 새로고침에서 복구한다.
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val checkSchedule by homeViewModel.checkSchedule.collectAsStateWithLifecycle()
    val checklistSiteLabel =
        (homeUiState as? HomeUiState.Success)
            ?.overview
            ?.let { overview ->
                listOfNotNull(overview.workplace, overview.teamName)
                    .filter(String::isNotBlank)
                    .joinToString(separator = " · ")
                    .takeIf(String::isNotBlank)
            }
    val homeRefreshCompletionCount by homeViewModel.refreshCompletionCount.collectAsStateWithLifecycle()
    val hasServerActiveEmergencyCall =
        (homeUiState as? HomeUiState.Success)
            ?.overview
            ?.hasActiveEmergencyCall == true
    LaunchedEffect(homeRefreshCompletionCount, hasServerActiveEmergencyCall) {
        if (hasServerActiveEmergencyCall) {
            emergencyViewModel.resumeActiveCall()
        }
    }

    // 회원탈퇴 안내·최종 확인·완료 화면이 입력값(비밀번호 포함)과 요청 상태를 공유한다. 같은 이유로 Activity 스코프이며,
    // 로그아웃·세션 만료로 이 Composable이 사라질 때 비밀번호가 메모리에 남지 않도록 여기서도 reset()한다.
    val withdrawViewModel: WithdrawViewModel = hiltViewModel()

    DisposableEffect(Unit) {
        onDispose {
            if (isLeavingMainFlow()) {
                withdrawViewModel.reset()
            }
        }
    }

    fun goToSaveSuccess() {
        if (backStack.lastOrNull() == HeartGuardDestination.RecordCancelConfirm) {
            backStack.removeLastOrNull()
        }
        backStack.add(HeartGuardDestination.SaveSuccess)
    }

    fun goToSaveFailure() {
        if (backStack.lastOrNull() == HeartGuardDestination.RecordCancelConfirm) {
            backStack.removeLastOrNull()
        }
        backStack.add(HeartGuardDestination.SaveFailure)
    }

    fun goBack() {
        if (backStack.lastOrNull() == HeartGuardDestination.RecordCancelConfirm) {
            backStack.removeLastOrNull()
            return
        }

        if (shouldConfirmRecordExit(backStack)) {
            backStack.add(HeartGuardDestination.RecordCancelConfirm)
            return
        }

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

    fun leaveRecordFlow() {
        val submissionState = recordDraftViewModel.submissionState.value
        if (
            submissionState !is RecordSubmissionState.Idle &&
            submissionState !is RecordSubmissionState.Failure
        ) {
            return
        }
        recordDraftViewModel.reset()
        goHome()
    }

    // 알림에서 연 화면이 연타로 중복해 쌓이지 않도록 맨 위와 같은 목적지는 다시 추가하지 않는다.
    fun openFromNotification(target: NotificationOpenTarget) {
        val destination =
            when (target) {
                is NotificationOpenTarget.RecordDetail -> {
                    HeartGuardDestination.RecordHistoryDetail(target.recordId)
                }

                NotificationOpenTarget.Inquiry -> {
                    HeartGuardDestination.Inquiry
                }

                // 진행 중인 호출이 없으면 새 호출을 시작하는 화면으로 보내지 않고 알림 목록에 머문다.
                NotificationOpenTarget.ActiveEmergencyCall -> {
                    val hasActiveEmergencyCall = emergencyViewModel.uiState.value.callId != null
                    if (hasActiveEmergencyCall) {
                        HeartGuardDestination.Calling
                    } else {
                        null
                    }
                }
            }
        if (destination != null && backStack.lastOrNull() != destination) {
            backStack.add(destination)
        }
    }

    val sceneStrategies: List<SceneStrategy<NavKey>> =
        listOf(
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
        entryProvider =
            entryProvider {
                entry<HeartGuardDestination.Home> {
                    HeartGuardHomeRoute(
                        homeViewModel = homeViewModel,
                        // 관리자 전화는 본사 연락처 시트로, 긴급 전화는 본사 대상의 긴급호출 흐름으로 이동한다.
                        onManagerCallClick = {
                            backStack.add(HeartGuardDestination.HeadquartersContact)
                        },
                        // 이미 진행 중인 긴급호출이 있으면 새로 호출하지 않도록 바로 호출 중 화면으로 보낸다.
                        onEmergencyClick = {
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
                        onChecklistClick = {
                            backStack.add(HeartGuardDestination.ChecklistOverview)
                        },
                        onNotificationsClick = {
                            backStack.add(HeartGuardDestination.TeamNotifications)
                        },
                        onWithdrawClick = {
                            backStack.add(HeartGuardDestination.WithdrawNotice)
                        },
                        onHomeClick = ::leaveRecordFlow,
                        onSiteSelectionClick = {
                            backStack.add(HeartGuardDestination.TeamSiteSelection)
                        },
                    )
                }
                entry<HeartGuardDestination.TeamSiteSelection> {
                    AppAccountSitesRoute(
                        onBackClick = ::goBack,
                        onUnlinkConfirmationRequest = { teamId, siteId ->
                            backStack.add(
                                HeartGuardDestination.SiteUnlinkConfirmation(
                                    teamId = teamId,
                                    siteId = siteId,
                                ),
                            )
                        },
                        viewModel = appAccountSitesViewModel,
                    )
                }
                entry<HeartGuardDestination.SiteUnlinkConfirmation>(
                    metadata =
                        DialogSceneStrategy.dialog(
                            DialogProperties(usePlatformDefaultWidth = false),
                        ),
                ) { destination ->
                    SiteUnlinkConfirmationRoute(
                        teamId = destination.teamId,
                        siteId = destination.siteId,
                        onDismiss = ::goBack,
                        viewModel = appAccountSitesViewModel,
                    )
                }
                entry<HeartGuardDestination.ChecklistOverview> {
                    ChecklistOverviewRoute(
                        checklistViewModel = checklistViewModel,
                        schedule = checkSchedule.takeIf { homeUiState is HomeUiState.Success },
                        siteLabel = checklistSiteLabel,
                        isScheduleLoading = homeUiState is HomeUiState.Loading,
                        hasScheduleLoadError = homeUiState is HomeUiState.Error,
                        onBackClick = ::goBack,
                        onScheduleRetryClick = homeViewModel::loadTeamSiteOverview,
                        onNewDay = homeViewModel::refreshForNewDay,
                        onPeriodClick = { time ->
                            if (checkSchedule?.nextCheckTime == time) {
                                backStack.add(HeartGuardDestination.ChecklistPeriod(time = time.toString()))
                            }
                        },
                    )
                }
                entry<HeartGuardDestination.ChecklistRoundDetail> { destination ->
                    LaunchedEffect(destination) {
                        if (backStack.lastOrNull() == destination) {
                            backStack.removeLastOrNull()
                            if (backStack.lastOrNull() != HeartGuardDestination.ChecklistOverview) {
                                backStack.add(HeartGuardDestination.ChecklistOverview)
                            }
                        }
                    }
                }
                entry<HeartGuardDestination.ChecklistDemoOverview> {
                    ChecklistOverviewRoute(
                        checklistViewModel = checklistViewModel,
                        schedule = checkSchedule.takeIf { homeUiState is HomeUiState.Success },
                        siteLabel = checklistSiteLabel,
                        isScheduleLoading = homeUiState is HomeUiState.Loading,
                        hasScheduleLoadError = homeUiState is HomeUiState.Error,
                        onBackClick = ::goBack,
                        onScheduleRetryClick = homeViewModel::loadTeamSiteOverview,
                        onNewDay = homeViewModel::refreshForNewDay,
                        onPeriodClick = { time ->
                            if (checkSchedule?.nextCheckTime == time) {
                                backStack.add(HeartGuardDestination.ChecklistPeriod(time = time.toString()))
                            }
                        },
                    )
                }
                entry<HeartGuardDestination.ChecklistDemoPeriod> { destination ->
                    val restoredTime = runCatching { java.time.LocalTime.parse(destination.time) }.getOrNull()
                    when (val currentHomeUiState = homeUiState) {
                        is HomeUiState.Loading -> {
                            ChecklistOverviewRoute(
                                checklistViewModel = checklistViewModel,
                                schedule = null,
                                siteLabel = checklistSiteLabel,
                                isScheduleLoading = true,
                                hasScheduleLoadError = false,
                                onBackClick = ::goBack,
                                onScheduleRetryClick = homeViewModel::loadTeamSiteOverview,
                                onNewDay = homeViewModel::refreshForNewDay,
                                onPeriodClick = { time ->
                                    if (checkSchedule?.nextCheckTime == time) {
                                        backStack.add(HeartGuardDestination.ChecklistPeriod(time = time.toString()))
                                    }
                                },
                            )
                        }

                        is HomeUiState.Error -> {
                            ChecklistOverviewRoute(
                                checklistViewModel = checklistViewModel,
                                schedule = null,
                                siteLabel = checklistSiteLabel,
                                isScheduleLoading = false,
                                hasScheduleLoadError = true,
                                onBackClick = ::goBack,
                                onScheduleRetryClick = homeViewModel::loadTeamSiteOverview,
                                onNewDay = homeViewModel::refreshForNewDay,
                                onPeriodClick = { time ->
                                    if (checkSchedule?.nextCheckTime == time) {
                                        backStack.add(HeartGuardDestination.ChecklistPeriod(time = time.toString()))
                                    }
                                },
                            )
                        }

                        is HomeUiState.Success -> {
                            val isCurrentTime =
                                restoredTime != null &&
                                    checkSchedule?.nextCheckTime == restoredTime &&
                                    currentHomeUiState.overview.checkTimes.any { rawTime ->
                                        runCatching { java.time.LocalTime.parse(rawTime) }.getOrNull() == restoredTime
                                    }
                            if (isCurrentTime) {
                                ChecklistRoute(
                                    checklistViewModel = checklistViewModel,
                                    checkSchedule = checkSchedule,
                                    selectedTime = restoredTime,
                                    siteLabel = checklistSiteLabel,
                                    onBackClick = ::goBack,
                                    onCompleteClick = ::goHome,
                                )
                            } else {
                                LaunchedEffect(destination) {
                                    if (backStack.lastOrNull() == destination) {
                                        backStack.removeLastOrNull()
                                        backStack.add(HeartGuardDestination.ChecklistOverview)
                                    }
                                }
                            }
                        }
                    }
                }
                entry<HeartGuardDestination.Checklist> {
                    val currentCheckTime = checkSchedule?.nextCheckTime
                    if (homeUiState is HomeUiState.Success && currentCheckTime != null) {
                        ChecklistRoute(
                            checklistViewModel = checklistViewModel,
                            checkSchedule = checkSchedule,
                            selectedTime = currentCheckTime,
                            siteLabel = checklistSiteLabel,
                            onBackClick = ::goBack,
                            onCompleteClick = ::goHome,
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            if (backStack.lastOrNull() == HeartGuardDestination.Checklist) {
                                backStack.removeLastOrNull()
                            }
                        }
                    }
                }
                entry<HeartGuardDestination.ChecklistPeriod> { destination ->
                    val selectedTime = runCatching { java.time.LocalTime.parse(destination.time) }.getOrNull()
                    if (homeUiState is HomeUiState.Success && selectedTime != null &&
                        selectedTime == checkSchedule?.nextCheckTime
                    ) {
                        ChecklistRoute(
                            checklistViewModel = checklistViewModel,
                            checkSchedule = checkSchedule,
                            selectedTime = selectedTime,
                            siteLabel = checklistSiteLabel,
                            onBackClick = ::goBack,
                            onCompleteClick = ::goHome,
                        )
                    } else {
                        LaunchedEffect(destination) {
                            if (backStack.lastOrNull() == destination) {
                                backStack.removeLastOrNull()
                            }
                        }
                    }
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
                    HeartGuardInquiryRoute(
                        onBackClick = ::goBack,
                        onRegisterClick = { backStack.add(HeartGuardDestination.InquiryRegistration) },
                        inquiryViewModel = inquiryViewModel,
                    )
                }
                entry<HeartGuardDestination.InquiryRegistration> {
                    HeartGuardInquiryRegistrationRoute(
                        onBackClick = ::goBack,
                        inquiryViewModel = inquiryViewModel,
                    )
                }
                entry<HeartGuardDestination.TeamNotifications> {
                    HeartGuardTeamNotificationsRoute(
                        onBackClick = ::goBack,
                        onOpenTarget = ::openFromNotification,
                    )
                }
                entry<HeartGuardDestination.RecordHistory> {
                    HeartGuardRecordHistoryRoute(
                        onBackClick = ::goBack,
                        recordDraftViewModel = recordDraftViewModel,
                        onRecordClick = { recordId ->
                            backStack.add(HeartGuardDestination.RecordHistoryDetail(recordId))
                        },
                        // Figma 23 빈 상태의 "기록하기"는 07 기록유형선택 시트를 연다.
                        onCreateRecordClick = {
                            backStack.add(HeartGuardDestination.RecordTypeSelection)
                        },
                        onResumeDraftClick = { recordType ->
                            val destination =
                                when (recordType) {
                                    RecordType.TEMPERATURE -> HeartGuardDestination.TemperatureRecord
                                    RecordType.WORK -> HeartGuardDestination.WorkPhoto
                                    RecordType.REST -> HeartGuardDestination.RestPhoto
                                }
                            backStack.removeLastOrNull()
                            backStack.add(destination)
                        },
                    )
                }
                entry<HeartGuardDestination.RecordHistoryDetail> { destination ->
                    HeartGuardRecordDetailRoute(
                        recordId = destination.recordId,
                        onBackClick = ::goBack,
                    )
                }
                entry<HeartGuardDestination.Emergency>(
                    metadata =
                        HeartGuardBottomSheetSceneStrategy.bottomSheet(
                            isDismissible = false,
                            onDismissAttempt = {
                                val emergencyState = emergencyViewModel.uiState.value
                                if (emergencyState.callId != null) {
                                    emergencyViewModel.updateCallStatus(EmergencyCallUpdateStatus.CANCELLED)
                                } else if (emergencyState.isRegistering || emergencyState.registrationMayExist) {
                                    emergencyViewModel.requestDismissDuringRegistration()
                                } else {
                                    goHome()
                                }
                            },
                        ),
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
                entry<HeartGuardDestination.HeadquartersContact>(
                    metadata = HeartGuardBottomSheetSceneStrategy.bottomSheet(),
                ) {
                    HomeHeadquartersContactRoute(
                        homeViewModel = homeViewModel,
                        onCallClick = {
                            backStack.add(HeartGuardDestination.HeadquartersCallConfirmation)
                        },
                    )
                }
                entry<HeartGuardDestination.HeadquartersCallConfirmation>(
                    metadata =
                        DialogSceneStrategy.dialog(
                            DialogProperties(usePlatformDefaultWidth = false),
                        ),
                ) {
                    HomeHeadquartersCallConfirmationRoute(
                        homeViewModel = homeViewModel,
                        onDismissRequest = ::goBack,
                        onDialerOpened = {
                            while (
                                backStack.lastOrNull() == HeartGuardDestination.HeadquartersCallConfirmation ||
                                backStack.lastOrNull() == HeartGuardDestination.HeadquartersContact
                            ) {
                                backStack.removeLastOrNull()
                            }
                        },
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
                        onHomeClick = ::leaveRecordFlow,
                        onProfileEditClick = { backStack.add(HeartGuardDestination.ProfileEdit) },
                        onInquiryClick = { backStack.add(HeartGuardDestination.Inquiry) },
                        onSiteSelectionClick = {
                            backStack.add(HeartGuardDestination.TeamSiteSelection)
                        },
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
                        onHomeClick = ::leaveRecordFlow,
                        onProfileEditClick = { backStack.add(HeartGuardDestination.ProfileEdit) },
                        onInquiryClick = { backStack.add(HeartGuardDestination.Inquiry) },
                        onSiteSelectionClick = {
                            backStack.add(HeartGuardDestination.TeamSiteSelection)
                        },
                        onWithdrawClick = { backStack.add(HeartGuardDestination.WithdrawNotice) },
                        onNotificationsClick = { backStack.add(HeartGuardDestination.TeamNotifications) },
                    ) { onMenuClick, onNotificationClick ->
                        HeartGuardFieldPhotoRoute(
                            onMenuClick = onMenuClick,
                            onNotificationClick = onNotificationClick,
                            recordDraftViewModel = recordDraftViewModel,
                            onSaveSuccess = ::goToSaveSuccess,
                            onSaveFailure = ::goToSaveFailure,
                        )
                    }
                }
                entry<HeartGuardDestination.WorkPhoto> {
                    MenuDrawerHost(
                        onHomeClick = ::leaveRecordFlow,
                        onProfileEditClick = { backStack.add(HeartGuardDestination.ProfileEdit) },
                        onInquiryClick = { backStack.add(HeartGuardDestination.Inquiry) },
                        onSiteSelectionClick = {
                            backStack.add(HeartGuardDestination.TeamSiteSelection)
                        },
                        onWithdrawClick = { backStack.add(HeartGuardDestination.WithdrawNotice) },
                        onNotificationsClick = { backStack.add(HeartGuardDestination.TeamNotifications) },
                    ) { onMenuClick, onNotificationClick ->
                        HeartGuardWorkPhotoRoute(
                            onMenuClick = onMenuClick,
                            onNotificationClick = onNotificationClick,
                            recordDraftViewModel = recordDraftViewModel,
                            onSaveSuccess = ::goToSaveSuccess,
                            onSaveFailure = ::goToSaveFailure,
                        )
                    }
                }
                entry<HeartGuardDestination.RestPhoto> {
                    MenuDrawerHost(
                        onHomeClick = ::leaveRecordFlow,
                        onProfileEditClick = { backStack.add(HeartGuardDestination.ProfileEdit) },
                        onInquiryClick = { backStack.add(HeartGuardDestination.Inquiry) },
                        onSiteSelectionClick = {
                            backStack.add(HeartGuardDestination.TeamSiteSelection)
                        },
                        onWithdrawClick = { backStack.add(HeartGuardDestination.WithdrawNotice) },
                        onNotificationsClick = { backStack.add(HeartGuardDestination.TeamNotifications) },
                    ) { onMenuClick, onNotificationClick ->
                        HeartGuardRestPhotoRoute(
                            onMenuClick = onMenuClick,
                            onNotificationClick = onNotificationClick,
                            recordDraftViewModel = recordDraftViewModel,
                            onSaveSuccess = ::goToSaveSuccess,
                            onSaveFailure = ::goToSaveFailure,
                        )
                    }
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
