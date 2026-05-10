package com.chat.base.entity;

public class WKAPPConfig {
    public int version;
    public String web_url;
    public int phone_search_off;
    public int shortno_edit_off;
    public int revoke_second;
    public int register_invite_on;
    public int send_welcome_message_on;
    public int invite_system_account_join_group_on;
    public int register_user_must_complete_info_on;
    public int can_modify_api_url;
    // 群聊禁言开关（含群发消息/建群/加群成员/加好友）
    public int disable_group_message_on;
    // 私聊禁言开关
    public int disable_private_message_on;
    // 群聊禁言客户端展示文案
    public String mute_text_of_group;
    // 私聊禁言客户端展示文案
    public String mute_text_of_private;
}
