package com.chat.base.entity;

/**
 * /v1/user/checkstatus 返回体
 *
 *  - banned:     是否被封禁；true 时客户端应提示原因并退出登录
 *  - match_type: 命中的维度（user / ip / device），仅用于日志或定制文案
 *  - reason:     服务端生成的面向用户的提示文案
 */
public class CheckStatusResult {
    public boolean banned;
    public String match_type;
    public String reason;
}
