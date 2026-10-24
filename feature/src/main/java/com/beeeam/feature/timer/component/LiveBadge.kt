package com.beeeam.feature.timer.component

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 깜빡이며 퍼지는 점이 붙은 상태 배지. */
@Composable
internal fun LiveBadge(text: String, modifier: Modifier = Modifier) {
    val ping = rememberInfiniteTransition(label = "livePing")
    val pingProgress by ping.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1000, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "livePingProgress",
    )

    Row(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.2f), CircleShape)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .graphicsLayer {
                        scaleX = 1f + pingProgress
                        scaleY = 1f + pingProgress
                        alpha = 1f - pingProgress
                    }
                    .background(Color.White, CircleShape),
            )
            Box(modifier = Modifier.size(10.dp).background(Color.White, CircleShape))
        }
        Text(text = text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
    }
}
