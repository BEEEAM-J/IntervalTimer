package com.beeeam.feature.timer

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeeam.feature.R
import com.beeeam.feature.component.DurationPicker
import com.beeeam.feature.component.PickerSuffix
import com.beeeam.feature.component.SettingItem
import com.beeeam.feature.component.WheelPicker
import com.beeeam.feature.timer.IntervalTimerScreen.Event
import com.beeeam.feature.timer.IntervalTimerScreen.State
import com.beeeam.feature.timer.component.TotalDurationSection
import com.beeeam.feature.util.REST_MINUTES_VALUES
import com.beeeam.feature.util.SETS_VALUES
import com.beeeam.feature.util.WORK_MINUTES_VALUES
import com.beeeam.feature.util.durationText
import kotlin.time.Duration.Companion.seconds

/** 세트 수, 운동/휴식 시간을 설정하는 화면. */
@Composable
internal fun TimerSetupContent(state: State, modifier: Modifier = Modifier) {
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

@Preview(showBackground = true)
@Composable
private fun TimerSetupContentPreview() {
    MaterialTheme {
        TimerSetupContent(
            State(
                sets = 1,
                workSeconds = 30,
                restSeconds = 30,
                totalDuration = 30.seconds,
                canStart = true,
                preparation = null,
                session = null,
                eventSink = {},
            ),
        )
    }
}
