package com.im.qx

import android.app.Activity
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.text.TextUtils
import androidx.multidex.MultiDexApplication
import com.chat.base.WKBaseApplication
import com.chat.base.common.WKCommonModel
import com.chat.base.config.WKApiConfig
import com.chat.base.config.WKConfig
import com.chat.base.config.WKConstants
import com.chat.base.config.WKSharedPreferencesUtil
import com.chat.base.endpoint.EndpointCategory
import com.chat.base.endpoint.EndpointManager
import com.chat.base.endpoint.EndpointSID
import com.chat.base.endpoint.entity.LoginMenu
import com.chat.base.endpoint.entity.ReadMsgMenu
import com.chat.base.endpoint.entity.WKSendMsgMenu
import com.xinbida.wukongim.entity.WKChannelType
import com.chat.base.ui.Theme
import com.chat.base.utils.ActManagerUtils
import com.chat.base.utils.WKPlaySound
import com.chat.base.utils.WKTimeUtils
import com.chat.base.utils.language.WKMultiLanguageUtil
import com.chat.login.WKLoginApplication
import com.chat.push.WKPushApplication
import com.chat.scan.WKScanApplication
import com.chat.uikit.TabActivity
import com.chat.uikit.WKUIKitApplication
import com.chat.uikit.chat.manager.WKIMUtils
import com.chat.uikit.user.service.UserModel
import com.chat.groupmanage.WKGroupManageApplication
import com.chat.video.WKVideoApplication
import com.chat.file.WKFileApplication
import com.chat.keepalive.WKKeepAliveApplication
import com.im.qx.R
import kotlin.system.exitProcess

