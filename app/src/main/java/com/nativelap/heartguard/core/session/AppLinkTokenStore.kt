package com.nativelap.heartguard.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject
import javax.inject.Singleton

/** App Link에서 받은 일회용 현장 토큰을 연결 화면이 소비할 때까지 메모리에만 보관한다. */
@Singleton
class AppLinkTokenStore
    @Inject
    constructor() {
        private val mutablePendingLink = MutableStateFlow<PendingAppLink?>(null)
        private var nextLinkId = 0L

        val pendingLink: StateFlow<PendingAppLink?> = mutablePendingLink.asStateFlow()

        fun accept(link: String?) {
            val token = link?.let(::extractToken)
            if (token != null) {
                nextLinkId += 1L
                mutablePendingLink.value = PendingAppLink(token, nextLinkId)
            }
        }

        fun consume(link: PendingAppLink) {
            if (mutablePendingLink.value == link) {
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
            val token = decodedPath.removePrefix(LINK_PATH_PREFIX)
            if (!rawPath.startsWith(LINK_PATH_PREFIX) ||
                token.isBlank() ||
                PATH_SEPARATOR in token
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
            const val LINK_PATH_PREFIX = "/api/v1/app/links/"
            const val NO_EXPLICIT_PORT = -1
        }
    }

data class PendingAppLink(
    val token: String,
    val linkId: Long,
) {
    override fun toString(): String = "PendingAppLink(linkId=$linkId)"
}
