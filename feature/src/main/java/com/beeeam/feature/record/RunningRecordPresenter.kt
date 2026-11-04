package com.beeeam.feature.record

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.beeeam.domain.repository.LocationRepository
import com.beeeam.feature.record.RunningRecordScreen.Event
import com.beeeam.feature.record.RunningRecordScreen.State
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@CircuitInject(RunningRecordScreen::class, AppScope::class)
@Inject
class RunningRecordPresenter(
    private val locationRepository: LocationRepository,
) : Presenter<State> {

    @Composable
    override fun present(): State {
        // 탭을 이동했다 돌아와도 권한 확인 결과와 마지막 위치를 유지한다.
        var permission by rememberRetained { mutableStateOf(LocationPermission.UNKNOWN) }
        var location by rememberRetained { mutableStateOf<CurrentLocation>(CurrentLocation.Idle) }
        // 화면을 벗어나면 진행 중인 위치 조회도 함께 취소된다.
        val scope = rememberCoroutineScope()

        fun refreshLocation() {
            if (location == CurrentLocation.Loading) return
            location = CurrentLocation.Loading
            scope.launch {
                location = try {
                    locationRepository.getCurrentLocation()
                        ?.let { CurrentLocation.Found(it) }
                        ?: CurrentLocation.Unavailable
                } catch (e: CancellationException) {
                    // 조회 중에 화면을 벗어나면 다시 들어왔을 때 처음부터 조회하도록 되돌린다.
                    location = CurrentLocation.Idle
                    throw e
                }
            }
        }

        return State(
            permission = permission,
            location = location,
        ) { event ->
            when (event) {
                is Event.PermissionUpdated -> {
                    permission = event.permission
                    when {
                        event.permission != LocationPermission.GRANTED -> location = CurrentLocation.Idle
                        // Ui 는 화면에 들어올 때마다 권한을 다시 알려주므로, 이미 찾은 위치가 있으면 다시 조회하지 않는다.
                        location !is CurrentLocation.Found -> refreshLocation()
                    }
                }
                Event.RefreshLocation -> {
                    if (permission == LocationPermission.GRANTED) refreshLocation()
                }
            }
        }
    }
}
