package com.beeeam.data.local.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.beeeam.domain.model.GeoPoint
import com.beeeam.domain.model.LocationPoint
import com.beeeam.domain.repository.LocationRepository
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.time.Duration
import kotlin.time.Instant

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

    override fun locationUpdates(interval: Duration, minInterval: Duration): Flow<LocationPoint> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval.inWholeMilliseconds)
            .setMinUpdateIntervalMillis(minInterval.inWholeMilliseconds)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                // 여러 위치가 한 번에 오면(일괄 전달) 오래된 것부터 순서대로 담겨 있다.
                result.locations.forEach { trySend(it.toLocationPoint()) }
            }
        }

        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            // 권한 확인 직후 사용자가 설정에서 권한을 거둔 경우
            close()
            return@callbackFlow
        }

        // 수집하는 쪽이 멈추면 호출된다. 해제하지 않으면 화면·Service 가 사라져도 위치를 계속 받아 배터리를 쓰고 누수가 생긴다.
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private fun hasLocationPermission(): Boolean =
        LOCATION_PERMISSIONS.any { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }

    private fun Location.toGeoPoint() = GeoPoint(latitude = latitude, longitude = longitude)

    private fun Location.toLocationPoint() = LocationPoint(
        point = toGeoPoint(),
        timestamp = Instant.fromEpochMilliseconds(time),
        // 정확도가 없는 위치를 0(완벽하게 정확)으로 두면 노이즈 필터를 그대로 통과하므로, 가장 부정확한 값으로 둔다.
        accuracy = if (hasAccuracy()) accuracy else Float.MAX_VALUE,
    )

    private companion object {
        val LOCATION_PERMISSIONS = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}
