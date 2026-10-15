package com.beeeam.domain.model

import kotlin.time.Duration

/**
 * 타이머 진행 상태를 나타내는 페이즈.
 *
 * @property WORK 운동 중.
 * @property REST 휴식 중.
 * @property FINISHED 운동이 끝난 상태.
 */
enum class Phase { WORK, REST, FINISHED }

/**
 * 특정 시점의 타이머 진행 상태.
 *
 * @property currentSet 현재 진행 중인 세트 (1부터 시작). 휴식 중에는 방금 끝낸 세트.
 * @property remainingSets 아직 운동을 끝내지 않은 세트 수 (운동 중인 세트 포함).
 * @property remainingInPhase 현재 페이즈(운동/휴식)의 남은 시간.
 * @property remainingTotal 전체 남은 시간.
 */
data class TimerProgress(
    val phase: Phase,
    val currentSet: Int,
    val remainingSets: Int,
    val remainingInPhase: Duration,
    val remainingTotal: Duration,
)
