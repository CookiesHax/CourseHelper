package com.cookieshax.coursehelper.feature.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.RenderProcessGoneDetail
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.MutableState
import com.cookieshax.coursehelper.R
import com.cookieshax.coursehelper.core.network.NetworkClient
import com.cookieshax.coursehelper.core.utils.showToast
import kotlinx.coroutines.runBlocking

object WebViewConfigurator {
    // 配置 WebView 的基础设置
    @SuppressLint("SetJavaScriptEnabled")
    fun configureSettings(settings: WebSettings) {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            builtInZoomControls = true
            displayZoomControls = false
            javaScriptCanOpenWindowsAutomatically = true
            setSupportZoom(true)
            textZoom = 100 // 防止系统字体大小影响
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL // 禁用自动调整视图
            userAgentString = runBlocking { NetworkClient.getUserAgent() }
        }
    }

    fun updateUserAgent(webView: WebView, url: String?) {
        if (url != null && url.contains("xuexi365.com")) {
            // xuexi365.com 不改变 UA (使用系统默认)
            webView.settings.userAgentString = null
        } else {
            webView.settings.userAgentString = runBlocking { NetworkClient.getUserAgent() }
        }
    }

    @SuppressLint("MissingOnRenderProcessGone")
    fun createWebViewClient(
        isLoading: MutableState<Boolean>,
        fixLayoutJs: String,
        onTitleChanged: (String?) -> Unit
    ): WebViewClient {
        return object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                view?.let { updateUserAgent(it, url) }
            }

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

            override fun onRenderProcessGone(
                view: WebView?,
                detail: RenderProcessGoneDetail
            ): Boolean {
                // 记录崩溃详细信息以进行调试
                if (detail.didCrash()) {
                    "由于内部错误，渲染进程崩溃".showToast()
                } else {
                    "渲染进程被系统杀死".showToast()
                }

                // 从视图层次结构中移除崩溃的 WebView 实例
                view?.destroy()

                // 返回 true 以信号代表处理了崩溃
                // Returning false will still crash the app.
                return true
            }
        }
    }

    fun createWebChromeClient(
        onShowFileChooser: (ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Boolean,
        onGeolocationPermissionsShowPrompt: (String?, GeolocationPermissions.Callback?) -> Unit
    ): WebChromeClient {
        return object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                return onShowFileChooser(filePathCallback, fileChooserParams)
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                onGeolocationPermissionsShowPrompt(origin, callback)
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
