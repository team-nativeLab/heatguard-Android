package com.nativelap.heartguard.view.screen.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.notification.model.NotificationCategory
import com.nativelap.heartguard.domain.notification.model.NotificationType
import com.nativelap.heartguard.domain.notification.model.TeamNotification
import com.nativelap.heartguard.ui.theme.HeartGuardBorderWidth
import com.nativelap.heartguard.ui.theme.HeartGuardIconSize
import com.nativelap.heartguard.ui.theme.HeartGuardRadius
import com.nativelap.heartguard.ui.theme.HeartGuardSpacing
import com.nativelap.heartguard.ui.theme.HeartGuardTheme
import com.nativelap.heartguard.ui.theme.extraColors
import com.nativelap.heartguard.view.component.ResponsivePageContent
import com.nativelap.heartguard.view.component.account.WithdrawTopBar
import com.nativelap.heartguard.view.component.history.koreanDateFormatter
import com.nativelap.heartguard.viewmodel.notification.TeamNotificationScreenEvent
import com.nativelap.heartguard.viewmodel.notification.TeamNotificationUiState
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun TeamNotificationScreen(
    uiState: TeamNotificationUiState,
    onEvent: (TeamNotificationScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = SnackbarHostState(),
) {
    val listState = rememberLazyListState()
    LaunchedEffect(
        listState,
        uiState.notifications.size,
        uiState.hasMore,
        uiState.isLoadingMore,
        uiState.hasLoadMoreError,
    ) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            layoutInfo.totalItemsCount > 0 && lastVisibleIndex >= layoutInfo.totalItemsCount - PAGINATION_PREFETCH_ITEMS
        }
            .distinctUntilChanged()
            .collect { shouldLoadMore ->
                if (shouldLoadMore && uiState.hasMore && !uiState.isLoadingMore && !uiState.hasLoadMoreError) {
                    onEvent(TeamNotificationScreenEvent.LoadMore)
                }
            }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.extraColors.pageBackground,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        ResponsivePageContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            WithdrawTopBar(
                title = stringResource(R.string.notification_title),
                backContentDescription = stringResource(R.string.common_back_description),
                onBackClick = { onEvent(TeamNotificationScreenEvent.BackClicked) },
                modifier = Modifier.padding(top = HeartGuardSpacing.Compact),
            )

            NotificationCategoryFilters(
                selectedCategory = uiState.selectedCategory,
                onCategoryClick = { category -> onEvent(TeamNotificationScreenEvent.CategorySelected(category)) },
            )

            when {
                uiState.isInitialLoading || !uiState.hasLoaded -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.hasInitialError -> {
                    NotificationLoadError(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = HeartGuardSpacing.AccountContentHorizontal),
                        onRetryClick = { onEvent(TeamNotificationScreenEvent.RetryInitialLoad) },
                    )
                }

                uiState.notifications.isEmpty() -> {
                    NotificationEmptyState(
                        category = uiState.selectedCategory,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }

                else -> {
                    val today = LocalDate.now()
                    val notificationGroups = uiState.notifications.groupBy { notification ->
                        notification.createdAt
                            ?.atZoneSameInstant(ZoneId.systemDefault())
                            ?.toLocalDate()
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = HeartGuardSpacing.AccountContentHorizontal,
                            end = HeartGuardSpacing.AccountContentHorizontal,
                            top = 14.dp,
                            bottom = HeartGuardSpacing.Section,
                        ),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        notificationGroups.forEach { (date, notifications) ->
                            item(key = "notification-day-$date") {
                                NotificationDateHeader(date = date, today = today)
                            }
                            item(key = "notification-group-$date") {
                                NotificationGroupCard(
                                    notifications = notifications,
                                    markingReadIds = uiState.markingReadIds,
                                    onNotificationClick = { notification ->
                                        onEvent(TeamNotificationScreenEvent.NotificationClicked(notification.notificationId))
                                    },
                                )
                            }
                        }
                        if (uiState.isLoadingMore) {
                            item(key = "notification-loading-more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(HeartGuardSpacing.Item),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        } else if (uiState.hasLoadMoreError) {
                            item(key = "notification-load-more-error") {
                                TextButton(
                                    onClick = { onEvent(TeamNotificationScreenEvent.RetryLoadMore) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(text = stringResource(R.string.notification_load_more_retry))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationDateHeader(
    date: LocalDate?,
    today: LocalDate,
) {
    val dateLabel = if (date == null) {
        stringResource(R.string.history_day_unknown)
    } else {
        val formattedDate = date.format(koreanDateFormatter(stringResource(R.string.history_day_format)))
        when (date) {
            today -> stringResource(R.string.history_day_today_format, formattedDate)
            today.minusDays(1) -> stringResource(R.string.history_day_yesterday_format, formattedDate)
            else -> formattedDate
        }
    }
    Text(
        text = dateLabel,
        modifier = Modifier.semantics { heading() },
        color = NOTIFICATION_META,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    )
}

@Composable
private fun NotificationCategoryFilters(
    selectedCategory: NotificationCategory,
    onCategoryClick: (NotificationCategory) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HeartGuardSpacing.AccountContentHorizontal)
            .horizontalScroll(rememberScrollState())
            .padding(top = HeartGuardSpacing.Item, bottom = HeartGuardSpacing.Compact),
        horizontalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact),
    ) {
        NOTIFICATION_CATEGORIES.forEach { option ->
            NotificationFilterChip(
                title = stringResource(option.titleResource),
                isSelected = selectedCategory == option.category,
                onClick = { onCategoryClick(option.category) },
            )
        }
    }
}

@Composable
private fun NotificationFilterChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.semantics {
            role = Role.Tab
            selected = isSelected
        },
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
            HeartGuardBorderWidth.Divider,
            NOTIFICATION_BORDER,
        ),
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            color = if (isSelected) Color.White else NOTIFICATION_BODY,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun NotificationGroupCard(
    notifications: List<TeamNotification>,
    markingReadIds: Set<String>,
    onNotificationClick: (TeamNotification) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(HeartGuardRadius.Card))
            .background(Color.White),
    ) {
        notifications.forEach { notification ->
            HorizontalDivider(thickness = HeartGuardBorderWidth.Divider, color = NOTIFICATION_BORDER)
            NotificationRow(
                notification = notification,
                isMarkingRead = notification.notificationId in markingReadIds,
                onClick = { onNotificationClick(notification) },
            )
        }
    }
}

@Composable
private fun NotificationRow(
    notification: TeamNotification,
    isMarkingRead: Boolean,
    onClick: () -> Unit,
) {
    val unreadStatus = stringResource(R.string.notification_unread)
    val readStatus = stringResource(R.string.notification_read)
    val isUnread = !notification.isRead
    val colors = notificationIconColors(notification.type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isUnread) NOTIFICATION_UNREAD_SURFACE else Color.White)
            .clickable(onClick = onClick)
            .semantics { stateDescription = if (isUnread) unreadStatus else readStatus }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = notificationIcon(notification.type),
                contentDescription = null,
                tint = colors.foreground,
                modifier = Modifier.size(HeartGuardIconSize.Navigation),
            )
        }
        Text(
            text = notification.title,
            modifier = Modifier.weight(1f),
            color = Color.Black,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium,
            ),
        )
        Text(
            text = notification.createdAt
                ?.atZoneSameInstant(ZoneId.systemDefault())
                ?.format(NOTIFICATION_TIME_FORMATTER)
                ?: stringResource(R.string.common_empty_value),
            color = NOTIFICATION_META,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
        )
        when {
            isMarkingRead -> CircularProgressIndicator(
                modifier = Modifier.size(8.dp),
                strokeWidth = 1.dp,
            )

            isUnread -> Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
private fun NotificationEmptyState(
    category: NotificationCategory,
    modifier: Modifier = Modifier,
) {
    val title = if (category == NotificationCategory.ALL) {
        stringResource(R.string.notification_empty_title)
    } else {
        stringResource(R.string.notification_filtered_empty_title)
    }
    val description = if (category == NotificationCategory.ALL) {
        stringResource(R.string.notification_empty_description)
    } else {
        stringResource(R.string.notification_filtered_empty_description)
    }
    Column(
        modifier = modifier
            .padding(horizontal = HeartGuardSpacing.AccountContentHorizontal)
            .padding(top = 128.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Item),
    ) {
            Box(
                modifier = Modifier
                    .size(HeartGuardIconSize.EmptyStateIllustration)
                    .clip(CircleShape)
                    .background(NOTIFICATION_ICON_SURFACE),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.NotificationsNone,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = NOTIFICATION_NEUTRAL_ICON,
                )
            }
            Text(
                text = title,
                color = MaterialTheme.extraColors.strongText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
            )
        Text(
            text = description,
            color = NOTIFICATION_BODY,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NotificationLoadError(
    modifier: Modifier = Modifier,
    onRetryClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(HeartGuardSpacing.Compact, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.notification_load_error_title),
            color = MaterialTheme.extraColors.strongText,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.notification_load_error_description),
            color = MaterialTheme.extraColors.secondaryText,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetryClick) {
            Text(text = stringResource(R.string.common_retry))
        }
    }
}

