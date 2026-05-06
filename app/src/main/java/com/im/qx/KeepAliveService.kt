package com.im.qx

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.chat.uikit.R as UIKitR
import com.chat.uikit.TabActivity

/**
 * IM 长连接保活前台服务。
 *
 * 在未配置任何厂商推送（FCM/小米/华为/OPPO/vivo）时，应用必须依赖 WuKongIM 的
 * TCP 长连接接收消息。Android 8+ 后台进程随时可能被系统/OEM 清理，导致长连接断
 * 开、无法收到消息通知。本服务通过常驻前台通知保持进程优先级，大幅降低被杀概率。
 *
 * 注意：即使挂了前台服务，小米/华为/OPPO/vivo 等机型在以下场景仍可能强杀：
 * 1. 用户在「最近任务」里上划清理
 * 2. 系统「一键清理」/ 低内存场景
 * 3. 未把应用加入「自启动」「后台活动」「电池优化白名单」
 * 因此仍需引导用户在系统设置里对本 App 进行保活设置。
 */
class KeepAliveService : Service() {

    companion object {
        private const val CHANNEL_ID = "ts_keep_alive"
        private const val CHANNEL_NAME = "后台运行"
        private const val NOTIFICATION_ID = 10086

        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Throwable) {
                // 在 Android 12+ 的某些后台受限场景可能抛 ForegroundServiceStartNotAllowedException，
                // 此时静默忽略，等下次回到前台再启动。
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, KeepAliveService::class.java))
            } catch (_: Throwable) {
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        // 进程被杀后尝试自动重启（非 100% 可靠，配合系统保活设置使用）
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // 用户从「最近任务」划掉 App 时也尽量保持服务存活
        val restartIntent = Intent(applicationContext, KeepAliveService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                applicationContext.startForegroundService(restartIntent)
            } else {
                applicationContext.startService(restartIntent)
            }
        } catch (_: Throwable) {
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun buildNotification(): Notification {
        ensureChannel()
        val appName = try {
            applicationContext.getString(applicationContext.applicationInfo.labelRes)
        } catch (_: Throwable) {
            "IM"
        }

        val clickIntent = Intent(this, TabActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            clickIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(UIKitR.mipmap.ic_logo)
            .setContentTitle("$appName 正在后台运行")
            .setContentText("用于持续接收新消息通知")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            description = "用于持续接收新消息通知，关闭后将无法在后台及时收到消息"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
        }
        manager.createNotificationChannel(channel)
    }
}
