package com.nativelap.heartguard.core.network

import com.nativelap.heartguard.core.session.SelectedTeamRequestSnapshot
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.core.session.SessionSnapshot
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.TokenStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SelectedTeamIdInterceptorTest {
    @Test
    fun `adds selected team header without copying site id`() =
        runBlocking<Unit> {
            val storage = TestTokenStorage()
            val sessionManager = SessionManager(storage, Dispatchers.Unconfined)
            sessionManager.onLoginSucceeded(SessionToken("token"))
            val snapshot = sessionManager.getSnapshot()
            sessionManager.saveSelectedTeamAndSiteIdsForSession("team-7", "site-9", null, null, snapshot)
            val teamSnapshot = requireNotNull(sessionManager.getSelectedTeamRequestSnapshot(snapshot))
            val interceptedRequest =
                SelectedTeamIdInterceptor(sessionManager).intercept(
                    testChain(
                        Request
                            .Builder()
                            .url("https://example.test/api/v1/team/checklist?date=2026-11-07")
                            .header("Authorization", "Bearer access-token")
                            .tag(SelectedTeamRequestSnapshot::class.java, teamSnapshot)
                            .put("payload".toRequestBody())
                            .tag(SessionSnapshot::class.java, snapshot)
                            .build(),
                    ),
                )

            assertEquals("team-7", interceptedRequest.request.header("X-Team-Id"))
            assertEquals(null, interceptedRequest.request.header("X-Site-Id"))
            assertEquals("Bearer access-token", interceptedRequest.request.header("Authorization"))
            assertEquals("2026-11-07", interceptedRequest.request.url.queryParameter("date"))
            assertEquals("PUT", interceptedRequest.request.method)
            assertEquals(7L, interceptedRequest.request.body?.contentLength())
        }

    @Test
    fun `aborts old checklist call when selection changes before transmission`() =
        runBlocking<Unit> {
            val storage = TestTokenStorage()
            val sessionManager = SessionManager(storage, Dispatchers.Unconfined)
            sessionManager.onLoginSucceeded(SessionToken("token"))
            val sessionSnapshot = sessionManager.getSnapshot()
            sessionManager.saveSelectedTeamAndSiteIdsForSession("team-old", "site-old", null, null, sessionSnapshot)
            val callTeamSnapshot = requireNotNull(sessionManager.getSelectedTeamRequestSnapshot(sessionSnapshot))
            sessionManager.saveSelectedTeamAndSiteIdsForSession(
                teamId = "team-new",
                siteId = "site-new",
                expectedTeamId = "team-old",
                expectedSiteId = "site-old",
                expectedSnapshot = sessionSnapshot,
            )
            val request =
                Request
                    .Builder()
                    .url("https://example.test/api/v1/team/checklist/items/item-1")
                    .tag(SessionSnapshot::class.java, sessionSnapshot)
                    .tag(SelectedTeamRequestSnapshot::class.java, callTeamSnapshot)
                    .build()

            assertFailsWith<SessionChangedException> {
                SelectedTeamIdInterceptor(sessionManager).intercept(testChain(request))
            }
        }

    @Test
    fun `does not proceed with persisted team until membership confirms it`() =
        runBlocking<Unit> {
            val sessionManager = SessionManager(TestTokenStorage(selectedTeamId = "stale-team"), Dispatchers.Unconfined)
            sessionManager.initialize()
            val request =
                Request
                    .Builder()
                    .url("https://example.test/api/v1/team/checklist")
                    .tag(SessionSnapshot::class.java, sessionManager.getSnapshot())
                    .build()

            assertFailsWith<TeamNotSelectedException> {
                SelectedTeamIdInterceptor(sessionManager).intercept(testChain(request))
            }
        }

    private fun testChain(request: Request): Interceptor.Chain {
        var proceededRequest: Request? = null
        return java.lang.reflect.Proxy.newProxyInstance(
            Interceptor.Chain::class.java.classLoader,
            arrayOf(Interceptor.Chain::class.java),
        ) { _, method, arguments ->
            when (method.name) {
                "request" -> {
                    request
                }

                "proceed" -> {
                    proceededRequest = arguments!![0] as Request
                    Response
                        .Builder()
                        .request(proceededRequest!!)
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .build()
                }

                else -> {
                    null
                }
            }
        } as Interceptor.Chain
    }
}

private class TestTokenStorage(
    private var selectedTeamId: String? = null,
) : TokenStorage {
    private var selectedSiteId: String? = null

    override fun readAccessToken(): String? = "restored-token"

    override fun saveAccessToken(accessToken: String) = Unit

    override fun readSessionRole(): SessionRole = SessionRole.TEAM_MEMBER

    override fun saveSession(
        accessToken: String,
        role: SessionRole,
    ) {
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
        selectedSiteId =
            siteId
    }

    override fun clearSelectedTeamAndSiteIds() {
        selectedTeamId = null
        selectedSiteId = null
    }

    override fun clear() = clearSelectedTeamAndSiteIds()
}
