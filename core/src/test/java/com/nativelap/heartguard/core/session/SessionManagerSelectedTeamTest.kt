package com.nativelap.heartguard.core.session

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionManagerSelectedTeamTest {
    @Test
    fun `team id is unavailable after restore until memberships confirms it`() =
        runBlocking {
            val tokenStorage = InMemoryTokenStorage(selectedTeamId = "team_01")
            val sessionManager = SessionManager(tokenStorage, Dispatchers.Unconfined)

            sessionManager.initialize()

            val sessionSnapshot = sessionManager.getSnapshot()
            assertEquals("team_01", sessionManager.getSelectedTeamId())
            assertNull(sessionManager.getSelectedTeamIdForSession(sessionSnapshot))
        }

    @Test
    fun `saving selected team and site makes team id available to owning session`() =
        runBlocking {
            val tokenStorage = InMemoryTokenStorage()
            val sessionManager = SessionManager(tokenStorage, Dispatchers.Unconfined)
            sessionManager.onLoginSucceeded(SessionToken("access-token"))
            val sessionSnapshot = sessionManager.getSnapshot()

            val wasSaved =
                sessionManager.saveSelectedTeamAndSiteIdsForSession(
                    teamId = "team_01",
                    siteId = "site_01",
                    expectedTeamId = null,
                    expectedSiteId = null,
                    expectedSnapshot = sessionSnapshot,
                )

            assertTrue(wasSaved)
            assertEquals("team_01", tokenStorage.readSelectedTeamId())
            assertEquals("site_01", tokenStorage.readSelectedSiteId())
            assertEquals("team_01", sessionManager.getSelectedTeamIdForSession(sessionSnapshot))
        }

    @Test
    fun `selected team cannot be saved after its session changes`() =
        runBlocking {
            val tokenStorage = InMemoryTokenStorage()
            val sessionManager = SessionManager(tokenStorage, Dispatchers.Unconfined)
            sessionManager.onLoginSucceeded(SessionToken("first-token"))
            val previousSession = sessionManager.getSnapshot()
            sessionManager.onLoginSucceeded(SessionToken("second-token"))

            val wasSaved =
                sessionManager.saveSelectedTeamAndSiteIdsForSession(
                    teamId = "team_01",
                    siteId = "site_01",
                    expectedTeamId = null,
                    expectedSiteId = null,
                    expectedSnapshot = previousSession,
                )

            assertFalse(wasSaved)
            assertNull(tokenStorage.readSelectedTeamId())
            assertNull(sessionManager.getSelectedTeamIdForSession(sessionManager.getSnapshot()))
        }

    @Test
    fun `clearing selected membership clears both ids and marks no selection as confirmed`() =
        runBlocking {
            val tokenStorage = InMemoryTokenStorage(selectedTeamId = "team_01", selectedSiteId = "site_01")
            val sessionManager = SessionManager(tokenStorage, Dispatchers.Unconfined)
            sessionManager.onLoginSucceeded(SessionToken("access-token"))
            tokenStorage.saveSelectedTeamAndSiteIds("team_01", "site_01")
            val sessionSnapshot = sessionManager.getSnapshot()

            val wasCleared =
                sessionManager.clearSelectedTeamAndSiteIdsForSession(
                    expectedTeamId = "team_01",
                    expectedSiteId = "site_01",
                    expectedSnapshot = sessionSnapshot,
                )

            assertTrue(wasCleared)
            assertNull(tokenStorage.readSelectedTeamId())
            assertNull(tokenStorage.readSelectedSiteId())
            assertNull(sessionManager.getSelectedTeamIdForSession(sessionSnapshot))
        }

    @Test
    fun `new session and logout clear stored team id`() =
        runBlocking {
            val tokenStorage = InMemoryTokenStorage(selectedTeamId = "team_old", selectedSiteId = "site_old")
            val sessionManager = SessionManager(tokenStorage, Dispatchers.Unconfined)

            sessionManager.onLoginSucceeded(SessionToken("new-token"))

            assertNull(tokenStorage.readSelectedTeamId())
            assertNull(tokenStorage.readSelectedSiteId())

            val sessionSnapshot = sessionManager.getSnapshot()
            sessionManager.saveSelectedTeamAndSiteIdsForSession(
                teamId = "team_01",
                siteId = "site_01",
                expectedTeamId = null,
                expectedSiteId = null,
                expectedSnapshot = sessionSnapshot,
            )
            sessionManager.expireSession(sessionSnapshot.generation)

            assertNull(tokenStorage.readSelectedTeamId())
            assertNull(tokenStorage.readSelectedSiteId())
        }
}

private class InMemoryTokenStorage(
    private var accessToken: String? = "restored-token",
    private var selectedTeamId: String? = null,
    private var selectedSiteId: String? = null,
) : TokenStorage {
    override fun readAccessToken(): String? = accessToken

    override fun saveAccessToken(accessToken: String) {
        this.accessToken = accessToken
    }

    override fun readSessionRole(): SessionRole = SessionRole.TEAM_MEMBER

    override fun saveSession(
        accessToken: String,
        role: SessionRole,
    ) {
        this.accessToken = accessToken
        selectedTeamId = null
        selectedSiteId = null
    }

    override fun readSelectedTeamId(): String? = selectedTeamId

    override fun readSelectedSiteId(): String? = selectedSiteId

    override fun saveSelectedTeamAndSiteIds(
        teamId: String,
        siteId: String,
    ) {
        selectedTeamId = teamId
        selectedSiteId = siteId
    }

    override fun clearSelectedTeamAndSiteIds() {
        selectedTeamId = null
        selectedSiteId = null
    }

    override fun clear() {
        accessToken = null
        clearSelectedTeamAndSiteIds()
    }
}
