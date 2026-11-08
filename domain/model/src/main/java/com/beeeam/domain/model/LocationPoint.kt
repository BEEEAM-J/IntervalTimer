package com.beeeam.domain.model

import kotlin.time.Instant

/**
 * 러닝 중 GPS 가 기록한 한 지점.
 *
 * 거리·페이스 계산과 노이즈 필터링에는 위치뿐 아니라 측정 시각과 정확도가 필요하므로 [GeoPoint] 에 이 정보를 더한다.
 *
 * @property point 위도 / 경도.
 * @property timestamp 위치를 측정한 시각.
 * @property accuracy 정확도 반경(미터). 실제 위치가 이 반경 안에 있을 가능성이 높다는 뜻으로, 작을수록 정확하다.
 */
data class LocationPoint(
    val point: GeoPoint,
    val timestamp: Instant,
    val accuracy: Float,
) {
    init {
        require(accuracy >= 0f) { "accuracy must not be negative: $accuracy" }
    }
}

/** 한 구간의 거리(미터). 이웃한 지점 사이의 직선 거리를 순서대로 더한다. */
fun List<LocationPoint>.distanceMeters(): Double =
    zipWithNext { from, to -> from.point.distanceTo(to.point) }.sum()
