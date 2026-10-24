package com.beeeam.feature.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeeam.feature.R
import com.beeeam.feature.util.AccentOrange
import com.beeeam.feature.util.SetupTextPrimary
import com.beeeam.feature.util.SetupTextSecondary
import com.beeeam.feature.util.SetupTextTertiary
import com.beeeam.feature.util.unitValueText

/**
 * 아이콘, 제목, 설명과 현재 값을 보여주는 설정 카드. 터치하면 아래에 [picker] 를 펼친다.
 *
 * @param accent 아이콘과 칩의 강조 색상.
 * @param accentText 칩 글자 색상. 강조 색상이 밝아 글자로 쓰기 어려울 때 지정한다.
 * @param valueParts 현재 값을 (숫자, 단위) 목록으로 나눈 것. (1분 30초 → [1 to 분, 30 to 초])
 * @param picker 펼쳤을 때 보여줄 Picker. 가운데 하이라이트 밴드 위에 가로로 배치된다.
 */
@Composable
internal fun SettingItem(
    @DrawableRes iconRes: Int,
    accent: Color,
    title: String,
    description: String,
    valueParts: List<Pair<Int, String>>,
    expanded: Boolean,
    onClick: () -> Unit,
    chip: String? = null,
    accentText: Color = accent,
    picker: @Composable RowScope.() -> Unit,
) {
    val valueColor = if (expanded) accent else SetupTextPrimary
    val arrowRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "arrowRotation")

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = if (expanded) 4.dp else 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick)
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accent.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = title,
                            color = SetupTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = if (expanded) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        if (chip != null) {
                            Text(
                                text = chip,
                                color = accentText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(accent.copy(alpha = 0.12f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text = description,
                        color = SetupTextTertiary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Text(
                    text = unitValueText(
                        parts = valueParts.map { (value, unit) -> value.toString() to unit },
                        numberStyle = SpanStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                        unitStyle = SpanStyle(fontSize = 14.sp),
                    ),
                    color = valueColor,
                )
                Icon(
                    painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(20.dp)
                        .rotate(arrowRotation),
                )
            }
            AnimatedVisibility(visible = expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Picker 영역 안의 터치는 여기서 소비해 바깥 터치로 처리되지 않게 한다.
                        .pointerInput(Unit) { detectTapGestures() }
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // 여러 휠에 걸쳐 선택된 줄을 표시하는 하이라이트 밴드
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .height(PickerItemHeight)
                            .background(valueColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        picker()
                    }
                }
            }
        }
    }
}
