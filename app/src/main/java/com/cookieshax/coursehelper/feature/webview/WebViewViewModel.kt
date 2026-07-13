package com.cookieshax.coursehelper.feature.webview

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

class WebViewViewModel : ViewModel() {
    @SuppressLint("StaticFieldLeak")
    var webView: WebView? = null
        private set

    var jsBridge: JsBridgeInterface? = null
        private set

    val currentUrl: MutableState<String> = mutableStateOf("")

    val hasLoadedInitially: MutableState<Boolean> = mutableStateOf(false)

    val webViewCurrentUrl: MutableState<String?> = mutableStateOf(null)

    val pageTitle: MutableState<String?> = mutableStateOf(null)

    fun initializeWebView(context: Context, isDarkTheme: Boolean) {
        if (webView == null) {
            webView = WebView(context).apply {
                WebViewConfigurator.configureSettings(settings)
                WebViewConfigurator.applyViewConfig(this)
                if (isDarkTheme) {
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
                        Log.d("WebViewDarkMode", "Enabled algorithmic darkening (Android 13+)")
                    }
                }
            }
        }
    }

    fun updateCurrentUrl(url: String) {
        currentUrl.value = url
    }

    fun setHasLoadedInitially(loaded: Boolean) {
        hasLoadedInitially.value = loaded
    }

    fun updateWebViewCurrentUrl(url: String?) {
        webViewCurrentUrl.value = url
    }

    fun loadUrl(url: String, headers: Map<String, String> = emptyMap()) {
        webView?.post {
            webView?.loadUrl(url, headers)
            updateWebViewCurrentUrl(url)
            setHasLoadedInitially(true)
        }
    }

    fun clearCacheAndStorage() {
        webView?.clearCache(true)

        // 清除所有域的 Web Storage (localStorage, sessionStorage)
        try {
            WebStorage.getInstance().deleteAllData()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun createJsBridge(onNotificationReceived: (String, String) -> Unit): JsBridgeInterface? {
        if (jsBridge == null) {
            jsBridge = JsBridgeInterface(webView, onNotificationReceived)
        }
        return jsBridge
    }

    override fun onCleared() {
        jsBridge?.destroy()
        jsBridge = null
        webView?.let { wv ->
            try {
                wv.stopLoading()
                wv.destroy()
            } catch (_: Exception) {
                // 忽略销毁异常
            }
        }
        webView = null
    }
}
