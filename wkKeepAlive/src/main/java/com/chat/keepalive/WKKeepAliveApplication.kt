package com.chat.keepalive

import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chat.base.endpoint.EndpointManager
import com.chat.base.utils.AndroidUtilities
import com.chat.base.utils.LayoutHelper
import com.chat.keepalive.ui.GuideSystemSettingActivity

class WKKeepAliveApplication {

    private object SingletonInstance {
        val INSTANCE = WKKeepAliveApplication()
    }

    companion object {
        val instance: WKKeepAliveApplication
            get() = SingletonInstance.INSTANCE
    }

    fun init() {
        EndpointManager.getInstance().setMethod(
            "show_keep_alive_item"
        ) { `object` -> getKeepAliveItemView(`object` as Context) }
    }

    private fun getKeepAliveItemView(context: Context): View {
        val linearLayout = LinearLayout(context)
        linearLayout.orientation = LinearLayout.HORIZONTAL
        linearLayout.background = ContextCompat.getDrawable(context, R.drawable.layout_bg)
        linearLayout.setPadding(
            AndroidUtilities.dp(15f),
            AndroidUtilities.dp(15f),
            AndroidUtilities.dp(15f),
            AndroidUtilities.dp(15f)
        )
        val centerLayout = LinearLayout(context)
        centerLayout.orientation = LinearLayout.VERTICAL

        val titleTv = TextView(context)
        titleTv.text = context.getString(R.string.backend_operation_protection)
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16f)
        titleTv.setTextColor(ContextCompat.getColor(context, R.color.colorDark))
        centerLayout.addView(
            titleTv,
            LayoutHelper.createLinear(
                LayoutHelper.WRAP_CONTENT,
                LayoutHelper.WRAP_CONTENT,
                Gravity.START
            )
        )
        val desTv = TextView(context)
        desTv.text = context.getString(R.string.backend_operation_protection_des)
        desTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f)
        desTv.setTextColor(ContextCompat.getColor(context, R.color.color999))
        centerLayout.addView(
            desTv,
            LayoutHelper.createLinear(
                LayoutHelper.WRAP_CONTENT,
                LayoutHelper.WRAP_CONTENT,
                Gravity.START
            )
        )

        linearLayout.addView(
            centerLayout, LayoutHelper.createLinear(
                0,
                LayoutHelper.WRAP_CONTENT,
                1f,
                0, 0, 10, 0
            )
        )
        val arrowIV = ImageView(context)
        arrowIV.setImageResource(R.mipmap.ic_arrow_right)
        linearLayout.addView(
            arrowIV,
            LayoutHelper.createLinear(
                14,
                14,
                Gravity.CENTER
            )
        )
        linearLayout.setOnClickListener {
            context.startActivity(Intent(context, GuideSystemSettingActivity::class.java))
        }
        val parentLayout = LinearLayout(context)
        parentLayout.orientation = LinearLayout.VERTICAL
        parentLayout.addView(linearLayout)
        val lineView = LinearLayout(context)
        lineView.setBackgroundColor(ContextCompat.getColor(context, R.color.homeColor))
        parentLayout.addView(lineView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 10))
        return parentLayout
    }
}