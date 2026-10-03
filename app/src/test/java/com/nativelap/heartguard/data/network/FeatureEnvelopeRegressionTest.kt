package com.nativelap.heartguard.data.network

import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.core.session.createUnauthenticatedTestSessionManager
import com.nativelap.heartguard.data.emergency.remote.EmergencyCallApiService
import com.nativelap.heartguard.data.emergency.remote.EmergencyCallRemoteDataSourceImpl
import com.nativelap.heartguard.data.inquiry.remote.InquiryApiService
import com.nativelap.heartguard.data.inquiry.remote.InquiryRemoteDataSourceImpl
import com.nativelap.heartguard.data.notification.remote.TeamNotificationApiService
import com.nativelap.heartguard.data.notification.remote.TeamNotificationRemoteDataSourceImpl
import com.nativelap.heartguard.data.profile.remote.WorkerProfileApiService
import com.nativelap.heartguard.data.profile.remote.WorkerProfileRemoteDataSourceImpl
import com.nativelap.heartguard.data.record.remote.RecordHistoryApiService
import com.nativelap.heartguard.data.record.remote.RecordHistoryRemoteDataSourceImpl
import com.nativelap.heartguard.data.site.remote.TeamSiteApiService
import com.nativelap.heartguard.data.site.remote.TeamSiteRemoteDataSourceImpl
import com.nativelap.heartguard.domain.emergency.model.EmergencyCallUpdateStatus
import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class FeatureEnvelopeRegressionTest(private val endpoint: Endpoint) {
    @Test
    fun successfulPayloadIncludesEmptyListsAndNoneCall() {
        assertTrue(call("""{"success":true,"data":${endpoint.payload},"error":null}""") is ApiResult.Success)
    }

    @Test
    fun failedEnvelopeCannotExposeItsPayload() {
        assertEquals(
            ApiResult.Failure(ApiError.Serialization),
            call("""{"success":false,"data":${endpoint.payload},"error":{"code":"REJECTED"}}"""),
        )
    }

    @Test
    fun successfulEnvelopeWithErrorCannotExposeItsPayload() {
        assertEquals(
            ApiResult.Failure(ApiError.Serialization),
            call("""{"success":true,"data":${endpoint.payload},"error":{"code":"REJECTED"}}"""),
        )
    }

    @Test
    fun missingRequiredDataFailsButPasswordEmptySuccessIsPreserved() {
        val response = call("""{"success":true,"data":null,"error":null}""")
        if (endpoint == Endpoint.PASSWORD_CHANGE) {
            assertTrue(response is ApiResult.Success)
        } else {
            assertEquals(ApiResult.Failure(ApiError.Serialization), response)
        }
    }

    @Test
    fun explicitRejectionWithoutDataPreservesItsCode() {
        assertEquals(
            ApiResult.Failure(ApiError.ServerRejected("REJECTED")),
            call("""{"success":false,"data":null,"error":{"code":"REJECTED"}}"""),
        )
    }

    private fun call(responseBody: String): ApiResult<*> = runBlocking {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse.Builder().code(200).body(responseBody).build())
            val factory = ApiRetrofitFactory(
                authenticatedApiClient = OkHttpClient(),
                unauthenticatedApiClient = OkHttpClient(),
                sessionManager = createUnauthenticatedTestSessionManager(),
                json = Json { ignoreUnknownKeys = true },
            )
            val executor = ApiExecutor()
            fun <Service : Any> service(serviceClass: Class<Service>): Service {
                return factory.createService(
                    baseUrl = server.url("/").toString(),
                    serviceClass = serviceClass,
                    authentication = ApiAuthentication.BEARER,
                )
            }
            when (endpoint) {
                Endpoint.NOTIFICATION_LIST -> TeamNotificationRemoteDataSourceImpl(
                    service(TeamNotificationApiService::class.java),
                    executor,
                ).getNotifications(
                    category = "ALL",
                    cursor = null,
                    limit = 20,
                )
                Endpoint.NOTIFICATION_READ -> TeamNotificationRemoteDataSourceImpl(
                    service(TeamNotificationApiService::class.java),
                    executor,
                ).markNotificationRead("notification")
                Endpoint.NOTIFICATION_READ_ALL -> TeamNotificationRemoteDataSourceImpl(
                    service(TeamNotificationApiService::class.java),
                    executor,
                ).markAllNotificationsRead()
                Endpoint.HISTORY_LIST -> RecordHistoryRemoteDataSourceImpl(
                    service(RecordHistoryApiService::class.java),
                    executor,
                ).getRecordPage(
                    date = "2026-10-04",
                    cursor = null,
                    limit = 20,
                )
                Endpoint.HISTORY_DETAIL -> RecordHistoryRemoteDataSourceImpl(
                    service(RecordHistoryApiService::class.java),
                    executor,
                ).getRecordDetail("record")
                Endpoint.HOME -> TeamSiteRemoteDataSourceImpl(
                    service(TeamSiteApiService::class.java),
                    executor,
                ).getTeamSite()
                Endpoint.CALL_REGISTER -> EmergencyCallRemoteDataSourceImpl(
                    service(EmergencyCallApiService::class.java),
                    executor,
                ).registerEmergencyCall(
                    idempotencyKey = "intent",
                    clientOccurredAt = Instant.parse("2026-10-04T01:00:00Z"),
                    message = null,
                )
                Endpoint.CALL_CURRENT -> EmergencyCallRemoteDataSourceImpl(
                    service(EmergencyCallApiService::class.java),
                    executor,
                ).getCurrentEmergencyCall()
                Endpoint.CALL_UPDATE -> EmergencyCallRemoteDataSourceImpl(
                    service(EmergencyCallApiService::class.java),
                    executor,
                ).updateEmergencyCallStatus(
                    callId = "call",
                    status = EmergencyCallUpdateStatus.CANCELLED,
                )
                Endpoint.INQUIRY_LIST -> InquiryRemoteDataSourceImpl(
                    service(InquiryApiService::class.java),
                    executor,
                ).getInquiryPage(
                    cursor = null,
                    limit = 20,
                )
                Endpoint.INQUIRY_SUBMIT -> InquiryRemoteDataSourceImpl(
                    service(InquiryApiService::class.java),
                    executor,
                ).submitInquiry(
                    title = "test",
                    content = "test",
                )
                Endpoint.PROFILE_GET -> WorkerProfileRemoteDataSourceImpl(
                    service(WorkerProfileApiService::class.java),
                    executor,
                ).getWorkerProfile()
                Endpoint.PROFILE_UPDATE -> WorkerProfileRemoteDataSourceImpl(
                    service(WorkerProfileApiService::class.java),
                    executor,
                ).updateWorkerName(
                    name = "test",
                    version = null,
                )
                Endpoint.PASSWORD_CHANGE -> WorkerProfileRemoteDataSourceImpl(
                    service(WorkerProfileApiService::class.java),
                    executor,
                ).changePassword(
                    currentPassword = "test-current",
                    newPassword = "test-new",
                )
            }
        } finally {
            server.close()
        }
    }

    enum class Endpoint(val payload: String) {
        NOTIFICATION_LIST("""{"items":[],"unreadCount":0}"""),
        NOTIFICATION_READ("""{"notificationId":"notification","read":true}"""),
        NOTIFICATION_READ_ALL("""{"updatedCount":0,"unreadCount":0}"""),
        HISTORY_LIST("""{"items":[]}"""),
        HISTORY_DETAIL("""{"recordId":"record"}"""),
        HOME("""{"team":{"teamId":"team"},"site":{"siteId":"site"},"weather":null}"""),
        CALL_REGISTER("""{"callId":"call","status":"ACTIVE"}"""),
        CALL_CURRENT("""{"callId":null,"status":"NONE"}"""),
        CALL_UPDATE("""{"callId":"call","status":"CANCELLED"}"""),
        INQUIRY_LIST("""{"items":[]}"""),
        INQUIRY_SUBMIT("""{"inquiryId":"inquiry","status":"OPEN","deliveryStatus":"PENDING","createdAt":"2026-10-04T01:00:00Z"}"""),
        PROFILE_GET("""{"userId":"worker"}"""),
        PROFILE_UPDATE("""{"userId":"worker"}"""),
        PASSWORD_CHANGE("""{"changedAt":"2026-10-04T01:00:00Z"}"""),
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun endpoints(): List<Array<Any>> {
            return Endpoint.entries.map { endpoint -> arrayOf<Any>(endpoint) }
        }
    }
}
