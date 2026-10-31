package com.nativelap.heartguard.viewmodel.checklist

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.domain.checklist.model.ChecklistRoundItemCheckUpdate
import com.nativelap.heartguard.domain.checklist.model.ChecklistRoundStatus
import com.nativelap.heartguard.domain.checklist.model.ChecklistRoundSummary
import com.nativelap.heartguard.domain.checklist.model.TeamChecklistRoundDetail
import com.nativelap.heartguard.domain.checklist.model.TeamChecklistRoundItem
import com.nativelap.heartguard.domain.checklist.model.TeamChecklistRoundSummary
import com.nativelap.heartguard.domain.checklist.model.TeamChecklistRounds
import com.nativelap.heartguard.domain.checklist.repository.TeamChecklistRepository
import com.nativelap.heartguard.domain.checklist.usecase.GetTeamChecklistRoundUseCase
import com.nativelap.heartguard.domain.checklist.usecase.GetTeamChecklistRoundsUseCase
import com.nativelap.heartguard.domain.checklist.usecase.SetTeamChecklistRoundItemCheckedUseCase
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ChecklistRoundsRefreshTest {
    @Test
    fun `detail retry target survives when failed load has no detail`() {
        val retryTarget =
            resolveChecklistRoundRetryTarget(
                hasLoadError = true,
                isLoading = false,
                requestedRoundId = "08:00",
                requestedRoundDate = LocalDate.parse("2026-10-30"),
            )

        assertEquals("08:00", retryTarget?.roundId)
        assertEquals(LocalDate.parse("2026-10-30"), retryTarget?.date)
    }

    @Test
    fun `detail retry is not scheduled without an error or while loading`() {
        assertNull(
            resolveChecklistRoundRetryTarget(
                hasLoadError = false,
                isLoading = false,
                requestedRoundId = "08:00",
                requestedRoundDate = LocalDate.parse("2026-10-30"),
            ),
        )
        assertNull(
            resolveChecklistRoundRetryTarget(
                hasLoadError = true,
                isLoading = true,
                requestedRoundId = "08:00",
                requestedRoundDate = LocalDate.parse("2026-10-30"),
            ),
        )
    }

    @Test
    fun `edit gate follows Seoul local date and inclusive start exclusive end`() {
        val seoulZone = ZoneId.of("Asia/Seoul")
        val roundDate = LocalDate.parse("2026-10-30")
        val startsAt = OffsetDateTime.parse("2026-10-30T08:00:00+09:00")
        val endsAt = OffsetDateTime.parse("2026-10-30T10:00:00+09:00")
        val atStart = Clock.fixed(Instant.parse("2026-10-29T23:00:00Z"), seoulZone)
        val atEnd = Clock.fixed(Instant.parse("2026-10-30T01:00:00Z"), seoulZone)
        val nextSeoulDay = Clock.fixed(Instant.parse("2026-10-30T15:00:00Z"), seoulZone)

        assertTrue(isChecklistRoundEditable(roundDate, ChecklistRoundStatus.IN_PROGRESS, startsAt, endsAt, atStart))
        assertTrue(!isChecklistRoundEditable(roundDate, ChecklistRoundStatus.IN_PROGRESS, startsAt, endsAt, atEnd))
        assertTrue(
            !isChecklistRoundEditable(roundDate, ChecklistRoundStatus.IN_PROGRESS, startsAt, endsAt, nextSeoulDay),
        )
        assertTrue(!isChecklistRoundEditable(roundDate, ChecklistRoundStatus.SCHEDULED, startsAt, endsAt, atStart))
    }

    @Test
    fun `refresh wakes at round boundary and otherwise stays under sixty seconds`() {
        val now = Instant.parse("2026-10-29T23:59:00Z")
        val startsAt = Instant.parse("2026-10-30T00:00:00Z")

        assertEquals(60_000L, calculateRoundRefreshDelayMillis(now, startsAt, null))
        assertEquals(30_000L, calculateRoundRefreshDelayMillis(now, now.plusSeconds(30), null))
        assertEquals(60_000L, calculateRoundRefreshDelayMillis(now, null, now.minusSeconds(1)))
    }

    @Test
    fun `detail request generation prevents an older get from overwriting post-save refresh`() {
        val generation = ChecklistRequestGeneration()
        val delayedBeforeSave = generation.begin()

        generation.invalidate()
        val postSaveRefresh = generation.begin()
        var checkedValueInUi = false

        if (generation.isCurrent(postSaveRefresh)) {
            checkedValueInUi = true
        }
        if (generation.isCurrent(delayedBeforeSave)) {
            checkedValueInUi = false
        }

        assertTrue(checkedValueInUi)
        assertTrue(generation.isCurrent(postSaveRefresh))
        assertFalse(generation.isCurrent(delayedBeforeSave))
    }

    @Test
    fun `detail polling does not start another request while saving`() {
        assertFalse(shouldStartChecklistRoundDetailLoad(isSaving = true, isLoadingSameDetail = false))
        assertFalse(shouldStartChecklistRoundDetailLoad(isSaving = false, isLoadingSameDetail = true))
        assertTrue(shouldStartChecklistRoundDetailLoad(isSaving = false, isLoadingSameDetail = false))
        assertFalse(shouldStartChecklistRoundsLoad(isSaving = true))
        assertTrue(shouldStartChecklistRoundsLoad(isSaving = false))
    }

    @Test
    fun `pre-save rounds response cannot overwrite refreshed summary`() {
        val generation = ChecklistRequestGeneration()
        val delayedBeforeSave = generation.begin()

        generation.invalidate()
        val postSaveRefresh = generation.begin()
        var summary = "old"

        if (generation.isCurrent(postSaveRefresh)) {
            summary = "updated"
        }
        if (generation.isCurrent(delayedBeforeSave)) {
            summary = "stale"
        }

        assertEquals("updated", summary)
        assertFalse(generation.isCurrent(delayedBeforeSave))
        assertTrue(generation.isCurrent(postSaveRefresh))
    }

    @Test
    fun `post-save detail and rounds refresh start together and keep independent outcomes`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val date = LocalDate.parse("2026-10-30")
        val firstDetailResult = CompletableDeferred<ApiResult<TeamChecklistRoundDetail>>()
        val firstRoundsResult = CompletableDeferred<ApiResult<TeamChecklistRounds>>()
        val secondDetailResult = CompletableDeferred<ApiResult<TeamChecklistRoundDetail>>()
        val secondRoundsResult = CompletableDeferred<ApiResult<TeamChecklistRounds>>()
        val firstDetailStarted = CompletableDeferred<Unit>()
        val firstRoundsStarted = CompletableDeferred<Unit>()
        val secondDetailStarted = CompletableDeferred<Unit>()
        val secondRoundsStarted = CompletableDeferred<Unit>()
        var detailRequestCount = 0
        var roundsRequestCount = 0
        val repository =
            FakeTeamChecklistRepository(
                getRound = { roundId, requestDate ->
                    detailRequestCount += 1
                    when (detailRequestCount) {
                        1 -> ApiResult.Success(roundDetail(requestDate, roundId))
                        2 -> {
                            firstDetailStarted.complete(Unit)
                            firstDetailResult.await()
                        }
                        else -> {
                            secondDetailStarted.complete(Unit)
                            secondDetailResult.await()
                        }
                    }
                },
                getRounds = { requestDate ->
                    roundsRequestCount += 1
                    when (roundsRequestCount) {
                        1 -> ApiResult.Success(roundsOverview(requestDate))
                        2 -> {
                            firstRoundsStarted.complete(Unit)
                            firstRoundsResult.await()
                        }
                        else -> {
                            secondRoundsStarted.complete(Unit)
                            secondRoundsResult.await()
                        }
                    }
                },
            )
        val (viewModel, sessionManager) = createLoadedViewModel(repository, dispatcher, date)

        try {
            viewModel.setRoundItemChecked(ROUND_ID, date, ITEM_ID, isChecked = true)
            runCurrent()

            assertTrue(firstDetailStarted.isCompleted)
            assertTrue(firstRoundsStarted.isCompleted)

            firstDetailResult.complete(ApiResult.Failure(ApiError.Network))
            runCurrent()

            assertTrue(viewModel.uiState.value.hasDetailLoadError)
            assertTrue(viewModel.uiState.value.isLoadingRounds)

            firstRoundsResult.complete(ApiResult.Success(roundsOverview(date, progressPercent = 100)))
            runCurrent()

            assertFalse(viewModel.uiState.value.hasRoundsLoadError)
            assertFalse(viewModel.uiState.value.isLoadingRounds)
            assertEquals(100, viewModel.uiState.value.rounds?.progressPercent)
            assertTrue(viewModel.uiState.value.hasDetailLoadError)

            viewModel.setRoundItemChecked(ROUND_ID, date, ITEM_ID, isChecked = false)
            runCurrent()

            assertTrue(secondDetailStarted.isCompleted)
            assertTrue(secondRoundsStarted.isCompleted)

            secondDetailResult.complete(
                ApiResult.Success(
                    roundDetail(date, ROUND_ID, checked = false),
                ),
            )
            secondRoundsResult.complete(ApiResult.Failure(ApiError.Network))
            runCurrent()

            assertFalse(viewModel.uiState.value.hasDetailLoadError)
            assertEquals(0, viewModel.uiState.value.roundDetail?.checkedItems)
            assertTrue(viewModel.uiState.value.hasRoundsLoadError)
            assertFalse(viewModel.uiState.value.isLoadingDetail)
            assertFalse(viewModel.uiState.value.isLoadingRounds)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `session change clears pending parallel refresh responses`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        val date = LocalDate.parse("2026-10-30")
        val detailStarted = CompletableDeferred<Unit>()
        val roundsStarted = CompletableDeferred<Unit>()
        val detailResult = CompletableDeferred<ApiResult<TeamChecklistRoundDetail>>()
        val roundsResult = CompletableDeferred<ApiResult<TeamChecklistRounds>>()
        var detailRequestCount = 0
        var roundsRequestCount = 0
        val repository =
            FakeTeamChecklistRepository(
                getRound = { roundId, requestDate ->
                    detailRequestCount += 1
                    if (detailRequestCount == 1) {
                        ApiResult.Success(roundDetail(requestDate, roundId))
                    } else {
                        detailStarted.complete(Unit)
                        detailResult.await()
                    }
                },
                getRounds = { requestDate ->
                    roundsRequestCount += 1
                    if (roundsRequestCount == 1) {
                        ApiResult.Success(roundsOverview(requestDate))
                    } else {
                        roundsStarted.complete(Unit)
                        roundsResult.await()
                    }
                },
            )
        val (viewModel, sessionManager) = createLoadedViewModel(repository, dispatcher, date)

        try {
            viewModel.setRoundItemChecked(ROUND_ID, date, ITEM_ID, isChecked = true)
            runCurrent()

            assertTrue(detailStarted.isCompleted)
            assertTrue(roundsStarted.isCompleted)

            sessionManager.onLoginSucceeded(SessionToken("next-session-token"))
            runCurrent()

            detailResult.complete(ApiResult.Success(roundDetail(date, ROUND_ID, checked = true)))
            roundsResult.complete(ApiResult.Success(roundsOverview(date, progressPercent = 100)))
            runCurrent()

            assertNull(viewModel.uiState.value.roundDetail)
            assertNull(viewModel.uiState.value.rounds)
            assertNull(viewModel.uiState.value.date)
            assertFalse(viewModel.uiState.value.hasDetailLoadError)
            assertFalse(viewModel.uiState.value.hasRoundsLoadError)
        } finally {
            viewModel.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private suspend fun TestScope.createLoadedViewModel(
        repository: FakeTeamChecklistRepository,
        dispatcher: kotlinx.coroutines.CoroutineDispatcher,
        date: LocalDate,
    ): Pair<ChecklistRoundsViewModel, SessionManager> {
        val sessionManager = SessionManager(TestTokenStorage(), dispatcher)
        sessionManager.onLoginSucceeded(SessionToken("initial-session-token"))
        val clock =
            Clock.fixed(
                Instant.parse("2026-10-30T00:30:00Z"),
                ZoneId.of("Asia/Seoul"),
            )
        val viewModel =
            ChecklistRoundsViewModel(
                getTeamChecklistRoundsUseCase = GetTeamChecklistRoundsUseCase(repository),
                getTeamChecklistRoundUseCase = GetTeamChecklistRoundUseCase(repository),
                setTeamChecklistRoundItemCheckedUseCase = SetTeamChecklistRoundItemCheckedUseCase(repository),
                sessionManager = sessionManager,
                clock = clock,
            )
        viewModel.loadRounds(date)
        viewModel.loadRound(ROUND_ID, date)
        runCurrent()
        return viewModel to sessionManager
    }

    private fun roundDetail(
        date: LocalDate,
        roundId: String,
        checked: Boolean = false,
    ) =
        TeamChecklistRoundDetail(
            date = date,
            teamId = "team-1",
            roundId = roundId,
            startsAt = OffsetDateTime.parse("2026-10-30T08:00:00+09:00"),
            endsAt = OffsetDateTime.parse("2026-10-30T10:00:00+09:00"),
            status = ChecklistRoundStatus.IN_PROGRESS,
            totalItems = 1,
            checkedItems = if (checked) 1 else 0,
            progressPercent = if (checked) 100 else 0,
            completedAt = null,
            items =
                listOf(
                    TeamChecklistRoundItem(
                        itemId = ITEM_ID,
                        text = "작업 상태 확인",
                        sortOrder = 0,
                        checked = checked,
                        checkedAt = null,
                    ),
                ),
        )

    private fun roundsOverview(
        date: LocalDate,
        progressPercent: Int = 0,
    ) =
        TeamChecklistRounds(
            date = date,
            teamId = "team-1",
            summary =
                ChecklistRoundSummary(
                    totalRounds = 1,
                    completedRounds = if (progressPercent == 100) 1 else 0,
                    missedRounds = 0,
                    remainingRounds = if (progressPercent == 100) 0 else 1,
                    progressPercent = progressPercent,
                ),
            items =
                listOf(
                    TeamChecklistRoundSummary(
                        roundId = ROUND_ID,
                        startsAt = OffsetDateTime.parse("2026-10-30T08:00:00+09:00"),
                        endsAt = OffsetDateTime.parse("2026-10-30T10:00:00+09:00"),
                        status = if (progressPercent == 100) ChecklistRoundStatus.COMPLETED else ChecklistRoundStatus.IN_PROGRESS,
                        totalItems = 1,
                        checkedItems = if (progressPercent == 100) 1 else 0,
                        progressPercent = progressPercent,
                        completedAt = null,
                    ),
                ),
        )

    private class FakeTeamChecklistRepository(
        private val getRound: suspend (String, LocalDate) -> ApiResult<TeamChecklistRoundDetail>,
        private val getRounds: suspend (LocalDate) -> ApiResult<TeamChecklistRounds>,
    ) : TeamChecklistRepository {
        override suspend fun getChecklist() = error("Checklist is not used in this test")

        override suspend fun setItemChecked(
            itemId: String,
            checked: Boolean,
        ) = error("Checklist item update is not used in this test")

        override suspend fun getRounds(date: LocalDate): ApiResult<TeamChecklistRounds> = getRounds.invoke(date)

        override suspend fun getRound(
            roundId: String,
            date: LocalDate,
        ): ApiResult<TeamChecklistRoundDetail> = getRound.invoke(roundId, date)

        override suspend fun setRoundItemChecked(
            roundId: String,
            itemId: String,
            checked: Boolean,
        ) =
            ApiResult.Success(
                ChecklistRoundItemCheckUpdate(
                    date = LocalDate.parse("2026-10-30"),
                    roundId = roundId,
                    itemId = itemId,
                    checked = checked,
                    checkedAt = null,
                ),
            )
    }

    private class TestTokenStorage : TokenStorage {
        override fun readAccessToken(): String? = null

        override fun saveAccessToken(accessToken: String) = Unit

        override fun clear() = Unit
    }

    private companion object {
        const val ROUND_ID = "08:00"
        const val ITEM_ID = "item-1"
    }
}
