package com.nativelap.heartguard.core.session

import com.nativelap.heartguard.core.session.SessionState.Authenticated
import com.nativelap.heartguard.core.session.SessionState.Unauthenticated
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {
    @Test
    fun initializeWithoutSavedAccessTokenSetsUnauthenticatedState() = runTest {
        val storage = FakeTokenStorage()
        val sessionManager = SessionManager(
            tokenStorage = storage,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        sessionManager.initialize()

        assertEquals(Unauthenticated, sessionManager.sessionState.value)
    }

    @Test
    fun initializeClearsLegacyPlaceholderTokenAndSetsUnauthenticatedState() = runTest {
        val storage = FakeTokenStorage().apply {
            accessToken = "placeholder-access-token"
        }
        val sessionManager = SessionManager(
            tokenStorage = storage,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        sessionManager.initialize()

        assertEquals(null, storage.accessToken)
        assertEquals(Unauthenticated, sessionManager.sessionState.value)
    }

    @Test
    fun loginAndExpireSessionUpdateStoredAccessTokenAndState() = runTest {
        val storage = FakeTokenStorage()
        val sessionManager = SessionManager(
            tokenStorage = storage,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        val sessionToken = SessionToken(accessToken = "access-token")

        sessionManager.onLoginSucceeded(sessionToken)

        assertEquals(sessionToken.accessToken, storage.accessToken)
        assertEquals(Authenticated, sessionManager.sessionState.value)

        sessionManager.expireSession()

        assertEquals(null, storage.accessToken)
        assertEquals(Unauthenticated, sessionManager.sessionState.value)
    }

    @Test
    fun expireSessionEmitsExpiredEvent() = runTest {
        val storage = FakeTokenStorage()
        val sessionManager = SessionManager(
            tokenStorage = storage,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

        sessionManager.onLoginSucceeded(SessionToken(accessToken = "access-token"))

        var receivedEvent: SessionEvent? = null
        val collectJob = launch {
            receivedEvent = sessionManager.sessionEvents.first()
        }

        sessionManager.expireSession()
        collectJob.join()

        assertEquals(SessionEvent.Expired, receivedEvent)
    }

    @Test
    fun lateRequestCannotExpireSameTokenRelogin() = runTest {
        val storage = FakeTokenStorage()
        val sessionManager = SessionManager(storage, StandardTestDispatcher(testScheduler))
        sessionManager.onLoginSucceeded(SessionToken("same-token"))
        val previousSnapshot = sessionManager.getSnapshot()
        sessionManager.onLoginSucceeded(SessionToken("same-token"))

        assertFalse(sessionManager.expireSessionForRequest(previousSnapshot))
        assertFalse(sessionManager.expireSession(previousSnapshot.generation))
        assertEquals("same-token", storage.accessToken)
        assertEquals(Authenticated, sessionManager.sessionState.value)
        assertTrue(previousSnapshot.generation != sessionManager.getSnapshot().generation)
    }

    @Test
    fun duplicateUnauthorizedResponseClearsStorageOnlyOnce() = runTest {
        val storage = FakeTokenStorage()
        val sessionManager = SessionManager(storage, StandardTestDispatcher(testScheduler))
        sessionManager.onLoginSucceeded(SessionToken("token"))
        val requestSnapshot = sessionManager.getSnapshot()
        val firstExpiry = launch { sessionManager.expireSessionForRequest(requestSnapshot) }
        val secondExpiry = launch { sessionManager.expireSessionForRequest(requestSnapshot) }
        firstExpiry.join()
        secondExpiry.join()

        assertEquals(1, storage.clearCount)
        assertEquals(Unauthenticated, sessionManager.sessionState.value)
    }

    @Test
    fun withdrawalInvalidatesRequestsButRetainsLogicalSessionUntilCompletion() = runTest {
        val storage = FakeTokenStorage()
        val sessionManager = SessionManager(storage, StandardTestDispatcher(testScheduler))
        sessionManager.onLoginSucceeded(SessionToken("token"))
        val requestSnapshot = sessionManager.getSnapshot()

        assertTrue(sessionManager.clearAccessTokenAfterWithdrawal(requestSnapshot.generation))
        assertFalse(sessionManager.expireSessionForRequest(requestSnapshot))
        sessionManager.initialize()
        sessionManager.initialize()
        assertEquals(Authenticated, sessionManager.sessionState.value)
        assertEquals(null, sessionManager.getSnapshot().accessToken)
        assertTrue(sessionManager.expireSession(requestSnapshot.generation))
        assertEquals(Unauthenticated, sessionManager.sessionState.value)
    }

    @Test
    fun previousWithdrawalCannotClearNewLogin() = runTest {
        val storage = FakeTokenStorage()
        val sessionManager = SessionManager(storage, StandardTestDispatcher(testScheduler))
        sessionManager.onLoginSucceeded(SessionToken("old-token"))
        val oldGeneration = sessionManager.getSnapshot().generation
        sessionManager.onLoginSucceeded(SessionToken("new-token"))

        assertFalse(sessionManager.clearAccessTokenAfterWithdrawal(oldGeneration))
        assertEquals("new-token", storage.accessToken)
        assertEquals(Authenticated, sessionManager.sessionState.value)
    }

    @Test
    fun initializationAndLoginAreSerializedAndInitializationIsIdempotent() = runTest {
        val storage = FakeTokenStorage()
        val sessionManager = SessionManager(storage, StandardTestDispatcher(testScheduler))
        val initializeJob = launch { sessionManager.initialize() }
        val loginJob = launch { sessionManager.onLoginSucceeded(SessionToken("token")) }
        initializeJob.join()
        loginJob.join()
        val loginSnapshot = sessionManager.getSnapshot()
        sessionManager.initialize()

        assertEquals(loginSnapshot, sessionManager.getSnapshot())
        assertEquals(Authenticated, sessionManager.sessionState.value)
    }

    @Test
    fun failedStorageWriteDoesNotPublishNewSession() = runTest {
        val storage = FakeTokenStorage()
        val sessionManager = SessionManager(storage, StandardTestDispatcher(testScheduler))
        sessionManager.onLoginSucceeded(SessionToken("old-token"))
        val oldSnapshot = sessionManager.getSnapshot()
        storage.shouldFailSave = true
        try {
            sessionManager.onLoginSucceeded(SessionToken("new-token"))
            throw AssertionError("Storage failure must propagate")
        } catch (_: IllegalStateException) {
            assertEquals(oldSnapshot, sessionManager.getSnapshot())
            assertEquals(Authenticated, sessionManager.sessionState.value)
        }
    }

    @Test
    fun failedStorageClearStillEndsMemorySessionAndPublishesExpiry() = runTest {
        val storage = FakeTokenStorage()
        val sessionManager = SessionManager(storage, StandardTestDispatcher(testScheduler))
        sessionManager.onLoginSucceeded(SessionToken("old-token"))
        val oldSnapshot = sessionManager.getSnapshot()
        val expiredEvents = mutableListOf<SessionEvent>()
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            sessionManager.sessionEvents.collect { sessionEvent -> expiredEvents += sessionEvent }
        }
        storage.shouldFailClear = true

        val isExpired = sessionManager.expireSession(oldSnapshot.generation)
        testScheduler.advanceUntilIdle()

        assertTrue(isExpired)
        assertEquals(Unauthenticated, sessionManager.sessionState.value)
        assertEquals(null, sessionManager.getSnapshot().accessToken)
        assertTrue(sessionManager.getSnapshot().generation > oldSnapshot.generation)
        assertEquals(listOf<SessionEvent>(SessionEvent.Expired), expiredEvents)
    }

    @Test
    fun failedStorageClearAfterWithdrawalStillClearsMemoryToken() = runTest {
        val storage = FakeTokenStorage()
        val sessionManager = SessionManager(storage, StandardTestDispatcher(testScheduler))
        sessionManager.onLoginSucceeded(SessionToken("token"))
        val withdrawnGeneration = sessionManager.getSnapshot().generation
        storage.shouldFailClear = true

        val isCleared = sessionManager.clearAccessTokenAfterWithdrawal(withdrawnGeneration)

        assertTrue(isCleared)
        assertEquals(null, sessionManager.getSnapshot().accessToken)
        assertEquals(withdrawnGeneration, sessionManager.getSnapshot().generation)
    }

    @Test
    fun unreadableStoredTokenStartsUnauthenticatedAndRetriesClear() = runTest {
        val storage = FakeTokenStorage().apply {
            accessToken = "token"
            shouldFailRead = true
        }
        val sessionManager = SessionManager(storage, StandardTestDispatcher(testScheduler))

        sessionManager.initialize()

        assertEquals(Unauthenticated, sessionManager.sessionState.value)
        assertEquals(1, storage.clearCount)
    }

    @Test
    fun failedLegacyTokenClearStillFinishesInitialization() = runTest {
        val storage = FakeTokenStorage().apply {
            accessToken = "placeholder-access-token"
            shouldFailClear = true
        }
        val sessionManager = SessionManager(storage, StandardTestDispatcher(testScheduler))

        sessionManager.initialize()

        assertEquals(Unauthenticated, sessionManager.sessionState.value)
    }

    @Test
    fun onlySessionRestoredFromStorageIsMarkedRestored() = runTest {
        val storage = FakeTokenStorage().apply {
            accessToken = "stored-token"
        }
        val sessionManager = SessionManager(storage, StandardTestDispatcher(testScheduler))

        sessionManager.initialize()
        assertTrue(sessionManager.isRestoredSession())

        sessionManager.expireSession()
        sessionManager.onLoginSucceeded(SessionToken("new-token"))
        assertFalse(sessionManager.isRestoredSession())
    }

    @Test
    fun logoutAndLoginAdvanceSessionGeneration() = runTest {
        val storage = FakeTokenStorage()
        val sessionManager = SessionManager(storage, StandardTestDispatcher(testScheduler))
        sessionManager.onLoginSucceeded(SessionToken("token"))
        val loginGeneration = sessionManager.sessionSnapshot.value.generation

        sessionManager.expireSession()
        val logoutGeneration = sessionManager.sessionSnapshot.value.generation
        sessionManager.onLoginSucceeded(SessionToken("token"))

        assertTrue(logoutGeneration > loginGeneration)
        assertTrue(sessionManager.sessionSnapshot.value.generation > logoutGeneration)
    }

    @Test
    fun cancellationDuringStorageCommitStillPublishesCommittedSession() = runTest {
        val storage = FakeTokenStorage()
        val sessionManager = SessionManager(storage, StandardTestDispatcher(testScheduler))
        lateinit var loginJob: Job
        storage.onSave = { loginJob.cancel() }
        loginJob = launch { sessionManager.onLoginSucceeded(SessionToken("token")) }
        loginJob.join()

        assertTrue(loginJob.isCancelled)
        assertEquals("token", storage.accessToken)
        assertEquals("token", sessionManager.getSnapshot().accessToken)
        assertEquals(Authenticated, sessionManager.sessionState.value)
    }

    @Test
    fun alreadyCancelledLoginDoesNotStartStorageCommit() = runTest {
        val storage = FakeTokenStorage()
        val sessionManager = SessionManager(storage, StandardTestDispatcher(testScheduler))
        val loginJob = launch {
            currentCoroutineContext()[Job]!!.cancel()
            sessionManager.onLoginSucceeded(SessionToken("token"))
        }
        loginJob.join()

        assertEquals(null, storage.accessToken)
        assertEquals(SessionState.Initializing, sessionManager.sessionState.value)
    }

    @Test
    fun slowExpiredEventSubscriberDoesNotBlockLaterLoginOrExpiry() = runTest {
        val storage = FakeTokenStorage()
        val sessionManager = SessionManager(storage, StandardTestDispatcher(testScheduler))
        val blockedSubscriber = CompletableDeferred<Unit>()
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            sessionManager.sessionEvents.collect { blockedSubscriber.await() }
        }
        repeat(4) {
            sessionManager.onLoginSucceeded(SessionToken("token"))
            sessionManager.expireSession()
        }
        sessionManager.onLoginSucceeded(SessionToken("final-token"))

        assertEquals("final-token", storage.accessToken)
        assertEquals(Authenticated, sessionManager.sessionState.value)
    }

    @Test
    fun snapshotStringDoesNotExposeToken() {
        assertFalse(SessionSnapshot(1L, "secret-token").toString().contains("secret-token"))
    }

    private class FakeTokenStorage : TokenStorage {
        var accessToken: String? = null
        var clearCount = 0
        var shouldFailSave = false
        var shouldFailClear = false
        var shouldFailRead = false
        var onSave: (() -> Unit)? = null

        override fun readAccessToken(): String? {
            check(!shouldFailRead)
            return accessToken
        }

        override fun saveAccessToken(accessToken: String) {
            check(!shouldFailSave)
            this.accessToken = accessToken
            onSave?.invoke()
        }

        override fun clear() {
            clearCount += 1
            check(!shouldFailClear)
            accessToken = null
        }
    }
}
