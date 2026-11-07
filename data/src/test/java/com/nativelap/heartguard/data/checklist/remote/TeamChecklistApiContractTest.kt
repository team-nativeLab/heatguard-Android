package com.nativelap.heartguard.data.checklist.remote

import com.nativelap.heartguard.core.network.SelectedTeamIdInterceptor
import com.nativelap.heartguard.core.network.SessionBoundCallFactory
import com.nativelap.heartguard.core.network.BearerTokenInterceptor
import com.nativelap.heartguard.core.session.SessionManager
import com.nativelap.heartguard.core.session.SessionRole
import com.nativelap.heartguard.core.session.SessionToken
import com.nativelap.heartguard.core.session.TokenStorage
import com.nativelap.heartguard.data.checklist.dto.UpdateChecklistItemRequestDto
import com.nativelap.heartguard.data.checklist.dto.UpdateChecklistRoundItemRequestDto
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TeamChecklistApiContractTest {
    @Test
    fun `all checklist methods preserve request contract and use selected team header`() = runBlocking {
        val storage = ChecklistTestTokenStorage()
        val sessionManager = SessionManager(storage, kotlinx.coroutines.Dispatchers.Unconfined)
        sessionManager.onLoginSucceeded(SessionToken("access-token"))
        sessionManager.saveSelectedTeamAndSiteIdsForSession("team-12", "site-99", null, null, sessionManager.getSnapshot())
        val capturedRequests = mutableListOf<Request>()
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerTokenInterceptor(sessionManager))
            .addInterceptor(SelectedTeamIdInterceptor(sessionManager))
            .addInterceptor { chain ->
                capturedRequests += chain.request()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(500)
                    .message("Captured")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        val callFactory = SessionBoundCallFactory(client, sessionManager, captureSelectedTeam = true)
        val service = Retrofit.Builder()
            .baseUrl("https://api.example.test/")
            .callFactory(callFactory)
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TeamChecklistApiService::class.java)

        captureHttpError { service.getChecklist() }
        captureHttpError { service.setItemChecked("item-7", UpdateChecklistItemRequestDto(checked = true)) }
        captureHttpError { service.getRounds("2026-11-07") }
        captureHttpError { service.getRound("round-3", "2026-11-07") }
        captureHttpError {
            service.setRoundItemChecked("round-3", "item-7", UpdateChecklistRoundItemRequestDto(checked = true))
        }

        assertEquals(
            listOf(
                "GET /api/v1/team/checklist",
                "PUT /api/v1/team/checklist/items/item-7",
                "GET /api/v1/team/checklist/rounds?date=2026-11-07",
                "GET /api/v1/team/checklist/rounds/round-3?date=2026-11-07",
                "PUT /api/v1/team/checklist/rounds/round-3/items/item-7",
            ),
            capturedRequests.map { "${it.method} ${it.url.encodedPath}${it.url.encodedQuery?.let { query -> "?$query" }.orEmpty()}" },
        )
        capturedRequests.forEach { request ->
            assertEquals("Bearer access-token", request.header("Authorization"))
            assertEquals("team-12", request.header("X-Team-Id"))
            assertNull(request.header("X-Site-Id"))
        }
        assertEquals("{\"checked\":true}", capturedRequests[1].bodyText())
        assertEquals("{\"checked\":true}", capturedRequests[4].bodyText())
    }

    private suspend fun captureHttpError(request: suspend () -> Any) {
        try {
            request()
        } catch (_: HttpException) {
            // The interceptor returns a synthetic 500 after recording the outgoing request.
        }
    }

    private fun Request.bodyText(): String =
        Buffer().also { body?.writeTo(it) }.readUtf8()
}

private class ChecklistTestTokenStorage : TokenStorage {
    private var teamId: String? = null
    private var siteId: String? = null
    override fun readAccessToken(): String? = null
    override fun saveAccessToken(accessToken: String) = Unit
    override fun readSessionRole(): SessionRole = SessionRole.TEAM_MEMBER
    override fun saveSession(accessToken: String, role: SessionRole) { teamId = null; siteId = null }
    override fun readSelectedTeamId(): String? = teamId
    override fun readSelectedSiteId(): String? = siteId
    override fun saveSelectedTeamAndSiteIds(teamId: String, siteId: String) { this.teamId = teamId; this.siteId = siteId }
    override fun clearSelectedTeamAndSiteIds() { teamId = null; siteId = null }
    override fun clear() { clearSelectedTeamAndSiteIds() }
}
