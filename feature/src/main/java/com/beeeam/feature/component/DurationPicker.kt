package com.beeeam.feature.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.beeeam.feature.R
import com.beeeam.feature.util.SECONDS_VALUES
import com.beeeam.feature.util.twoDigits

/** 초 단위 값을 분 / 초 두 개의 휠로 나눠 고른다. 두 휠은 같은 너비를 차지한다. */
@Composable
internal fun RowScope.DurationPicker(totalSeconds: Int, onTotalSecondsChange: (Int) -> Unit, accent: Color, minuteValues: List<Int>) {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    WheelPicker(
        value = minutes,
        onValueChange = { onTotalSecondsChange(it * 60 + seconds) },
        values = minuteValues,
        unit = stringResource(R.string.timer_minutes_suffix),
        accent = accent,
        format = ::twoDigits,
        modifier = Modifier.weight(1f),
    )
    WheelPicker(
        value = seconds,
        onValueChange = { onTotalSecondsChange(minutes * 60 + it) },
        values = SECONDS_VALUES,
        unit = stringResource(R.string.timer_seconds_suffix),
        accent = accent,
        format = ::twoDigits,
        modifier = Modifier.weight(1f),
    )
}
