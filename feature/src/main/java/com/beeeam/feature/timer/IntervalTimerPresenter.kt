package com.beeeam.feature.timer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beeeam.domain.model.IntervalConfig
import com.beeeam.domain.model.Phase
import com.beeeam.domain.model.TimerProgress
import com.beeeam.feature.timer.IntervalTimerScreen.Event
import com.beeeam.feature.timer.IntervalTimerScreen.Preparation
import com.beeeam.feature.timer.IntervalTimerScreen.Session
import com.beeeam.feature.timer.IntervalTimerScreen.State
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class IntervalTimerPresenter(
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : Presenter<State> {

    @Composable
    override fun present(): State {
        var sets by rememberRetained { mutableStateOf(DEFAULT_SETS) }
        var workSeconds by rememberRetained { mutableStateOf(DEFAULT_WORK_SECONDS) }
        var restSeconds by rememberRetained { mutableStateOf(DEFAULT_REST_SECONDS) }

        // 준비 카운트다운 중인 설정. null 이면 준비 중이 아님.
        var preparing by rememberRetained { mutableStateOf<IntervalConfig?>(null) }
        // 준비를 시작한 시점
        var preparedAt by rememberRetained { mutableStateOf<TimeMark?>(null) }
        var prepareElapsed by rememberRetained { mutableStateOf(Duration.ZERO) }

        // 진행 중인 타이머. config 가 null 이면 시작 전.
        var config by rememberRetained { mutableStateOf<IntervalConfig?>(null) }
        // 일시정지 전까지 누적된 경과 시간
        var accumulated by rememberRetained { mutableStateOf(Duration.ZERO) }
        // 마지막으로 시작(재개)한 시점. null 이면 일시정지 상태.
        var resumedAt by rememberRetained { mutableStateOf<TimeMark?>(null) }
        var elapsed by rememberRetained { mutableStateOf(Duration.ZERO) }

        fun startWorkout(target: IntervalConfig) {
            preparing = null
            preparedAt = null
            prepareElapsed = Duration.ZERO
            config = target
            accumulated = Duration.ZERO
            elapsed = Duration.ZERO
            resumedAt = timeSource.markNow()
        }

        // 준비 카운트다운이 끝나면 "Go!" 를 잠시 보여준 뒤 운동을 시작한다.
        LaunchedEffect(preparing, preparedAt) {
            val target = preparing ?: return@LaunchedEffect
            val mark = preparedAt ?: return@LaunchedEffect
            while (true) {
                prepareElapsed = mark.elapsedNow()
                if (prepareElapsed >= PREPARE_DURATION + GO_DURATION) {
                    startWorkout(target)
                    break
                }
                delay(TICK)
            }
        }

        // 경과 시간을 TimeMark 기준으로 계산하므로, 탭 이동 등으로 잠시 멈췄다 돌아와도 실제 시간을 따라잡는다.
        LaunchedEffect(config, resumedAt) {
            val running = config ?: return@LaunchedEffect
            val mark = resumedAt ?: return@LaunchedEffect
            while (true) {
                elapsed = accumulated + mark.elapsedNow()
                if (elapsed >= running.totalDuration) {
                    accumulated = running.totalDuration
                    resumedAt = null
                    break
                }
                delay(TICK)
            }
        }

        val inputConfig = toConfig(sets, workSeconds, restSeconds)
        val preparation = preparing?.let { target ->
            val remaining = (PREPARE_DURATION - prepareElapsed).coerceAtLeast(Duration.ZERO)
            Preparation(
                totalSets = target.sets,
                totalDuration = target.totalDuration,
                remaining = remaining,
                progress = (remaining / PREPARE_DURATION).toFloat(),
                isGo = remaining == Duration.ZERO,
            )
        }
        val session = config?.let { running ->
            val progress = running.progressAt(elapsed)
            val phaseDuration = running.durationOf(progress.phase)
            val nextPhase = running.nextPhaseAfter(progress)
            Session(
                progress = progress,
                totalSets = running.sets,
                isPaused = resumedAt == null,
                phaseProgress = if (phaseDuration.isPositive()) (progress.remainingInPhase / phaseDuration).toFloat() else 0f,
                nextPhase = nextPhase,
                nextPhaseDuration = running.durationOf(nextPhase),
            )
        }

        return State(
            sets = sets,
            workSeconds = workSeconds,
            restSeconds = restSeconds,
            totalDuration = inputConfig?.totalDuration ?: Duration.ZERO,
            canStart = inputConfig != null,
            preparation = preparation,
            session = session,
        ) { event ->
            when (event) {
                is Event.SetsChanged -> sets = event.value
                is Event.WorkSecondsChanged -> workSeconds = event.value
                is Event.RestSecondsChanged -> restSeconds = event.value
                Event.Start -> {
                    if (inputConfig == null) return@State
                    preparing = inputConfig
                    prepareElapsed = Duration.ZERO
                    preparedAt = timeSource.markNow()
                }
                Event.SkipPreparation -> {
                    val target = preparing ?: return@State
                    startWorkout(target)
                }
                Event.Pause -> {
                    val mark = resumedAt ?: return@State
                    accumulated += mark.elapsedNow()
                    elapsed = accumulated
                    resumedAt = null
                }
                Event.Resume -> {
                    if (session == null || session.progress.phase == Phase.FINISHED) return@State
                    resumedAt = timeSource.markNow()
                }
                Event.Stop -> {
                    preparing = null
                    preparedAt = null
                    prepareElapsed = Duration.ZERO
                    config = null
                    accumulated = Duration.ZERO
                    elapsed = Duration.ZERO
                    resumedAt = null
                }
            }
        }
    }

    private companion object {
        val TICK = 100.milliseconds
        val PREPARE_DURATION = 10.seconds
        val GO_DURATION = 1.seconds
        const val DEFAULT_SETS = 1
        const val DEFAULT_WORK_SECONDS = 30
        const val DEFAULT_REST_SECONDS = 30

        /** [phase] 한 구간의 길이. */
        fun IntervalConfig.durationOf(phase: Phase): Duration = when (phase) {
            Phase.WORK -> workDuration
            Phase.REST -> restDuration
            Phase.FINISHED -> Duration.ZERO
        }

        /** [progress] 의 페이즈가 끝난 뒤 이어질 페이즈. 휴식 시간이 0 이면 다음 세트 운동으로 바로 넘어간다. */
        fun IntervalConfig.nextPhaseAfter(progress: TimerProgress): Phase = when (progress.phase) {
            Phase.WORK -> when {
                progress.currentSet >= sets -> Phase.FINISHED
                restDuration.isPositive() -> Phase.REST
                else -> Phase.WORK
            }
            Phase.REST -> Phase.WORK
            Phase.FINISHED -> Phase.FINISHED
        }

        /** 설정값이 유효하지 않으면 null. (예: 운동 시간 0초) */
        fun toConfig(sets: Int, workSeconds: Int, restSeconds: Int): IntervalConfig? {
            if (sets < 1 || workSeconds < 1 || restSeconds < 0) return null
            return IntervalConfig(sets = sets, workDuration = workSeconds.seconds, restDuration = restSeconds.seconds)
        }
    }
}
