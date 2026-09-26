package com.nativelap.heartguard.domain.account.usecase

import com.nativelap.heartguard.core.network.ApiResult
import com.nativelap.heartguard.domain.account.model.WithdrawReason
import com.nativelap.heartguard.domain.account.repository.AccountRepository
import javax.inject.Inject

/** 현재 사용자의 계정을 탈퇴 처리한다. 회원탈퇴 최종 확인에서 호출되며,
 * [password]는 본인 확인용 현재 비밀번호, [reason]은 선택하지 않았으면 null이다. 성공 여부를 [ApiResult]로 돌려준다. */
class WithdrawAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(
        password: String,
        reason: WithdrawReason?,
    ): ApiResult<Unit> {
        return accountRepository.withdraw(
            password = password,
            reason = reason,
        )
    }
}
