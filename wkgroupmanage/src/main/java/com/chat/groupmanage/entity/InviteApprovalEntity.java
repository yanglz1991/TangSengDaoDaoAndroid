package com.chat.groupmanage.entity;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

/**
 * 群邀请审批记录
 */
public class InviteApprovalEntity {
    @JSONField(name = "invite_no")
    public String inviteNo;
    @JSONField(name = "group_no")
    public String groupNo;
    public String inviter;
    @JSONField(name = "inviter_name")
    public String inviterName;
    public String remark;
    /**
     * 0.待审核 1.已通过 2.已拒绝
     */
    public int status;
    @JSONField(name = "created_at")
    public String createdAt;
    public List<Item> items;

    public static class Item {
        public String uid;
        public String name;
    }
}
