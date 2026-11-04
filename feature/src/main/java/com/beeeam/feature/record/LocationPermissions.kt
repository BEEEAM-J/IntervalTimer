package com.beeeam.feature.record

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 현재 위치 표시에 필요한 권한.
 *
 * Android 12 이상에서 사용자는 대략적 위치(COARSE)만 허용할 수 있으므로 두 권한을 함께 요청하고, 하나라도 허용되면 허용으로 본다.
 * (`:data:local` 의 manifest 와 LocationRepositoryImpl 에도 같은 목록이 있다.)
 */
internal val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

internal fun Context.hasLocationPermission(): Boolean =
    LOCATION_PERMISSIONS.any { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }

/** 거부된 뒤에도 true 면 다시 요청할 수 있고, false 면 "다시 묻지 않음" 상태다. */
internal fun Activity.shouldShowLocationRationale(): Boolean =
    LOCATION_PERMISSIONS.any { ActivityCompat.shouldShowRequestPermissionRationale(this, it) }

/** Compose 의 LocalContext 는 ContextWrapper 로 감싸져 있을 수 있으므로 Activity 까지 거슬러 올라간다. */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** 영구 거부된 권한은 앱에서 다시 요청할 수 없으므로 앱 설정 화면으로 보낸다. */
internal fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    startActivity(intent)
}
