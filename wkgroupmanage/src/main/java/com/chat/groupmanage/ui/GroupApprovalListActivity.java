package com.chat.groupmanage.ui;

import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.chat.base.act.WKWebViewActivity;
import com.chat.base.base.WKBaseActivity;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.utils.WKToastUtils;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.groupmanage.R;
import com.chat.groupmanage.adapter.GroupApprovalListAdapter;
import com.chat.groupmanage.databinding.ActGroupApprovalListLayoutBinding;
import com.chat.groupmanage.entity.InviteApprovalEntity;
import com.chat.groupmanage.service.GroupManageModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 群邀请审批记录
 */
public class GroupApprovalListActivity extends WKBaseActivity<ActGroupApprovalListLayoutBinding> {
    private String groupId;
    private GroupApprovalListAdapter adapter;

    @Override
    protected ActGroupApprovalListLayoutBinding getViewBinding() {
        return ActGroupApprovalListLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.group_approval_records);
    }

    @Override
    protected void initPresenter() {

    }

    @Override
    protected void initView() {
        wkVBinding.refreshLayout.setEnableLoadMore(false);
        adapter = new GroupApprovalListAdapter(new ArrayList<>());
        initAdapter(wkVBinding.recyclerView, adapter);
    }

    @Override
    protected void initListener() {
        wkVBinding.refreshLayout.setOnRefreshListener(refreshLayout -> loadData());

        adapter.setOnItemClickListener((a, view, position) -> SingleClickUtil.determineTriggerSingleClick(view, v -> {
            InviteApprovalEntity entity = (InviteApprovalEntity) a.getItem(position);
            if (entity == null || TextUtils.isEmpty(entity.inviteNo)) return;
            GroupManageModel.getInstance().getH5confirmUrl(groupId, entity.inviteNo, (code, msg) -> {
                if (code == HttpResponseCode.success && !TextUtils.isEmpty(msg)) {
                    Intent intent = new Intent(GroupApprovalListActivity.this, WKWebViewActivity.class);
                    intent.putExtra("url", msg);
                    startActivity(intent);
                } else if (!TextUtils.isEmpty(msg)) {
                    WKToastUtils.getInstance().showToastNormal(msg);
                }
            });
        }));
    }

    @Override
    protected void initData() {
        super.initData();
        groupId = getIntent().getStringExtra("groupId");
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从详情页（H5 审核结果）回来后刷新列表
        if (!TextUtils.isEmpty(groupId)) {
            loadData();
        }
    }

    private void loadData() {
        if (TextUtils.isEmpty(groupId)) {
            wkVBinding.refreshLayout.finishRefresh();
            return;
        }
        GroupManageModel.getInstance().getPendingInvites(groupId, (code, msg, list) -> {
            wkVBinding.refreshLayout.finishRefresh();
            if (code == HttpResponseCode.success) {
                List<InviteApprovalEntity> data = list == null ? new ArrayList<>() : list;
                adapter.setList(data);
                wkVBinding.emptyTv.setVisibility(data.isEmpty() ? View.VISIBLE : View.GONE);
            } else if (!TextUtils.isEmpty(msg)) {
                WKToastUtils.getInstance().showToastNormal(msg);
            }
        });
    }
}
