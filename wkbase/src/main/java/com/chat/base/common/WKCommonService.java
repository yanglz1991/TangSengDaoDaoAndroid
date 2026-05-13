package com.chat.base.common;

import com.alibaba.fastjson.JSONObject;
import com.chat.base.entity.AppModule;
import com.chat.base.entity.ChannelInfoEntity;
import com.chat.base.entity.WKAPPConfig;
import com.chat.base.entity.AppVersion;
import com.chat.base.entity.WKChannelState;
import com.chat.base.entity.CheckStatusResult;
import com.chat.base.net.entity.CommonResponse;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * 4/21/21 6:25 PM
 */
interface WKCommonService {
    @GET("common/appversion/android/{version}")
    Observable<AppVersion> getAppNewVersion(@Path("version") String version);

    @GET("common/appconfig")
    Observable<WKAPPConfig> getAppConfig();

    @GET("channel/state")
    Observable<WKChannelState> getChannelState(@Query("channel_id") String channelID, @Query("channel_type") byte channelType);

    @GET("channels/{channelID}/{channelType}")
    Observable<ChannelInfoEntity> getChannel(@Path("channelID") String channelID, @Path("channelType") byte channelType);

    @GET("common/appmodule")
    Observable<List<AppModule>> getAppModule();

    // 客户端启动 / 从后台恢复时主动查询当前账号 / IP / 设备是否被管理后台封禁
    // 配合 /v1/user/checkstatus 接口使用，服务端会按 uid → ip → device 顺序返回最先命中的维度
    @GET("user/checkstatus")
    Observable<CheckStatusResult> checkBanStatus(@Query("device_id") String deviceID);

    // 上报消息已读
    @POST("message/readed")
    Observable<CommonResponse> readedMsg(@Body JSONObject jsonObject);
}
