package com.beeeam.feature.timer

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeeam.domain.model.Phase
import com.beeeam.domain.model.TimerProgress
import com.beeeam.feature.R
import com.beeeam.feature.timer.IntervalTimerScreen.Event
import com.beeeam.feature.timer.IntervalTimerScreen.Session
import com.beeeam.feature.timer.component.cornerGlow
import com.beeeam.feature.util.FinishButtonBottom
import com.beeeam.feature.util.FinishColor
import com.beeeam.feature.util.FinishHighlight
import com.beeeam.feature.util.OnFinishButton
import com.beeeam.feature.util.SetupBackground
import com.beeeam.feature.util.durationParts
import com.beeeam.feature.util.toClockText
import com.beeeam.feature.util.twoDigits
import com.beeeam.feature.util.unitValueText
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** 모든 세트를 마친 화면. 이번 운동의 총 시간과 세트, 운동 / 휴식 구간 합계를 보여준다. */
@Composable
internal fun TimerFinishedContent(session: Session, eventSink: (Event) -> Unit, modifier: Modifier = Modifier) {
    val cardShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SetupBackground)
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .shadow(elevation = 12.dp, shape = cardShape)
                .clip(cardShape)
                .background(FinishColor)
                .cornerGlow(lightOnLeft = false)
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            FinishedHeader()
            FinishBadge()
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FinishedSummary(session = session)
                DoneButton(onClick = { eventSink(Event.Stop) })
            }
        }
    }
}

@Composable
private fun FinishedHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(FinishHighlight.copy(alpha = 0.28f), FinishHighlight.copy(alpha = 0.12f))))
                .border(1.dp, FinishHighlight.copy(alpha = 0.5f), CircleShape)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_military_tech),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.timer_finished_badge),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.7.sp,
            )
        }
        Text(
            text = stringResource(R.string.timer_phase_finished),
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

/** 트로피와 "FINISH!" 를 담은 원형 배지. */
@Composable
private fun FinishBadge() {
    Column(
        modifier = Modifier
            .padding(vertical = 16.dp)
            .size(224.dp)
            .background(Color.White.copy(alpha = 0.1f), CircleShape)
            .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_emoji_events),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = stringResource(R.string.timer_finished_hero),
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Text(
            text = stringResource(R.string.timer_finished_hero_caption),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** 총 운동 시간, 완주 세트, 운동 / 휴식 구간 합계. */
@Composable
private fun FinishedSummary(session: Session) {
    val completedSets = session.progress.currentSet
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
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
                    parts = durationParts(session.totalDuration.inWholeSeconds.toInt())
                        .map { (value, unit) -> twoDigits(value) to " $unit" },
                    numberStyle = SpanStyle(fontSize = 52.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    unitStyle = SpanStyle(fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f)),
                ),
                color = Color.White,
                style = TextStyle(lineHeight = 52.sp),
            )
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SummaryLabel(iconRes = R.drawable.ic_flag_circle, text = stringResource(R.string.timer_finished_sets_label))
            Text(
                text = stringResource(R.string.timer_finished_sets_value, completedSets, session.totalSets),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.timer_finished_rate, completedSets * 100 / session.totalSets),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .clip(CircleShape)
                    .background(FinishHighlight.copy(alpha = 0.3f))
                    .border(1.dp, FinishHighlight.copy(alpha = 0.45f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PhaseTotal(
                label = stringResource(R.string.timer_finished_work_total),
                iconRes = R.drawable.ic_bolt,
                iconTint = Color(0xFFFFDBD1),
                total = session.totalWorkDuration,
                modifier = Modifier.weight(1f),
            )
            PhaseTotal(
                label = stringResource(R.string.timer_finished_rest_total),
                iconRes = R.drawable.ic_air,
                iconTint = Color(0xFFFFB5A0),
                total = session.totalRestDuration,
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

@Composable
private fun DoneButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Button(
        onClick = onClick,
        shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = OnFinishButton),
        border = BorderStroke(1.dp, FinishHighlight.copy(alpha = 0.8f)),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(elevation = 8.dp, shape = shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.White, FinishButtonBottom))),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.timer_done), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerFinishedContentPreview() {
    MaterialTheme {
        TimerFinishedContent(
            session = Session(
                progress = TimerProgress(
                    phase = Phase.FINISHED,
                    currentSet = 5,
                    remainingSets = 0,
                    remainingInPhase = Duration.ZERO,
                    remainingTotal = Duration.ZERO,
                ),
                totalSets = 5,
                isPaused = true,
                phaseProgress = 0f,
                nextPhase = Phase.FINISHED,
                nextPhaseDuration = Duration.ZERO,
                totalDuration = 570.seconds,
                totalWorkDuration = 450.seconds,
                totalRestDuration = 120.seconds,
            ),
            eventSink = {},
        )
    }
}
