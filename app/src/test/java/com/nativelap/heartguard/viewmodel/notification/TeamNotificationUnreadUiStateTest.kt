package com.nativelap.heartguard.viewmodel.notification

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TeamNotificationUnreadUiStateTest {
    @Test
    fun `unread indicator is hidden before a count is known and when count is zero`() {
        assertFalse(TeamNotificationUnreadUiState().hasUnread)
        assertFalse(TeamNotificationUnreadUiState(unreadCount = 0).hasUnread)
    }

    @Test
    fun `unread indicator is shown when server count is positive`() {
        assertTrue(TeamNotificationUnreadUiState(unreadCount = 1).hasUnread)
        assertTrue(TeamNotificationUnreadUiState(unreadCount = 27).hasUnread)
    }
}
