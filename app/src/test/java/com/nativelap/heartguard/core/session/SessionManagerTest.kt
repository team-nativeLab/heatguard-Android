package com.nativelap.heartguard.core.session

import com.nativelap.heartguard.core.session.SessionState.Authenticated
import com.nativelap.heartguard.core.session.SessionState.Unauthenticated
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
    fun loginAndSessionClearUpdateStoredAccessTokenAndState() = runTest {
        val storage = FakeTokenStorage()
        val sessionManager = SessionManager(
            tokenStorage = storage,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        val sessionToken = SessionToken(accessToken = "access-token")

        sessionManager.onLoginSucceeded(sessionToken)

        assertEquals(sessionToken.accessToken, storage.accessToken)
        assertEquals(Authenticated, sessionManager.sessionState.value)

        sessionManager.clearSession()

        assertEquals(null, storage.accessToken)
        assertEquals(Unauthenticated, sessionManager.sessionState.value)
    }

    private class FakeTokenStorage : TokenStorage {
        var accessToken: String? = null

        override fun readAccessToken(): String? = accessToken

        override fun saveAccessToken(accessToken: String) {
            this.accessToken = accessToken
        }

        override fun clear() {
            accessToken = null
        }
    }
}
