package com.beeeam.feature.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf

/** 탭 화면이 메인 화면의 바텀바 표시 여부를 바꿀 때 사용한다. 인자는 표시 여부. */
internal val LocalBottomBarVisibility = staticCompositionLocalOf<(Boolean) -> Unit> { {} }

/** [hidden] 이 true 인 동안 바텀바를 숨긴다. 화면이 사라지면 다시 보이게 한다. */
@Composable
internal fun HideBottomBarEffect(hidden: Boolean) {
    val setVisible = LocalBottomBarVisibility.current
    DisposableEffect(hidden, setVisible) {
        setVisible(!hidden)
        onDispose { setVisible(true) }
    }
}
