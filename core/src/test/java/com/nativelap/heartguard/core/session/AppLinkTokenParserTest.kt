package com.nativelap.heartguard.core.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppLinkTokenParserTest {
    @Test
    fun `extractToken accepts custom team invitation URI`() {
        assertEquals(
            "token-123",
            AppLinkTokenParser.extractToken("heatguard://team/invitations/token-123"),
        )
    }

    @Test
    fun `extractToken rejects previous team invitation URI scheme`() {
        assertNull(
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
            AppLinkTokenParser.extractToken("heatguard://other/invitations/token-123"),
        )
        assertNull(
            AppLinkTokenParser.extractToken("heatguard://team/links/token-123"),
        )
        assertNull(
            AppLinkTokenParser.extractToken("heatguard:team/invitations/token-123"),
        )
        assertNull(
            AppLinkTokenParser.extractToken("heatguard://user@team/invitations/token-123"),
        )
        assertNull(
            AppLinkTokenParser.extractToken("heatguard://team:443/invitations/token-123"),
        )
    }

    @Test
    fun `extractToken rejects invalid token paths and URI components`() {
        assertNull(AppLinkTokenParser.extractToken("heatguard://team/invitations/"))
        assertNull(
            AppLinkTokenParser.extractToken("heatguard://team/invitations/token/extra"),
        )
        assertNull(
            AppLinkTokenParser.extractToken("heatguard://team/invitations/token?source=mail"),
        )
        assertNull(
            AppLinkTokenParser.extractToken("heatguard://team/invitations/token#section"),
        )
        assertNull(
            AppLinkTokenParser.extractToken("heatguard://team/invitations/token%20with%20space"),
        )
        assertNull(
            AppLinkTokenParser.extractToken("heatguard://team/invitations/%0Atoken"),
        )
        assertNull(
            AppLinkTokenParser.extractToken(
                "heatguard://team/invitations/${"t".repeat(257)}",
            ),
        )
        assertNull(
            AppLinkTokenParser.extractToken("heatguard://team/invitations/%2F"),
        )
    }
}
