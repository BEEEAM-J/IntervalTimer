package com.beeeam.feature.timer

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.timer_setup_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        NumberField(
            value = state.setsInput,
            onValueChange = { state.eventSink(Event.SetsChanged(it)) },
            label = stringResource(R.string.timer_sets_label),
            suffix = stringResource(R.string.timer_sets_suffix),
        )
        NumberField(
            value = state.workSecondsInput,
            onValueChange = { state.eventSink(Event.WorkSecondsChanged(it)) },
            label = stringResource(R.string.timer_work_label),
            suffix = stringResource(R.string.timer_seconds_suffix),
        )
        NumberField(
            value = state.restSecondsInput,
            onValueChange = { state.eventSink(Event.RestSecondsChanged(it)) },
            label = stringResource(R.string.timer_rest_label),
            suffix = stringResource(R.string.timer_seconds_suffix),
            imeAction = ImeAction.Done,
        )
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
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(suffix) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = imeAction),
        modifier = Modifier.fillMaxWidth(),
    )
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
                setsInput = "5",
                workSecondsInput = "30",
                restSecondsInput = "10",
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
                setsInput = "5",
                workSecondsInput = "30",
                restSecondsInput = "10",
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
