package com.beeeam.feature.timer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeeam.domain.model.Phase
import com.beeeam.domain.model.TimerProgress
import com.beeeam.feature.R
import com.beeeam.feature.timer.IntervalTimerScreen.Event
import com.beeeam.feature.timer.IntervalTimerScreen.Session
import com.beeeam.feature.timer.component.FinishedSummary
import com.beeeam.feature.timer.component.TimerCard
import com.beeeam.feature.util.FinishButtonBottom
import com.beeeam.feature.util.FinishColor
import com.beeeam.feature.util.FinishHighlight
import com.beeeam.feature.util.OnFinishButton
import com.beeeam.feature.util.SetupBackground
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** 모든 세트를 마친 화면. 이번 운동의 총 시간과 세트, 운동 / 휴식 구간 합계를 보여준다. */
@Composable
internal fun TimerFinishedContent(session: Session, eventSink: (Event) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SetupBackground)
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        TimerCard(color = FinishColor, lightOnLeft = false, modifier = Modifier.fillMaxSize()) {
            FinishedHeader()
            FinishBadge()
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                FinishedSummary(
                    totalDuration = session.totalDuration,
                    completedSets = session.progress.currentSet,
                    totalSets = session.totalSets,
                    totalWorkDuration = session.totalWorkDuration,
                    totalRestDuration = session.totalRestDuration,
                )
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
