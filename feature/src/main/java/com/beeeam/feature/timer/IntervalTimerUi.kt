package com.beeeam.feature.timer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.beeeam.feature.main.HideBottomBarEffect
import com.beeeam.feature.timer.IntervalTimerScreen.State
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope

/** 타이머 상태에 따라 설정 / 준비 중 / 진행 중 화면을 보여준다. */
@CircuitInject(IntervalTimerScreen::class, AppScope::class)
@Composable
fun IntervalTimerUi(state: State, modifier: Modifier = Modifier) {
    val preparation = state.preparation
    val session = state.session
    // 준비 중 / 운동 중에는 탭 이동을 막기 위해 바텀바를 숨긴다.
    HideBottomBarEffect(hidden = preparation != null || session != null)
    when {
        preparation != null -> TimerPreparingContent(preparation = preparation, eventSink = state.eventSink, modifier = modifier)
        session != null -> TimerRunningContent(session = session, eventSink = state.eventSink, modifier = modifier)
        else -> TimerSetupContent(state = state, modifier = modifier)
    }
}
