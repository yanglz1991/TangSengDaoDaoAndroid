package com.chat.base.utils;

import android.text.TextUtils;

import com.chat.base.config.WKConfig;
import com.chat.base.entity.WKAPPConfig;
import com.xinbida.wukongim.entity.WKChannelType;

/**
 * 全局禁言（管理后台「禁言设置」）工具类。
 * - 群聊禁言：disable_group_message_on=1 时禁止 群发消息 / 建群 / 加群成员 / 加好友
 * - 私聊禁言：disable_private_message_on=1 时禁止 私聊发消息
 * Web 端不限制；该工具仅供 Android 客户端用。
 */
public class MuteUtils {

    private static final String DEFAULT_GROUP_MUTE_TIP = "群聊禁言中";
    private static final String DEFAULT_PRIVATE_MUTE_TIP = "私聊禁言中";

    /**
     * 是否开启了群聊相关禁言
     */
    public static boolean isGroupMuted() {
        WKAPPConfig cfg = WKConfig.getInstance().getAppConfig();
        return cfg != null && cfg.disable_group_message_on == 1;
    }

    /**
     * 是否开启了私聊禁言
     */
    public static boolean isPrivateMuted() {
        WKAPPConfig cfg = WKConfig.getInstance().getAppConfig();
        return cfg != null && cfg.disable_private_message_on == 1;
    }

    /**
     * 群聊禁言提示文案（管理后台可配置，未配置则使用默认）
     */
    public static String groupMuteTip() {
        WKAPPConfig cfg = WKConfig.getInstance().getAppConfig();
        if (cfg != null && !TextUtils.isEmpty(cfg.mute_text_of_group)) {
            return cfg.mute_text_of_group;
        }
        return DEFAULT_GROUP_MUTE_TIP;
    }

    /**
     * 私聊禁言提示文案
     */
    public static String privateMuteTip() {
        WKAPPConfig cfg = WKConfig.getInstance().getAppConfig();
        if (cfg != null && !TextUtils.isEmpty(cfg.mute_text_of_private)) {
            return cfg.mute_text_of_private;
        }
        return DEFAULT_PRIVATE_MUTE_TIP;
    }

    /**
     * 当前 channel 是否处于全局禁言中
     *
     * @param channelType 频道类型 {@link WKChannelType#PERSONAL} 或 {@link WKChannelType#GROUP} 等
     */
    public static boolean isChannelMuted(byte channelType) {
        if (channelType == WKChannelType.GROUP) {
            return isGroupMuted();
        }
        if (channelType == WKChannelType.PERSONAL || channelType == WKChannelType.CUSTOMER_SERVICE) {
            return isPrivateMuted();
        }
        return false;
    }

    /**
     * 获取当前 channel 对应的禁言提示文案；若未禁言返回 null
     */
    public static String channelMuteTip(byte channelType) {
        if (channelType == WKChannelType.GROUP && isGroupMuted()) {
            return groupMuteTip();
        }
        if ((channelType == WKChannelType.PERSONAL || channelType == WKChannelType.CUSTOMER_SERVICE) && isPrivateMuted()) {
            return privateMuteTip();
        }
        return null;
    }

    /**
     * 若处于禁言中：toast 提示并返回 true，调用方可据此 return 直接拦截动作
     */
    public static boolean blockIfChannelMuted(byte channelType) {
        String tip = channelMuteTip(channelType);
        if (!TextUtils.isEmpty(tip)) {
            WKToastUtils.getInstance().showToast(tip);
            return true;
        }
        return false;
    }

    /**
     * 若开启了群聊相关禁言：toast 提示并返回 true（用于建群、加群成员、加好友等动作）
     */
    public static boolean blockIfGroupMuted() {
        if (isGroupMuted()) {
            WKToastUtils.getInstance().showToast(groupMuteTip());
            return true;
        }
        return false;
    }
}
