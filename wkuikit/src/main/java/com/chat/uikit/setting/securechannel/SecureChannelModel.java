package com.chat.uikit.setting.securechannel;

import com.alibaba.fastjson.JSONObject;
import com.chat.base.base.WKBaseModel;
import com.chat.base.config.WKSharedPreferencesUtil;
import com.chat.base.net.HttpResponseCode;
import com.chat.base.net.IRequestResultListener;

public class SecureChannelModel extends WKBaseModel {

    private static final String SP_PASSWORD_KEY = "secure_channel_password";

    private SecureChannelModel() {
    }

    private static class Holder {
        private static final SecureChannelModel I = new SecureChannelModel();
    }

    public static SecureChannelModel getInstance() {
        return Holder.I;
    }

    public interface ISecureChannelConfig {
        void onResult(int code, String msg, boolean enabled, String name);
    }

    public interface ISecureChannelVerify {
        void onResult(int code, String msg, String url);
    }

    /**
     * 拉取加密通道配置(不含 url 与 password)
     */
    public void getConfig(final ISecureChannelConfig listener) {
        request(createService(SecureChannelService.class).getSecureChannel(), new IRequestResultListener<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                boolean enabled = result != null && result.getBooleanValue("enabled");
                String name = result != null ? result.getString("name") : "";
                listener.onResult(HttpResponseCode.success, "", enabled, name == null ? "" : name);
            }

            @Override
            public void onFail(int code, String msg) {
                listener.onResult(code, msg, false, "");
            }
        });
    }

    /**
     * 用密码验证,通过后下发 url
     */
    public void verify(String password, final ISecureChannelVerify listener) {
        JSONObject body = new JSONObject();
        body.put("password", password == null ? "" : password);
        request(createService(SecureChannelService.class).verifySecureChannel(body), new IRequestResultListener<JSONObject>() {
            @Override
            public void onSuccess(JSONObject result) {
                String url = result != null ? result.getString("url") : "";
                listener.onResult(HttpResponseCode.success, "", url == null ? "" : url);
            }

            @Override
            public void onFail(int code, String msg) {
                listener.onResult(code, msg, "");
            }
        });
    }

    /**
     * 本地保存的密码(通过验证的那个)。用于免密自动验证。
     */
    public String getSavedPassword() {
        return WKSharedPreferencesUtil.getInstance().getSPWithUID(SP_PASSWORD_KEY);
    }

    public void saveSavedPassword(String password) {
        WKSharedPreferencesUtil.getInstance().putSPWithUID(SP_PASSWORD_KEY, password == null ? "" : password);
    }

    public void clearSavedPassword() {
        WKSharedPreferencesUtil.getInstance().putSPWithUID(SP_PASSWORD_KEY, "");
    }
}
