package com.beeeam.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

/**
 * 인터벌 타이머 설정.
 *
 * 각 세트는 운동 → 휴식 순서로 진행되며, 마지막 세트 뒤에는 휴식이 없다.
 */
data class IntervalConfig(
    val sets: Int,
    val workDuration: Duration,
    val restDuration: Duration,
) {
    init {
        require(sets >= 1) { "sets must be at least 1: $sets" }
        require(workDuration.isPositive()) { "workDuration must be positive: $workDuration" }
        require(!restDuration.isNegative()) { "restDuration must not be negative: $restDuration" }
    }

    val totalDuration: Duration
        get() = workDuration * sets + restDuration * (sets - 1)

    /**
     * 시작 후 [elapsed] 만큼 지났을 때의 진행 상태.
     *
     * 페이즈 경계 시점은 다음 페이즈로 본다. (운동 종료 시점 = 휴식 시작)
     */
    fun progressAt(elapsed: Duration): TimerProgress {
        require(!elapsed.isNegative()) { "elapsed must not be negative: $elapsed" }

        val total = totalDuration
        if (elapsed >= total) {
            return TimerProgress(
                phase = Phase.FINISHED,
                currentSet = sets,
                remainingSets = 0,
                remainingInPhase = Duration.ZERO,
                remainingTotal = Duration.ZERO,
            )
        }

        val cycleNanos = (workDuration + restDuration).inWholeNanoseconds   // 한 세트의 운동 + 휴식 시간
        val elapsedNanos = elapsed.inWholeNanoseconds                       // 경과 시간
        val currentSet =
            (elapsedNanos / cycleNanos).toInt() + 1            // 현재 세트 (1부터 시작)   (- 12초 → 몫 0 → 1세트)  (- 52초 → 몫 1 → 2세트)
        val offset =
            (elapsedNanos % cycleNanos).nanoseconds // 이번 사이클 안에서 몇 초 지났는지 = 나머지 (- 52초 → 52 % 40 = 12초 (2세트가 시작되고 12초 지남))

        return if (offset < workDuration) {
            TimerProgress(
                phase = Phase.WORK,
                currentSet = currentSet,
                remainingSets = sets - currentSet + 1,
                remainingInPhase = workDuration - offset,
                remainingTotal = total - elapsed,
            )
        } else {
            TimerProgress(
                phase = Phase.REST,
                currentSet = currentSet,
                remainingSets = sets - currentSet,
                remainingInPhase = workDuration + restDuration - offset,
                remainingTotal = total - elapsed,
            )
        }
    }
}
