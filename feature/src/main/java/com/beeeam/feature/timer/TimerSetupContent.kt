package com.beeeam.feature.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeeam.feature.R
import com.beeeam.feature.component.DurationPicker
import com.beeeam.feature.component.SettingItem
import com.beeeam.feature.component.WheelPicker
import com.beeeam.feature.timer.IntervalTimerScreen.Event
import com.beeeam.feature.timer.IntervalTimerScreen.State
import com.beeeam.feature.timer.component.TotalDurationSection
import com.beeeam.feature.util.AccentOrange
import com.beeeam.feature.util.REST_MINUTES_VALUES
import com.beeeam.feature.util.RestColor
import com.beeeam.feature.util.SETS_VALUES
import com.beeeam.feature.util.SetupBackground
import com.beeeam.feature.util.WORK_MINUTES_VALUES
import com.beeeam.feature.util.WorkColor
import com.beeeam.feature.util.durationParts
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
            .background(SetupBackground)
            // 다른 요소가 소비하지 않은 터치(= Picker 영역 바깥)가 발생하면 Picker 를 접는다.
            .pointerInput(Unit) { detectTapGestures(onTap = { expanded = null }) }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingItem(
                iconRes = R.drawable.ic_repeat,
                accent = AccentOrange,
                title = stringResource(R.string.timer_sets_label),
                description = stringResource(R.string.timer_sets_description),
                valueParts = listOf(state.sets to stringResource(R.string.timer_sets_suffix)),
                expanded = expanded == SetupField.SETS,
                onClick = { toggle(SetupField.SETS) },
            ) {
                WheelPicker(
                    value = state.sets,
                    onValueChange = { state.eventSink(Event.SetsChanged(it)) },
                    values = SETS_VALUES,
                    unit = stringResource(R.string.timer_sets_suffix),
                    accent = AccentOrange,
                    modifier = Modifier.weight(1f),
                )
            }
            SettingItem(
                iconRes = R.drawable.ic_tab_record,
                accent = WorkColor,
                title = stringResource(R.string.timer_work_label),
                description = stringResource(R.string.timer_work_description),
                chip = stringResource(R.string.timer_work_chip),
                valueParts = durationParts(state.workSeconds),
                expanded = expanded == SetupField.WORK,
                onClick = { toggle(SetupField.WORK) },
            ) {
                DurationPicker(
                    totalSeconds = state.workSeconds,
                    onTotalSecondsChange = { state.eventSink(Event.WorkSecondsChanged(it)) },
                    accent = WorkColor,
                    minuteValues = WORK_MINUTES_VALUES,
                )
            }
            SettingItem(
                iconRes = R.drawable.ic_snooze,
                accent = RestColor,
                title = stringResource(R.string.timer_rest_label),
                description = stringResource(R.string.timer_rest_description),
                chip = stringResource(R.string.timer_rest_chip),
                valueParts = durationParts(state.restSeconds),
                expanded = expanded == SetupField.REST,
                onClick = { toggle(SetupField.REST) },
            ) {
                DurationPicker(
                    totalSeconds = state.restSeconds,
                    onTotalSecondsChange = { state.eventSink(Event.RestSecondsChanged(it)) },
                    accent = RestColor,
                    minuteValues = REST_MINUTES_VALUES,
                )
            }
        }
        TotalDurationSection(state = state)
        Button(
            onClick = { state.eventSink(Event.Start) },
            enabled = state.canStart,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, contentColor = Color.White),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_play_arrow),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.timer_start), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerSetupContentPreview() {
    MaterialTheme {
        TimerSetupContent(
            State(
                sets = 5,
                workSeconds = 90,
                restSeconds = 30,
                totalDuration = 570.seconds,
                canStart = true,
                preparation = null,
                session = null,
                eventSink = {},
            ),
        )
    }
}
