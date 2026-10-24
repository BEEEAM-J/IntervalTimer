package com.beeeam.feature.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.beeeam.feature.R
import kotlin.time.Duration

internal fun twoDigits(value: Int): String = "%02d".format(value)

/** 초 단위로 올림한 값. (29.4초 → 30) */
internal fun Duration.inWholeSecondsCeil(): Long = (inWholeMilliseconds + 999) / 1000

/** 남은 시간을 올림한 초 단위로 mm:ss 형식 변환. (29.4초 → 00:30) */
internal fun Duration.toClockText(): String {
    val totalSeconds = inWholeSecondsCeil()
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/** 초 단위 값을 (숫자, 단위) 목록으로 나눈다. 0 인 단위는 생략하고, 모두 0 이면 0초. */
@Composable
internal fun durationParts(totalSeconds: Int): List<Pair<Int, String>> {
    val secondsUnit = stringResource(R.string.timer_seconds_suffix)
    val parts = listOf(
        totalSeconds / 3600 to stringResource(R.string.timer_hours_suffix),
        totalSeconds % 3600 / 60 to stringResource(R.string.timer_minutes_suffix),
        totalSeconds % 60 to secondsUnit,
    ).filter { (value, _) -> value > 0 }
    return parts.ifEmpty { listOf(0 to secondsUnit) }
}

/** 초 단위 값을 "N시간 N분 N초" 형식으로 변환. 0 인 단위는 생략한다. (90초 → 1분 30초, 30초 → 30초) */
@Composable
internal fun durationText(totalSeconds: Int): String =
    durationParts(totalSeconds).joinToString(" ") { (value, unit) -> "$value$unit" }

/** (숫자, 단위) 목록을 숫자와 단위에 각각 다른 스타일을 적용한 텍스트로 만든다. (1분 30초) */
internal fun unitValueText(
    parts: List<Pair<String, String>>,
    numberStyle: SpanStyle,
    unitStyle: SpanStyle,
): AnnotatedString = buildAnnotatedString {
    parts.forEachIndexed { index, (number, unit) ->
        if (index > 0) append(' ')
        withStyle(numberStyle) { append(number) }
        withStyle(unitStyle) { append(unit) }
    }
}
