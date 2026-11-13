package com.beeeam.intervaltimer.tracking

import com.beeeam.domain.model.LocationNoiseFilter
import com.beeeam.domain.model.LocationPoint
import com.beeeam.domain.model.RunningSession
import com.beeeam.domain.model.TrackingState
import com.beeeam.domain.repository.LocationRepository
import com.beeeam.domain.repository.RunningTracker
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * 위치를 받아 러닝 경로를 기록하는 [RunningTracker] 구현체.
 *
 * 앱 전체에서 하나만 있어서([SingleIn]) 화면과 위치 추적 Service 가 같은 기록 상태를 본다.
 * 기록 규칙(노이즈 필터, 구간, 시간 계산)은 `docs/running-record-logic.md` 를 따른다.
 *
 * @param timeSource 운동 시간을 재는 시계. 기기 시간이 바뀌어도 영향을 받지 않는 단조 시계를 쓴다.
 * @param clock 시작·종료 시각(기록에 남길 실제 시각)을 얻는 시계.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class RunningTrackerImpl(
    private val locationRepository: LocationRepository,
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val clock: Clock = Clock.System,
) : RunningTracker {

    // 위치 수신, 1초 시간 갱신, 시작·일시정지 등의 조작이 동시에 일어나도 상태가 꼬이지 않도록
    // 모든 상태 변경을 한 스레드에서 순서대로 처리한다.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))

    private val _state = MutableStateFlow<TrackingState>(TrackingState.Idle)
    override val state: StateFlow<TrackingState> = _state.asStateFlow()

    private val noiseFilter = LocationNoiseFilter()

    /** 기록 중에만 돌아가는 작업. 위치 수집과 1초 시간 갱신을 함께 담는다. */
    private var recordingJob: Job? = null

    private var startedMark: TimeMark? = null
    private var pausedMark: TimeMark? = null
    private var pausedTotal = Duration.ZERO

    override fun start() {
        scope.launch {
            val current = _state.value
            if (current is TrackingState.Running || current is TrackingState.Paused) return@launch

            startedMark = timeSource.markNow()
            pausedMark = null
            pausedTotal = Duration.ZERO
            noiseFilter.reset()
            _state.value = TrackingState.Running(
                RunningSession(
                    // 아직 저장하지 않은 기록이라 id 가 없다. 저장 기능이 생기면 저장할 때 정한다.
                    id = 0,
                    startedAt = clock.now(),
                    endedAt = null,
                    segments = listOf(emptyList()),
                    duration = Duration.ZERO,
                    pausedDuration = Duration.ZERO,
                ),
            )
            startRecording()
        }
    }

    override fun pause() {
        scope.launch {
            val current = _state.value as? TrackingState.Running ?: return@launch
            // 위치 수집을 멈추면 위치 요청도 해제되어 일시정지 중에는 GPS 를 쓰지 않는다.
            stopRecording()
            pausedMark = timeSource.markNow()
            _state.value = TrackingState.Paused(current.session.withCurrentTimes())
        }
    }

    override fun resume() {
        scope.launch {
            val current = _state.value as? TrackingState.Paused ?: return@launch
            pausedMark?.let { pausedTotal += it.elapsedNow() }
            pausedMark = null
            // 재개하면 새 구간을 시작한다. 필터도 초기화해서 재개 후 첫 지점을 일시정지 전 지점과 비교하지 않게 한다.
            noiseFilter.reset()
            val session = current.session
            _state.value = TrackingState.Running(
                session.copy(segments = session.segments + listOf(emptyList())).withCurrentTimes(),
            )
            startRecording()
        }
    }

    override fun stop() {
        scope.launch {
            val session = when (val current = _state.value) {
                is TrackingState.Running -> current.session
                is TrackingState.Paused -> current.session
                TrackingState.Idle, is TrackingState.Finished -> return@launch
            }
            stopRecording()
            // 일시정지 중에 마쳤다면 그 일시정지 시간까지 반영한 뒤 시계를 정리한다.
            val finished = session.withCurrentTimes().copy(
                endedAt = clock.now(),
                // 지점이 하나도 없는 구간(재개 직후 바로 종료 등)은 경로에 의미가 없으므로 뺀다.
                segments = session.segments.filter { it.isNotEmpty() },
            )
            startedMark = null
            pausedMark = null
            pausedTotal = Duration.ZERO
            _state.value = TrackingState.Finished(finished)
        }
    }

    private fun startRecording() {
        recordingJob = scope.launch {
            launch {
                locationRepository.locationUpdates(LOCATION_INTERVAL, MIN_LOCATION_INTERVAL).collect { point ->
                    if (noiseFilter.accept(point)) addToCurrentSegment(point)
                }
            }
            // 위치가 들어오지 않는 동안에도 화면의 운동 시간이 흐르도록 주기적으로 갱신한다.
            while (isActive) {
                delay(TIME_UPDATE_INTERVAL)
                updateRunning { it }
            }
        }
    }

    private fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null
    }

    private fun addToCurrentSegment(point: LocationPoint) {
        updateRunning { session ->
            session.copy(segments = session.segments.dropLast(1) + listOf(session.segments.last() + point))
        }
    }

    /** 기록 중일 때만 [change] 를 적용하고, 운동 시간도 지금 기준으로 맞춘다. */
    private inline fun updateRunning(change: (RunningSession) -> RunningSession) {
        val current = _state.value as? TrackingState.Running ?: return
        _state.value = TrackingState.Running(change(current.session).withCurrentTimes())
    }

    /**
     * 지금 기준의 운동 시간과 일시정지 시간을 반영한다.
     *
     * 운동 시간 = 시작 후 흐른 시간 - 일시정지 시간. 일시정지 중이면 진행 중인 일시정지 시간도 포함해서 뺀다.
     */
    private fun RunningSession.withCurrentTimes(): RunningSession {
        val started = startedMark ?: return this
        val paused = pausedTotal + (pausedMark?.elapsedNow() ?: Duration.ZERO)
        return copy(
            duration = (started.elapsedNow() - paused).coerceAtLeast(Duration.ZERO),
            pausedDuration = paused,
        )
    }

    private companion object {
        /** 위치를 받고 싶은 간격. 경로가 충분히 매끄러우면서 배터리를 덜 쓰도록 2초로 시작한다. */
        val LOCATION_INTERVAL = 2.seconds

        /** 다른 앱의 요청 등으로 위치가 더 자주 들어와도 이보다 빠르게는 받지 않는다. */
        val MIN_LOCATION_INTERVAL = 1.seconds

        /** 기록 중 운동 시간을 갱신하는 간격. */
        val TIME_UPDATE_INTERVAL = 1.seconds
    }
}
