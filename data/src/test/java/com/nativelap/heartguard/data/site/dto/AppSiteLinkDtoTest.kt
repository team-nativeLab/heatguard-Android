package com.nativelap.heartguard.data.site.dto

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppSiteLinkDtoTest {
    @Test
    fun linkedSitesResponseParsesTeamAndSiteFieldsWithoutLinkedAt() {
        val response =
            Json.decodeFromString<AppLinkedSitesResponseDto>(
                """
                {
                  "items": [
                    {
                      "teamId": "team_01",
                      "teamName": "철근팀",
                      "siteId": "site_01",
                      "siteName": "서울현장",
                      "companyId": "company_01"
                    }
                  ],
                  "selectedTeamId": "team_01"
                }
                """.trimIndent(),
            )

        assertEquals("team_01", response.items.single().teamId)
        assertEquals("철근팀", response.items.single().teamName)
        assertEquals("site_01", response.items.single().siteId)
        assertEquals("서울현장", response.items.single().siteName)
        assertEquals("company_01", response.items.single().companyId)
        assertEquals("team_01", response.selectedTeamId)
    }

    @Test
    fun linkedSitesResponseAllowsNoSelectedTeam() {
        val response =
            Json.decodeFromString<AppLinkedSitesResponseDto>(
                """
                {"items": [], "selectedTeamId": null}
                """.trimIndent(),
            )

        assertEquals(emptyList(), response.items)
        assertEquals(null, response.selectedTeamId)
    }

    @Test
    fun linkedSitesResponseRequiresSelectedTeamIdKey() {
        assertFailsWith<SerializationException> {
            Json.decodeFromString<AppLinkedSitesResponseDto>("""{"items": []}""")
        }
    }

    @Test
    fun selectionResponseParsesTeamAndSiteFieldsWithoutSiteName() {
        val response =
            Json.decodeFromString<AppSiteSelectionResponseDto>(
                """
                {
                  "teamId": "team_01",
                  "teamName": "철근팀",
                  "siteId": "site_01"
                }
                """.trimIndent(),
            )

        assertEquals("team_01", response.teamId)
        assertEquals("철근팀", response.teamName)
        assertEquals("site_01", response.siteId)
    }
}
