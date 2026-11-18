package com.beeeam.domain.model

/**
 * 러닝 기록 진행 상태.
 *
 * ```
 * Idle ─start─▶ Running ─pause─▶ Paused ─resume─▶ Running
 *                  │                 │
 *                  └──────stop───────┴──▶ Finished ─start─▶ Running (새 기록)
 * ```
 */
sealed interface TrackingState {

    /** 기록을 시작하지 않은 상태. */
    data object Idle : TrackingState

    /** 위치를 받아 경로를 기록하는 중. */
    data class Running(val session: RunningSession) : TrackingState

    /** 일시정지. 위치를 받지 않고, 운동 시간도 늘지 않는다. */
    data class Paused(val session: RunningSession) : TrackingState

    /** 기록을 마친 상태. 요약 화면에서 보여 줄 수 있도록 마지막 기록을 들고 있다. */
    data class Finished(val session: RunningSession) : TrackingState
}
