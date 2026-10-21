package com.beeeam.feature.timer

import com.beeeam.domain.model.TimerProgress
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.ParcelableScreen
import kotlinx.parcelize.Parcelize
import kotlin.time.Duration

@Parcelize
data object IntervalTimerScreen : ParcelableScreen {

    /**
     * 인터벌 타이머 화면 상태.
     *
     * @property session 타이머가 시작된 뒤의 진행 상태. null 이면 설정(입력) 화면.
     * @property totalDuration 현재 설정값으로 계산한 총 운동 시간. 설정값이 유효하지 않으면 0.
     * @property canStart 설정값이 모두 유효해 운동을 시작할 수 있는지 여부.
     */
    data class State(
        val sets: Int,
        val workSeconds: Int,
        val restSeconds: Int,
        val totalDuration: Duration,
        val canStart: Boolean,
        val session: Session?,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    /**
     * @property totalSets 전체 세트 수.
     * @property isPaused 일시정지 여부.
     */
    data class Session(
        val progress: TimerProgress,
        val totalSets: Int,
        val isPaused: Boolean,
    )

    sealed interface Event : CircuitUiEvent {
        data class SetsChanged(val value: Int) : Event
        data class WorkSecondsChanged(val value: Int) : Event
        data class RestSecondsChanged(val value: Int) : Event
        data object Start : Event
        data object Pause : Event
        data object Resume : Event
        data object Stop : Event
    }
}
