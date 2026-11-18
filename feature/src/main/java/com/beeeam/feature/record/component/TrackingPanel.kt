package com.beeeam.feature.record.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beeeam.domain.model.RunningSession
import com.beeeam.domain.model.TrackingState
import com.beeeam.feature.R
import com.beeeam.feature.util.AccentOrange
import com.beeeam.feature.util.SetupBackground
import com.beeeam.feature.util.SetupTextPrimary
import com.beeeam.feature.util.SetupTextSecondary
import kotlin.time.Duration

/**
 * 러닝 기록을 조작하고 진행 상황을 보여 주는 지도 아래쪽 패널.
 *
 * 위치 추적 기능을 기기에서 확인하기 위한 **임시 UI** 다. 디자인이 나오면 #15 에서 교체한다.
 */
@Composable
internal fun TrackingPanel(
    tracking: TrackingState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val session = tracking.sessionOrNull()
            if (session != null) {
                StatusLabel(tracking)
                Stats(session)
            }
            Buttons(tracking = tracking, onStart = onStart, onPause = onPause, onResume = onResume, onStop = onStop)
        }
    }
}

@Composable
private fun StatusLabel(tracking: TrackingState) {
    val text = when (tracking) {
        is TrackingState.Paused -> stringResource(R.string.record_tracking_paused)
        is TrackingState.Finished -> stringResource(R.string.record_tracking_finished)
        TrackingState.Idle, is TrackingState.Running -> return
    }
    Text(text = text, color = AccentOrange, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun Stats(session: RunningSession) {
    val stats = session.stats()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Stat(
                label = stringResource(R.string.record_stat_distance),
                value = stringResource(R.string.record_distance_value, stats.distanceMeters / 1000),
                modifier = Modifier.weight(1f),
            )
            Stat(
                label = stringResource(R.string.record_stat_elapsed),
                value = stats.elapsed.toElapsedText(),
                modifier = Modifier.weight(1f),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Stat(
                label = stringResource(R.string.record_stat_current_pace),
                value = paceText(stats.currentPace),
                modifier = Modifier.weight(1f),
            )
            Stat(
                label = stringResource(R.string.record_stat_average_pace),
                value = paceText(stats.averagePace),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, color = SetupTextSecondary, style = MaterialTheme.typography.labelMedium)
        Text(
            text = value,
            color = SetupTextPrimary,
            style = MaterialTheme.typography.titleLarge.merge(TextStyle(fontFeatureSettings = "tnum")),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun Buttons(
    tracking: TrackingState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (tracking) {
            TrackingState.Idle -> PrimaryButton(stringResource(R.string.record_tracking_start), onStart, Modifier.weight(1f))
            is TrackingState.Finished ->
                PrimaryButton(stringResource(R.string.record_tracking_restart), onStart, Modifier.weight(1f))
            is TrackingState.Running -> {
                PrimaryButton(stringResource(R.string.record_tracking_pause), onPause, Modifier.weight(1f))
                SecondaryButton(stringResource(R.string.record_tracking_stop), onStop, Modifier.weight(1f))
            }
            is TrackingState.Paused -> {
                PrimaryButton(stringResource(R.string.record_tracking_resume), onResume, Modifier.weight(1f))
                SecondaryButton(stringResource(R.string.record_tracking_stop), onStop, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, contentColor = SetupBackground),
        modifier = modifier.height(48.dp),
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(48.dp),
    ) {
        Text(text = text, color = SetupTextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

/** 페이스(1km 당 시간)를 `5'30"` 형식으로. 거리가 짧아 계산하지 않았으면 `-'--"`. */
@Composable
private fun paceText(pace: Duration?): String {
    if (pace == null) return stringResource(R.string.record_pace_empty)
    val totalSeconds = pace.inWholeSeconds
    return stringResource(R.string.record_pace_value, (totalSeconds / 60).toInt(), (totalSeconds % 60).toInt())
}

/**
 * 운동 시간을 `12:34` 또는 `1:02:03` 형식으로.
 *
 * 타이머의 `toClockText()` 는 남은 시간을 보여 주려고 올림하지만, 지난 시간은 내림해야 해서 따로 둔다.
 */
private fun Duration.toElapsedText(): String = toComponents { hours, minutes, seconds, _ ->
    if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}

private fun TrackingState.sessionOrNull(): RunningSession? = when (this) {
    TrackingState.Idle -> null
    is TrackingState.Running -> session
    is TrackingState.Paused -> session
    is TrackingState.Finished -> session
}
