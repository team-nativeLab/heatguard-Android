package com.nativelap.heartguard.data.account.mapper

import com.nativelap.heartguard.core.network.ApiError
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.account.model.WithdrawAccountResult

/** 회원탈퇴 API가 돌려주는 서버 오류 코드 중 앱이 구분해서 처리하는 값이다. Domain·Presentation에는 노출하지 않는다. */
enum class AccountServerErrorCode {
    INVALID_CREDENTIALS,
    UNKNOWN,
    ;

    companion object {
        fun from(errorCode: String?): AccountServerErrorCode {
            return when (errorCode) {
                "INVALID_CREDENTIALS" -> INVALID_CREDENTIALS
                else -> UNKNOWN
            }
        }
    }
}

/** 회원탈퇴 원격 결과를 도메인 결과로 바꾼다. 비밀번호 불일치(401 INVALID_CREDENTIALS)만 따로 구분하고
 * 나머지 오류(네트워크·권한·서버 오류 등)는 모두 일반 실패로 모은다. */
fun ApiResult<Unit>.toWithdrawAccountResult(): WithdrawAccountResult {
    return when (this) {
        is ApiResult.Success -> WithdrawAccountResult.Success
        is ApiResult.Failure -> error.toWithdrawAccountResult()
    }
}

private fun ApiError.toWithdrawAccountResult(): WithdrawAccountResult {
    if (this !is ApiError.Http) {
        return WithdrawAccountResult.Failure
    }

    return when (AccountServerErrorCode.from(errorCode)) {
        AccountServerErrorCode.INVALID_CREDENTIALS -> WithdrawAccountResult.InvalidPassword
        AccountServerErrorCode.UNKNOWN -> WithdrawAccountResult.Failure
    }
}
