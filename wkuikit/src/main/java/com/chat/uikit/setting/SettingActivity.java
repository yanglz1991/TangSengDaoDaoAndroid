package com.chat.uikit.setting;

import android.app.AlertDialog;
import android.content.Intent;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.chat.uikit.setting.securechannel.SecureChannelModel;

import com.chat.base.act.WKWebViewActivity;
import com.chat.base.base.WKBaseActivity;
import com.chat.base.common.WKCommonModel;
import com.chat.base.config.WKApiConfig;
import com.chat.base.endpoint.EndpointManager;
import com.chat.base.endpoint.entity.ChatBgItemMenu;
import com.chat.base.ui.Theme;
import com.chat.base.utils.AndroidUtilities;
import com.chat.base.utils.DataCleanManager;
import com.chat.base.utils.WKDeviceUtils;
import com.chat.base.utils.WKDialogUtils;
import com.chat.base.utils.WKLogUtils;
import com.chat.base.utils.singleclick.SingleClickUtil;
import com.chat.uikit.R;
import com.chat.uikit.WKUIKitApplication;
import com.chat.uikit.databinding.ActSettingLayoutBinding;
import com.chat.uikit.message.BackupRestoreMessageActivity;
import com.chat.uikit.user.service.UserModel;
import com.xinbida.wukongim.WKIM;
import com.xinbida.wukongim.entity.WKChannelType;

/**
 * 2020-03-22 21:11
 * 设置页面
 */
public class SettingActivity extends WKBaseActivity<ActSettingLayoutBinding> {
    private String str;

