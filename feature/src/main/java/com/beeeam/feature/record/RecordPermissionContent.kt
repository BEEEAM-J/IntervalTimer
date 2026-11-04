package com.beeeam.feature.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.beeeam.feature.R
import com.beeeam.feature.util.AccentOrange
import com.beeeam.feature.util.SetupBackground
import com.beeeam.feature.util.SetupTextPrimary
import com.beeeam.feature.util.SetupTextSecondary

/**
 * 위치 권한이 거부된 상태의 안내 화면.
 *
 * 다시 요청할 수 있는 거부와 설정에서만 허용할 수 있는 영구 거부는 [description] 과 버튼 동작만 다르다.
 */
@Composable
internal fun RecordPermissionContent(
    description: String,
    buttonText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.record_permission_title),
            color = SetupTextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = description,
            color = SetupTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange, contentColor = SetupBackground),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text(text = buttonText, fontWeight = FontWeight.SemiBold)
        }
    }
}
