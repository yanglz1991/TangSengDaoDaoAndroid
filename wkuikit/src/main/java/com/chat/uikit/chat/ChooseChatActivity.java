package com.chat.uikit.chat;

import android.content.Intent;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import androidx.core.content.ContextCompat;

import com.chat.base.base.WKBaseActivity;
import com.chat.base.config.WKConfig;
import com.chat.base.msgitem.WKChannelMemberRole;
import com.chat.base.utils.SoftKeyboardUtils;
import com.chat.base.utils.WKReader;
import com.chat.uikit.WKUIKitApplication;
import com.chat.uikit.R;
import com.chat.uikit.chat.adapter.ChooseChatAdapter;
import com.chat.uikit.contacts.ChooseContactsActivity;
import com.chat.uikit.databinding.ActChooseChatLayoutBinding;
import com.chat.uikit.group.GroupEntity;
import com.chat.uikit.group.service.GroupModel;
import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKChannel;
import com.xinbida.wukongim.entity.WKChannelMember;
import com.xinbida.wukongim.entity.WKChannelStatus;
import com.xinbida.wukongim.entity.WKChannelType;
import com.xinbida.wukongim.entity.WKUIConversationMsg;
import com.xinbida.wukongim.msgmodel.WKMessageContent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 2019-12-08 13:47
 * 选择会话页面
 */
public class ChooseChatActivity extends WKBaseActivity<ActChooseChatLayoutBinding> {
    private static final int TAB_RECENT = 0;
    private static final int TAB_GROUP = 1;
    private static final int TAB_FRIEND = 2;
    private static final int MAX_SELECT_COUNT = 9;

    ChooseChatAdapter chooseChatAdapter;
    Button rightBtn;
    private boolean isChoose;
    /**
     * 当前 Tab 实际渲染的列表(指向 recentList/groupList/friendList 之一)
     */
    List<ChooseChatEntity> allList;

    private final List<ChooseChatEntity> recentList = new ArrayList<>();
    private final List<ChooseChatEntity> groupList = new ArrayList<>();
    private final List<ChooseChatEntity> friendList = new ArrayList<>();

    /**
     * 跨 Tab 累积的已选频道(LinkedHashMap 保持选择顺序),key = channelType + ":" + channelID
     */
    private final Map<String, WKChannel> selectedChannels = new LinkedHashMap<>();

    private int currentTab = TAB_RECENT;
    private String currentKeyword = "";

