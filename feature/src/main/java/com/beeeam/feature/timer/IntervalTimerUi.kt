package com.beeeam.feature.timer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.beeeam.feature.timer.IntervalTimerScreen.State

/** 타이머 상태에 따라 설정 / 준비 중 / 진행 중 화면을 보여준다. */
@Composable
fun IntervalTimerUi(state: State, modifier: Modifier = Modifier) {
    val preparation = state.preparation
    val session = state.session
    when {
        preparation != null -> TimerPreparingContent(preparation = preparation, eventSink = state.eventSink, modifier = modifier)
        session != null -> TimerRunningContent(session = session, eventSink = state.eventSink, modifier = modifier)
        else -> TimerSetupContent(state = state, modifier = modifier)
    }
}
