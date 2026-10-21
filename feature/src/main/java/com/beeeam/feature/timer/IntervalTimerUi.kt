package com.beeeam.feature.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeeam.domain.model.Phase
import com.beeeam.domain.model.TimerProgress
import com.beeeam.feature.R
import com.beeeam.feature.timer.IntervalTimerScreen.Event
import com.beeeam.feature.timer.IntervalTimerScreen.Session
import com.beeeam.feature.timer.IntervalTimerScreen.State
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val WorkColor = Color(0xFF2E7D32)
private val RestColor = Color(0xFF1565C0)

private val SETS_VALUES = (1..20).toList()
private val WORK_MINUTES_VALUES = (0..20).toList()
private val REST_MINUTES_VALUES = (0..10).toList()
private val SECONDS_VALUES = (0..55 step 5).toList()

/** 설정 화면에서 펼쳐진 Picker. */
private enum class SetupField { SETS, WORK, REST }

@Composable
fun IntervalTimerUi(state: State, modifier: Modifier = Modifier) {
    val session = state.session
    if (session == null) {
        TimerSetup(state = state, modifier = modifier)
    } else {
        TimerRunning(session = session, eventSink = state.eventSink, modifier = modifier)
    }
}

@Composable
private fun TimerSetup(state: State, modifier: Modifier = Modifier) {
    // 한 번에 하나의 Picker 만 펼친다. null 이면 모두 접힌 상태.
    var expanded by rememberSaveable { mutableStateOf<SetupField?>(null) }
    val toggle = { field: SetupField -> expanded = if (expanded == field) null else field }

    Column(
        modifier = modifier
            .fillMaxSize()
            // 다른 요소가 소비하지 않은 터치(= Picker 영역 바깥)가 발생하면 Picker 를 접는다.
            .pointerInput(Unit) { detectTapGestures(onTap = { expanded = null }) }
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.timer_setup_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        SettingItem(
            label = stringResource(R.string.timer_sets_label),
            value = stringResource(R.string.timer_sets_value, state.sets),
            expanded = expanded == SetupField.SETS,
            onClick = { toggle(SetupField.SETS) },
        ) {
            WheelPicker(
                value = state.sets,
                onValueChange = { state.eventSink(Event.SetsChanged(it)) },
                values = SETS_VALUES,
            )
            PickerSuffix(stringResource(R.string.timer_sets_suffix))
        }
        SettingItem(
            label = stringResource(R.string.timer_work_label),
            value = durationText(state.workSeconds),
            expanded = expanded == SetupField.WORK,
            onClick = { toggle(SetupField.WORK) },
        ) {
            DurationPicker(
                totalSeconds = state.workSeconds,
                onTotalSecondsChange = { state.eventSink(Event.WorkSecondsChanged(it)) },
                minuteValues = WORK_MINUTES_VALUES,
            )
        }
        SettingItem(
            label = stringResource(R.string.timer_rest_label),
            value = durationText(state.restSeconds),
            expanded = expanded == SetupField.REST,
            onClick = { toggle(SetupField.REST) },
        ) {
            DurationPicker(
                totalSeconds = state.restSeconds,
                onTotalSecondsChange = { state.eventSink(Event.RestSecondsChanged(it)) },
                minuteValues = REST_MINUTES_VALUES,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { state.eventSink(Event.Start) },
            enabled = state.canStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(text = stringResource(R.string.timer_start), fontSize = 18.sp)
        }
    }
}

/** 현재 값을 텍스트로 보여주고, 터치하면 아래에 [picker] 를 펼친다. */
@Composable
private fun SettingItem(
    label: String,
    value: String,
    expanded: Boolean,
    onClick: () -> Unit,
    picker: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
            AnimatedVisibility(visible = expanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Picker 영역 안의 터치는 여기서 소비해 바깥 터치로 처리되지 않게 한다.
                        .pointerInput(Unit) { detectTapGestures() }
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    picker()
                }
            }
        }
    }
}

