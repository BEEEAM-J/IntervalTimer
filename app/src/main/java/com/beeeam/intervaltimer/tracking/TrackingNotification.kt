package com.beeeam.intervaltimer.tracking

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.beeeam.domain.model.TrackingState
import com.beeeam.intervaltimer.R
import kotlin.time.Duration

/**
 * 러닝을 기록하는 동안 띄우는 알림.
 *
 * 포그라운드 Service 는 알림이 반드시 있어야 하고, 사용자는 이 알림으로 기록 중임을 알 수 있다.
 * 기록 중에는 경과 시간을 알림의 크로노미터가 스스로 세므로, 알림은 거리나 상태가 바뀔 때만 다시 그린다.
 */
internal class TrackingNotification(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    /** 마지막으로 그린 알림의 내용. 같은 내용이면 다시 그리지 않는다. */
    private var lastContent: Content? = null

    init {
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(context.getString(R.string.tracking_channel_name))
                .setDescription(context.getString(R.string.tracking_channel_description))
                .build(),
        )
    }

    /** 포그라운드 Service 를 시작할 때 쓸 알림. */
    fun build(state: TrackingState): Notification {
        val content = Content.of(state)
        lastContent = content
        return buildNotification(content)
    }

    /** 기록 상태가 바뀌면 알림을 갱신한다. 거리(10m 단위)나 일시정지 여부가 바뀌었을 때만 다시 그린다. */
    @SuppressLint("MissingPermission") // canPostNotifications() 로 확인한다.
    fun update(state: TrackingState) {
        val content = Content.of(state)
        if (content == lastContent) return
        lastContent = content
        if (canPostNotifications()) manager.notify(NOTIFICATION_ID, buildNotification(content))
    }

    private fun buildNotification(content: Content): Notification {
        val distanceKm = content.distanceTenMeters / 100.0
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_running)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            // Android 12 부터 포그라운드 Service 알림은 최대 10초 늦게 보일 수 있어, 기록 시작 즉시 보이도록 한다.
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openAppIntent())

        return if (content.isPaused) {
            builder
                .setContentTitle(context.getString(R.string.tracking_title_paused))
                .setContentText(
                    context.getString(R.string.tracking_distance_with_elapsed, distanceKm, content.pausedElapsed.toClockText()),
                )
                .setShowWhen(false)
                .build()
        } else {
            builder
                .setContentTitle(context.getString(R.string.tracking_title_running))
                .setContentText(context.getString(R.string.tracking_distance, distanceKm))
                // 크로노미터가 (지금 - 운동 시간) 부터 세게 하면 알림을 다시 그리지 않아도 경과 시간이 흐른다.
                .setUsesChronometer(true)
                .setWhen(System.currentTimeMillis() - content.runningElapsed.inWholeMilliseconds)
                .setShowWhen(true)
                .build()
        }
    }

    /** 알림을 누르면 앱을 연다. 이미 열려 있던 화면이 있으면 그 화면으로 돌아간다. */
    private fun openAppIntent(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * 알림에 보여 줄 내용. 같으면 다시 그리지 않도록 비교에 쓴다.
     *
     * 기록 중의 운동 시간은 크로노미터가 세므로 비교에서 뺀다(매초 바뀌기 때문). 일시정지 중에는 시간이 멈춰 있으므로 포함한다.
     */
    private data class Content(
        val isPaused: Boolean,
        val distanceTenMeters: Long,
        val pausedElapsed: Duration,
    ) {
        /** 기록 중일 때 크로노미터의 시작점을 정하는 데만 쓴다. */
        var runningElapsed: Duration = Duration.ZERO
            private set

        companion object {
            fun of(state: TrackingState): Content {
                val session = when (state) {
                    is TrackingState.Running -> state.session
                    is TrackingState.Paused -> state.session
                    TrackingState.Idle, is TrackingState.Finished -> null
                }
                val isPaused = state is TrackingState.Paused
                return Content(
                    isPaused = isPaused,
                    distanceTenMeters = ((session?.distanceMeters ?: 0.0) / 10).toLong(),
                    pausedElapsed = if (isPaused) session?.duration ?: Duration.ZERO else Duration.ZERO,
                ).apply { runningElapsed = session?.duration ?: Duration.ZERO }
            }
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "running_tracking"

        /** 운동 시간을 `12:34` 또는 `1:02:03` 형식으로 만든다. */
        private fun Duration.toClockText(): String = toComponents { hours, minutes, seconds, _ ->
            if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
        }
    }
}
