package com.beeeam.feature.timer

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import com.beeeam.feature.timer.component.ProgressRing
import com.beeeam.feature.timer.component.cornerGlow
import com.beeeam.feature.util.AccentOrange
import com.beeeam.feature.util.AccentOrangeContainer
import com.beeeam.feature.util.NeutralContainer
import com.beeeam.feature.util.RestColor
import com.beeeam.feature.util.SetupBackground
import com.beeeam.feature.util.SetupTextPrimary
import com.beeeam.feature.util.SetupTextSecondary
import com.beeeam.feature.util.WorkColor
import com.beeeam.feature.util.WorkContainer
import com.beeeam.feature.util.durationText
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
        NextStepCard(session = session)
    }
}

@Composable
private fun PhaseCard(session: Session, eventSink: (Event) -> Unit, modifier: Modifier = Modifier) {
    val progress = session.progress
    val isWork = progress.phase == Phase.WORK
    val cardShape = RoundedCornerShape(16.dp)
    val cardColor by animateColorAsState(targetValue = if (isWork) WorkColor else RestColor, label = "phaseCard")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = cardShape)
            .clip(cardShape)
            .background(cardColor)
            .cornerGlow(lightOnLeft = true)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
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

/** 깜빡이며 퍼지는 점이 붙은 상태 배지. */
@Composable
private fun LiveBadge(text: String) {
    val ping = rememberInfiniteTransition(label = "livePing")
    val pingProgress by ping.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1000, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "livePingProgress",
    )

    Row(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer {
                        scaleX = 1f + pingProgress
                        scaleY = 1f + pingProgress
                        alpha = 1f - pingProgress
                    }
                    .background(Color.White, CircleShape),
            )
            Box(modifier = Modifier.size(10.dp).background(Color.White, CircleShape))
        }
        Text(text = text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
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
    val buttonShape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = { eventSink(if (isPaused) Event.Resume else Event.Pause) },
            shape = buttonShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = SetupTextPrimary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
        ) {
            Icon(
                painter = painterResource(if (isPaused) R.drawable.ic_play_arrow else R.drawable.ic_pause),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(if (isPaused) R.string.timer_resume else R.string.timer_pause),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        OutlinedButton(
            onClick = { eventSink(Event.Stop) },
            shape = buttonShape,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White.copy(alpha = 0.2f),
                contentColor = Color.White,
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_stop),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.timer_stop), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** 현재 페이즈가 끝난 뒤 이어질 단계를 미리 알려주는 카드. */
@Composable
private fun NextStepCard(session: Session) {
    val duration = durationText(session.nextPhaseDuration.inWholeSeconds.toInt())
    val step = when (session.nextPhase) {
        Phase.WORK -> NextStep(
            iconRes = R.drawable.ic_tab_record,
            iconColor = WorkColor,
            iconBackground = WorkContainer,
            title = stringResource(R.string.timer_next_work, duration),
            chip = stringResource(R.string.timer_next_work_chip),
        )
        Phase.REST -> NextStep(
            iconRes = R.drawable.ic_self_improvement,
            iconColor = RestColor,
            iconBackground = RestColor.copy(alpha = 0.12f),
            title = stringResource(R.string.timer_next_rest, duration),
            chip = stringResource(R.string.timer_next_rest_chip),
        )
        Phase.FINISHED -> NextStep(
            iconRes = R.drawable.ic_flag,
            iconColor = AccentOrange,
            iconBackground = AccentOrangeContainer,
            title = stringResource(R.string.timer_next_finished),
            chip = stringResource(R.string.timer_next_finished_chip),
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(step.iconBackground, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(step.iconRes),
                    contentDescription = null,
                    tint = step.iconColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.timer_next_step_label),
                    color = SetupTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(text = step.title, color = SetupTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = step.chip,
                color = SetupTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(NeutralContainer, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

private data class NextStep(
    @DrawableRes val iconRes: Int,
    val iconColor: Color,
    val iconBackground: Color,
    val title: String,
    val chip: String,
)

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
