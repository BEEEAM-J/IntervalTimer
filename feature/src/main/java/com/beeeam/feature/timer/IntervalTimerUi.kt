package com.beeeam.feature.timer

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.beeeam.feature.component.DurationPicker
import com.beeeam.feature.util.durationText
import com.beeeam.feature.component.PickerSuffix
import com.beeeam.feature.component.SettingItem
import com.beeeam.feature.timer.component.TimerControls
import com.beeeam.feature.timer.component.TotalDurationSection
import com.beeeam.feature.component.WheelPicker
import com.beeeam.feature.timer.IntervalTimerScreen.Event
import com.beeeam.feature.timer.IntervalTimerScreen.Session
import com.beeeam.feature.timer.IntervalTimerScreen.State
import com.beeeam.feature.util.REST_MINUTES_VALUES
import com.beeeam.feature.util.RestColor
import com.beeeam.feature.util.SETS_VALUES
import com.beeeam.feature.util.WORK_MINUTES_VALUES
import com.beeeam.feature.util.WorkColor
import com.beeeam.feature.util.toClockText
import kotlin.time.Duration.Companion.seconds

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
        TotalDurationSection(state = state)
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

@Preview(showBackground = true)
@Composable
private fun TimerSetupPreview() {
    MaterialTheme {
        IntervalTimerUi(
            State(
                sets = 1,
                workSeconds = 30,
                restSeconds = 30,
                totalDuration = 30.seconds,
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
                totalDuration = 30.seconds,
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
