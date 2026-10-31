package com.nativelap.heartguard.navigation

import androidx.activity.ComponentActivity
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelStore
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.inquiry.model.InquiryPage
import com.nativelap.heartguard.domain.inquiry.model.InquirySubmission
import com.nativelap.heartguard.domain.inquiry.repository.InquiryRepository
import com.nativelap.heartguard.domain.inquiry.usecase.GetInquiriesUseCase
import com.nativelap.heartguard.domain.inquiry.usecase.SubmitInquiryUseCase
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.route.inquiry.HeartGuardInquiryRegistrationRoute
import com.nativelap.heartguard.view.route.inquiry.HeartGuardInquiryRoute
import com.nativelap.heartguard.viewmodel.inquiry.InquiryViewModel
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class InquiryRegistrationNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun successfulSubmissionPopsRegistrationAndLoadsListOnceOnResume() {
        val repository = CountingInquiryRepository()
        lateinit var viewModel: InquiryViewModel
        val viewModelStore = ViewModelStore()

        composeRule.activity.runOnUiThread {
            viewModel =
                InquiryViewModel(
                    submitInquiryUseCase = SubmitInquiryUseCase(repository),
                    getInquiriesUseCase = GetInquiriesUseCase(repository),
                    sessionManager = SessionManager(InMemoryTokenStorage(), Dispatchers.Unconfined),
                )
            viewModelStore.put("inquiry-test", viewModel)
        }

        try {
            composeRule.setContent {
                HeartGuardTheme {
                    InquiryTestNavHost(viewModel)
                }
            }

            composeRule.waitUntil(timeoutMillis = 5_000) {
                repository.getCallCount.get() == 1
            }
            composeRule.onNodeWithText("문의 등록하기").performClick()
            composeRule.onNodeWithContentDescription("제목").performTextInput("테스트 제목")
            composeRule.onNodeWithContentDescription("내용").performTextInput("테스트 문의 내용")
            composeRule.onAllNodesWithText("문의 등록")[1].performClick()

            composeRule.waitUntil(timeoutMillis = 5_000) {
                repository.submitCallCount.get() == 1 && repository.getCallCount.get() == 2
            }
            composeRule.onNodeWithText("내 문의 목록").assertIsDisplayed()
            composeRule.onNodeWithText("문의 등록하기").assertIsDisplayed()
            assertEquals(1, repository.submitCallCount.get())
            assertEquals(2, repository.getCallCount.get())
        } finally {
            composeRule.activity.runOnUiThread {
                viewModelStore.clear()
            }
        }
    }

    @Composable
    private fun InquiryTestNavHost(viewModel: InquiryViewModel) {
        val backStack = rememberNavBackStack(HeartGuardDestination.Inquiry)

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
                    entry<HeartGuardDestination.Inquiry> {
                        HeartGuardInquiryRoute(
                            onBackClick = { backStack.removeLastOrNull() },
                            onRegisterClick = {
                                backStack.add(HeartGuardDestination.InquiryRegistration)
                            },
                            inquiryViewModel = viewModel,
                        )
                    }
                    entry<HeartGuardDestination.InquiryRegistration> {
                        HeartGuardInquiryRegistrationRoute(
                            onBackClick = { backStack.removeLastOrNull() },
                            inquiryViewModel = viewModel,
                        )
                    }
                },
        )
    }

    private class CountingInquiryRepository : InquiryRepository {
        val submitCallCount = AtomicInteger(0)
        val getCallCount = AtomicInteger(0)

        override suspend fun submitInquiry(
            title: String,
            content: String,
        ): ApiResult<InquirySubmission> {
            submitCallCount.incrementAndGet()
            return ApiResult.Success(SUBMISSION)
        }

        override suspend fun getInquiryPage(cursor: String?): ApiResult<InquiryPage> {
            getCallCount.incrementAndGet()
            return ApiResult.Success(InquiryPage(inquiries = emptyList(), nextCursor = null))
        }
    }

    private class InMemoryTokenStorage : TokenStorage {
        override fun readAccessToken(): String? = null

        override fun saveAccessToken(accessToken: String) = Unit

        override fun clear() = Unit
    }

    private companion object {
        val SUBMISSION = InquirySubmission("inquiry-test", "OPEN", "PENDING", "2026-10-31T12:00:00Z")
    }
}
