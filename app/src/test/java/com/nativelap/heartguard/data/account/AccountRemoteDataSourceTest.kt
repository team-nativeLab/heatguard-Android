package com.nativelap.heartguard.data.account

import com.nativelap.heartguard.core.network.ApiAuthentication
import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.core.network.ApiRetrofitFactory
import com.nativelap.heartguard.data.account.mapper.toWithdrawAccountResult
import com.nativelap.heartguard.data.account.remote.AccountApiService
import com.nativelap.heartguard.data.account.remote.AccountRemoteDataSourceImpl
import com.nativelap.heartguard.domain.account.model.WithdrawAccountResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test

/** 회원탈퇴 API(DELETE /api/v1/site/profile)의 실제 HTTP 왕복을 MockWebServer로 확인한다. */
class AccountRemoteDataSourceTest {
    @Test
    fun sendsDeleteWithCurrentPasswordAndTreatsNoContentAsSuccess() = runTest {
        withAccountDataSource(
            MockResponse.Builder().code(204).build(),
        ) { dataSource, mockWebServer ->
            val withdrawResult = dataSource.withdraw(currentPassword = "pw1234")

            assertEquals(ApiResult.Success(Unit), withdrawResult)
            assertEquals(WithdrawAccountResult.Success, withdrawResult.toWithdrawAccountResult())

            val recordedRequest = mockWebServer.takeRequest()
            assertEquals("DELETE", recordedRequest.method)
            assertEquals("/api/v1/site/profile", recordedRequest.url.encodedPath)
            assertEquals("""{"currentPassword":"pw1234"}""", recordedRequest.body?.utf8())
        }
    }

    @Test
    fun mapsInvalidCredentialsToInvalidPassword() = runTest {
        withAccountDataSource(
            MockResponse.Builder()
                .code(401)
                .body("""{"success":false,"data":null,"error":{"code":"INVALID_CREDENTIALS","message":"비밀번호 불일치"}}""")
                .build(),
        ) { dataSource, _ ->
            val withdrawResult = dataSource.withdraw(currentPassword = "wrong")

            assertEquals(
                ApiResult.Failure(ApiError.Http(statusCode = 401, errorCode = "INVALID_CREDENTIALS")),
                withdrawResult,
            )
            assertEquals(WithdrawAccountResult.InvalidPassword, withdrawResult.toWithdrawAccountResult())
        }
    }

    @Test
    fun mapsOtherServerErrorsToFailure() = runTest {
        withAccountDataSource(
            MockResponse.Builder()
                .code(500)
                .body("""{"success":false,"data":null,"error":{"code":"INTERNAL_ERROR","message":"오류"}}""")
                .build(),
        ) { dataSource, _ ->
            val withdrawResult = dataSource.withdraw(currentPassword = "pw1234")

            assertEquals(WithdrawAccountResult.Failure, withdrawResult.toWithdrawAccountResult())
        }
    }

    private suspend fun withAccountDataSource(
        mockResponse: MockResponse,
        block: suspend (AccountRemoteDataSourceImpl, MockWebServer) -> Unit,
    ) {
        val mockWebServer = MockWebServer()
        mockWebServer.start()
        try {
            mockWebServer.enqueue(mockResponse)

            val client = OkHttpClient()
            val apiRetrofitFactory = ApiRetrofitFactory(
                authenticatedApiClient = client,
                unauthenticatedApiClient = client,
                json = Json {
                    ignoreUnknownKeys = true
                },
            )
            val accountApiService = apiRetrofitFactory.createService(
                baseUrl = mockWebServer.url("/").toString(),
                serviceClass = AccountApiService::class.java,
                authentication = ApiAuthentication.BEARER,
            )
            val dataSource = AccountRemoteDataSourceImpl(
                accountApiService = accountApiService,
                apiExecutor = ApiExecutor(),
            )

            block(dataSource, mockWebServer)
        } finally {
            mockWebServer.close()
        }
    }
}
