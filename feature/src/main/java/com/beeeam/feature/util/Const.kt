package com.beeeam.feature.util

import androidx.compose.ui.graphics.Color

internal val WorkColor = Color(0xFF2E7D32)
// 흰 글씨를 올려도 읽히도록 채도와 밝기를 낮춘 하늘색
internal val RestColor = Color(0xFF5E9CBA)
internal val WorkContainer = Color(0xFFE8F5E9)
internal val NeutralContainer = Color(0xFFEFEDED)

internal val AccentOrange = Color(0xFFFF5722)
internal val AccentOrangeContainer = Color(0xFFFFECE6)
internal val AccentOrangeDeep = Color(0xFFB02F00)
internal val OnAccentOrangeContainer = Color(0xFF541200)
internal val SetupBackground = Color(0xFFFBF9F9)
internal val SetupTextPrimary = Color(0xFF1B1C1C)
internal val SetupTextSecondary = Color(0xFF5B4039)
internal val SetupTextTertiary = Color(0xFF5C5C5C)

internal val SETS_VALUES = (1..20).toList()
internal val WORK_MINUTES_VALUES = (0..20).toList()
internal val REST_MINUTES_VALUES = (0..10).toList()
internal val SECONDS_VALUES = (0..55 step 5).toList()