package com.beeeam.feature.record

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.beeeam.feature.R
import com.beeeam.feature.record.RunningRecordScreen.Event
import com.beeeam.feature.record.RunningRecordScreen.State
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import dev.zacsweers.metro.AppScope

/**
 * 위치 권한 상태에 따라 지도 또는 권한 안내를 보여준다.
 *
 * 권한 확인과 요청에는 Activity 가 필요하므로 Ui 에서 처리하고, 결과만 [Event.PermissionUpdated] 로 Presenter 에 알린다.
 */
@CircuitInject(RunningRecordScreen::class, AppScope::class)
@Composable
fun RunningRecordUi(state: State, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // LifecycleResumeEffect 는 키가 바뀔 때만 다시 만들어지므로, 그 안에서는 항상 최신 state 를 읽도록 한다.
    val currentState by rememberUpdatedState(state)
    // 처음 들어왔을 때 자동으로 권한을 요청했는지. 회전하거나 탭을 이동해도 다시 자동 요청하지 않도록 유지한다.
    var hasRequestedOnEnter by rememberRetained { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val permission = when {
            // 대략적 위치만 허용해도 지도에 위치를 표시할 수 있다.
            result.values.any { it } -> LocationPermission.GRANTED
            context.findActivity()?.shouldShowLocationRationale() == true -> LocationPermission.DENIED
            else -> LocationPermission.PERMANENTLY_DENIED
        }
        currentState.eventSink(Event.PermissionUpdated(permission))
    }

    // 화면에 들어올 때와 설정 화면 등에서 돌아올 때마다 권한을 다시 확인한다.
    LifecycleResumeEffect(Unit) {
        when {
            context.hasLocationPermission() -> currentState.eventSink(Event.PermissionUpdated(LocationPermission.GRANTED))
            // 처음 들어왔을 때만 바로 요청한다. 이미 거부한 사용자에게는 안내 화면의 버튼으로 다시 요청하게 한다.
            // 권한 팝업이 닫히면 결과 콜백 직후 곧바로 onResume 이 다시 불리는데, 그 시점에는 Presenter 의 새 상태가
            // 아직 Ui 에 반영되기 전이다. 그래서 state.permission 대신 팝업을 띄우는 순간 기록한 이 값으로 판단한다.
            !hasRequestedOnEnter -> {
                hasRequestedOnEnter = true
                permissionLauncher.launch(LOCATION_PERMISSIONS)
            }
            // 앱을 사용하는 중에 설정에서 권한을 거둔 경우
            currentState.permission == LocationPermission.GRANTED ->
                currentState.eventSink(Event.PermissionUpdated(LocationPermission.DENIED))
        }
        onPauseOrDispose { }
    }

    when (state.permission) {
        LocationPermission.UNKNOWN -> Box(modifier = modifier.fillMaxSize())
        LocationPermission.GRANTED -> RecordMapContent(
            location = state.location,
            onRetry = { state.eventSink(Event.RefreshLocation) },
            modifier = modifier,
        )
        LocationPermission.DENIED -> RecordPermissionContent(
            description = stringResource(R.string.record_permission_denied_description),
            buttonText = stringResource(R.string.record_permission_request),
            onClick = { permissionLauncher.launch(LOCATION_PERMISSIONS) },
            modifier = modifier,
        )
        LocationPermission.PERMANENTLY_DENIED -> RecordPermissionContent(
            description = stringResource(R.string.record_permission_permanently_denied_description),
            buttonText = stringResource(R.string.record_open_settings),
            onClick = { context.openAppSettings() },
            modifier = modifier,
        )
    }
}
