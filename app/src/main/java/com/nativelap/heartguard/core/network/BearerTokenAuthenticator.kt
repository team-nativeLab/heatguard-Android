package com.nativelap.heartguard.core.network

import com.nativelap.heartguard.core.session.SessionManager
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * 인증이 필요한 API가 401을 반환했을 때 세션을 정리하는 진입점이다.
 *
 * 아직 서버의 refresh token 재발급 계약이 확정되지 않아 access token을 실제로 갱신할 방법이 없다.
 * 그래서 지금은 401을 받으면 재시도하지 않고 즉시 세션을 만료시켜 로그인 화면으로 돌려보낸다.
 * refresh 엔드포인트가 추가되면 이 클래스에서 재발급을 먼저 시도하고, 그 재발급이 실패했을 때만
 * expireSession()을 호출하도록 교체해야 한다.
 */
class BearerTokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // 비밀번호 확인 요청(회원탈퇴 등)의 INVALID_CREDENTIALS는 토큰이 아니라 입력한 비밀번호가 틀렸다는 뜻이므로
        // 세션을 유지한 채 401을 그대로 돌려 화면이 비밀번호 오류를 안내하게 한다.
        if (response.isPasswordMismatch()) {
            return null
        }

        // OkHttp Authenticator는 동기 콜백으로 호출되는 계약이라 suspend로 선언할 수 없다.
        // SessionManager 외에 Repository·UseCase를 직접 호출하지 않으므로 runBlocking 범위를 최소로 둔다.
        runBlocking {
            sessionManager.expireSession()
        }

        // 갱신할 수 있는 토큰이 없으므로 재시도용 Request를 만들지 않고 원래 401 응답을 그대로 전달한다.
        // TODO: refresh 엔드포인트가 추가되면 재발급을 먼저 시도하고, response.priorResponse 체인 길이로
        // 재시도 횟수를 제한해 재발급도 401이 나는 경우 무한 재시도가 발생하지 않도록 막아야 한다.
        return null
    }

    // PasswordConfirmationRequest 태그가 붙은 요청이면서 서버 오류 코드가 INVALID_CREDENTIALS일 때만 true다.
    // peekBody는 원본 응답 본문을 소비하지 않으므로 이후 Retrofit·ApiExecutor가 같은 본문을 다시 읽을 수 있다.
    private fun Response.isPasswordMismatch(): Boolean {
        if (request.tag(PasswordConfirmationRequest::class.java) == null) {
            return false
        }

        val errorBody = try {
            peekBody(ApiErrorCodeReader.MAX_ERROR_BODY_BYTES).string()
        } catch (_: IOException) {
            return false
        }

        return ApiErrorCodeReader.read(errorBody) == INVALID_CREDENTIALS_CODE
    }

    private companion object {
        const val INVALID_CREDENTIALS_CODE = "INVALID_CREDENTIALS"
    }
}
