package com.nativelap.heartguard.viewmodel.checklist

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.createTestSessionManager
import com.nativelap.heartguard.domain.checklist.model.ChecklistItemCheckUpdate
import com.nativelap.heartguard.domain.checklist.model.TeamChecklist
import com.nativelap.heartguard.domain.checklist.model.TeamChecklistItem
import com.nativelap.heartguard.domain.checklist.repository.TeamChecklistRepository
import com.nativelap.heartguard.domain.checklist.usecase.GetTeamChecklistUseCase
import com.nativelap.heartguard.domain.checklist.usecase.SetChecklistItemCheckedUseCase
import com.nativelap.heartguard.domain.site.model.CheckSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ChecklistViewModelTest {
    private val fixedClock = Clock.fixed(Instant.parse("2026-10-19T03:00:00Z"), ZoneId.of("Asia/Seoul"))

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `조회 성공 응답의 서버 항목과 체크 상태를 노출한다`() =
        runTest {
            val repository = FakeTeamChecklistRepository(checklist(items = listOf(checklistItem(checked = true))))
            val viewModel = createViewModel(repository)

            viewModel.loadChecklist()
            advanceUntilIdle()

            assertEquals(
                listOf("item_01"),
                viewModel.uiState.value.items
                    .map(ChecklistItemUiModel::itemId),
            )
            assertTrue(
                viewModel.uiState.value.items
                    .single()
                    .isChecked,
            )
            assertTrue(viewModel.uiState.value.isComplete)
        }

    @Test
    fun `빈 서버 목록은 완료로 취급하지 않는다`() =
        runTest {
            val viewModel = createViewModel(FakeTeamChecklistRepository(checklist(items = emptyList())))

            viewModel.loadChecklist()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.hasLoaded)
            assertTrue(
                viewModel.uiState.value.items
                    .isEmpty(),
            )
            assertFalse(viewModel.uiState.value.isComplete)
        }

    @Test
    fun `조회 실패 후 재시도하면 서버 목록을 표시한다`() =
        runTest {
            val repository = FakeTeamChecklistRepository(ApiResult.Failure(ApiError.Network))
            val viewModel = createViewModel(repository)
            viewModel.loadChecklist()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.hasLoadError)

            repository.checklistResult = checklist(items = listOf(checklistItem(checked = false)))
            viewModel.retryLoad()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.hasLoadError)
            assertEquals(
                "식수 확인",
                viewModel.uiState.value.items
                    .single()
                    .title,
            )
        }

    @Test
    fun `요청한 날짜와 다른 날짜의 응답은 표시하지 않고 오류로 처리한다`() =
        runTest {
            val repository =
                FakeTeamChecklistRepository(
                    ApiResult.Success(
                        TeamChecklist(
                            date = LocalDate.of(2026, 10, 20),
                            items = listOf(checklistItem(checked = true)),
                        ),
                    ),
                )
            val viewModel = createViewModel(repository)

            viewModel.loadChecklist()
            advanceUntilIdle()

            assertEquals(LocalDate.of(2026, 10, 19), viewModel.uiState.value.date)
            assertTrue(viewModel.uiState.value.hasLoadError)
            assertTrue(
                viewModel.uiState.value.items
                    .isEmpty(),
            )
            assertFalse(viewModel.uiState.value.hasLoaded)
        }

    @Test
    fun `일정 UI 모델은 체크 시각의 분 단위도 보존한다`() =
        runTest {
            val viewModel = createViewModel(FakeTeamChecklistRepository(checklist(items = emptyList())))
            val checkTimes = listOf(LocalTime.of(9, 0), LocalTime.of(9, 30))

            viewModel.setCheckSchedule(
                CheckSchedule(
                    checkTimes = checkTimes,
                    nextCheckTime = checkTimes.first(),
                    minutesUntilNextCheck = 0,
                ),
            )

            assertEquals(
                checkTimes,
                viewModel.uiState.value.periods
                    .map(ChecklistPeriodUiModel::time),
            )
        }

    @Test
    fun `체크 변경 성공은 서버 응답을 반영한다`() =
        runTest {
            val repository = FakeTeamChecklistRepository(checklist(items = listOf(checklistItem(checked = false))))
            repository.updateResult = ApiResult.Success(ChecklistItemCheckUpdate("item_01", true, null))
            val viewModel = createViewModel(repository)
            viewModel.loadChecklist()
            advanceUntilIdle()

            viewModel.setChecklistItemChecked("item_01", true)
            viewModel.setChecklistItemChecked("item_01", true)
            advanceUntilIdle()

            assertTrue(
                viewModel.uiState.value.items
                    .single()
                    .isChecked,
            )
            assertTrue(viewModel.uiState.value.isComplete)
            assertEquals(listOf(true), repository.requestedCheckedValues)
        }

    @Test
    fun `완료된 항목 해제 성공도 서버 결과에 반영한다`() =
        runTest {
            val repository = FakeTeamChecklistRepository(checklist(items = listOf(checklistItem(checked = true))))
            repository.updateResult = ApiResult.Success(ChecklistItemCheckUpdate("item_01", false, null))
            val viewModel = createViewModel(repository)
            viewModel.loadChecklist()
            advanceUntilIdle()

            viewModel.setChecklistItemChecked("item_01", false)
            advanceUntilIdle()

            assertFalse(
                viewModel.uiState.value.items
                    .single()
                    .isChecked,
            )
            assertFalse(viewModel.uiState.value.isComplete)
            assertEquals(listOf(false), repository.requestedCheckedValues)
        }

    @Test
    fun `체크 변경 실패는 값을 되돌리고 오류를 표시한다`() =
        runTest {
            val repository = FakeTeamChecklistRepository(checklist(items = listOf(checklistItem(checked = false))))
            repository.updateResult = ApiResult.Failure(ApiError.Network)
            val viewModel = createViewModel(repository)
            viewModel.loadChecklist()
            advanceUntilIdle()

            viewModel.setChecklistItemChecked("item_01", true)
            advanceUntilIdle()

            assertFalse(
                viewModel.uiState.value.items
                    .single()
                    .isChecked,
            )
            assertTrue(viewModel.uiState.value.hasSaveError)
            assertFalse(viewModel.uiState.value.isComplete)
        }

    private suspend fun TestScope.createViewModel(repository: FakeTeamChecklistRepository): ChecklistViewModel {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val sessionManager = createTestSessionManager(StandardTestDispatcher(testScheduler))
        sessionManager.onLoginSucceeded(SessionToken("test-token"))
        return ChecklistViewModel(
            getTeamChecklistUseCase = GetTeamChecklistUseCase(repository),
            setChecklistItemCheckedUseCase = SetChecklistItemCheckedUseCase(repository),
            sessionManager = sessionManager,
            clock = fixedClock,
        )
    }

    private fun checklist(items: List<TeamChecklistItem>): ApiResult<TeamChecklist> =
        ApiResult.Success(TeamChecklist(date = LocalDate.of(2026, 10, 19), items = items))

    private fun checklistItem(checked: Boolean) =
        TeamChecklistItem(
            itemId = "item_01",
            text = "식수 확인",
            sortOrder = 1,
            checked = checked,
            checkedAt = null,
        )

    private class FakeTeamChecklistRepository(
        var checklistResult: ApiResult<TeamChecklist>,
    ) : TeamChecklistRepository {
        var updateResult: ApiResult<ChecklistItemCheckUpdate> =
            ApiResult.Success(
                ChecklistItemCheckUpdate("item_01", true, OffsetDateTime.parse("2026-10-19T12:00:00+09:00")),
            )
        val requestedCheckedValues = mutableListOf<Boolean>()

        override suspend fun getChecklist(date: LocalDate): ApiResult<TeamChecklist> = checklistResult

        override suspend fun setItemChecked(
            itemId: String,
            checked: Boolean,
        ): ApiResult<ChecklistItemCheckUpdate> {
            requestedCheckedValues += checked
            return updateResult
        }
    }
}
