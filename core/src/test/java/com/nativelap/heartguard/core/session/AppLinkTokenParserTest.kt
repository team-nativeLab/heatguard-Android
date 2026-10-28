package com.nativelap.heartguard.core.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppLinkTokenParserTest {
    @Test
    fun `extractToken accepts custom team invitation URI`() {
        assertEquals(
            "token-123",
            AppLinkTokenParser.extractToken("heartguard://team/invitations/token-123"),
        )
    }

    @Test
    fun `extractToken rejects old HTTPS links`() {
        assertNull(
            AppLinkTokenParser.extractToken(
                "https://heatguard-temp.https.gsmsv.site/api/v1/app/links/token-123",
            ),
        )
        assertNull(
            AppLinkTokenParser.extractToken(
                "https://heatguard-temp.https.gsmsv.site/api/v1/team/invitations/token-123",
            ),
        )
    }

    @Test
    fun `extractToken rejects incorrect authorities and paths`() {
        assertNull(
            AppLinkTokenParser.extractToken("heartguard://other/invitations/token-123"),
        )
        assertNull(
            AppLinkTokenParser.extractToken("heartguard://team/links/token-123"),
        )
        assertNull(
            AppLinkTokenParser.extractToken("heartguard:team/invitations/token-123"),
        )
        assertNull(
            AppLinkTokenParser.extractToken("heartguard://user@team/invitations/token-123"),
        )
        assertNull(
            AppLinkTokenParser.extractToken("heartguard://team:443/invitations/token-123"),
        )
    }

    @Test
    fun `extractToken rejects invalid token paths and URI components`() {
        assertNull(AppLinkTokenParser.extractToken("heartguard://team/invitations/"))
        assertNull(
            AppLinkTokenParser.extractToken("heartguard://team/invitations/token/extra"),
        )
        assertNull(
            AppLinkTokenParser.extractToken("heartguard://team/invitations/token?source=mail"),
        )
        assertNull(
            AppLinkTokenParser.extractToken("heartguard://team/invitations/token#section"),
        )
        assertNull(
            AppLinkTokenParser.extractToken("heartguard://team/invitations/token%20with%20space"),
        )
        assertNull(
            AppLinkTokenParser.extractToken("heartguard://team/invitations/%0Atoken"),
        )
        assertNull(
            AppLinkTokenParser.extractToken(
                "heartguard://team/invitations/${"t".repeat(257)}",
            ),
        )
        assertNull(
            AppLinkTokenParser.extractToken("heartguard://team/invitations/%2F"),
        )
    }
}
