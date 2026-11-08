package com.beeeam.domain.model

import kotlin.time.Duration
import kotlin.time.Instant

/**
 * 한 번의 러닝 기록.
 *
 * 일시정지했다가 재개할 때마다 새 구간을 시작한다. 거리는 구간 안에서만 더하므로,
 * 일시정지 중에 이동한 거리(이전 구간의 마지막 지점 → 다음 구간의 첫 지점)는 달린 거리로 치지 않는다.
 *
 * @property startedAt 처음 시작한 시각.
 * @property endedAt 종료한 시각. 진행 중이면 null.
 * @property segments 구간별 경로. 일시정지 → 재개마다 새 리스트가 추가된다.
 * @property duration 운동 시간. 시작부터 지금(또는 종료)까지의 시간에서 [pausedDuration] 을 뺀 값.
 * @property pausedDuration 일시정지한 시간의 합계.
 */
data class RunningSession(
    val id: Long,
    val startedAt: Instant,
    val endedAt: Instant?,
    val segments: List<List<LocationPoint>>,
    val duration: Duration,
    val pausedDuration: Duration,
) {
    init {
        require(endedAt == null || endedAt >= startedAt) { "endedAt must not be before startedAt: $startedAt > $endedAt" }
        require(!duration.isNegative()) { "duration must not be negative: $duration" }
        require(!pausedDuration.isNegative()) { "pausedDuration must not be negative: $pausedDuration" }
    }

    /** 달린 거리(미터). 구간별 거리의 합으로, 구간 사이(일시정지 중 이동한 거리)는 더하지 않는다. */
    val distanceMeters: Double
        get() = segments.sumOf { it.distanceMeters() }

    /**
     * 현재까지의 통계.
     *
     * 현재 페이스는 마지막 구간만 본다. 재개 직후에는 새 구간의 지점이 모일 때까지 null 이다.
     */
    fun stats(): RunningStats {
        val distance = distanceMeters
        return RunningStats(
            distanceMeters = distance,
            elapsed = duration,
            currentPace = segments.lastOrNull()?.recentPace(),
            averagePace = paceOf(distance, duration),
        )
    }
}
