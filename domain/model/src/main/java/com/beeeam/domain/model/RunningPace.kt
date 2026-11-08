package com.beeeam.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** 현재 페이스를 계산할 때 보는 최근 구간의 길이. */
val CURRENT_PACE_WINDOW: Duration = 30.seconds

/**
 * 페이스를 계산할 최소 거리(미터).
 *
 * 출발 직후처럼 거리가 거의 0 이면 페이스가 수십 분 / km 처럼 비정상적으로 커지므로 이보다 짧으면 계산하지 않는다.
 */
const val MIN_PACE_DISTANCE_METERS = 20.0

/** [distanceMeters] 를 [duration] 동안 달렸을 때 1km 를 달리는 데 걸리는 시간. 거리가 너무 짧으면 null. */
fun paceOf(distanceMeters: Double, duration: Duration): Duration? {
    if (distanceMeters < MIN_PACE_DISTANCE_METERS || !duration.isPositive()) return null
    return duration * (1000 / distanceMeters)
}

/**
 * 구간의 마지막 [window] 동안의 페이스.
 *
 * 마지막 지점의 시각을 기준으로 [window] 안에 들어오는 지점들만 본다.
 * 지점이 하나뿐이거나 그 사이 거리가 너무 짧으면 null.
 */
fun List<LocationPoint>.recentPace(window: Duration = CURRENT_PACE_WINDOW): Duration? {
    val last = lastOrNull() ?: return null
    val recent = takeLastWhile { last.timestamp - it.timestamp <= window }
    if (recent.size < 2) return null
    return paceOf(recent.distanceMeters(), last.timestamp - recent.first().timestamp)
}
