package com.beeeam.feature.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeeam.feature.R
import com.beeeam.feature.timer.IntervalTimerScreen.Event
import com.beeeam.feature.timer.IntervalTimerScreen.Preparation
import com.beeeam.feature.util.PrepareColor
import com.beeeam.feature.util.durationText
import com.beeeam.feature.util.inWholeSecondsCeil
import kotlin.time.Duration.Companion.seconds

/** 운동 시작 전 준비 카운트다운 화면. 카운트다운이 끝나면 "Go!" 를 보여준다. */
@Composable
internal fun TimerPreparingContent(preparation: Preparation, eventSink: (Event) -> Unit, modifier: Modifier = Modifier) {
    val contentColor = Color.White

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PrepareColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.timer_preparing),
            color = contentColor,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.timer_sets_value, preparation.totalSets),
            color = contentColor,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(
                R.string.timer_preparing_total_duration,
                durationText(preparation.totalDuration.inWholeSeconds.toInt()),
            ),
            color = contentColor.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyLarge,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (preparation.isGo) {
                    stringResource(R.string.timer_go)
                } else {
                    preparation.remaining.inWholeSecondsCeil().toString()
                },
                color = contentColor,
                fontSize = 120.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Button(
                onClick = { eventSink(Event.SkipPreparation) },
                colors = ButtonDefaults.buttonColors(containerColor = contentColor, contentColor = Color.Black),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Text(stringResource(R.string.timer_skip_preparation), fontSize = 18.sp)
            }
            OutlinedButton(
                onClick = { eventSink(Event.Stop) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
            ) {
                Text(stringResource(R.string.timer_stop), fontSize = 18.sp)
            }
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
                totalDuration = 190.seconds,
                remaining = 7.seconds,
                isGo = false,
            ),
            eventSink = {},
        )
    }
}
