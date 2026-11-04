package com.beeeam.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoPointTest {

    @Test
    fun `위도와 경도의 경계값은 허용한다`() {
        val southWest = GeoPoint(latitude = -90.0, longitude = -180.0)
        val northEast = GeoPoint(latitude = 90.0, longitude = 180.0)

        assertEquals(-90.0, southWest.latitude, 0.0)
        assertEquals(180.0, northEast.longitude, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `위도가 90 을 넘으면 예외가 발생한다`() {
        GeoPoint(latitude = 90.1, longitude = 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `위도가 -90 보다 작으면 예외가 발생한다`() {
        GeoPoint(latitude = -90.1, longitude = 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `경도가 180 을 넘으면 예외가 발생한다`() {
        GeoPoint(latitude = 0.0, longitude = 180.1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `경도가 -180 보다 작으면 예외가 발생한다`() {
        GeoPoint(latitude = 0.0, longitude = -180.1)
    }
}
