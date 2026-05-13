package com.chat.groupmanage.adapter;

import android.text.TextUtils;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.chat.base.ui.components.AvatarView;
import com.chat.base.utils.WKTimeUtils;
import com.chat.groupmanage.R;
import com.chat.groupmanage.entity.InviteApprovalEntity;
import com.xinbida.wukongim.entity.WKChannelType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 群邀请审批列表 adapter
 */
public class GroupApprovalListAdapter extends BaseQuickAdapter<InviteApprovalEntity, BaseViewHolder> {

    public GroupApprovalListAdapter(@Nullable List<InviteApprovalEntity> data) {
        super(R.layout.item_group_approval_layout, data);
    }

    @Override
    protected void convert(@NotNull BaseViewHolder helper, InviteApprovalEntity entity) {
        AvatarView avatarView = helper.getView(R.id.avatarView);
        avatarView.showAvatar(entity.inviter, WKChannelType.PERSONAL);

        helper.setText(R.id.inviterNameTv, entity.inviterName == null ? "" : entity.inviterName);
        helper.setText(R.id.timeTv, formatTime(entity.createdAt));

        int memberCount = entity.items == null ? 0 : entity.items.size();
        StringBuilder names = new StringBuilder();
        if (entity.items != null) {
            for (int i = 0; i < entity.items.size(); i++) {
                if (i > 0) names.append("、");
                names.append(entity.items.get(i).name == null ? "" : entity.items.get(i).name);
            }
        }
        String content = getContext().getString(R.string.group_approval_content_format, memberCount, names.toString());
        helper.setText(R.id.contentTv, content);

        if (!TextUtils.isEmpty(entity.remark)) {
            helper.setGone(R.id.remarkTv, false);
            helper.setText(R.id.remarkTv, "\"" + entity.remark + "\"");
        } else {
            helper.setGone(R.id.remarkTv, true);
        }

        helper.setText(R.id.statusTv, R.string.group_approval_status_wait);
    }

    private String formatTime(String createdAt) {
        if (TextUtils.isEmpty(createdAt)) return "";
        long seconds = WKTimeUtils.getInstance().date2TimeStamp(createdAt, "yyyy-MM-dd HH:mm:ss");
        if (seconds <= 0) return createdAt;
        return WKTimeUtils.getInstance().getShowDate(seconds);
    }
}
