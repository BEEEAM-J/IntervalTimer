package com.beeeam.intervaltimer.tracking

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.beeeam.domain.model.TrackingState
import com.beeeam.domain.repository.RunningTracker
import com.beeeam.intervaltimer.IntervalTimerApplication
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 러닝을 기록하는 동안 앱을 포그라운드로 유지하는 Service.
 *
 * 화면이 꺼지거나 앱이 백그라운드로 가면 시스템이 위치 수신을 제한하거나 앱을 종료할 수 있다.
 * 포그라운드 Service 로 실행되는 동안에는 위치를 계속 받을 수 있다.
 *
 * 위치를 받고 기록하는 일은 [RunningTracker] 가 하고, 이 Service 는 포그라운드 유지와 알림만 맡는다.
 * [RunningTracker] 가 기록을 시작할 때 이 Service 를 띄우고, 기록이 끝나면 Service 가 상태를 보고 스스로 종료한다.
 */
class LocationTrackingService : Service() {

    @Inject
    lateinit var tracker: RunningTracker

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null
    private lateinit var notification: TrackingNotification

    override fun onCreate() {
        // 다른 콜백보다 먼저 주입해, 이후 모든 콜백에서 tracker 를 쓸 수 있게 한다.
        (application as IntervalTimerApplication).appGraph.inject(this)
        super.onCreate()
        notification = TrackingNotification(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForegroundService 로 시작되면 몇 초 안에 포그라운드로 바꾸지 않으면 앱이 종료되므로 가장 먼저 한다.
        ServiceCompat.startForeground(
            this,
            TrackingNotification.NOTIFICATION_ID,
            notification.build(tracker.state.value),
            foregroundServiceType(),
        )

        // 기록 중 start() 가 다시 불려 Service 가 여러 번 시작돼도 상태는 한 번만 관찰한다.
        if (observeJob == null) {
            observeJob = scope.launch {
                tracker.state.collect { state ->
                    when (state) {
                        is TrackingState.Running, is TrackingState.Paused -> notification.update(state)
                        TrackingState.Idle, is TrackingState.Finished -> stopTracking()
                    }
                }
            }
        }

        // 프로세스가 강제 종료되면 기록도 사라지므로(복구 정책: 기록 폐기, #14), 시스템이 Service 를 다시 시작하지 않게 한다.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        // 기록 중에 Service 가 먼저 사라지면(시스템이 종료하는 등) 위치를 계속 받을 수 없으므로 기록도 마친다.
        // 기록이 끝나서 스스로 종료한 경우에는 이미 Finished 라 아무 일도 하지 않는다.
        tracker.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopTracking() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0
}