/** 초 단위 값을 분 / 초 두 개의 휠로 나눠 고른다. */
@Composable
private fun DurationPicker(totalSeconds: Int, onTotalSecondsChange: (Int) -> Unit, minuteValues: List<Int>) {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    WheelPicker(
        value = minutes,
        onValueChange = { onTotalSecondsChange(it * 60 + seconds) },
        values = minuteValues,
        format = ::twoDigits,
    )
    PickerSuffix(stringResource(R.string.timer_minutes_suffix))
    Spacer(modifier = Modifier.width(16.dp))
    WheelPicker(
        value = seconds,
        onValueChange = { onTotalSecondsChange(minutes * 60 + it) },
        values = SECONDS_VALUES,
        format = ::twoDigits,
    )
    PickerSuffix(stringResource(R.string.timer_seconds_suffix))
}

@Composable
private fun PickerSuffix(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 8.dp),
    )
}

@Composable
private fun durationText(totalSeconds: Int): String =
    stringResource(R.string.timer_duration_value, totalSeconds / 60, totalSeconds % 60)

private fun twoDigits(value: Int): String = "%02d".format(value)

@Composable
private fun TimerRunning(session: Session, eventSink: (Event) -> Unit, modifier: Modifier = Modifier) {
    val progress = session.progress
    val targetColor = when (progress.phase) {
        Phase.WORK -> WorkColor
        Phase.REST -> RestColor
        Phase.FINISHED -> MaterialTheme.colorScheme.surface
    }
    val contentColor = if (progress.phase == Phase.FINISHED) MaterialTheme.colorScheme.onSurface else Color.White
    val backgroundColor by animateColorAsState(targetValue = targetColor, label = "phaseBackground")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(
                when (progress.phase) {
                    Phase.WORK -> R.string.timer_phase_work
                    Phase.REST -> R.string.timer_phase_rest
                    Phase.FINISHED -> R.string.timer_phase_finished
                },
            ),
            color = contentColor,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        if (progress.phase != Phase.FINISHED) {
            Text(
                text = progress.remainingInPhase.toClockText(),
                color = contentColor,
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.timer_set_progress, progress.currentSet, session.totalSets),
                color = contentColor,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.timer_total_remaining, progress.remainingTotal.toClockText()),
                color = contentColor.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Spacer(modifier = Modifier.height(48.dp))
        TimerControls(session = session, contentColor = contentColor, eventSink = eventSink)
    }
}

@Composable
private fun TimerControls(session: Session, contentColor: Color, eventSink: (Event) -> Unit) {
    val finished = session.progress.phase == Phase.FINISHED
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!finished) {
            Button(
                onClick = { eventSink(if (session.isPaused) Event.Resume else Event.Pause) },
                colors = ButtonDefaults.buttonColors(containerColor = contentColor, contentColor = Color.Black),
            ) {
                Text(stringResource(if (session.isPaused) R.string.timer_resume else R.string.timer_pause))
            }
        }
        OutlinedButton(
            onClick = { eventSink(Event.Stop) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        ) {
            Text(stringResource(if (finished) R.string.timer_done else R.string.timer_stop))
        }
    }
}

/** 남은 시간을 올림한 초 단위로 mm:ss 형식 변환. (29.4초 → 00:30) */
private fun Duration.toClockText(): String {
    val totalSeconds = (inWholeMilliseconds + 999) / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Preview(showBackground = true)
@Composable
private fun TimerSetupPreview() {
    MaterialTheme {
        IntervalTimerUi(
            State(
                sets = 1,
                workSeconds = 30,
                restSeconds = 30,
                canStart = true,
                session = null,
                eventSink = {},
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerWorkPreview() {
    MaterialTheme {
        IntervalTimerUi(
            State(
                sets = 1,
                workSeconds = 30,
                restSeconds = 30,
                canStart = true,
                session = Session(
                    progress = TimerProgress(
                        phase = Phase.WORK,
                        currentSet = 2,
                        remainingSets = 4,
                        remainingInPhase = 18.seconds,
                        remainingTotal = 148.seconds,
                    ),
                    totalSets = 5,
                    isPaused = false,
                ),
                eventSink = {},
            ),
        )
    }
}
