package com.beeeam.feature.timer.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beeeam.feature.R
import com.beeeam.feature.timer.IntervalTimerScreen
import com.beeeam.feature.util.AccentOrange
import com.beeeam.feature.util.SetupTextPrimary
import com.beeeam.feature.util.SetupTextSecondary
import com.beeeam.feature.util.SetupTextTertiary
import com.beeeam.feature.util.durationText

/** 총 운동 시간과 그 계산식, 휴식 적용 규칙을 보여주는 카드. */
@Composable
internal fun TotalDurationSection(state: IntervalTimerScreen.State) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.timer_total_duration_label),
                    color = SetupTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = durationText(state.totalDuration.inWholeSeconds.toInt()),
                    color = AccentOrange,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            // 시작할 수 없는 설정이면 총 시간이 0 이라 계산식과 맞지 않으므로 숨긴다.
            if (state.canStart) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_calculate),
                        contentDescription = null,
                        tint = SetupTextTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(
                            R.string.timer_total_duration_formula,
                            durationText(state.workSeconds),
                            state.sets,
                            durationText(state.restSeconds),
                            state.sets - 1,
                        ),
                        color = SetupTextSecondary,
                        fontSize = 14.sp,
                    )
                }
            }
            Text(
                text = stringResource(R.string.timer_total_duration_rule),
                color = SetupTextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
