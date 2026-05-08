package com.chat.uikit.setting.securechannel;

import com.alibaba.fastjson.JSONObject;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

interface SecureChannelService {
    // 查询加密通道配置(仅返回 enabled + name)
    @GET("common/secure_channel")
    Observable<JSONObject> getSecureChannel();

    // 验证密码,通过后返回 url
    @POST("common/secure_channel/verify")
    Observable<JSONObject> verifySecureChannel(@Body JSONObject body);
}
