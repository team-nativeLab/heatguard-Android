package com.nativelap.heartguard.core.session

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerDataCleanupTest {
    @Test
    fun expireSessionWaitsForPersistentDataCleanup() = runBlocking {
        val cleanupStarted = CompletableDeferred<Unit>()
        val finishCleanup = CompletableDeferred<Unit>()
        var shouldWaitForCleanup = false
        val cleaner = SessionScopedDataCleaner {
            if (shouldWaitForCleanup) {
                cleanupStarted.complete(Unit)
                finishCleanup.await()
            }
        }
        val manager = createSessionManager(cleaner)
        manager.initialize()
        manager.onLoginSucceeded(SessionToken("team-token"))
        shouldWaitForCleanup = true
        val sessionGeneration = manager.getSnapshot().generation

        val expireJob = launch { manager.expireSession(sessionGeneration) }
        cleanupStarted.await()

        assertNull(manager.getSnapshot().accessToken)
        assertEquals(SessionState.Authenticated, manager.sessionState.value)
        assertFalse(expireJob.isCompleted)

        finishCleanup.complete(Unit)
        expireJob.join()

        assertEquals(SessionState.Unauthenticated, manager.sessionState.value)
    }

    @Test
    fun withdrawalWaitsForPersistentDataCleanup() = runBlocking {
        val cleanupStarted = CompletableDeferred<Unit>()
        val finishCleanup = CompletableDeferred<Unit>()
        var shouldWaitForCleanup = false
        val manager =
            createSessionManager(
                SessionScopedDataCleaner {
                    if (shouldWaitForCleanup) {
                        cleanupStarted.complete(Unit)
                        finishCleanup.await()
                    }
                },
            )
        manager.initialize()
        manager.onLoginSucceeded(SessionToken("team-token"))
        shouldWaitForCleanup = true
        val sessionGeneration = manager.getSnapshot().generation

        val withdrawalJob = launch { manager.clearAccessTokenAfterWithdrawal(sessionGeneration) }
        cleanupStarted.await()

        assertNull(manager.getSnapshot().accessToken)
        assertEquals(SessionState.Authenticated, manager.sessionState.value)
        assertFalse(withdrawalJob.isCompleted)

        finishCleanup.complete(Unit)
        withdrawalJob.join()

        assertTrue(withdrawalJob.isCompleted)
    }

    @Test
    fun newLoginCreatesNewPersistentCacheSessionIdentifier() = runBlocking {
        val manager = createSessionManager(SessionScopedDataCleaner {})
        manager.initialize()
        manager.onLoginSucceeded(SessionToken("same-token"))
        val firstIdentifier = manager.getSnapshot().sessionIdentifier

        manager.expireSession()
        manager.onLoginSucceeded(SessionToken("same-token"))

        assertTrue(firstIdentifier != manager.getSnapshot().sessionIdentifier)
    }

    @Test
    fun restoredSessionPrunesScopedDataBeforeAuthenticationIsPublished() = runBlocking {
        var restoredIdentifier: String? = null
        var stateDuringPrune: SessionState? = null
        lateinit var manager: SessionManager
        val cleaner =
            object : SessionScopedDataCleaner {
                override suspend fun clearSessionData() = Unit

                override suspend fun onSessionRestored(sessionIdentifier: String) {
                    restoredIdentifier = sessionIdentifier
                    stateDuringPrune = manager.sessionState.value
                }
            }
        manager =
            createSessionManager(
                cleaner = cleaner,
                tokenStorage = FakeTokenStorage("restored-token", "persisted-session-id"),
            )

        manager.initialize()

        assertEquals("persisted-session-id", restoredIdentifier)
        assertEquals(SessionState.Initializing, stateDuringPrune)
        assertEquals(SessionState.Authenticated, manager.sessionState.value)
    }

    private fun createSessionManager(
        cleaner: SessionScopedDataCleaner,
        tokenStorage: FakeTokenStorage = FakeTokenStorage(),
    ) =
        SessionManager(
            tokenStorage = tokenStorage,
            ioDispatcher = Dispatchers.Unconfined,
            sessionScopedDataCleaners = setOf(cleaner),
        )

    private class FakeTokenStorage(
        private var accessToken: String? = null,
        private var sessionIdentifier: String? = null,
    ) : TokenStorage {

        override fun readAccessToken(): String? = accessToken

        override fun saveAccessToken(accessToken: String) {
            this.accessToken = accessToken
        }

        override fun saveSession(accessToken: String, role: SessionRole) {
            this.accessToken = accessToken
        }

        override fun readSessionIdentifier(): String? = sessionIdentifier

        override fun saveSessionIdentifier(identifier: String) {
            sessionIdentifier = identifier
        }

        override fun clear() {
            accessToken = null
            sessionIdentifier = null
        }
    }
}
