package com.beeeam.feature.history

import com.slack.circuit.runtime.screen.ParcelableScreen
import com.slack.circuit.runtime.screen.StaticScreen
import kotlinx.parcelize.Parcelize

/** 러닝 기록 관리 화면. (구현 예정) */
@Parcelize
data object RunningHistoryScreen : ParcelableScreen, StaticScreen