    @Override
    protected ActSettingLayoutBinding getViewBinding() {
        return ActSettingLayoutBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void setTitle(TextView titleTv) {
        titleTv.setText(R.string.setting);
    }

    @Override
    protected void initPresenter() {
        wkVBinding.refreshLayout.setEnableOverScrollDrag(true);
        wkVBinding.refreshLayout.setEnableLoadMore(false);
        wkVBinding.refreshLayout.setEnableRefresh(false);
    }

    @Override
    protected void initView() {
        getCacheSize();
        EndpointManager.getInstance().invoke("set_chat_bg_view", new ChatBgItemMenu(this, wkVBinding.chatBgLayout, "", WKChannelType.PERSONAL));
        // 隐藏 通用-企业版模块/第三方信息共享清单/开发日志 入口（如需放开请删除以下几行）
        wkVBinding.moduleLayout.setVisibility(View.GONE);
        wkVBinding.thirdShareLayout.setVisibility(View.GONE);
        wkVBinding.errorLogLayout.setVisibility(View.GONE);
        wkVBinding.dividerBeforeModule.setVisibility(View.GONE);
        wkVBinding.dividerBeforeThirdShare.setVisibility(View.GONE);
    }

    @Override
    protected void initListener() {
        String wk_theme_pref = Theme.getTheme();
        if (wk_theme_pref.equals(Theme.DARK_MODE)) {
            wkVBinding.darkStatusTv.setText(R.string.enabled);
        } else {
            wkVBinding.darkStatusTv.setText(R.string.disabled);
        }
        wkVBinding.loginOutTv.setOnClickListener(v -> WKDialogUtils.getInstance().showDialog(this, getString(R.string.login_out), getString(R.string.login_out_dialog), true, "", getString(R.string.login_out), 0, 0, index -> {
            if (index == 1) {
                UserModel.getInstance().quit(null);
                WKUIKitApplication.getInstance().exitLogin(0);
            }
        }));
        SingleClickUtil.onSingleClick(wkVBinding.languageLayout, view1 -> startActivity(new Intent(this, WKLanguageActivity.class)));
        SingleClickUtil.onSingleClick(wkVBinding.darkLayout, view1 -> startActivity(new Intent(this, WKThemeSettingActivity.class)));
        wkVBinding.clearImgCacheLayout.setOnClickListener(v -> showDialog(getString(R.string.clear_img_cache_tips), index -> {
            if (index == 1) {
                DataCleanManager.clearAllCache(SettingActivity.this);
                str = "0.00M";
                wkVBinding.imageCacheTv.setText(str);
            }
        }));
        wkVBinding.clearChatMsgLayout.setOnClickListener(v -> showDialog(getString(R.string.clear_all_msg_tips), index -> {
            if (index == 1) {
                WKIM.getInstance().getConversationManager().clearAll();
                WKIM.getInstance().getMsgManager().clearAll();
            }
        }));
        SingleClickUtil.onSingleClick(wkVBinding.moduleLayout, view1 -> startActivity(new Intent(this, AppModulesActivity.class)));
        SingleClickUtil.onSingleClick(wkVBinding.aboutLayout, view1 -> startActivity(new Intent(this, WKAboutActivity.class)));
        SingleClickUtil.onSingleClick(wkVBinding.fontSizeLayout, view1 -> startActivity(new Intent(this, WKSetFontSizeActivity.class)));
        WKCommonModel.getInstance().getAppNewVersion(false, version -> {
            String localV = WKDeviceUtils.getInstance().getVersionName(SettingActivity.this);
            boolean hasNewer = version != null
                    && !TextUtils.isEmpty(version.download_url)
                    && WKDeviceUtils.isRemoteVersionNewer(version.app_version, localV);
            wkVBinding.newVersionIv.setVisibility(hasNewer ? View.VISIBLE : View.GONE);
        });

        SingleClickUtil.onSingleClick(wkVBinding.msgBackupLayout, view1 -> {
            Intent intent = new Intent(this, BackupRestoreMessageActivity.class);
            intent.putExtra("handle_type", 1);
            startActivity(intent);
        });
        SingleClickUtil.onSingleClick(wkVBinding.msgRecoveryLayout, view1 -> {
            Intent intent = new Intent(this, BackupRestoreMessageActivity.class);
            intent.putExtra("handle_type", 2);
            startActivity(intent);
        });
        SingleClickUtil.onSingleClick(wkVBinding.thirdShareLayout, view1 -> {
            Intent intent = new Intent(this, WKWebViewActivity.class);
            intent.putExtra("url", WKApiConfig.baseWebUrl + "sdkinfo.html");
            startActivity(intent);
        });
        SingleClickUtil.onSingleClick(wkVBinding.errorLogLayout, view1 -> startActivity(new Intent(this, ErrorLogsActivity.class)));

        setupSecureChannel();
    }

    private void setupSecureChannel() {
        wkVBinding.secureChannelLayout.setVisibility(View.GONE);
        SecureChannelModel.getInstance().getConfig((code, msg, enabled, name) -> {
            if (!enabled || TextUtils.isEmpty(name)) {
                wkVBinding.secureChannelLayout.setVisibility(View.GONE);
                return;
            }
            wkVBinding.secureChannelLayout.setVisibility(View.VISIBLE);
            wkVBinding.secureChannelTv.setText(name);
            SingleClickUtil.onSingleClick(wkVBinding.secureChannelLayout, v -> onSecureChannelClick());
        });
    }

    private void onSecureChannelClick() {
        String saved = SecureChannelModel.getInstance().getSavedPassword();
        if (!TextUtils.isEmpty(saved)) {
            // 免密自动验证;失败则清除并弹密码框
            SecureChannelModel.getInstance().verify(saved, (code, msg, url) -> {
                if (!TextUtils.isEmpty(url)) {
                    openSecureChannelWebView(url);
                } else {
                    SecureChannelModel.getInstance().clearSavedPassword();
                    showSecureChannelPasswordDialog();
                }
            });
        } else {
            showSecureChannelPasswordDialog();
        }
    }

    private void showSecureChannelPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.secure_channel_input_password));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = AndroidUtilities.dp(20);
        container.setPadding(padding, AndroidUtilities.dp(8), padding, 0);

        EditText editText = new EditText(this);
        editText.setHint(getString(R.string.secure_channel_password_hint));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        editText.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        container.addView(editText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        builder.setView(container);
        builder.setNegativeButton(getString(R.string.cancel), null);
        builder.setPositiveButton(getString(R.string.sure), null); // 覆盖,避免输入错误时关闭

        AlertDialog dialog = builder.create();
        dialog.show();
        TextView positive = (TextView) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positive != null) {
            positive.setTextColor(ContextCompat.getColor(this, R.color.colorAccent));
            positive.setOnClickListener(v -> {
                String pwd = editText.getText() == null ? "" : editText.getText().toString();
                if (TextUtils.isEmpty(pwd)) {
                    Toast.makeText(SettingActivity.this, getString(R.string.secure_channel_password_empty), Toast.LENGTH_SHORT).show();
                    return;
                }
                SecureChannelModel.getInstance().verify(pwd, (code, msg, url) -> {
                    if (!TextUtils.isEmpty(url)) {
                        SecureChannelModel.getInstance().saveSavedPassword(pwd);
                        dialog.dismiss();
                        openSecureChannelWebView(url);
                    } else {
                        Toast.makeText(SettingActivity.this,
                                TextUtils.isEmpty(msg) ? getString(R.string.secure_channel_password_wrong) : msg,
                                Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
    }

    private void openSecureChannelWebView(String url) {
        Intent intent = new Intent(this, WKWebViewActivity.class);
        intent.putExtra("url", url);
        startActivity(intent);
    }


    //获取缓存大小
    private void getCacheSize() {
        new Thread(() -> {
            try {
                str = DataCleanManager.getTotalCacheSize(SettingActivity.this);
                if (str.equalsIgnoreCase("0.0Byte")) {
                    str = "0.00M";
                }
                AndroidUtilities.runOnUIThread(() -> wkVBinding.imageCacheTv.setText(str));
            } catch (Exception e) {
                WKLogUtils.e("获取图片缓存大小错误");
            }
        }).start();

    }

}
