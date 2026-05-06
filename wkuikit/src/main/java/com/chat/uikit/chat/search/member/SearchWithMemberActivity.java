package com.chat.uikit.chat.search.member;

import android.text.TextUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.EndpointSID;
import com.chat.base.endpoint.entity.ChatViewMenu;
import com.chat.base.entity.GlobalMessage;
import com.chat.base.msgitem.WKContentType;
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
    private static final int FETCH_BATCH = 60;

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
        // 已改为基于本地 SDK 数据库分页查询，不再调用服务端 /v1/search/global，
        // 避免 WuKongIM 搜索插件未启用时报「查询悟空IM消息错误」。
        // 单批拉取 FETCH_BATCH 条文本/文件消息，按 fromUID 客户端过滤后填充结果列表。
        final boolean isFirstPage = oldestOrderSeq == 0L;
        final int[] types = new int[]{WKContentType.WK_TEXT, WKContentType.WK_FILE};
        final List<GlobalMessage> result = new ArrayList<>();
        long cursor = oldestOrderSeq;
        boolean reachedEnd = false;
        // safety bound: 最多翻 20 次，避免极端场景死循环
        for (int safety = 0; safety < 20 && result.size() < PAGE_SIZE; safety++) {
            List<WKMsg> batch = WKIM.getInstance().getMsgManager()
                    .searchMsgWithChannelAndContentTypes(channelID, WKChannelType.GROUP, cursor, FETCH_BATCH, types);
            if (batch == null || batch.isEmpty()) {
                reachedEnd = true;
                break;
            }
            for (WKMsg m : batch) {
                if (m == null) continue;
                cursor = m.orderSeq;
                if (fromUID != null && fromUID.equals(m.fromUID)) {
                    result.add(GlobalMessage.fromWKMsg(m));
                    if (result.size() >= PAGE_SIZE) break;
                }
            }
            if (batch.size() < FETCH_BATCH) {
                reachedEnd = true;
                break;
            }
        }
        oldestOrderSeq = cursor;

        wkVBinding.refreshLayout.finishLoadMore();
        wkVBinding.refreshLayout.finishRefresh();
        if (result.isEmpty()) {
            if (reachedEnd) {
                wkVBinding.refreshLayout.setEnableLoadMore(false);
            }
            if (isFirstPage) {
                adapter.setList(new ArrayList<>());
            }
            return;
        }
        if (isFirstPage) {
            adapter.setList(result);
        } else {
            adapter.addData(result);
        }
        if (reachedEnd) {
            wkVBinding.refreshLayout.setEnableLoadMore(false);
        }
    }
}
