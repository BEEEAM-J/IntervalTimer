package com.beeeam.feature.timer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.beeeam.domain.model.IntervalConfig
import com.beeeam.domain.model.IntervalSession
import com.beeeam.domain.model.Phase
import com.beeeam.domain.model.TimerProgress
import com.beeeam.domain.repository.IntervalRepository
import com.beeeam.feature.timer.IntervalTimerScreen.Event
import com.beeeam.feature.timer.IntervalTimerScreen.Preparation
import com.beeeam.feature.timer.IntervalTimerScreen.Session
import com.beeeam.feature.timer.IntervalTimerScreen.State
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.TimeMark
import kotlin.time.TimeSource

// timeSource 는 그래프에 바인딩이 없으면 기본값(Monotonic)을 쓰고, 테스트에서는 TestTimeSource 를 넣을 수 있다.
// clock 은 기록에 남길 시작·종료 시각(실제 시각)을 얻는 시계다. 운동 시간은 timeSource 로 잔다.
// 기록 저장 시점은 `docs/interval-record-logic.md` 를 따른다.
@CircuitInject(IntervalTimerScreen::class, AppScope::class)
@Inject
class IntervalTimerPresenter(
    private val intervalRepository: IntervalRepository,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val clock: Clock = Clock.System,
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

        // 진행 중인 운동 기록. 시작 시각이 null 이면 기록할 운동이 없다.
        var startedAt by rememberRetained { mutableStateOf<Instant?>(null) }
        // 종료까지 저장한 뒤에는 같은 기록을 다시 바꾸지 않는다. (완료 화면의 "종료", 탭에 돌아왔을 때 등)
        var recordEnded by rememberRetained { mutableStateOf(false) }
        // 저장소가 정한 기록 id. 처음 저장하기 전에는 0.
        var recordId by rememberRetained { mutableLongStateOf(0L) }
        // 저장을 호출한 순서대로 처리해, 기록을 만들기 전에 갱신이 실행되지 않게 한다.
        val saveMutex = rememberRetained { Mutex() }
        val scope = rememberCoroutineScope()

        /**
         * 지금 상태로 기록을 저장한다. 운동을 시작한 뒤 처음이면 새로 만들고, 이후에는 같은 기록을 갱신한다.
         *
         * 저장은 짧은 DB 쓰기라, 저장 중에 탭을 옮겨 화면이 사라져도 끝까지 실행한다.
         */
        fun saveRecord(target: IntervalConfig, ended: Boolean, isNew: Boolean = false) {
            val started = startedAt ?: return
            if (recordEnded) return
            if (ended) recordEnded = true
            val session = IntervalSession(
                id = 0,
                startedAt = started,
                endedAt = if (ended) clock.now() else null,
                config = target,
                completedSets = target.completedSetsAt(elapsed),
                // 타이머는 일정 간격으로 경과 시간을 갱신하므로, 끝난 직후에는 전체 시간을 조금 넘을 수 있다.
                duration = elapsed.coerceAtMost(target.totalDuration),
            )
            scope.launch {
                withContext(NonCancellable) {
                    saveMutex.withLock {
                        if (isNew) {
                            recordId = intervalRepository.create(session)
                        } else if (recordId != 0L) {
                            intervalRepository.update(session.copy(id = recordId))
                        }
                    }
                }
            }
        }

        fun startWorkout(target: IntervalConfig) {
            preparing = null
            preparedAt = null
            prepareElapsed = Duration.ZERO
            config = target
            accumulated = Duration.ZERO
            elapsed = Duration.ZERO
            resumedAt = timeSource.markNow()
            startedAt = clock.now()
            recordEnded = false
            saveRecord(target, ended = false, isNew = true)
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

        // 세트를 마칠 때마다 진행 상황을 저장하고, 모든 세트를 마치면 종료로 저장한다.
        // 다른 탭에 있는 동안 끝난 세트는 돌아와 경과 시간을 따라잡을 때 저장된다.
        val completedSets = config?.completedSetsAt(elapsed)
        LaunchedEffect(config, completedSets) {
            val running = config ?: return@LaunchedEffect
            if (completedSets == null || completedSets == 0) return@LaunchedEffect
            saveRecord(running, ended = completedSets == running.sets)
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
                totalDuration = running.totalDuration,
                totalWorkDuration = running.workDuration * running.sets,
                totalRestDuration = running.restDuration * (running.sets - 1),
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
                    config?.let { saveRecord(it, ended = false) }
                }
                Event.Resume -> {
                    if (session == null || session.progress.phase == Phase.FINISHED) return@State
                    resumedAt = timeSource.markNow()
                }
                Event.Stop -> {
                    // 중간에 그만둬도 마친 세트까지 종료로 저장한다. 완료 화면에서는 이미 저장했으므로 바뀜지 않는다.
                    config?.let { running ->
                        resumedAt?.let { elapsed = accumulated + it.elapsedNow() }
                        saveRecord(running, ended = true)
                    }
                    startedAt = null
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
