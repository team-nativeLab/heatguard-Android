package com.nativelap.heartguard.data.account.remote

import com.nativelap.heartguard.core.network.ApiExecutor
import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.account.model.WithdrawReason
import javax.inject.Inject

/** 회원탈퇴 원격 요청의 임시 구현이다.
 * TODO: 계정 삭제 API 명세가 확정되면 AccountApiService·요청 DTO를 추가하고, 아래 execute 블록 안에서
 *  apiService를 호출하도록 교체한다. 명세가 없는 상태에서 endpoint·필드명을 추측해 만들지 않기 위해
 *  지금은 서버를 호출하지 않고 항상 성공을 돌려준다. 따라서 실제 계정은 서버에서 삭제되지 않는다.
 *  ApiExecutor 경로는 그대로 두어 반환 타입·취소 처리·오류 변환이 실제 구현과 같게 유지된다.
 *  비밀번호 오류를 서버가 401로 응답하면 BearerTokenAuthenticator가 세션을 즉시 만료시키므로, 연동 시 400/403 사용을 서버와 협의한다. */
class AccountRemoteDataSourceImpl @Inject constructor(
    private val apiExecutor: ApiExecutor,
) : AccountRemoteDataSource {

    override suspend fun withdraw(
        password: String,
        reason: WithdrawReason?,
    ): ApiResult<Unit> {
        return apiExecutor.execute {
            Unit
        }
    }
}
