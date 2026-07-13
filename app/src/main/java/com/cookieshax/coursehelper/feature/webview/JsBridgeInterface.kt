package com.cookieshax.coursehelper.feature.webview

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.errorprone.annotations.Keep

class JsBridgeInterface(
    internal var webView: WebView?,
    private var onNotificationReceived: ((String, String) -> Unit)?,
) {
    // 注册 JavaScript 接口 使得 WebView 中的 JavaScript 可以调用这个类的方法
    init {
        webView?.addJavascriptInterface(this, "androidjsbridge")
    }

    // 供 WebView JavaScript 调用的方法 用于接收通知
    // 此方法通过反射调用
    @Keep
    @JavascriptInterface
    fun postNotification(notificationName: String, paramsJson: String) {
        webView?.post {
            onNotificationReceived?.let { it(notificationName, paramsJson) }
            Log.d("JsBridge", "Received notification: $notificationName, $paramsJson")
        }
    }

    // 供 Native 代码调用的方法 用于向 WebView 发送消息
    fun sendMessageToWebView(notificationName: String, paramsJson: String) {
        val currentWebView = webView ?: return
        currentWebView.post {
            val jsCode =
                "window.jsBridge && window.jsBridge.trigger('$notificationName', $paramsJson)"

            Log.d("JsBridge", "Executing JS: $jsCode")

            currentWebView.evaluateJavascript(jsCode) { result ->
                Log.d("JsBridge", "Result: $result")
            }
        }
    }

    fun destroy() {
        webView?.removeJavascriptInterface("androidjsbridge")
        webView = null
        onNotificationReceived = null
    }
}
