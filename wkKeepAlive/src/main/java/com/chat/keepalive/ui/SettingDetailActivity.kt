package com.chat.keepalive.ui

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.View.OnTouchListener
import android.widget.TextView
import com.chat.base.base.WKBaseActivity
import com.chat.base.config.WKApiConfig
import com.chat.base.config.WKBinder
import com.chat.base.utils.AndroidUtilities
import com.chat.base.utils.systembar.WKOSUtils
import com.chat.keepalive.R
import com.chat.keepalive.components.X5CallBackClient
import com.chat.keepalive.components.X5ProxyWebViewClientExtension
import com.chat.keepalive.databinding.ActSettingDetailLayoutBinding
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView


class SettingDetailActivity : WKBaseActivity<ActSettingDetailLayoutBinding>() {
    private lateinit var type: String
    override fun getViewBinding(): ActSettingDetailLayoutBinding {
        return ActSettingDetailLayoutBinding.inflate(layoutInflater)
    }

    override fun setTitle(titleTv: TextView?) {
        titleTv!!.text = getString(R.string.tutorial_detail)
    }

    override fun initPresenter() {
        super.initPresenter()
        type = intent.getStringExtra("type")!!
    }

    override fun initView() {
        val callbackClient =
            X5CallBackClient(
                wkVBinding.webView.view,
                wkVBinding.webView
            )
        wkVBinding.webView.webViewClientExtension = X5ProxyWebViewClientExtension(callbackClient)
        wkVBinding.webView.setWebViewCallbackClient(callbackClient)

        val webSettings = wkVBinding.webView.settings
        webSettings.javaScriptEnabled = true // 设置支持javascript脚本
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);

        webSettings.useWideViewPort = true
        webSettings.pluginState = WebSettings.PluginState.ON
        webSettings.loadWithOverviewMode = true
        webSettings.defaultTextEncodingName = "UTF-8"
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        webSettings.setAppCacheEnabled(true)
        webSettings.setSupportMultipleWindows(true)
        webSettings.javaScriptCanOpenWindowsAutomatically = true
        webSettings.savePassword = false
        webSettings.saveFormData = false // 禁止保存表单

        webSettings.domStorageEnabled = true
        webSettings.setAppCacheMaxSize((1024 * 1024 * 8).toLong())
        //webSettings.setAllowFileAccess(true);
        //webSettings.setAllowFileAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(false)
        webSettings.setAllowFileAccessFromFileURLs(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSettings.mixedContentMode = 0
        }
        if (WKBinder.isDebug && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        //支持屏幕缩放
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        wkVBinding.webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        // wkVBinding.webView.setBackgroundColor(ContextCompat.getColor(this, R.color.homeColor));

        when (type) {
            "common_setting" -> {
                // 通用设置
                if (WKOSUtils.isOppo()) {
                    wkVBinding.openSystemSettingTv.text = String.format(
                        getString(R.string.oppo_common_setting),
                        getString(R.string.app_name)
                    )
                    playVideo("oppo_common_setting.mp4")
                } else if (WKOSUtils.isMiui()) {
                    wkVBinding.openSystemSettingTv.text = String.format(
                        getString(R.string.xiaomi_common_setting),
                        getString(R.string.app_name)
                    )
                    playVideo("xiaomi_common_setting.mp4")
                } else if (WKOSUtils.isVivo()) {
                    wkVBinding.openSystemSettingTv.text = String.format(
                        getString(R.string.vivo_common_setting),
                        getString(R.string.app_name)
                    )
                    playVideo("vivo_common_setting.mp4")
                }
            }

            "float_window" -> {
                // 悬浮窗
                wkVBinding.openSystemSettingTv.text = String.format(
                    getString(R.string.huawei_float_window_setting),
                    getString(R.string.app_name)
                )
                playVideo("hw_float_window.mp4")
            }

            "power_white_list" -> {
                // 高耗电白名单
                wkVBinding.openSystemSettingTv.text = String.format(
                    getString(R.string.power_white_list_setting),
                    getString(R.string.app_name)
                )
                playVideo("vivo_flow_white.mp4")
            }

            "keep_connection" -> {
                // 休眠保持连接
                wkVBinding.openSystemSettingTv.text = getString(R.string.huawei_keep_connection)
                playVideo("hw_keep_network.mp4")
            }

            "self_start" -> {
                // 自启动
                wkVBinding.openSystemSettingTv.text = String.format(
                    getString(R.string.huawei_self_start),
                    getString(R.string.app_name)
                )
                playVideo("hw_self_start.mp4")
            }

            "lock_task" -> {
                wkVBinding.gotoSettingTv.visibility = View.GONE
                // 后台任务锁
                if (WKOSUtils.isMiui()) {
                    wkVBinding.openSystemSettingTv.text = String.format(
                        getString(R.string.xiaomi_lock_task_setting),
                        getString(R.string.app_name),
                        getString(R.string.app_name)
                    )
                    playVideo("xiaomi_lock_task.mp4")
                } else {
                    wkVBinding.openSystemSettingTv.text = String.format(
                        getString(R.string.other_lock_task_setting),
                        getString(R.string.app_name)
                    )
                    if (WKOSUtils.isMiui()) {
                        playVideo("hw_lock_task.mp4")
                    } else if (WKOSUtils.isVivo()) {
                        playVideo("vivo_lock_task.mp4")
                    } else if (WKOSUtils.isOppo()) {
                        playVideo("oppo_lock_task.mp4")
                    } else {
                        playVideo("hw_lock_task.mp4")
                    }
                }
            }

            "provincial_traffic_volume" -> {
                // 省流量
                if (WKOSUtils.isVivo()) {
                    wkVBinding.openSystemSettingTv.text = String.format(
                        getString(R.string.vivo_flow_setting),
                        getString(R.string.app_name)
                    )
                    playVideo("vivo_flow_white.mp4")
                } else if (WKOSUtils.isEmui()) {
                    wkVBinding.openSystemSettingTv.text = String.format(
                        getString(R.string.hauwei_provincial_traffic_volume),
                        getString(R.string.app_name),
                        getString(R.string.app_name)
                    )
                    playVideo("hw_flow_white.mp4")
                }
            }
        }

    }

    override fun initListener() {

        wkVBinding.gotoSettingTv.setOnClickListener {
            when (type) {
                "power_white_list" -> {
                    // 高耗电白名单
                    val intent = Intent("/")
                    val cn =
                        ComponentName.unflattenFromString("com.android.settings/.Settings\$PowerUsageSummaryActivity")
                    intent.setComponent(cn)
                    intent.setAction("android.intent.action.VIEW")
                    startActivity(intent)
                }

                "common_setting" -> {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.setData(Uri.parse("package:$packageName"))
                    startActivity(intent)
                }

                "float_window" -> {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }

                else -> {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }
    }

    private fun playVideo(videoName: String) {
        val divStyle = "width: 70%;margin: 0 auto;"
        val url = WKApiConfig.baseUrl + "common/keepalive?video_name=" + videoName
        val htmlContent =
            "<html><body><div style='$divStyle'><video style='width: 100%;height: auto;object-fit: cover;' src='$url' controls autoplay loop muted></video></div></body></html>"

        wkVBinding.webView.loadData(htmlContent, "text/html", "utf-8")
    }

    override fun onDestroy() {
        super.onDestroy()
        wkVBinding.webView.destroy()
    }
}