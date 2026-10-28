package com.nativelap.heartguard.core.session

import java.net.URI
import java.net.URISyntaxException

internal object AppLinkTokenParser {
    fun extractToken(link: String): String? {
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
        if (!rawPath.startsWith(LINK_PATH_PREFIX)) {
            return null
        }
        val token = decodedPath.removePrefix(LINK_PATH_PREFIX)
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

    private const val SUPPORTED_SCHEME = "heartguard"
    private const val SUPPORTED_HOST = "team"
    private const val LINK_PATH_PREFIX = "/invitations/"
    private const val PATH_SEPARATOR = '/'
    private const val NO_EXPLICIT_PORT = -1
    private const val MAX_TOKEN_LENGTH = 256
}