@Composable
private fun notificationIcon(type: NotificationType) = when (type) {
    NotificationType.RECORD_CREATED -> Icons.Filled.PhotoCamera
    NotificationType.EMERGENCY_ACKNOWLEDGED -> Icons.Filled.PriorityHigh
    NotificationType.INQUIRY_ANSWERED,
    NotificationType.UNKNOWN,
    -> Icons.Outlined.NotificationsNone
}

private fun notificationIconColors(type: NotificationType): NotificationIconColors = when (type) {
    NotificationType.RECORD_CREATED -> NotificationIconColors(
        background = NOTIFICATION_ICON_SURFACE,
        foreground = NOTIFICATION_RECORD_ICON,
    )

    NotificationType.EMERGENCY_ACKNOWLEDGED -> NotificationIconColors(
        background = NOTIFICATION_ALERT_SURFACE,
        foreground = NOTIFICATION_ALERT_ICON,
    )

    NotificationType.INQUIRY_ANSWERED,
    NotificationType.UNKNOWN,
    -> NotificationIconColors(
        background = NOTIFICATION_NEUTRAL_SURFACE,
        foreground = NOTIFICATION_NEUTRAL_ICON,
    )
}

@Preview(showBackground = true, widthDp = 402, heightDp = 978)
@Composable
private fun TeamNotificationScreenPreview() {
    HeartGuardTheme {
        TeamNotificationScreen(
            uiState = TeamNotificationUiState(hasLoaded = true),
            onEvent = {},
        )
    }
}

