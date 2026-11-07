package com.nativelap.heartguard.view.route.home

import com.nativelap.heartguard.R
import com.nativelap.heartguard.domain.site.model.SkyStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeRouteWeatherImageTest {
    @Test
    fun `snow status uses the snow weather image`() {
        assertEquals(
            R.drawable.weather_snow,
            skyStatusImageRes(SkyStatus.SNOW),
        )
    }

    @Test
    fun `other statuses keep their existing weather images`() {
        assertEquals(R.drawable.weather_sunny, skyStatusImageRes(SkyStatus.CLEAR))
        assertEquals(R.drawable.weather_cloud, skyStatusImageRes(SkyStatus.CLOUDY))
        assertEquals(R.drawable.weather_rain, skyStatusImageRes(SkyStatus.RAIN))
        assertEquals(
            R.drawable.heartguard_home_weather,
            skyStatusImageRes(SkyStatus.PARTLY_CLOUDY),
        )
        assertEquals(
            R.drawable.heartguard_home_weather,
            skyStatusImageRes(SkyStatus.UNKNOWN),
        )
        assertEquals(R.drawable.heartguard_home_weather, skyStatusImageRes(null))
    }
}
