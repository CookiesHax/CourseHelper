package com.cookieshax.coursehelper.feature.webview

import android.webkit.CookieManager
import com.cookieshax.coursehelper.core.network.CookieManager as NetworkCookieManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl

object WebViewCookieManager {
    suspend fun loadAndSetCookies(targetUrl: String, currentUserId: String?) {
        if (currentUserId == null || targetUrl.isBlank()) return

        // 后台加载 Cookie
        val cookies = withContext(Dispatchers.IO) {
            val userCookieJar = NetworkCookieManager.getOrLoadCookieJar(currentUserId)
            val urlObj = targetUrl.toHttpUrl()
            userCookieJar.loadForRequest(urlObj)
        }

        // 主线程操作 CookieManager
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        // 等待旧 Cookie 清除完成
        cookieManager.removeAllCookies { }

        // 设置新 Cookie
        cookies.forEach { cookie ->
            buildCookieString(cookie)?.let { cookieStr ->
                val domainUrl = getCookieDomainUrl(cookie, targetUrl)
                cookieManager.setCookie(domainUrl, cookieStr)
            }
        }
        cookieManager.flush()

        // 强制同步 Cookie 以使 WebView 网络栈立即识别
        cookieManager.getCookie(targetUrl)
    }

    private fun getCookieDomainUrl(cookie: Cookie, fallbackUrl: String): String {
        return if (cookie.domain.isNotBlank()) {
            val host = cookie.domain.removePrefix(".")
            val protocol = if (cookie.secure) "https" else "http"
            "$protocol://$host/"
        } else {
            fallbackUrl
        }
    }

    private fun buildCookieString(cookie: Cookie): String? {
        return buildString {
            append("${cookie.name}=${cookie.value}")
            append("; Domain=${cookie.domain}")
            append("; Path=${cookie.path}")
            if (cookie.secure) append("; Secure")
            if (cookie.httpOnly) append("; HttpOnly")
        }.takeIf { it.isNotBlank() }
    }
}
