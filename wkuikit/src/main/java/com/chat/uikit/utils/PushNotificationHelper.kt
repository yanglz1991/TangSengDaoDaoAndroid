package com.chat.uikit.utils

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.chat.base.WKBaseApplication
import com.chat.base.config.WKConstants
import com.chat.base.utils.NotificationCompatUtil
import com.chat.uikit.R
import com.chat.uikit.TabActivity

object PushNotificationHelper {

    /**
     * 通知渠道-聊天消息
     *
     * 关键点：
     *   - importance = HIGH：API 26+ 锁屏 / 解锁状态都以浮动通知（heads-up）形式弹出；
     *     API 25 及以下通过 NotificationCompat.Builder.setPriority(PRIORITY_HIGH) 生效。
     *   - lockScreenVisibility = PUBLIC：锁屏完整显示标题/正文，避免"只有声音没有顶部信息"。
     *
     * 注意：渠道行为属性（importance/lockscreen）一旦在系统首次创建后即被锁定，App 无法再改。
     * 渠道的真正首次创建发生在 TSApplication.createNotificationChannel()，这里的字段主要用于
     * 兼容低版本 (API < 26) 的 setPriority / setVisibility 走 Builder 路径生效。
     */
    private val MESSAGE = NotificationCompatUtil.Channel(
        channelId = WKConstants.newMsgChannelID,
        name = WKBaseApplication.getInstance().context.getString(R.string.new_msg_notification),
        importance = NotificationManager.IMPORTANCE_HIGH,
        lockScreenVisibility = NotificationCompat.VISIBILITY_PUBLIC,
        sound = Uri.parse("android.resource://" + WKBaseApplication.getInstance().context.packageName + "/" + R.raw.newmsg)
    )

    /** 通知渠道-@提醒消息(重要性级别-紧急：发出提示音，并以浮动通知的形式显示 & 锁屏显示 & 振动0.25s )*/
    private val MENTION = NotificationCompatUtil.Channel(
        channelId = WKConstants.newMsgChannelID,
        name = WKBaseApplication.getInstance().context.getString(R.string.new_msg_notification),
        importance = NotificationManager.IMPORTANCE_HIGH,
        lockScreenVisibility = NotificationCompat.VISIBILITY_PUBLIC,
        vibrate = longArrayOf(0, 250),
        sound = Uri.parse("android.resource://" + WKBaseApplication.getInstance().context.packageName + "/" + R.raw.newmsg)
    )

    /** 通知渠道-系统通知(重要性级别-中：无提示音) */
    private val NOTICE = NotificationCompatUtil.Channel(
        channelId = WKConstants.newMsgChannelID,
        name = WKBaseApplication.getInstance().context.getString(R.string.new_msg_notification),
        importance = NotificationManager.IMPORTANCE_LOW
    )

    /** 通知渠道-音视频通话(重要性级别-紧急：发出提示音，并以浮动通知的形式显示 & 锁屏显示 & 振动4s停2s再振动4s ) */
    private val CALL = NotificationCompatUtil.Channel(
        channelId = WKConstants.newRTCChannelID,
        name = WKBaseApplication.getInstance().context.getString(R.string.new_rtc_notification),
        importance = NotificationManager.IMPORTANCE_HIGH,
        lockScreenVisibility = NotificationCompat.VISIBILITY_PUBLIC,
        vibrate = longArrayOf(0, 4000, 2000, 4000),
        sound = Uri.parse("android.resource://" + WKBaseApplication.getInstance().context.packageName + "/" + R.raw.newrtc)
    )

    /**
     * 显示聊天消息
     * @param context 上下文
     * @param id      通知的唯一ID
     * @param title   标题
     * @param text    正文文本
     */
    fun notifyMessage(
        context: Context,
        id: Int,
        title: String?,
        text: String?
    ) {
        val intent = Intent(context, TabActivity::class.java)

        val builder = NotificationCompatUtil.createNotificationBuilder(
            context,
            MESSAGE,
            title,
            text,
            intent
        )

        // 默认情况下，通知的文字内容会被截断以放在一行。如果您想要更长的通知，可以使用 setStyle() 添加样式模板来启用可展开的通知。
        builder.setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(text)
        )
        // 标记为消息类，部分 OEM（MIUI/HyperOS、ColorOS 等）会因此放宽悬浮通知策略
        builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)

        NotificationCompatUtil.notify(context, id, buildDefaultConfig(builder));
    }

    /**
     * 显示@提醒消息
     * @param context 上下文
     * @param id      通知的唯一ID
     * @param title   标题
     * @param text    正文文本
     */
    fun notifyMention(
        context: Context,
        id: Int,
        title: String?,
        text: String?
    ) {
        val intent = Intent(context, TabActivity::class.java)

        val builder = NotificationCompatUtil.createNotificationBuilder(
            context,
            MENTION,
            title,
            text,
            intent
        )

        // 默认情况下，通知的文字内容会被截断以放在一行。如果您想要更长的通知，可以使用 setStyle() 添加样式模板来启用可展开的通知。
        builder.setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(text)
        )
        // @ 提醒同样按消息类处理
        builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)

        NotificationCompatUtil.notify(context, id, buildDefaultConfig(builder));
    }

    /**
     * 显示系统通知
     * @param context 上下文
     * @param id      通知的唯一ID
     * @param title   标题
     * @param text    正文文本
     */
    fun notifyNotice(
        context: Context,
        id: Int,
        title: String?,
        text: String?
    ) {
        val intent = Intent(context, TabActivity::class.java)
        val builder = NotificationCompatUtil.createNotificationBuilder(
            context,
            NOTICE,
            title,
            text,
            intent
        )

        NotificationCompatUtil.notify(context, id, buildDefaultConfig(builder));
    }

    /**
     * 显示音视频通话
     * @param context 上下文
     * @param id      通知的唯一ID
     * @param title   标题
     * @param text    正文文本
     */
    fun notifyCall(
        context: Context,
        id: Int,
        title: String?,
        text: String?
    ) {
        val intent = Intent(context, TabActivity::class.java)
        val builder = NotificationCompatUtil.createNotificationBuilder(
            context,
            CALL,
            title,
            text,
            intent
        )
        // 通话类通知用 CATEGORY_CALL，让系统识别为来电级别
        builder.setCategory(NotificationCompat.CATEGORY_CALL)
        NotificationCompatUtil.notify(context, id, buildDefaultConfig(builder));
    }

    /**
     * 构建应用通知的默认配置
     * @param builder 构建器
     */
    private fun buildDefaultConfig(builder: NotificationCompat.Builder): Notification {
        builder.setSmallIcon(R.mipmap.ic_logo)
        builder.color = 0xFF007BF9.toInt()
        return builder.build()
    }
}