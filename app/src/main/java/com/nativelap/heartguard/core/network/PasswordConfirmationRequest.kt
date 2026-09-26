package com.nativelap.heartguard.core.network

/** 현재 비밀번호를 함께 보내 본인 확인을 하는 요청임을 표시하는 OkHttp 요청 태그다.
 * ApiService 메소드에 Retrofit `@Tag` 매개변수로 붙인다. 이런 요청에서 401 INVALID_CREDENTIALS는 세션 만료가 아니라
 * 비밀번호 불일치이므로, BearerTokenAuthenticator는 이 태그가 있을 때만 해당 코드로 세션을 끊지 않는다. */
object PasswordConfirmationRequest
