package com.beeeam.domain.repository

import com.beeeam.domain.model.TrackingState
import kotlinx.coroutines.flow.StateFlow

/**
 * 러닝 경로 기록을 시작·일시정지·재개·종료하고, 진행 상태를 알려 준다.
 *
 * 화면(feature)은 이 인터페이스만 알고, 위치를 어떻게 받는지(Service, Play 서비스 등)는 구현체가 맡는다.
 * 상태가 맞지 않는 호출(예: 기록 중이 아닐 때 pause)은 무시한다.
 */
interface RunningTracker {

    /** 현재 기록 상태. 기록 중에는 위치가 들어오거나 시간이 흐를 때마다 갱신된다. */
    val state: StateFlow<TrackingState>

    /** 새 기록을 시작한다. 대기 중이거나 이전 기록을 마친 상태에서만 동작한다. */
    fun start()

    fun pause()

    fun resume()

    /** 기록을 마친다. 마친 기록은 [TrackingState.Finished] 로 남는다. */
    fun stop()
}
