package com.beeeam.feature.record

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beeeam.domain.model.GeoPoint
import com.beeeam.feature.record.component.LocationUnavailableBanner
import com.beeeam.feature.util.AccentOrange
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

/** 위치 권한이 허용된 상태의 화면. 지도에 내 위치를 표시하고, 위치를 찾으면 카메라를 옮긴다. */
// 권한이 허용된 경우에만 호출되므로 isMyLocationEnabled 의 권한 경고는 무시한다.
@SuppressLint("MissingPermission")
@Composable
internal fun RecordMapContent(
    location: CurrentLocation,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cameraPositionState = rememberCameraPositionState {
        // 이미 찾은 위치가 있으면(탭을 이동했다 돌아온 경우 등) 기본 위치를 거치지 않고 바로 그곳에서 시작한다.
        val start = (location as? CurrentLocation.Found)?.point?.toLatLng() ?: DEFAULT_LOCATION
        val zoom = if (location is CurrentLocation.Found) LOCATION_ZOOM else DEFAULT_ZOOM
        position = CameraPosition.fromLatLngZoom(start, zoom)
    }

    LaunchedEffect(location) {
        if (location is CurrentLocation.Found) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(location.point.toLatLng(), LOCATION_ZOOM))
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true),
            uiSettings = MapUiSettings(myLocationButtonEnabled = true),
        )
        when (location) {
            CurrentLocation.Loading -> CircularProgressIndicator(
                color = AccentOrange,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp),
            )
            CurrentLocation.Unavailable -> LocationUnavailableBanner(
                onRetry = onRetry,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
            )
            CurrentLocation.Idle, is CurrentLocation.Found -> Unit
        }
    }
}

/** 위치를 아직 찾지 못했을 때 보여줄 기본 위치. (서울시청) */
private val DEFAULT_LOCATION = LatLng(37.5665, 126.9780)
private const val DEFAULT_ZOOM = 10f
private const val LOCATION_ZOOM = 16f

private fun GeoPoint.toLatLng() = LatLng(latitude, longitude)
