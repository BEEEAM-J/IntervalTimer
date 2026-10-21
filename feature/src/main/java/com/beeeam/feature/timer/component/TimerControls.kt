package com.beeeam.feature.timer.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.beeeam.domain.model.Phase
import com.beeeam.feature.R
import com.beeeam.feature.timer.IntervalTimerScreen.Event
import com.beeeam.feature.timer.IntervalTimerScreen.Session

@Composable
internal fun TimerControls(session: Session, contentColor: Color, eventSink: (Event) -> Unit) {
    val finished = session.progress.phase == Phase.FINISHED
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!finished) {
            Button(
                onClick = { eventSink(if (session.isPaused) Event.Resume else Event.Pause) },
                colors = ButtonDefaults.buttonColors(containerColor = contentColor, contentColor = Color.Black),
            ) {
                Text(stringResource(if (session.isPaused) R.string.timer_resume else R.string.timer_pause))
            }
        }
        OutlinedButton(
            onClick = { eventSink(Event.Stop) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        ) {
            Text(stringResource(if (finished) R.string.timer_done else R.string.timer_stop))
        }
    }
}