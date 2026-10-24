package com.beeeam.feature.timer.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeeam.feature.R
import com.beeeam.feature.util.FinishHighlight
import com.beeeam.feature.util.durationParts
import com.beeeam.feature.util.toClockText
import com.beeeam.feature.util.twoDigits
import com.beeeam.feature.util.unitValueText
import kotlin.time.Duration

/** 총 운동 시간, 완주 세트, 운동 / 휴식 구간 합계. */
@Composable
internal fun FinishedSummary(
    totalDuration: Duration,
    totalWorkDuration: Duration,
    totalRestDuration: Duration,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(FinishHighlight.copy(alpha = 0.18f), Color(0x408A5D0E))))
            .border(1.dp, FinishHighlight.copy(alpha = 0.35f), shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SummaryLabel(iconRes = R.drawable.ic_tab_timer, text = stringResource(R.string.timer_total_duration_label))
            Text(
                text = unitValueText(
                    parts = durationParts(totalDuration.inWholeSeconds.toInt())
                        .map { (value, unit) -> twoDigits(value) to " $unit" },
                    numberStyle = SpanStyle(fontSize = 52.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    unitStyle = SpanStyle(fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f)),
                ),
                color = Color.White,
                style = TextStyle(lineHeight = 52.sp),
            )
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PhaseTotal(
                label = stringResource(R.string.timer_work_chip),
                iconRes = R.drawable.ic_bolt,
                iconTint = Color(0xFFFFDBD1),
                total = totalWorkDuration,
                modifier = Modifier.weight(1f),
            )
            PhaseTotal(
                label = stringResource(R.string.timer_finished_rest_total),
                iconRes = R.drawable.ic_air,
                iconTint = Color(0xFFFFB5A0),
                total = totalRestDuration,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RowScope.SummaryLabel(@DrawableRes iconRes: Int, text: String) {
    Row(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(20.dp),
        )
        Text(text = text, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 운동 또는 휴식 구간의 합계 시간 타일. */
@Composable
private fun PhaseTotal(
    label: String,
    @DrawableRes iconRes: Int,
    iconTint: Color,
    total: Duration,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = total.toClockText(),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
    }
}
