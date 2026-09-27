package com.nativelap.heartguard.domain.account.usecase

import com.nativelap.heartguard.domain.account.model.WithdrawAccountResult
import com.nativelap.heartguard.domain.account.repository.AccountRepository
import javax.inject.Inject

/** 현재 사용자의 계정을 탈퇴 처리한다. 회원탈퇴 최종 확인에서 호출되며,
 * [currentPassword]는 본인 확인용 현재 비밀번호다. 비밀번호 불일치와 그 외 실패를 구분해 [WithdrawAccountResult]로 돌려준다. */
class WithdrawAccountUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
) {
    suspend operator fun invoke(currentPassword: String): WithdrawAccountResult {
        return accountRepository.withdraw(currentPassword = currentPassword)
    }
}
