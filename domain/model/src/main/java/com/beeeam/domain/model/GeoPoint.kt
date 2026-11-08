package com.beeeam.domain.model

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 위도 / 경도 좌표.
 *
 * 도메인을 순수 Kotlin 으로 유지하기 위해 Android 의 `Location` 이나 지도 SDK 의 `LatLng` 대신 사용한다.
 * 변환은 data 계층(위치 조회)과 UI 계층(지도 표시)에서 각각 한다.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in -90.0..90.0) { "latitude must be in -90..90: $latitude" }
        require(longitude in -180.0..180.0) { "longitude must be in -180..180: $longitude" }
    }

    /**
     * [other] 까지의 직선 거리(미터).
     *
     * 지구를 구로 보고 대원 거리를 구하는 Haversine 공식을 쓴다. 러닝처럼 수 km 이내의 거리에서는
     * 지구가 완전한 구가 아니어서 생기는 오차(0.5% 이내)보다 GPS 자체의 오차가 훨씬 크다.
     */
    fun distanceTo(other: GeoPoint): Double {
        val lat1 = latitude.toRadians()
        val lat2 = other.latitude.toRadians()
        val dLat = lat2 - lat1
        val dLng = (other.longitude - longitude).toRadians()
        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
    }

    private companion object {
        /** 지구의 평균 반지름(미터). */
        const val EARTH_RADIUS_METERS = 6_371_000.0

        // 도메인을 순수 Kotlin 으로 유지하기 위해 java.lang.Math.toRadians 대신 직접 변환한다.
        fun Double.toRadians(): Double = this * PI / 180
    }
}
