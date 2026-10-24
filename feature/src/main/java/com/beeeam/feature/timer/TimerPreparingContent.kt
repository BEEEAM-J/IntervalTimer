package com.beeeam.feature.timer

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeeam.feature.R
import com.beeeam.feature.timer.IntervalTimerScreen.Event
import com.beeeam.feature.timer.IntervalTimerScreen.Preparation
import com.beeeam.feature.timer.component.ProgressRing
import com.beeeam.feature.timer.component.cornerGlow
import com.beeeam.feature.util.AccentOrange
import com.beeeam.feature.util.AccentOrangeDeep
import com.beeeam.feature.util.OnAccentOrangeContainer
import com.beeeam.feature.util.SetupBackground
import com.beeeam.feature.util.durationText
import com.beeeam.feature.util.inWholeSecondsCeil
import kotlin.time.Duration.Companion.seconds

/** 운동 시작 전 준비 카운트다운 화면. 카운트다운이 끝나면 "GO!" 를 보여준다. */
@Composable
internal fun TimerPreparingContent(preparation: Preparation, eventSink: (Event) -> Unit, modifier: Modifier = Modifier) {
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
                .background(AccentOrange)
                .cornerGlow(lightOnLeft = false)
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            PreparingHeader()
            CountdownRing(preparation = preparation)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                PreparingSummary(preparation = preparation)
                PreparingControls(eventSink = eventSink)
            }
        }
    }
}

@Composable
private fun PreparingHeader() {
    val spin = rememberInfiniteTransition(label = "badgeSpin")
    val rotation by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 3000, easing = LinearEasing), RepeatMode.Restart),
        label = "badgeRotation",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_sync),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(rotation),
            )
            Text(
                text = stringResource(R.string.timer_preparing_badge),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
            )
        }
        Text(
            text = stringResource(R.string.timer_preparing),
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = stringResource(R.string.timer_preparing_description),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** 남은 비율만큼 줄어드는 원형 링과 가운데 카운트다운 숫자. */
@Composable
private fun CountdownRing(preparation: Preparation) {
    ProgressRing(
        progress = preparation.progress,
        size = 224.dp,
        strokeWidth = 9.dp,
        trackAlpha = 0.2f,
        modifier = Modifier.padding(vertical = 16.dp),
    ) {
        Text(
            text = if (preparation.isGo) {
                stringResource(R.string.timer_go)
            } else {
                preparation.remaining.inWholeSecondsCeil().toString()
            },
            color = Color.White,
            fontSize = 62.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(fontFeatureSettings = "tnum"),
        )
    }
}

/** 사용자가 설정한 총 세트 수와 총 운동 시간. */
@Composable
private fun PreparingSummary(preparation: Preparation) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        SummaryRow(
            iconRes = R.drawable.ic_repeat,
            label = stringResource(R.string.timer_preparing_total_sets),
            value = stringResource(R.string.timer_sets_value, preparation.totalSets),
        )
        HorizontalDivider(
            color = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.padding(vertical = 8.dp),
        )
        SummaryRow(
            iconRes = R.drawable.ic_tab_timer,
            label = stringResource(R.string.timer_total_duration_label),
            value = durationText(preparation.totalDuration.inWholeSeconds.toInt()),
        )
    }
}

@Composable
private fun SummaryRow(@DrawableRes iconRes: Int, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp),
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PreparingControls(eventSink: (Event) -> Unit) {
    val buttonShape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = { eventSink(Event.SkipPreparation) },
            shape = buttonShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = OnAccentOrangeContainer),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bolt),
                contentDescription = null,
                tint = AccentOrangeDeep,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.timer_skip_preparation), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(
            onClick = { eventSink(Event.Stop) },
            shape = buttonShape,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White.copy(alpha = 0.2f),
                contentColor = Color.White,
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.timer_stop), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerPreparingContentPreview() {
    MaterialTheme {
        TimerPreparingContent(
            preparation = Preparation(
                totalSets = 5,
                totalDuration = 570.seconds,
                remaining = 2.seconds,
                progress = 0.2f,
                isGo = false,
            ),
            eventSink = {},
        )
    }
}
