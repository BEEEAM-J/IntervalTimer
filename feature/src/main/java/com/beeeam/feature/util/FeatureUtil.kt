package com.beeeam.feature.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.beeeam.feature.R
import kotlin.time.Duration

internal fun twoDigits(value: Int): String = "%02d".format(value)

/** 남은 시간을 올림한 초 단위로 mm:ss 형식 변환. (29.4초 → 00:30) */
internal fun Duration.toClockText(): String {
    val totalSeconds = (inWholeMilliseconds + 999) / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/** 초 단위 값을 "N분 N초" 형식으로 변환. 1시간 이상이면 "N시간 N분 N초". */
@Composable
internal fun durationText(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        stringResource(R.string.timer_duration_hours_value, hours, minutes, seconds)
    } else {
        stringResource(R.string.timer_duration_value, minutes, seconds)
    }
}
