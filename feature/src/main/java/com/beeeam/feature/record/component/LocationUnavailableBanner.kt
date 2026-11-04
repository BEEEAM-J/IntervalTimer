package com.beeeam.feature.record.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.beeeam.feature.R
import com.beeeam.feature.util.AccentOrange
import com.beeeam.feature.util.SetupTextPrimary

/** 권한은 있지만 위치를 찾지 못했을 때 지도 위에 띄우는 안내. (기기의 위치 설정이 꺼져 있는 등) */
@Composable
internal fun LocationUnavailableBanner(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 4.dp)) {
            Text(
                text = stringResource(R.string.record_location_unavailable),
                color = SetupTextPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onRetry, modifier = Modifier.align(Alignment.End)) {
                Text(text = stringResource(R.string.record_retry), color = AccentOrange, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
