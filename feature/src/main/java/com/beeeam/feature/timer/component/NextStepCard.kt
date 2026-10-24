package com.beeeam.feature.timer.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.beeeam.domain.model.Phase
import com.beeeam.feature.R
import com.beeeam.feature.util.AccentOrange
import com.beeeam.feature.util.AccentOrangeContainer
import com.beeeam.feature.util.NeutralContainer
import com.beeeam.feature.util.RestColor
import com.beeeam.feature.util.SetupTextPrimary
import com.beeeam.feature.util.SetupTextSecondary
import com.beeeam.feature.util.WorkColor
import com.beeeam.feature.util.WorkContainer
import com.beeeam.feature.util.durationText
import kotlin.time.Duration

/**
 * 현재 페이즈가 끝난 뒤 이어질 단계를 미리 알려주는 카드.
 *
 * @param nextPhase 이어질 페이즈. [Phase.FINISHED] 면 운동 완료를 알린다.
 * @param nextPhaseDuration [nextPhase] 의 길이.
 */
@Composable
internal fun NextStepCard(nextPhase: Phase, nextPhaseDuration: Duration, modifier: Modifier = Modifier) {
    val duration = durationText(nextPhaseDuration.inWholeSeconds.toInt())
    val step = when (nextPhase) {
        Phase.WORK -> NextStep(
            iconRes = R.drawable.ic_tab_record,
            iconColor = WorkColor,
            iconBackground = WorkContainer,
            title = stringResource(R.string.timer_next_work, duration),
            chip = stringResource(R.string.timer_next_work_chip),
        )
        Phase.REST -> NextStep(
            iconRes = R.drawable.ic_self_improvement,
            iconColor = RestColor,
            iconBackground = RestColor.copy(alpha = 0.12f),
            title = stringResource(R.string.timer_next_rest, duration),
            chip = stringResource(R.string.timer_next_rest_chip),
        )
        Phase.FINISHED -> NextStep(
            iconRes = R.drawable.ic_flag,
            iconColor = AccentOrange,
            iconBackground = AccentOrangeContainer,
            title = stringResource(R.string.timer_next_finished),
            chip = stringResource(R.string.timer_next_finished_chip),
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(step.iconBackground, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(step.iconRes),
                    contentDescription = null,
                    tint = step.iconColor,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.timer_next_step_label),
                    color = SetupTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(text = step.title, color = SetupTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = step.chip,
                color = SetupTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(NeutralContainer, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

private data class NextStep(
    @DrawableRes val iconRes: Int,
    val iconColor: Color,
    val iconBackground: Color,
    val title: String,
    val chip: String,
)
