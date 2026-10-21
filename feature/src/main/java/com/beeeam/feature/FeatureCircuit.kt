package com.beeeam.feature

import androidx.compose.ui.res.stringResource
import com.beeeam.feature.history.RunningHistoryScreen
import com.beeeam.feature.main.MainPresenter
import com.beeeam.feature.main.MainScreen
import com.beeeam.feature.main.MainUi
import com.beeeam.feature.record.RunningRecordScreen
import com.beeeam.feature.timer.IntervalTimerPresenter
import com.beeeam.feature.timer.IntervalTimerScreen
import com.beeeam.feature.timer.IntervalTimerUi
import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.CircuitUiState

/** feature 모듈의 모든 화면(Presenter/Ui)을 Circuit 에 등록한다. */
fun Circuit.Builder.addFeatureScreens(): Circuit.Builder = apply {
    addPresenter<MainScreen, MainScreen.State> { _, _, _ -> MainPresenter() }
    addUi<MainScreen, MainScreen.State> { state, modifier -> MainUi(state, modifier) }

    addPresenter<IntervalTimerScreen, IntervalTimerScreen.State> { _, _, _ -> IntervalTimerPresenter() }
    addUi<IntervalTimerScreen, IntervalTimerScreen.State> { state, modifier -> IntervalTimerUi(state, modifier) }

    addStaticUi<RunningRecordScreen, CircuitUiState> { _, modifier ->
        PlaceholderUi(title = stringResource(R.string.tab_record), modifier = modifier)
    }
    addStaticUi<RunningHistoryScreen, CircuitUiState> { _, modifier ->
        PlaceholderUi(title = stringResource(R.string.tab_history), modifier = modifier)
    }
}
