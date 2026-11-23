package com.beeeam.domain.model

import kotlin.time.Duration
import kotlin.time.Instant

/**
 * 목록에 보여 줄 마친 러닝 기록의 요약.
 *
 * 목록에서 기록마다 경로(지점 수천 개)를 읽지 않도록 경로 없이 저장된 값만 담는다.
 * 경로가 필요하면 id 로 [RunningSession] 을 따로 읽는다.
 *
 * @property distanceMeters 저장할 때 경로로 계산한 거리.
 */
data class RunningSummary(
    val id: Long,
    val startedAt: Instant,
    val endedAt: Instant,
    val duration: Duration,
    val pausedDuration: Duration,
    val distanceMeters: Double,
) {
    /** 평균 페이스. 거리가 너무 짧으면 null. */
    val averagePace: Duration?
        get() = paceOf(distanceMeters, duration)
}
