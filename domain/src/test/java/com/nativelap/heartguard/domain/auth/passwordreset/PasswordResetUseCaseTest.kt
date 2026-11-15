package com.nativelap.heartguard.domain.auth.passwordreset

import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetCompletion
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetEmailCode
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetError
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetResult
import com.nativelap.heartguard.domain.auth.passwordreset.model.PasswordResetVerification
import com.nativelap.heartguard.domain.auth.passwordreset.repository.PasswordResetRepository
import com.nativelap.heartguard.domain.auth.passwordreset.usecase.RequestPasswordResetEmailCodeUseCase
import com.nativelap.heartguard.domain.auth.passwordreset.usecase.ResetPasswordUseCase
import com.nativelap.heartguard.domain.auth.passwordreset.usecase.VerifyPasswordResetEmailCodeUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordResetUseCaseTest {
    @Test
    fun requestEmailCodeNormalizesEmailBeforeCallingRepository() =
        runBlocking {
            val repository = FakePasswordResetRepository()
            val useCase = RequestPasswordResetEmailCodeUseCase(repository)

            useCase(" Person@Example.com ")

            assertEquals("person@example.com", repository.requestedEmail)
        }

    @Test
    fun verifyEmailCodeKeepsLeadingZeroes() =
        runBlocking {
            val repository = FakePasswordResetRepository()
            val useCase = VerifyPasswordResetEmailCodeUseCase(repository)

            val verificationResult = useCase(" Person@Example.com ", "012345")

            assertEquals("012345", repository.verifiedCode)
            assertTrue(verificationResult is PasswordResetResult.Success)
        }

    @Test
    fun verificationToStringDoesNotExposeResetToken() {
        val verification =
            PasswordResetVerification(expiresAt = "2026-11-15T12:10:00Z", resetToken = "sensitive-reset-token")

        assertFalse(verification.toString().contains("sensitive-reset-token"))
    }

    @Test
    fun verifyEmailCodeRejectsNonSixDigitCodeWithoutCallingRepository() =
        runBlocking {
            val repository = FakePasswordResetRepository()
            val useCase = VerifyPasswordResetEmailCodeUseCase(repository)

            val verificationResult = useCase("person@example.com", "1234")

            assertEquals(
                PasswordResetError.Validation,
                (verificationResult as PasswordResetResult.Failure).error,
            )
            assertFalse(repository.didVerifyCode)
        }

    @Test
    fun resetPasswordRejectsWeakOrMismatchedPasswordWithoutCallingRepository() =
        runBlocking {
            val repository = FakePasswordResetRepository()
            val useCase = ResetPasswordUseCase(repository)
            val validPassword = listOf("Field", "Worker", "2048").joinToString(separator = "")
            val mismatchedPassword = listOf("Field", "Worker", "2049").joinToString(separator = "")

            val weakPasswordResult =
                useCase(
                    email = "person@example.com",
                    resetToken = "temporary-token",
                    newPassword = "short1",
                    confirmPassword = "short1",
                )
            val mismatchedPasswordResult =
                useCase(
                    email = "person@example.com",
                    resetToken = "temporary-token",
                    newPassword = validPassword,
                    confirmPassword = mismatchedPassword,
                )

            assertEquals(
                PasswordResetError.Validation,
                (weakPasswordResult as PasswordResetResult.Failure).error,
            )
            assertEquals(
                PasswordResetError.Validation,
                (mismatchedPasswordResult as PasswordResetResult.Failure).error,
            )
            assertFalse(repository.didResetPassword)
        }

    private class FakePasswordResetRepository : PasswordResetRepository {
        var requestedEmail: String? = null
        var verifiedCode: String? = null
        var didVerifyCode = false
        var didResetPassword = false

        override suspend fun requestEmailCode(email: String): PasswordResetResult<PasswordResetEmailCode> {
            requestedEmail = email
            return PasswordResetResult.Success(PasswordResetEmailCode(expiresAt = "2026-11-15T12:10:00Z"))
        }

        override suspend fun verifyEmailCode(
            email: String,
            verificationCode: String,
        ): PasswordResetResult<PasswordResetVerification> {
            verifiedCode = verificationCode
            didVerifyCode = true
            return PasswordResetResult.Success(
                PasswordResetVerification(
                    resetToken = "temporary-token",
                    expiresAt = "2026-11-15T12:10:00Z",
                ),
            )
        }

        override suspend fun resetPassword(
            email: String,
            resetToken: String,
            newPassword: String,
            confirmPassword: String,
        ): PasswordResetResult<PasswordResetCompletion> {
            didResetPassword = true
            return PasswordResetResult.Success(
                PasswordResetCompletion(changedAt = "2026-11-15T12:10:00Z"),
            )
        }
    }
}
