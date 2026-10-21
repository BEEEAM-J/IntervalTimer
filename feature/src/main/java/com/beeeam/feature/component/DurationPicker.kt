package com.beeeam.feature.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.beeeam.feature.R
import com.beeeam.feature.util.SECONDS_VALUES
import com.beeeam.feature.util.twoDigits

/** 초 단위 값을 분 / 초 두 개의 휠로 나눠 고른다. */
@Composable
internal fun DurationPicker(totalSeconds: Int, onTotalSecondsChange: (Int) -> Unit, minuteValues: List<Int>) {
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