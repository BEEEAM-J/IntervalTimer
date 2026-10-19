package com.beeeam.feature.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.beeeam.feature.R
import com.beeeam.feature.history.RunningHistoryScreen
import com.beeeam.feature.record.RunningRecordScreen
import com.beeeam.feature.timer.IntervalTimerScreen
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.retained.rememberRetainedStateHolder
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.ParcelableScreen
import com.slack.circuit.runtime.screen.Screen
import kotlinx.parcelize.Parcelize

/** 하단 바텀바로 각 기능 화면을 전환하는 메인 화면. */
@Parcelize
data object MainScreen : ParcelableScreen {

    data class State(
        val tabs: List<MainTab>,
        val selectedTab: MainTab,
        val eventSink: (Event) -> Unit,
    ) : CircuitUiState

    sealed interface Event : CircuitUiEvent {
        data class TabSelected(val tab: MainTab) : Event
    }
}

enum class MainTab(
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
    val screen: Screen,
) {
    TIMER(R.string.tab_timer, R.drawable.ic_tab_timer, IntervalTimerScreen),
    RECORD(R.string.tab_record, R.drawable.ic_tab_record, RunningRecordScreen),
    HISTORY(R.string.tab_history, R.drawable.ic_tab_history, RunningHistoryScreen),
}


