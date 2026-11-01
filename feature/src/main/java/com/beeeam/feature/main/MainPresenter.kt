package com.beeeam.feature.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject

@CircuitInject(MainScreen::class, AppScope::class)
@Inject
class MainPresenter : Presenter<MainScreen.State> {
    @Composable
    override fun present(): MainScreen.State {
        var selectedTab by rememberSaveable { mutableStateOf(MainTab.TIMER) }
        return MainScreen.State(
            tabs = MainTab.entries,
            selectedTab = selectedTab,
        ) { event ->
            when (event) {
                is MainScreen.Event.TabSelected -> selectedTab = event.tab
            }
        }
    }
}