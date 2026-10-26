package com.nativelap.heartguard.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject
import javax.inject.Singleton

/** App Link에서 받은 일회용 토큰을 암호화 저장소와 메모리에 보관한다. */
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
            val token = link?.let(::extractToken)
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

        private fun extractToken(link: String): String? {
            val uri =
                try {
                    URI(link)
                } catch (_: URISyntaxException) {
                    return null
                }

            if (!uri.scheme.equals(SUPPORTED_SCHEME, ignoreCase = true) ||
                !uri.host.equals(SUPPORTED_HOST, ignoreCase = true) ||
                uri.rawUserInfo != null ||
                uri.port != NO_EXPLICIT_PORT ||
                uri.rawQuery != null ||
                uri.rawFragment != null
            ) {
                return null
            }

            val rawPath = uri.rawPath ?: return null
            val decodedPath = uri.path ?: return null
            val linkPathPrefix =
                LINK_PATH_PREFIXES.firstOrNull { prefix -> rawPath.startsWith(prefix) }
                    ?: return null
            val token = decodedPath.removePrefix(linkPathPrefix)
            if (
                token.isBlank() ||
                PATH_SEPARATOR in token ||
                token.length > MAX_TOKEN_LENGTH
            ) {
                return null
            }

            return token.takeIf { value ->
                value.none(Char::isISOControl) &&
                    value.none(Char::isWhitespace)
            }
        }

        private companion object {
            const val SUPPORTED_SCHEME = "https"
            const val SUPPORTED_HOST = "heatguard-temp.https.gsmsv.site"
            const val PATH_SEPARATOR = '/'
            val LINK_PATH_PREFIXES =
                listOf(
                    "/api/v1/app/links/",
                    "/api/v1/team/invitations/",
                )
            const val NO_EXPLICIT_PORT = -1
            const val MAX_TOKEN_LENGTH = 256
        }
    }

data class PendingAppLink(
    val token: String,
    val linkId: Long,
) {
    override fun toString(): String = "PendingAppLink(linkId=$linkId)"
}
