package com.beeeam.feature.timer.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beeeam.feature.R
import com.beeeam.feature.timer.IntervalTimerScreen
import com.beeeam.feature.util.durationText

/** 총 운동 시간과 그 계산식, 휴식 적용 규칙을 보여준다. */
@Composable
internal fun TotalDurationSection(state: IntervalTimerScreen.State) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.timer_total_duration_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = durationText(state.totalDuration.inWholeSeconds.toInt()),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        // 시작할 수 없는 설정이면 총 시간이 0 이라 계산식과 맞지 않으므로 숨긴다.
        if (state.canStart) {
            Text(
                text = stringResource(
                    R.string.timer_total_duration_formula,
                    durationText(state.workSeconds),
                    state.sets,
                    durationText(state.restSeconds),
                    state.sets - 1,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.timer_total_duration_rule),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}