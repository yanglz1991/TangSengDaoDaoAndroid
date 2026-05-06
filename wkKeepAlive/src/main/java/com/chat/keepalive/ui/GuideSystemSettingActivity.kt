package com.chat.keepalive.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.TextView
import com.chat.base.base.WKBaseActivity
import com.chat.base.utils.systembar.WKOSUtils
import com.chat.keepalive.R
import com.chat.keepalive.databinding.ActGuideSystemSettingLayoutBinding


class GuideSystemSettingActivity : WKBaseActivity<ActGuideSystemSettingLayoutBinding>() {
    override fun getViewBinding(): ActGuideSystemSettingLayoutBinding {
        return ActGuideSystemSettingLayoutBinding.inflate(layoutInflater)
    }

    override fun initPresenter() {
        wkVBinding.refreshLayout.setEnableOverScrollDrag(true)
        wkVBinding.refreshLayout.setEnableLoadMore(false)
        wkVBinding.refreshLayout.setEnableRefresh(false)
    }

    override fun setTitle(titleTv: TextView?) {
        titleTv!!.text = getString(R.string.backend_operation_protection)
    }

    override fun initView() {
        wkVBinding.openSystemSettingTv.text = String.format(
            getString(R.string.open_system_setting_tips),
            getString(R.string.app_name),
            getString(R.string.app_name)
        )
        wkVBinding.selfStartDescTv.text = String.format(
            getString(R.string.self_start_desc),
            getString(R.string.app_name)
        )
        wkVBinding.lockTaskDescTv.text = String.format(
            getString(R.string.lock_tasks_desc),
            getString(R.string.app_name)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            val hasIgnored = powerManager.isIgnoringBatteryOptimizations(packageName)
            //  判断当前APP是否有加入电池优化的白名单，如果没有，弹出加入电池优化的白名单的设置对话框。
            if (hasIgnored) {
                wkVBinding.neglectingBatteryOptimizationLayout.visibility = View.GONE
            }
            if (WKOSUtils.isEmui()){
                wkVBinding.floatWindowLayout.visibility=View.VISIBLE
            }
        } else {
            wkVBinding.neglectingBatteryOptimizationLayout.visibility = View.GONE
        }
        if (WKOSUtils.isEmui()) {
            wkVBinding.settingLayout.visibility = View.GONE
            wkVBinding.keepNetWorkConnectionLayout.visibility = View.VISIBLE
//            wkVBinding.floatWindowLayout.visibility = View.VISIBLE
        }
        if (WKOSUtils.isOppo() || WKOSUtils.isMiui()) {
            wkVBinding.settingLayout.visibility = View.VISIBLE
            wkVBinding.keepNetWorkConnectionLayout.visibility = View.GONE
            wkVBinding.selfStartLayout.visibility = View.GONE
            wkVBinding.provincialTrafficVolumeLayout.visibility = View.GONE
        }
        if (WKOSUtils.isVivo()) {
            wkVBinding.powerLayout.visibility = View.VISIBLE
            wkVBinding.selfStartLayout.visibility = View.GONE
        }
    }

    override fun initListener() {
        wkVBinding.appSettingTv.setOnClickListener {
            gotoSettingDetail("common_setting")
        }
        wkVBinding.floatWindowTv.setOnClickListener {
            // 悬浮窗
            gotoSettingDetail("float_window")
        }
        wkVBinding.gotoSettingTv.setOnClickListener {
            // 忽略电池优化
            setPower()
        }
        wkVBinding.keepConnectionTv.setOnClickListener {
            // 休眠
            gotoSettingDetail("keep_connection")
        }
        wkVBinding.selfStartTv.setOnClickListener {
            // 自启动
            gotoSettingDetail("self_start")
        }
        wkVBinding.lockTaskTv.setOnClickListener {
            // 后台任务锁
            gotoSettingDetail("lock_task")
        }
        wkVBinding.provincialTrafficTv.setOnClickListener {
            // 省流量
            gotoSettingDetail("provincial_traffic_volume")
        }
        wkVBinding.powerWhiteTv.setOnClickListener {
            // 高耗电白名单
            gotoSettingDetail("power_white_list")
        }
    }

    private fun gotoSettingDetail(type: String) {
        val intent = Intent(this, SettingDetailActivity::class.java)
        intent.putExtra("type", type)
        startActivity(intent)
    }

    private fun setPower() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val hasIgnored = powerManager.isIgnoringBatteryOptimizations(packageName)
        //  判断当前APP是否有加入电池优化的白名单，如果没有，弹出加入电池优化的白名单的设置对话框。
        if (!hasIgnored) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.setData(Uri.parse("package:$packageName"))
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        }
    }
}