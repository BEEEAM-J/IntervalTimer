package com.beeeam.feature.timer.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 준비 / 운동 / 완료 화면의 본문을 담는 큰 색 카드. 자식은 가운데 정렬되어 위아래로 고르게 배치된다.
 *
 * @param lightOnLeft true 면 모서리 빛이 왼쪽 위 / 그림자가 오른쪽 아래, false 면 반대로 그린다.
 */
@Composable
internal fun TimerCard(
    color: Color,
    lightOnLeft: Boolean,
    modifier: Modifier = Modifier,
    verticalPadding: Dp = 32.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = shape)
            .clip(shape)
            .background(color)
            .cornerGlow(lightOnLeft)
            .padding(horizontal = 20.dp, vertical = verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        content = content,
    )
}

/** 카드 위쪽 모서리에 은은한 빛, 반대편 아래쪽 모서리에 옅은 그림자를 그리는 장식. */
private fun Modifier.cornerGlow(lightOnLeft: Boolean): Modifier = drawBehind {
    val radius = 200.dp.toPx()
    val lightCenter = Offset(if (lightOnLeft) 0f else size.width, 0f)
    val shadeCenter = Offset(if (lightOnLeft) size.width else 0f, size.height)
    drawCircle(
        brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent), lightCenter, radius),
        radius = radius,
        center = lightCenter,
    )
    drawCircle(
        brush = Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.10f), Color.Transparent), shadeCenter, radius),
        radius = radius,
        center = shadeCenter,
    )
}
