package com.beeeam.feature.timer

import com.beeeam.domain.model.Phase
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
     * @property progress 카운트다운의 남은 비율. 시작 시 1 에서 끝날 때 0 으로 줄어든다.
     * @property isGo 카운트다운이 끝나 "Go!" 를 보여주는 중인지 여부.
     */
    data class Preparation(
        val totalSets: Int,
        val totalDuration: Duration,
        val remaining: Duration,
        val progress: Float,
        val isGo: Boolean,
    )

    /**
     * @property totalSets 전체 세트 수.
     * @property isPaused 일시정지 여부.
     * @property phaseProgress 현재 페이즈의 남은 비율. 페이즈 시작 시 1 에서 끝날 때 0 으로 줄어든다.
     * @property nextPhase 현재 페이즈가 끝난 뒤 이어질 페이즈. 마지막 세트 운동 중이면 [Phase.FINISHED].
     * @property nextPhaseDuration [nextPhase] 의 길이. [Phase.FINISHED] 이면 0.
     * @property totalDuration 전체 운동 시간. (운동 + 휴식)
     * @property totalWorkDuration 모든 세트의 운동 시간 합계.
     * @property totalRestDuration 모든 휴식 시간 합계.
     */
    data class Session(
        val progress: TimerProgress,
        val totalSets: Int,
        val isPaused: Boolean,
        val phaseProgress: Float,
        val nextPhase: Phase,
        val nextPhaseDuration: Duration,
        val totalDuration: Duration,
        val totalWorkDuration: Duration,
        val totalRestDuration: Duration,
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