    @Override
    protected ActChooseChatLayoutBinding getViewBinding() {
        return ActChooseChatLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.choose_chat);
    }

    @Override
    protected String getRightBtnText(Button titleRightBtn) {
        rightBtn = titleRightBtn;
        return getString(R.string.sure);
    }

    @Override
    protected void rightButtonClick() {
        super.rightButtonClick();

        if (selectedChannels.isEmpty()) return;

        ArrayList<WKChannel> list = new ArrayList<>(selectedChannels.values());
        if (isChoose) {
            if (WKUIKitApplication.getInstance().getMessageContentList() != null) {
                WKUIKitApplication.getInstance().showChatConfirmDialog(this, list, WKUIKitApplication.getInstance().getMessageContentList(), new WKUIKitApplication.IShowChatConfirm() {
                    @Override
                    public void onBack(@NonNull List<WKChannel> list, @NonNull List<WKMessageContent> messageContentList) {
                        WKUIKitApplication.getInstance().sendChooseChatBack(list);
                        finish();
                    }
                });
            } else {
                WKUIKitApplication.getInstance().sendChooseChatBack(list);
                finish();
            }
        } else {
            Intent intent = new Intent();
            intent.putParcelableArrayListExtra("list", (ArrayList<? extends Parcelable>) list);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    protected void initPresenter() {
        isChoose = getIntent().getBooleanExtra("isChoose", false);
    }

    @Override
    protected void initView() {
        chooseChatAdapter = new ChooseChatAdapter(new ArrayList<>());
        initAdapter(wkVBinding.recyclerView, chooseChatAdapter);
        chooseChatAdapter.addHeaderView(getHeader());
    }

    @Override
    protected void rightLayoutClick() {
        super.rightLayoutClick();
    }

    @Override
    protected void initListener() {
        chooseChatAdapter.setOnItemClickListener((adapter, view1, position) -> {
            ChooseChatEntity chooseChatEntity = (ChooseChatEntity) adapter.getItem(position);
            if (chooseChatEntity == null) return;
            if (chooseChatEntity.isBan || chooseChatEntity.isForbidden) return;
            WKChannel channel = chooseChatEntity.uiConveursationMsg.getWkChannel();
            if (channel == null) return;
            String key = entityKey(channel);
            if (selectedChannels.containsKey(key)) {
                selectedChannels.remove(key);
                chooseChatEntity.isCheck = false;
            } else {
                if (selectedChannels.size() >= MAX_SELECT_COUNT) {
                    showSingleBtnDialog(String.format(getString(R.string.max_select_count_chat), MAX_SELECT_COUNT));
                    return;
                }
                selectedChannels.put(key, channel);
                chooseChatEntity.isCheck = true;
            }
            adapter.notifyItemChanged(position + adapter.getHeaderLayoutCount(), chooseChatEntity);
            updateConfirmBtn();
        });

        wkVBinding.tabRecentLayout.setOnClickListener(v -> switchTab(TAB_RECENT));
        wkVBinding.tabGroupLayout.setOnClickListener(v -> switchTab(TAB_GROUP));
        wkVBinding.tabFriendLayout.setOnClickListener(v -> switchTab(TAB_FRIEND));

        wkVBinding.searchEt.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        wkVBinding.searchEt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                SoftKeyboardUtils.getInstance().hideSoftKeyboard(ChooseChatActivity.this);
                return true;
            }
            return false;
        });
        wkVBinding.searchEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                searchUser(editable.toString());
            }
        });
    }

    private void searchUser(String content) {
        currentKeyword = content == null ? "" : content;
        chooseChatAdapter.setList(filterList(allList, currentKeyword));
    }

    private List<ChooseChatEntity> filterList(List<ChooseChatEntity> source, String content) {
        if (source == null) return new ArrayList<>();
        if (TextUtils.isEmpty(content)) return new ArrayList<>(source);
        String lower = content.toLowerCase(Locale.getDefault());
        List<ChooseChatEntity> tempList = new ArrayList<>();
        for (int i = 0, size = source.size(); i < size; i++) {
            ChooseChatEntity e = source.get(i);
            WKChannel channel = e.uiConveursationMsg == null ? null : e.uiConveursationMsg.getWkChannel();
            if (channel == null) continue;
            String name = channel.channelName == null ? "" : channel.channelName.toLowerCase(Locale.getDefault());
            String remark = channel.channelRemark == null ? "" : channel.channelRemark.toLowerCase(Locale.getDefault());
            if (name.contains(lower) || remark.contains(lower)) {
                tempList.add(e);
            }
        }
        return tempList;
    }

    private String entityKey(WKChannel channel) {
        return channel.channelType + ":" + channel.channelID;
    }

    private void updateConfirmBtn() {
        int count = selectedChannels.size();
        if (count > 0) {
            rightBtn.setVisibility(View.VISIBLE);
            rightBtn.setText(String.format("%s(%s)", getString(R.string.sure), count));
        } else {
            rightBtn.setText(R.string.sure);
            rightBtn.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * 同步当前列表 isCheck 与 selectedChannels(用于切 Tab 后保持一致)
     */
    private void syncCheckStateForList(List<ChooseChatEntity> list) {
        if (list == null) return;
        for (ChooseChatEntity e : list) {
            WKChannel ch = e.uiConveursationMsg == null ? null : e.uiConveursationMsg.getWkChannel();
            if (ch == null) {
                e.isCheck = false;
                continue;
            }
            e.isCheck = selectedChannels.containsKey(entityKey(ch));
        }
    }

    private void switchTab(int tab) {
        if (currentTab == tab) return;
        currentTab = tab;
        switch (tab) {
            case TAB_RECENT:
                allList = recentList;
                break;
            case TAB_GROUP:
                allList = groupList;
                if (groupList.isEmpty()) {
                    loadGroups();
                }
                break;
            case TAB_FRIEND:
                allList = friendList;
                if (friendList.isEmpty()) {
                    loadFriends();
                }
                break;
        }
        applyTabUI();
        syncCheckStateForList(allList);
        chooseChatAdapter.setList(filterList(allList, currentKeyword));
    }

    private void applyTabUI() {
        int active = ContextCompat.getColor(this, R.color.colorAccent);
        int normal = ContextCompat.getColor(this, R.color.color999);
        int transparent = ContextCompat.getColor(this, R.color.transparent);

        wkVBinding.tabRecentTv.setTextColor(currentTab == TAB_RECENT ? active : normal);
        wkVBinding.tabRecentTv.setTypeface(null, currentTab == TAB_RECENT ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        wkVBinding.tabRecentIndicator.setBackgroundColor(currentTab == TAB_RECENT ? active : transparent);

        wkVBinding.tabGroupTv.setTextColor(currentTab == TAB_GROUP ? active : normal);
        wkVBinding.tabGroupTv.setTypeface(null, currentTab == TAB_GROUP ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        wkVBinding.tabGroupIndicator.setBackgroundColor(currentTab == TAB_GROUP ? active : transparent);

        wkVBinding.tabFriendTv.setTextColor(currentTab == TAB_FRIEND ? active : normal);
        wkVBinding.tabFriendTv.setTypeface(null, currentTab == TAB_FRIEND ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        wkVBinding.tabFriendIndicator.setBackgroundColor(currentTab == TAB_FRIEND ? active : transparent);
    }

    @Override
    protected void initData() {
        super.initData();
        loadRecent();
        // 默认 Tab = 最近聊天
        currentTab = TAB_RECENT;
        allList = recentList;
        applyTabUI();
        chooseChatAdapter.setList(allList);
        rightBtn.setVisibility(View.GONE);
    }

    private void loadRecent() {
        recentList.clear();
        List<WKUIConversationMsg> list = WKIM.getInstance().getConversationManager().getAll();
        if (list != null) {
            for (int i = 0, size = list.size(); i < size; i++) {
                ChooseChatEntity chooseChatEntity = new ChooseChatEntity(list.get(i));
                WKChannel channel = list.get(i).getWkChannel();
                if (channel != null) {
                    WKChannelMember mChannelMember = WKIM.getInstance().getChannelMembersManager().getMember(channel.channelID, channel.channelType, WKConfig.getInstance().getUid());
                    if (channel.forbidden == 1) {
                        // 禁言中
                        if (mChannelMember != null) {
                            chooseChatEntity.isForbidden = mChannelMember.role == WKChannelMemberRole.normal;
                        }
                    } else {
                        if (mChannelMember != null)
                            chooseChatEntity.isForbidden = mChannelMember.forbiddenExpirationTime > 0;
                        else chooseChatEntity.isForbidden = false;
                    }
                    chooseChatEntity.isBan = channel.status == WKChannelStatus.statusDisabled;
                }
                if (selectedChannels.containsKey(entityKey(channel != null ? channel : new WKChannel(list.get(i).channelID, list.get(i).channelType)))) {
                    chooseChatEntity.isCheck = true;
                }
                recentList.add(chooseChatEntity);
            }
        }
    }

    private void loadGroups() {
        // 后端 /group/my 仅返回 save=1 的群,不是"我加入的所有群"。
        // 因此先收集本地会话中的群(用户最近聊过的),再用接口补充已保存但无最近会话的群,channelID 去重。
        groupList.clear();
        Map<String, Boolean> seen = new HashMap<>();

        // 1. 本地会话里的群组
        List<WKUIConversationMsg> conversations = WKIM.getInstance().getConversationManager().getAll();
        if (conversations != null) {
            for (WKUIConversationMsg msg : conversations) {
                if (msg == null || msg.channelType != WKChannelType.GROUP) continue;
                if (TextUtils.isEmpty(msg.channelID)) continue;
                if (seen.containsKey(msg.channelID)) continue;
                WKChannel channel = msg.getWkChannel();
                if (channel == null) {
                    channel = WKIM.getInstance().getChannelManager().getChannel(msg.channelID, WKChannelType.GROUP);
                }
                if (channel == null) {
                    channel = new WKChannel(msg.channelID, WKChannelType.GROUP);
                    WKIM.getInstance().getChannelManager().fetchChannelInfo(msg.channelID, WKChannelType.GROUP);
                }
                seen.put(msg.channelID, true);
                groupList.add(buildEntityFromChannel(channel));
            }
        }

        // 2. 接口拉"已保存"的群,补充未在最近会话中的
        GroupModel.getInstance().getMyGroups((code, msg, list) -> {
            if (list != null) {
                for (GroupEntity g : list) {
                    if (g == null || TextUtils.isEmpty(g.group_no)) continue;
                    if (seen.containsKey(g.group_no)) continue;
                    WKChannel channel = WKIM.getInstance().getChannelManager().getChannel(g.group_no, WKChannelType.GROUP);
                    if (channel == null) {
                        channel = new WKChannel(g.group_no, WKChannelType.GROUP);
                        channel.channelName = TextUtils.isEmpty(g.name) ? g.group_no : g.name;
                        channel.channelRemark = g.remark == null ? "" : g.remark;
                        channel.avatar = g.avatar == null ? "" : g.avatar;
                        channel.forbidden = g.forbidden;
                        channel.status = g.status;
                    }
                    seen.put(g.group_no, true);
                    groupList.add(buildEntityFromChannel(channel));
                    WKIM.getInstance().getChannelManager().fetchChannelInfo(g.group_no, WKChannelType.GROUP);
                }
            }
            if (currentTab == TAB_GROUP) {
                syncCheckStateForList(groupList);
                chooseChatAdapter.setList(filterList(groupList, currentKeyword));
            }
        });

        // 接口未返回前先把本地会话部分渲染出来
        if (currentTab == TAB_GROUP) {
            syncCheckStateForList(groupList);
            chooseChatAdapter.setList(filterList(groupList, currentKeyword));
        }
    }

    private void loadFriends() {
        friendList.clear();
        List<WKChannel> channels = WKIM.getInstance().getChannelManager().getWithFollowAndStatus(WKChannelType.PERSONAL, 1, 1);
        if (channels != null) {
            for (WKChannel ch : channels) {
                if (ch == null || TextUtils.isEmpty(ch.channelID)) continue;
                friendList.add(buildEntityFromChannel(ch));
            }
        }
        if (currentTab == TAB_FRIEND) {
            syncCheckStateForList(friendList);
            chooseChatAdapter.setList(filterList(friendList, currentKeyword));
        }
    }

    /**
     * 由 WKChannel 构造 ChooseChatEntity(伪造一个 WKUIConversationMsg 壳子,以复用现有 adapter)
     */
    private ChooseChatEntity buildEntityFromChannel(WKChannel channel) {
        WKUIConversationMsg msg = new WKUIConversationMsg();
        msg.channelID = channel.channelID;
        msg.channelType = channel.channelType;
        msg.setWkChannel(channel);
        ChooseChatEntity entity = new ChooseChatEntity(msg);
        entity.isBan = channel.status == WKChannelStatus.statusDisabled;
        // 群禁言:保守起见对群组 tab 不按 forbidden 标记禁选(转发不应受群禁言限制)
        entity.isForbidden = false;
        entity.isCheck = selectedChannels.containsKey(entityKey(channel));
        return entity;
    }

    public static class ChooseChatEntity {
        ChooseChatEntity(WKUIConversationMsg uiConveursationMsg) {
            this.uiConveursationMsg = uiConveursationMsg;
        }

        public WKUIConversationMsg uiConveursationMsg;
        public boolean isCheck;
        // 禁言中
        public boolean isForbidden;
        // 禁用中
        public boolean isBan;
    }

    private View getHeader() {
        View view = LayoutInflater.from(this).inflate(R.layout.choose_chat_header_layout, wkVBinding.recyclerView, false);
        View headerView = view.findViewById(R.id.createTv);
        headerView.setOnClickListener(view1 -> {
            Intent intent = new Intent(this, ChooseContactsActivity.class);
            if (WKUIKitApplication.getInstance().getMessageContentList() != null)
                intent.putParcelableArrayListExtra("msgContentList", (ArrayList<? extends Parcelable>) WKUIKitApplication.getInstance().getMessageContentList());
            startActivity(intent);
        });
        return view;
    }
}
