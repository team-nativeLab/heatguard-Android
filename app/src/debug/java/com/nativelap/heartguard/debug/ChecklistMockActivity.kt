package com.nativelap.heartguard.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nativelap.heartguard.domain.checklist.model.ChecklistRoundStatus
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.view.screen.checklist.ChecklistOverviewScreen
import com.nativelap.heartguard.viewmodel.checklist.ChecklistRoundCardUiModel
import com.nativelap.heartguard.viewmodel.checklist.ChecklistRoundsOverviewUiModel
import com.nativelap.heartguard.viewmodel.checklist.ChecklistRoundsUiState
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import android.graphics.Color as AndroidColor

internal class ChecklistMockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.light(
                    scrim = AndroidColor.TRANSPARENT,
                    darkScrim = AndroidColor.TRANSPARENT,
                ),
            navigationBarStyle =
                SystemBarStyle.light(
                    scrim = AndroidColor.TRANSPARENT,
                    darkScrim = AndroidColor.TRANSPARENT,
                ),
        )

        val roundsUiState = createChecklistMockRoundsUiState()

        setContent {
            HeartGuardTheme {
                ChecklistOverviewScreen(
                    schedule = null,
                    siteLabel = CHECKLIST_MOCK_SITE_LABEL,
                    isLoading = false,
                    hasLoadError = false,
                    onBackClick = {},
                    onRetryClick = {},
                    onPeriodClick = {},
                    roundsUiState = roundsUiState,
                )
            }
        }
    }
}

private fun createChecklistMockRoundsUiState(): ChecklistRoundsUiState {
    val date = LocalDate.now(KOREA_ZONE)
    val rounds =
        listOf(
            createChecklistMockRound(date, 8, ChecklistRoundStatus.COMPLETED, 6, 6, completedMinute = 12),
            createChecklistMockRound(date, 10, ChecklistRoundStatus.COMPLETED, 6, 6, completedMinute = 5),
            createChecklistMockRound(date, 12, ChecklistRoundStatus.MISSED, 6, 3),
            createChecklistMockRound(date, 14, ChecklistRoundStatus.COMPLETED, 6, 6, completedMinute = 20),
            createChecklistMockRound(date, 16, ChecklistRoundStatus.MISSED, 6, 0),
            createChecklistMockRound(date, 18, ChecklistRoundStatus.COMPLETED, 6, 6, completedMinute = 9),
            createChecklistMockRound(date, 20, ChecklistRoundStatus.IN_PROGRESS, 6, 3),
            createChecklistMockRound(date, 22, ChecklistRoundStatus.SCHEDULED, 6, 0),
        )

    return ChecklistRoundsUiState(
        date = date,
        rounds =
            ChecklistRoundsOverviewUiModel(
                date = date,
                teamId = "debug-mock-team",
                totalRounds = rounds.size,
                completedRounds = 4,
                missedRounds = 2,
                remainingRounds = 2,
                progressPercent = 50,
                rounds = rounds,
            ),
    )
}

private fun createChecklistMockRound(
    date: LocalDate,
    startHour: Int,
    status: ChecklistRoundStatus,
    totalItems: Int,
    checkedItems: Int,
    completedMinute: Int? = null,
): ChecklistRoundCardUiModel {
    val startsAt = date.atTime(LocalTime.of(startHour, 0)).atZone(KOREA_ZONE).toOffsetDateTime()
    val endsAt = startsAt.plus(59, ChronoUnit.MINUTES)
    val completedAt = completedMinute?.let { minute -> startsAt.plusMinutes(minute.toLong()) }

    return ChecklistRoundCardUiModel(
        roundId = "debug-round-$startHour",
        startsAt = startsAt,
        endsAt = endsAt,
        status = status,
        totalItems = totalItems,
        checkedItems = checkedItems,
        progressPercent = if (totalItems == 0) 0 else checkedItems * 100 / totalItems,
        completedAt = completedAt,
    )
}

private const val CHECKLIST_MOCK_SITE_LABEL = "인천 복합물류센터 신축 · A팀"
private val KOREA_ZONE: ZoneId = ZoneId.of("Asia/Seoul")
