package com.nativelap.heartguard.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 커스텀 URI에서 받은 일회용 토큰을 암호화 저장소와 메모리에 보관한다. */
@Singleton
class AppLinkTokenStore
    @Inject
    constructor(
        private val tokenStorage: AndroidKeystoreTokenStorage,
    ) {
        private var nextLinkId = 0L
        private val mutablePendingLink =
            MutableStateFlow(
                tokenStorage.readPendingAppLinkToken()?.let { token ->
                    PendingAppLink(token, ++nextLinkId)
                },
            )

        val pendingLink: StateFlow<PendingAppLink?> = mutablePendingLink.asStateFlow()

        @Synchronized
        fun accept(link: String?): Boolean {
            val token = link?.let(AppLinkTokenParser::extractToken)
            if (token != null) {
                try {
                    tokenStorage.savePendingAppLinkToken(token)
                } catch (_: Exception) {
                    runCatching(tokenStorage::clearPendingAppLinkToken)
                    mutablePendingLink.value = null
                    return false
                }
                nextLinkId += 1L
                mutablePendingLink.value = PendingAppLink(token, nextLinkId)
                return true
            }
            return false
        }

        @Synchronized
        fun consume(link: PendingAppLink) {
            if (mutablePendingLink.value == link) {
                try {
                    tokenStorage.clearPendingAppLinkToken()
                } catch (_: Exception) {
                    // Persisted cleanup is best effort; completing the link flow must continue.
                }
                mutablePendingLink.value = null
            }
        }
    }

data class PendingAppLink(
    val token: String,
    val linkId: Long,
) {
    override fun toString(): String = "PendingAppLink(linkId=$linkId)"
}
