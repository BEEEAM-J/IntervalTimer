package com.beeeam.feature.main.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.beeeam.feature.main.MainTab
import com.beeeam.feature.util.AccentOrange
import com.beeeam.feature.util.AccentOrangeContainer
import com.beeeam.feature.util.SetupTextSecondary

/** 아이콘만 표시하는 하단 탭 바. 선택된 탭은 알약 모양 배경으로 강조한다. */
@Composable
internal fun MainBottomBar(
    tabs: List<MainTab>,
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
) {
    Surface(color = Color.White, shadowElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(80.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    MainBottomBarItem(
                        tab = tab,
                        selected = tab == selectedTab,
                        onClick = { onTabSelected(tab) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MainBottomBarItem(tab: MainTab, selected: Boolean, onClick: () -> Unit) {
    // Color.Transparent(투명한 검정)로 애니메이션하면 중간에 회색이 비치므로, 같은 색의 알파만 바꾼다.
    val indicatorColor by animateColorAsState(
        targetValue = if (selected) AccentOrangeContainer else AccentOrangeContainer.copy(alpha = 0f),
        label = "tabIndicator",
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) AccentOrange else SetupTextSecondary,
        label = "tabIcon",
    )
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 68.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .background(indicatorColor)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(tab.iconRes),
            // 라벨을 표시하지 않으므로 탭 이름은 접근성 설명으로만 제공한다.
            contentDescription = stringResource(tab.labelRes),
            tint = iconColor,
            modifier = Modifier.size(28.dp),
        )
    }
}
