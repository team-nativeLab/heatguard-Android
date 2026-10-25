package com.nativelap.heartguard.domain.profile.usecase

import com.nativelap.heartguard.domain.profile.model.PasswordChangeResult
import com.nativelap.heartguard.domain.profile.repository.WorkerProfileRepository
import javax.inject.Inject

/** 현재 비밀번호 확인 후 새 비밀번호로 변경한다. 새 비밀번호가 앱 규칙([isValidNewPassword])을 어기면 서버에 보내지 않는다. */
class ChangeWorkerPasswordUseCase
    @Inject
    constructor(
        private val workerProfileRepository: WorkerProfileRepository,
    ) {
        suspend operator fun invoke(
            currentPassword: String,
            newPassword: String,
        ): PasswordChangeResult {
            if (!isValidNewPassword(newPassword)) {
                return PasswordChangeResult.InvalidNewPassword
            }

            return workerProfileRepository.changePassword(
                currentPassword = currentPassword,
                newPassword = newPassword,
            )
        }

        companion object {
            private const val MIN_PASSWORD_LENGTH = 8

            /** Figma 26 입력 규칙 "영문, 숫자를 포함해 8자 이상"이다. 서버는 8자 이상만 검사한다. */
            fun isValidNewPassword(newPassword: String): Boolean =
                newPassword.length >= MIN_PASSWORD_LENGTH &&
                    newPassword.any { passwordChar -> passwordChar.isAsciiLetter() } &&
                    newPassword.any { passwordChar -> passwordChar.isDigit() }

            private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'
        }
    }