class TSApplication : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()

        val processName = getProcessName(this, Process.myPid())
        if (processName != null) {
            val defaultProcess = processName == getAppPackageName()
            if (defaultProcess) {
                initAll()
            }
        }
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(p0: Activity, p1: Bundle?) {
            }

            override fun onActivityStarted(p0: Activity) {
            }

            override fun onActivityResumed(p0: Activity) {
                ActManagerUtils.getInstance().currentActivity = p0
            }

            override fun onActivityPaused(p0: Activity) {
            }

            override fun onActivityStopped(p0: Activity) {
            }

            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
            }

            override fun onActivityDestroyed(p0: Activity) {
            }
        })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (applicationContext != null && applicationContext.resources != null && applicationContext.resources.configuration != null && applicationContext.resources.configuration.uiMode != newConfig.uiMode) {
            WKMultiLanguageUtil.getInstance().setConfiguration()
            Theme.applyTheme()
            killAppProcess()
        }
    }

    private fun killAppProcess() {
        ActManagerUtils.getInstance().clearAllActivity()
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(WKMultiLanguageUtil.getInstance().attachBaseContext(base))
    }

    private fun initAll() {

        WKMultiLanguageUtil.getInstance().init(this)
        WKBaseApplication.getInstance().init(getAppPackageName(), this)
        Theme.applyTheme()
        initApi()
        WKLoginApplication.getInstance().init(this)
        WKScanApplication.getInstance().init(this)
        WKUIKitApplication.getInstance().init(this)
        WKPushApplication.getInstance().init(getAppPackageName(), this)
        
        WKGroupManageApplication.getInstance().init();//群管模块
        WKVideoApplication.getInstance().init(this);//视频模块
        WKFileApplication.getInstance().init(this);//文件模块
        WKKeepAliveApplication.instance.init();//后台运行保护引导模块（注册 show_keep_alive_item endpoint）

        addAppFrontBack()
        addListener()
        // 若已登录，立即启动 IM 长连接保活服务（冷启动恢复登录态时生效）
        if (!TextUtils.isEmpty(WKConfig.getInstance().token)) {
            KeepAliveService.start(this)
            // 冷启动时兜底检查一次封禁状态（防止客户端离线期间收到的 forceLogout CMD 丢失）
            // 延迟 2s 等待 IM 长连建立 / Activity 创建，避免在 LaunchActivity 空窗期弹窗
            Handler(Looper.getMainLooper()).postDelayed({
                WKUIKitApplication.getInstance().checkBanStatusAndHandle()
            }, 2000)
        }
    }

    private fun initApi() {
        var apiURL = WKSharedPreferencesUtil.getInstance().getSP("api_base_url")
        if (TextUtils.isEmpty(apiURL)) {
            apiURL = "https://qx.qhfhasina.com/api"
            WKApiConfig.initBaseURL(apiURL)
        } else {
            WKApiConfig.initBaseURLIncludeIP(apiURL)
        }
    }

    private fun getAppPackageName(): String {
        return packageName  // 动态获取实际的 applicationId
    }

    private fun getProcessName(cxt: Context, pid: Int): String? {
        val am = cxt.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val runningApps = am.runningAppProcesses ?: return null
        for (app in runningApps) {
            if (app.pid == pid) {
                return app.processName
            }
        }
        return null
    }

    private fun addAppFrontBack() {
        val helper = AppFrontBackHelper()
        helper.register(this, object : AppFrontBackHelper.OnAppStatusListener {
            override fun onFront() {
                if (!TextUtils.isEmpty(WKConfig.getInstance().token)) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        EndpointManager.getInstance()
                            .invoke("chow_check_lock_screen_pwd", null)
                    }, 1000)
                    // initIMListener 内部按 key="system" 覆盖注册，重复调用安全
                    WKIMUtils.getInstance().initIMListener()
                    WKUIKitApplication.getInstance().startChat()
                    UserModel.getInstance().getOnlineUsers()
                    // 已登录状态下确保保活服务处于运行（幂等）
                    KeepAliveService.start(this@TSApplication)
                    // 从后台回到前台也兜底检查一次封禁状态
                    WKUIKitApplication.getInstance().checkBanStatusAndHandle()
                }
            }

            override fun onBack() {
                // 未配置厂商推送（FCM/小米/华为/OPPO/vivo）时，后台消息只能依赖长连接，
                // 因此这里不再主动断开 IM，也不再移除新消息监听器，否则 showNotification
                // 在后台永远不会被触发，用户将收不到任何新消息提示。
                WKSharedPreferencesUtil.getInstance()
                    .putLong("lock_start_time", WKTimeUtils.getInstance().currentSeconds)
            }
        })
    }

    private fun addListener() {
        createNotificationChannel()
        // 登录成功后启动 IM 长连接保活服务
        EndpointManager.getInstance().setMethod(
            "ts_keep_alive_on_login",
            EndpointCategory.loginMenus
        ) { LoginMenu { KeepAliveService.start(this@TSApplication) } }

        // 已读回执相关：私聊默认开启已读，群聊不开启
        // 发送消息时按频道类型覆盖 setting.receipt，避免依赖服务端 channel.receipt 设置
        // 注意 WKSendMsgUtils 通过 invokes(category) 触发，必须把 category 设置为 EndpointSID.sendMessage
        EndpointManager.getInstance().setMethod(
            "ts_send_message_receipt", EndpointSID.sendMessage
        ) { obj ->
            if (obj is WKSendMsgMenu) {
                obj.option.setting.receipt =
                    if (obj.channel.channelType == WKChannelType.PERSONAL) 1 else 0
            }
            null
        }
        // 上报"消息已读"，对应付费模块中的 message/readed 接口
        EndpointManager.getInstance().setMethod("read_msg") { obj ->
            if (obj is ReadMsgMenu && obj.channelType == WKChannelType.PERSONAL) {
                WKCommonModel.getInstance().readMsg(obj.channelID, obj.channelType, obj.msgIds)
            }
            null
        }

        EndpointManager.getInstance().setMethod("main_show_home_view") { `object` ->
            // exitLogin() 会调用 main_show_home_view 跳回登录页，在此统一停止保活服务
            KeepAliveService.stop(this@TSApplication)
            if (`object` != null) {
                val from = `object` as Int
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("from", from)
                startActivity(intent)
            }
            null
        }
        EndpointManager.getInstance().setMethod("show_tab_home") {
            val intent = Intent(applicationContext, TabActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            null
        }

        EndpointManager.getInstance().setMethod("play_new_msg_Media") {
            WKPlaySound.getInstance().playRecordMsg(R.raw.newmsg)
            null
        }
    }


    private fun createNotificationChannel() {
        // Android 8.0+ 才有 NotificationChannel 概念
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = applicationContext.getSystemService(
                NotificationManager::class.java
            )
            // 清理历史渠道：老渠道使用 IMPORTANCE_DEFAULT 且未设置 lockscreenVisibility，
            // 锁屏没有横幅。Android 不允许 in-place 升级渠道行为，因此删除旧 id 让用户
            // 设置页不残留无用项，新通知统一走 v2 channelId。
            runCatching {
                notificationManager.deleteNotificationChannel(WKConstants.legacyNewMsgChannelID)
                notificationManager.deleteNotificationChannel(WKConstants.legacyNewRTCChannelID)
            }

            val name: CharSequence = applicationContext.getString(R.string.new_msg_notification)
            val description = applicationContext.getString(R.string.new_msg_notification_desc)
            // IMPORTANCE_HIGH：锁屏 / 解锁状态都会以浮动通知（heads-up）形式弹出
            val channel = NotificationChannel(
                WKConstants.newMsgChannelID,
                name,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = description
            channel.enableVibration(true) // 是否有震动
            // 锁屏完整显示标题/正文，解决"锁屏只有声音没有顶部信息"的问题
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channel.setSound(
                Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.newmsg),
                Notification.AUDIO_ATTRIBUTES_DEFAULT
            )
            // 渠道一旦创建后无法再修改 importance / lockscreenVisibility 等行为属性
            notificationManager.createNotificationChannel(channel)
        }
        createNotificationRTCChannel()
    }

    private fun createNotificationRTCChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = applicationContext.getString(R.string.new_rtc_notification)
            val description = applicationContext.getString(R.string.new_rtc_notification_desc)
            // 通话需要锁屏弹出，必须 IMPORTANCE_HIGH + VISIBILITY_PUBLIC
            val channel = NotificationChannel(
                WKConstants.newRTCChannelID,
                name,
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = description
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(0, 100, 100, 100, 100, 100)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channel.setSound(
                Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.newrtc),
                Notification.AUDIO_ATTRIBUTES_DEFAULT
            )
            val notificationManager = applicationContext.getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

}