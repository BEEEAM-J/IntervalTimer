package com.beeeam.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.beeeam.feature.PlaceholderUi
import com.beeeam.feature.R
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.screen.ParcelableScreen
import com.slack.circuit.runtime.screen.StaticScreen
import dev.zacsweers.metro.AppScope
import kotlinx.parcelize.Parcelize

/** 러닝 기록 관리 화면. (구현 예정) */
@Parcelize
data object RunningHistoryScreen : ParcelableScreen, StaticScreen

@CircuitInject(RunningHistoryScreen::class, AppScope::class)
@Composable
fun RunningHistoryUi(modifier: Modifier = Modifier) {
    PlaceholderUi(title = stringResource(R.string.tab_history), modifier = modifier)
}
