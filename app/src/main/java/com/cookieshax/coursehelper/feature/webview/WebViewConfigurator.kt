package com.cookieshax.coursehelper.feature.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.ValueCallback
import android.net.Uri
import androidx.compose.runtime.MutableState
import com.cookieshax.coursehelper.R
import com.cookieshax.coursehelper.core.network.NetworkClient

object WebViewConfigurator {
    // 配置 WebView 的基础设置
    @SuppressLint("SetJavaScriptEnabled")
    fun configureSettings(settings: WebSettings) {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            textZoom = 100 // 防止系统字体大小影响
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL // 禁用自动调整视图
            userAgentString = NetworkClient.getUserAgent()
        }
    }

    fun createWebViewClient(
        isLoading: MutableState<Boolean>,
        fixLayoutJs: String,
        onTitleChanged: (String?) -> Unit
    ): WebViewClient {
        return object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isLoading.value = false
                onTitleChanged(view?.title)

                // 更改 jsBridge 为 Android (默认为 ios)
                view?.evaluateJavascript("jsBridge.setDevice('android');", null)
                url?.let {
                    if (it.contains("sign/")) {
                        // 清除签到记录
                        view?.evaluateJavascript("window.localStorage.clear();", null)
                        // 签到页面显示异常修复
                        view?.evaluateJavascript(fixLayoutJs, null)
                    }
                }
            }
        }
    }

    fun createWebChromeClient(
        onShowFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean
    ): WebChromeClient {
        return object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                return onShowFileChooser(filePathCallback, fileChooserParams)
            }
        }
    }

    fun applyViewConfig(webView: WebView) {
        // 设置 WebView 的布局参数
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // 设置背景色以避免主题切换时的白色闪烁
        webView.setBackgroundColor(Color.TRANSPARENT)
    }

    fun loadFixLayoutJs(context: Context): String {
        val inputStream = context.resources.openRawResource(R.raw.fix_layout)
        return inputStream.bufferedReader().use { it.readText() }
    }
}
