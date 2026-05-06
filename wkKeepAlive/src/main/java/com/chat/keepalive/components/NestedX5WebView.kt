package com.chat.keepalive.components

import android.content.Context
import android.util.AttributeSet
import com.tencent.smtt.sdk.WebView

class NestedX5WebView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : WebView(context, attrs) {

    init {
        val callbackClient =
            X5CallBackClient(
                this.view,
                this
            )

        this.webViewClientExtension = X5ProxyWebViewClientExtension(callbackClient)
        this.setWebViewCallbackClient(callbackClient)
    }
}