package com.beeeam.domain.model

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
}
