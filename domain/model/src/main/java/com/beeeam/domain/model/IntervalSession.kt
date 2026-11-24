package com.beeeam.domain.model

import kotlin.time.Duration
import kotlin.time.Instant

/**
 * 한 번의 인터벌 운동 기록.
 *
 * 운동을 시작할 때 진행 중인 기록으로 저장하고, 세트를 마칠 때마다 갱신한다.
 * 중간에 종료하거나 앱이 강제 종료돼도 마친 세트까지는 남는다.
 *
 * @property startedAt 준비 카운트다운이 끝나 운동을 시작한 시각.
 * @property endedAt 종료한 시각. 진행 중이면 null.
 * @property config 운동할 때의 설정(세트 수, 운동·휴식 시간).
 * @property completedSets 마친 세트 수. 운동 구간을 끝낸 세트만 센다. ([IntervalConfig.completedSetsAt])
 * @property duration 운동한 시간(일시정지 제외).
 */
data class IntervalSession(
    val id: Long,
    val startedAt: Instant,
    val endedAt: Instant?,
    val config: IntervalConfig,
    val completedSets: Int,
    val duration: Duration,
) {
    init {
        require(endedAt == null || endedAt >= startedAt) { "endedAt must not be before startedAt: $startedAt > $endedAt" }
        require(completedSets in 0..config.sets) { "completedSets must be in 0..${config.sets}: $completedSets" }
        require(!duration.isNegative()) { "duration must not be negative: $duration" }
    }

    /** 모든 세트를 마쳤는지 여부. false 이고 [endedAt] 이 있으면 중간에 그만둔 기록이다. */
    val isCompleted: Boolean
        get() = completedSets == config.sets
}
