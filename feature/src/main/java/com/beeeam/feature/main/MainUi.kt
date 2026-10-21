package com.beeeam.feature.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.beeeam.feature.main.component.MainBottomBar
import com.slack.circuit.foundation.CircuitContent
import com.slack.circuit.retained.rememberRetainedStateHolder

@Composable
fun MainUi(state: MainScreen.State, modifier: Modifier = Modifier) {
    // 탭을 이동해도 각 화면의 retained 상태(진행 중인 타이머 등)를 유지한다.
    val retainedStateHolder = rememberRetainedStateHolder()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            MainBottomBar(
                tabs = state.tabs,
                selectedTab = state.selectedTab,
                onTabSelected = { state.eventSink(MainScreen.Event.TabSelected(it)) },
            )
        },
    ) { innerPadding ->
        retainedStateHolder.RetainedStateProvider(key = state.selectedTab.name) {
            CircuitContent(
                screen = state.selectedTab.screen,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MainBottomBarPreview() {
    MaterialTheme {
        MainBottomBar(tabs = MainTab.entries, selectedTab = MainTab.TIMER, onTabSelected = {})
    }
}