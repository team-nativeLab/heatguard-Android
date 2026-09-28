package com.nativelap.heartguard.viewmodel.auth

import com.nativelap.heartguard.domain.auth.model.TeamLoginResult
import com.nativelap.heartguard.domain.auth.repository.TeamAuthRepository
import com.nativelap.heartguard.domain.auth.usecase.TeamLoginUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TeamLoginViewModelTest {

    @Test
    fun loginForwardsNonEmailIdentifierToRepository() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))

        try {
            var requestedEmail: String? = null
            val repository = object : TeamAuthRepository {
                override suspend fun login(email: String, password: String): TeamLoginResult {
                    requestedEmail = email
                    return TeamLoginResult.Failure
                }

                override suspend fun logout() = Unit
            }
            val viewModel = TeamLoginViewModel(
                teamLoginUseCase = TeamLoginUseCase(repository),
            )

            viewModel.login(email = "test1234", password = "sample-password")
            advanceUntilIdle()

            assertEquals("test1234", requestedEmail)
            assertEquals(TeamLoginFailure.GENERIC, viewModel.uiState.value.failure)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
