package com.beeeam.feature.timer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.beeeam.domain.model.IntervalConfig
import com.beeeam.domain.model.Phase
import com.beeeam.feature.timer.IntervalTimerScreen.Event
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
        var setsInput by rememberRetained { mutableStateOf("") }
        var workSecondsInput by rememberRetained { mutableStateOf("") }
        var restSecondsInput by rememberRetained { mutableStateOf("") }

        // 진행 중인 타이머. config 가 null 이면 시작 전.
        var config by rememberRetained { mutableStateOf<IntervalConfig?>(null) }
        // 일시정지 전까지 누적된 경과 시간
        var accumulated by rememberRetained { mutableStateOf(Duration.ZERO) }
        // 마지막으로 시작(재개)한 시점. null 이면 일시정지 상태.
        var resumedAt by rememberRetained { mutableStateOf<TimeMark?>(null) }
        var elapsed by rememberRetained { mutableStateOf(Duration.ZERO) }

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

        val inputConfig = parseConfig(setsInput, workSecondsInput, restSecondsInput)
        val session = config?.let { running ->
            Session(
                progress = running.progressAt(elapsed),
                totalSets = running.sets,
                isPaused = resumedAt == null,
            )
        }

        return State(
            setsInput = setsInput,
            workSecondsInput = workSecondsInput,
            restSecondsInput = restSecondsInput,
            canStart = inputConfig != null,
            session = session,
        ) { event ->
            when (event) {
                is Event.SetsChanged -> setsInput = event.value.digitsOnly(MAX_SETS_LENGTH)
                is Event.WorkSecondsChanged -> workSecondsInput = event.value.digitsOnly(MAX_SECONDS_LENGTH)
                is Event.RestSecondsChanged -> restSecondsInput = event.value.digitsOnly(MAX_SECONDS_LENGTH)
                Event.Start -> {
                    if (inputConfig == null) return@State
                    config = inputConfig
                    accumulated = Duration.ZERO
                    elapsed = Duration.ZERO
                    resumedAt = timeSource.markNow()
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
        const val MAX_SETS_LENGTH = 3
        const val MAX_SECONDS_LENGTH = 4

        fun String.digitsOnly(maxLength: Int): String = filter(Char::isDigit).take(maxLength)

        /** 입력값이 유효하지 않으면 null. */
        fun parseConfig(sets: String, workSeconds: String, restSeconds: String): IntervalConfig? {
            val setCount = sets.toIntOrNull() ?: return null
            val work = workSeconds.toIntOrNull() ?: return null
            val rest = restSeconds.toIntOrNull() ?: return null
            if (setCount < 1 || work < 1 || rest < 0) return null
            return IntervalConfig(sets = setCount, workDuration = work.seconds, restDuration = rest.seconds)
        }
    }
}
