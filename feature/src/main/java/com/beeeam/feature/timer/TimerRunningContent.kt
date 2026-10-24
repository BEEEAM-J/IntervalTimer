package com.beeeam.feature.timer

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.beeeam.feature.timer.component.TimerControls
import com.beeeam.feature.util.RestColor
import com.beeeam.feature.util.WorkColor
import com.beeeam.feature.util.toClockText
import kotlin.time.Duration.Companion.seconds

/**
 * 운동 중 / 휴식 중 / 운동 완료 화면.
 *
 * 페이즈마다 배경색과 문구만 다르므로 하나의 Composable 에서 그려, 페이즈 전환 시 배경색이 자연스럽게 바뀌도록 한다.
 */
@Composable
internal fun TimerRunningContent(session: Session, eventSink: (Event) -> Unit, modifier: Modifier = Modifier) {
    val progress = session.progress
    val targetColor = when (progress.phase) {
        Phase.WORK -> WorkColor
        Phase.REST -> RestColor
        Phase.FINISHED -> MaterialTheme.colorScheme.surface
    }
    val contentColor = if (progress.phase == Phase.FINISHED) MaterialTheme.colorScheme.onSurface else Color.White
    val backgroundColor by animateColorAsState(targetValue = targetColor, label = "phaseBackground")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(
                when (progress.phase) {
                    Phase.WORK -> R.string.timer_phase_work
                    Phase.REST -> R.string.timer_phase_rest
                    Phase.FINISHED -> R.string.timer_phase_finished
                },
            ),
            color = contentColor,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        if (progress.phase != Phase.FINISHED) {
            Text(
                text = progress.remainingInPhase.toClockText(),
                color = contentColor,
                fontSize = 96.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.timer_set_progress, progress.currentSet, session.totalSets),
                color = contentColor,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.timer_total_remaining, progress.remainingTotal.toClockText()),
                color = contentColor.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Spacer(modifier = Modifier.height(48.dp))
        TimerControls(session = session, contentColor = contentColor, eventSink = eventSink)
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
                    remainingInPhase = 18.seconds,
                    remainingTotal = 148.seconds,
                ),
                totalSets = 5,
                isPaused = false,
            ),
            eventSink = {},
        )
    }
}
