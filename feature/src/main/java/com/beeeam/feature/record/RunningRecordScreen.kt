package com.beeeam.feature.record

import com.slack.circuit.runtime.screen.ParcelableScreen
import com.slack.circuit.runtime.screen.StaticScreen
import kotlinx.parcelize.Parcelize

/** 러닝 기록 측정 화면. (구현 예정) */
@Parcelize
data object RunningRecordScreen : ParcelableScreen, StaticScreen
