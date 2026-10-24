package com.beeeam.feature.timer

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.beeeam.feature.timer.component.CardPrimaryButton
import com.beeeam.feature.timer.component.CardSecondaryButton
import com.beeeam.feature.timer.component.LiveBadge
import com.beeeam.feature.timer.component.NextStepCard
import com.beeeam.feature.timer.component.ProgressRing
import com.beeeam.feature.timer.component.TimerCard
import com.beeeam.feature.util.RestColor
import com.beeeam.feature.util.SetupBackground
import com.beeeam.feature.util.SetupTextPrimary
import com.beeeam.feature.util.WorkColor
import com.beeeam.feature.util.toClockText
import kotlin.time.Duration.Companion.seconds

/**
 * 운동 중 / 휴식 중 화면. 모든 세트를 마치면 [TimerFinishedContent] 를 보여준다.
 *
 * 운동과 휴식은 카드 색과 문구만 다르므로 하나의 카드로 그려, 페이즈 전환 시 카드 색이 자연스럽게 바뀌도록 한다.
 */
@Composable
internal fun TimerRunningContent(session: Session, eventSink: (Event) -> Unit, modifier: Modifier = Modifier) {
    if (session.progress.phase == Phase.FINISHED) {
        TimerFinishedContent(session = session, eventSink = eventSink, modifier = modifier)
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SetupBackground)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PhaseCard(session = session, eventSink = eventSink, modifier = Modifier.weight(1f))
        NextStepCard(nextPhase = session.nextPhase, nextPhaseDuration = session.nextPhaseDuration)
    }
}

@Composable
private fun PhaseCard(session: Session, eventSink: (Event) -> Unit, modifier: Modifier = Modifier) {
    val progress = session.progress
    val isWork = progress.phase == Phase.WORK
    val cardColor by animateColorAsState(targetValue = if (isWork) WorkColor else RestColor, label = "phaseCard")

    TimerCard(color = cardColor, lightOnLeft = true, modifier = modifier, verticalPadding = 24.dp) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (isWork) {
                LiveBadge(text = stringResource(R.string.timer_work_badge))
            }
            Text(
                text = stringResource(if (isWork) R.string.timer_work_title else R.string.timer_phase_rest),
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        ProgressRing(progress = session.phaseProgress, size = 256.dp, strokeWidth = 15.dp, trackAlpha = 0.18f) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.timer_remaining_in_phase),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = progress.remainingInPhase.toClockText(),
                    color = Color.White,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SessionSummary(session = session)
            RunningControls(isPaused = session.isPaused, eventSink = eventSink)
        }
    }
}

/** 세트 진행 상황과 전체 남은 시간. */
@Composable
private fun SessionSummary(session: Session) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_repeat),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.timer_set_progress, session.progress.currentSet, session.totalSets),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.timer_total_remaining),
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_tab_timer),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = session.progress.remainingTotal.toClockText(),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                )
            }
        }
    }
}

@Composable
private fun RunningControls(isPaused: Boolean, eventSink: (Event) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CardPrimaryButton(
            text = stringResource(if (isPaused) R.string.timer_resume else R.string.timer_pause),
            iconRes = if (isPaused) R.drawable.ic_play_arrow else R.drawable.ic_pause,
            contentColor = SetupTextPrimary,
            onClick = { eventSink(if (isPaused) Event.Resume else Event.Pause) },
            modifier = Modifier.weight(1f),
        )
        CardSecondaryButton(
            text = stringResource(R.string.timer_stop),
            iconRes = R.drawable.ic_stop,
            onClick = { eventSink(Event.Stop) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TimerRunningContentPreview() {
    MaterialTheme {
        TimerRunningContent(
            session = Session(
                progress = TimerProgress(
                    phase = Phase.WORK,
                    currentSet = 2,
                    remainingSets = 4,
                    remainingInPhase = 12.seconds,
                    remainingTotal = 148.seconds,
                ),
                totalSets = 5,
                isPaused = false,
                phaseProgress = 0.2f,
                nextPhase = Phase.REST,
                nextPhaseDuration = 60.seconds,
                totalDuration = 690.seconds,
                totalWorkDuration = 450.seconds,
                totalRestDuration = 240.seconds,
            ),
            eventSink = {},
        )
    }
}
