package com.nativelap.heartguard.data.site.dto

import com.nativelap.heartguard.core.network.ApiEnvelope
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TeamSiteResponseDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `기상값 미입력 상태의 실서버 홈 응답을 역직렬화한다`() {
        // 2026-09-28 테스트 계정으로 받은 GET /api/v1/team 응답 구조(식별자만 예시 값으로 교체)
        val responseBody = """
            {"success":true,"data":{"date":"2026-09-28",
            "team":{"teamId":"team_01","name":"Test Team","workplace":"Temporary","leaderName":"Test User",
            "leaderPhone":"010-0000-0000","workerCount":1,"version":1,"active":true},
            "site":{"name":"Test Site","managerName":"","managerEmail":"","managerPhone":"","active":true,
            "version":1,"siteId":"site_01"},
            "weather":{"temperature":null,"humidity":null,"apparentTemperature":null,"heatLevel":null,"observedAt":null},
            "heatLevel":null,"checkTimes":[],"checklistSummary":{"total":3,"completed":0},"activeEmergencyCall":null},
            "error":null,"meta":{"requestId":"req_01"}}
        """.trimIndent()

        val envelope = json.decodeFromString<ApiEnvelope<TeamSiteResponseDto>>(responseBody)
        val homeResponse = envelope.data ?: error("data missing")

        assertEquals("Test Team", homeResponse.team.name)
        assertEquals("Temporary", homeResponse.team.workplace)
        assertEquals("", homeResponse.site.managerPhone)
        assertNull(homeResponse.heatLevel)
        assertNull(homeResponse.weather?.temperature)
    }

    @Test
    fun `명세 v0_1의 본사 연락처와 하늘 상태, 온도 변화량을 역직렬화한다`() {
        val responseBody = """
            {"success":true,"data":{"date":"2026-09-29",
            "company":{"companyId":"cmp_01","name":"Test Company","phone":"02-000-0000"},
            "team":{"teamId":"team_01","name":"Test Team","workplace":"Temporary"},
            "site":{"siteId":"site_01","name":"Test Site","managerPhone":"010-0000-0000"},
            "weather":{"temperature":31.5,"humidity":60,"apparentTemperature":33.0,"heatLevel":1,
            "observedAt":"2026-09-29T10:00:00+09:00","skyStatus":"CLEAR","temperatureDelta":-1.5,
            "comparisonTemperature":33.0,"comparisonObservedAt":"2026-09-29T09:00:00+09:00",
            "comparisonBasis":"PREVIOUS_OBSERVATION"},
            "unreadNotificationCount":0,"heatLevel":1,"checkTimes":["09:00"],"activeEmergencyCall":null},
            "error":null,"meta":{"requestId":"req_01"}}
        """.trimIndent()

        val envelope = json.decodeFromString<ApiEnvelope<TeamSiteResponseDto>>(responseBody)
        val homeResponse = envelope.data ?: error("data missing")

        assertEquals("02-000-0000", homeResponse.company?.phone)
        assertEquals("CLEAR", homeResponse.weather?.skyStatus)
        assertEquals(-1.5, homeResponse.weather?.temperatureDelta ?: error("delta missing"), 0.0)
    }

    @Test
    fun `company가 null이어도 홈 응답을 역직렬화한다`() {
        val responseBody = """
            {"success":true,"data":{"company":null,
            "team":{"teamId":"team_01"},"site":{"siteId":"site_01"},
            "weather":{"skyStatus":null,"temperatureDelta":null},"checkTimes":[]},
            "error":null,"meta":{"requestId":"req_01"}}
        """.trimIndent()

        val envelope = json.decodeFromString<ApiEnvelope<TeamSiteResponseDto>>(responseBody)
        val homeResponse = envelope.data ?: error("data missing")

        assertNull(homeResponse.company)
        assertNull(homeResponse.weather?.skyStatus)
        assertNull(homeResponse.weather?.temperatureDelta)
    }
}
