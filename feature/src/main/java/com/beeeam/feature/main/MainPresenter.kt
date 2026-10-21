package com.beeeam.feature.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.slack.circuit.runtime.presenter.Presenter

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