private data class NotificationCategoryOption(
    val category: NotificationCategory,
    val titleResource: Int,
)

private data class NotificationIconColors(
    val background: Color,
    val foreground: Color,
)

private val NOTIFICATION_CATEGORIES = listOf(
    NotificationCategoryOption(NotificationCategory.ALL, R.string.notification_filter_all),
    NotificationCategoryOption(NotificationCategory.RECORD, R.string.notification_filter_record),
    NotificationCategoryOption(NotificationCategory.EMERGENCY, R.string.notification_filter_emergency),
    NotificationCategoryOption(NotificationCategory.NOTICE, R.string.notification_filter_notice),
)

private val NOTIFICATION_BORDER = Color(0xFFE8EBF5)
private val NOTIFICATION_UNREAD_SURFACE = Color(0xFFF4F7FE)
private val NOTIFICATION_ICON_SURFACE = Color(0xFFEDF1FB)
private val NOTIFICATION_ALERT_SURFACE = Color(0xFFFFECEC)
private val NOTIFICATION_NEUTRAL_SURFACE = Color(0xFFF1F2F5)
private val NOTIFICATION_BODY = Color(0xFF54596B)
private val NOTIFICATION_META = Color(0xFF6B7280)
private val NOTIFICATION_ALERT_ICON = Color(0xFFF03B43)
private val NOTIFICATION_RECORD_ICON = Color(0xFF20A15D)
private val NOTIFICATION_NEUTRAL_ICON = Color(0xFF373B45)
private val NOTIFICATION_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREAN)

private const val PAGINATION_PREFETCH_ITEMS = 3
