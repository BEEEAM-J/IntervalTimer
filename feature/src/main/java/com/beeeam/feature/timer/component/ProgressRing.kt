package com.beeeam.feature.timer.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp

/**
 * 12시 방향부터 시계 방향으로 [progress] 만큼 채워지는 흰색 원형 링. 가운데에 [content] 를 놓는다.
 *
 * Presenter 는 일정 간격으로만 남은 시간을 갱신하므로, 그 사이를 보간해 링이 끊기지 않고 줄어들게 한다.
 */
@Composable
internal fun ProgressRing(
    progress: Float,
    size: Dp,
    strokeWidth: Dp,
    trackAlpha: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "ringProgress",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            // 공간이 부족해 캔버스가 직사각형이 되어도 원으로 그리도록, 트랙과 진행 링 모두 짧은 변 기준의 정사각형에 그린다.
            val diameter = this.size.minDimension - stroke
            drawCircle(
                color = Color.White.copy(alpha = trackAlpha),
                radius = diameter / 2,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = Color.White,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset((this.size.width - diameter) / 2, (this.size.height - diameter) / 2),
                size = Size(diameter, diameter),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        content()
    }
}
