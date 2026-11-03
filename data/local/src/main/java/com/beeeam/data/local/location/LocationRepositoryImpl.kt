package com.beeeam.data.local.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.beeeam.domain.model.GeoPoint
import com.beeeam.domain.repository.LocationRepository
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await

/**
 * Google Play 서비스의 [FusedLocationProviderClient][com.google.android.gms.location.FusedLocationProviderClient] 로
 * 기기 위치를 조회하는 [LocationRepository] 구현체.
 *
 * [context] 는 그래프에서 주입되는 Application Context 다.
 */
@ContributesBinding(AppScope::class)
@Inject
class LocationRepositoryImpl(
    private val context: Context,
) : LocationRepository {

    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    // CancellationTokenSource 를 받는 await 는 실험적 API 지만, 코루틴 취소를 위치 요청에 전달하는 유일한 방법이다.
    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getCurrentLocation(): GeoPoint? {
        if (!hasLocationPermission()) return null

        // 코루틴이 취소되면(화면을 벗어나는 등) 진행 중인 위치 요청도 함께 취소한다.
        val cancellation = CancellationTokenSource()
        return try {
            // 대략적 위치만 허용된 경우에는 Play 서비스가 정확도를 낮춰서 돌려준다.
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token)
                .await(cancellation)
                ?.toGeoPoint()
        } catch (e: SecurityException) {
            // 권한 확인 직후 사용자가 설정에서 권한을 거둔 경우
            null
        } catch (e: ApiException) {
            // Play 서비스를 사용할 수 없는 기기 등
            null
        }
    }

    private fun hasLocationPermission(): Boolean =
        LOCATION_PERMISSIONS.any { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

    private fun Location.toGeoPoint() = GeoPoint(latitude = latitude, longitude = longitude)

    private companion object {
        val LOCATION_PERMISSIONS = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}
