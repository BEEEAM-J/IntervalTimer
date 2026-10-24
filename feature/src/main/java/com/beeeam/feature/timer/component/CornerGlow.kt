package com.beeeam.feature.timer.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 카드 위쪽 모서리에 은은한 빛, 반대편 아래쪽 모서리에 옅은 그림자를 그리는 장식.
 *
 * @param lightOnLeft true 면 빛이 왼쪽 위 / 그림자가 오른쪽 아래, false 면 반대로 그린다.
 */
internal fun Modifier.cornerGlow(lightOnLeft: Boolean): Modifier = drawBehind {
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
