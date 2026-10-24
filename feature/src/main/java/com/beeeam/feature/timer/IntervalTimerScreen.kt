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
     * @property preparation 운동 시작 전 준비(카운트다운) 상태. null 이면 준비 중이 아님.
     * @property session 타이머가 시작된 뒤의 진행 상태. preparation 과 함께 null 이면 설정(입력) 화면.
     * @property totalDuration 현재 설정값으로 계산한 총 운동 시간. 설정값이 유효하지 않으면 0.
     * @property canStart 설정값이 모두 유효해 운동을 시작할 수 있는지 여부.
     */
    data class State(
        val sets: Int,
        val workSeconds: Int,
        val restSeconds: Int,
        val totalDuration: Duration,
        val canStart: Boolean,
        val preparation: Preparation?,
        val session: Session?,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    /**
     * @property totalSets 사용자가 설정한 총 세트 수.
     * @property totalDuration 사용자가 설정한 총 운동 시간.
     * @property remaining 준비 카운트다운의 남은 시간.
     * @property isGo 카운트다운이 끝나 "Go!" 를 보여주는 중인지 여부.
     */
    data class Preparation(
        val totalSets: Int,
        val totalDuration: Duration,
        val remaining: Duration,
        val isGo: Boolean,
    )

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
        data object SkipPreparation : Event
        data object Pause : Event
        data object Resume : Event
        data object Stop : Event
    }
}
