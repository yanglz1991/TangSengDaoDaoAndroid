package com.chat.uikit.chat.search.member;

import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.EndpointSID;
import com.chat.base.endpoint.entity.ChatViewMenu;
import com.chat.base.entity.GlobalMessage;
import com.chat.uikit.R;
import com.chat.uikit.databinding.ActCommonRefreshListLayoutBinding;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;
import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKChannel;
import com.xinbida.wukongim.entity.WKChannelMember;
import com.xinbida.wukongim.entity.WKChannelType;
import com.xinbida.wukongim.entity.WKMsg;

import java.util.ArrayList;
import java.util.List;


public class SearchWithMemberActivity extends WKBaseActivity<ActCommonRefreshListLayoutBinding> {
    SearchWithMemberAdapter adapter;
    private String channelID;
    private String fromUID;
    // 本地 SDK 分页游标：从最新开始（0）逐页向后翻
    private long oldestOrderSeq = 0;
    private static final int PAGE_SIZE = 20;

    @Override
    protected ActCommonRefreshListLayoutBinding getViewBinding() {
        return ActCommonRefreshListLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.uikit_search_with_member);
    }

    @Override
    protected void initPresenter() {
        channelID = getIntent().getStringExtra("channelID");
        fromUID = getIntent().getStringExtra("fromUID");
    }

    @Override
    protected void initView() {
        String name = "";
        String avatarKey = "";
        WKChannelMember member = WKIM.getInstance().getChannelMembersManager().getMember(channelID, WKChannelType.GROUP, fromUID);
        if (member != null) {
            name = TextUtils.isEmpty(member.memberRemark) ? member.memberName : member.memberRemark;
        }
        WKChannel channel = WKIM.getInstance().getChannelManager().getChannel(fromUID, WKChannelType.PERSONAL);
        if (channel != null) {
            if (!TextUtils.isEmpty(channel.channelRemark))
                name = channel.channelRemark;
            avatarKey = channel.avatarCacheKey;
        }
        adapter = new SearchWithMemberAdapter(name, avatarKey);
        initAdapter(wkVBinding.recyclerView, adapter);
    }

    @Override
    protected void initListener() {
        wkVBinding.refreshLayout.setEnableRefresh(false);
        wkVBinding.refreshLayout.setOnRefreshLoadMoreListener(new OnRefreshLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                getData();
            }

            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {

            }
        });
        adapter.setOnItemClickListener((adapter, view, position) -> {
            GlobalMessage msg = (GlobalMessage) adapter.getData().get(position);
            if (msg != null) {
                long orderSeq = WKIM.getInstance().getMsgManager().getMessageOrderSeq(
                        msg.getMessage_seq(),
                        msg.getChannel().getChannel_id(),
                        msg.getChannel().getChannel_type()
                );
                EndpointManager.getInstance().invoke(EndpointSID.chatView, new ChatViewMenu(SearchWithMemberActivity.this, channelID, WKChannelType.GROUP, orderSeq, false));
            }
        });
        getData();
    }

    private void getData() {
        // 基于本地 SDK 按发送者分页查询，不再依赖服务端 /v1/search/global（搜索插件可能未启用）。
        // 直接使用 SDK 的 getWithFromUID 接口，覆盖该成员发送的所有消息类型
        // （文本/图片/语音/视频/文件/位置/卡片/表情等），不限 contentType。
        final boolean isFirstPage = oldestOrderSeq == 0L;
        List<WKMsg> list = WKIM.getInstance().getMsgManager()
                .getWithFromUID(channelID, WKChannelType.GROUP, fromUID, oldestOrderSeq, PAGE_SIZE);

        wkVBinding.refreshLayout.finishLoadMore();
        wkVBinding.refreshLayout.finishRefresh();

        if (list == null || list.isEmpty()) {
            wkVBinding.refreshLayout.setEnableLoadMore(false);
            if (isFirstPage) {
                adapter.setList(new ArrayList<>());
            }
            return;
        }

        List<GlobalMessage> result = new ArrayList<>(list.size());
        for (WKMsg m : list) {
            if (m == null) continue;
            result.add(GlobalMessage.fromWKMsg(m));
            // SDK 返回顺序为按 orderSeq 倒序（最新在前），用最后一条更新游标
            oldestOrderSeq = m.orderSeq;
        }

        if (isFirstPage) {
            adapter.setList(result);
        } else {
            adapter.addData(result);
        }
        if (list.size() < PAGE_SIZE) {
            wkVBinding.refreshLayout.setEnableLoadMore(false);
        }
    }
}
