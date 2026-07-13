package com.cookieshax.coursehelper.core.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cookieshax.coursehelper.app.CourseHelperApplication
import com.cookieshax.coursehelper.core.utils.StringUtils.gson
import com.cookieshax.coursehelper.feature.account.model.AccountRepository
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap

private val Context.cookieDataStore: DataStore<Preferences> by preferencesDataStore(name = "cookie_manager")

// Cookie 数据类
data class SerializableCookie(
    val name: String,
    val value: String,
    val expiresAt: Long,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean,
    val hostOnly: Boolean
) {
    companion object {
        fun fromCookie(cookie: Cookie): SerializableCookie {
            return SerializableCookie(
                name = cookie.name,
                value = cookie.value,
                expiresAt = cookie.expiresAt,
                domain = cookie.domain,
                path = cookie.path,
                secure = cookie.secure,
                httpOnly = cookie.httpOnly,
                hostOnly = cookie.hostOnly
            )
        }

        fun toCookie(cookie: SerializableCookie, url: HttpUrl): Cookie {
            return Cookie.Builder()
                .name(cookie.name)
                .value(cookie.value)
                .expiresAt(cookie.expiresAt)
                .path(cookie.path)
                .let { builder ->
                    if (cookie.hostOnly) {
                        builder.hostOnlyDomain(url.host)
                    } else {
                        builder.domain(cookie.domain)
                    }
                }
                .let { builder ->
                    if (cookie.secure) builder.secure() else builder
                }
                .let { builder ->
                    if (cookie.httpOnly) builder.httpOnly() else builder
                }
                .build()
        }
    }
}

object CookieManager {
    private const val CHAOXING_BASE_URI = "https://chaoxing.com"

    // 用于上下文 Cookie 的 CookieJar
    private val contextCookieJar = ClearableCookieJar()

    private val context by lazy { CourseHelperApplication.context.applicationContext }
    private val applicationScope by lazy { CourseHelperApplication.applicationScope }

    // UID -> CookieJar
    private val _userCookieJars = ConcurrentHashMap<String, ClearableCookieJar>()

    // 用于管理每个用户初始化过程的锁
    private val userInitializationLocks = ConcurrentHashMap<String, Any>()

    // 用于管理持久化顺序的互斥锁 (防止同一用户并发写入磁盘冲突)
    private val userPersistenceLocks = ConcurrentHashMap<String, Mutex>()

    // Cookie 拦截器 用于处理请求和响应中的 Cookie
    class CookieInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()

            val userTag = originalRequest.tag(RequestAsUser::class.java)
            val targetUserId = if (userTag != null) {
                // 如果显式传递 Tag 以 Tag 为准
                // 即使 userId 是 null
                userTag.userId
            } else {
                // 只有完全没传 Tag 的普通请求才使用当前活跃账户
                AccountRepository.activeAccountIdFlow.value
            }

            val cookieJar = getOrLoadCookieJar(targetUserId)

            // 加载 Cookie 并构建新请求
            val cookies = cookieJar.loadForRequest(originalRequest.url)
            val requestBuilder = originalRequest.newBuilder()

            if (cookies.isNotEmpty()) {
                val cookieString = cookies.joinToString("; ") { "${it.name}=${it.value}" }
                requestBuilder.header("Cookie", cookieString)
            }

            val response = chain.proceed(requestBuilder.build())

            // 处理响应中的 Set-Cookie
            val setCookieHeaders = response.headers.values("Set-Cookie")
            if (setCookieHeaders.isNotEmpty()) {
                val updatedCookies = setCookieHeaders.mapNotNull { header ->
                    Cookie.parse(response.request.url, header)
                }

                if (updatedCookies.isNotEmpty()) {
                    cookieJar.saveFromResponse(response.request.url, updatedCookies)

                    // 如果是用户 异步持久化到磁盘
                    if (targetUserId != null) {
                        applicationScope.launch {
                            val context = CourseHelperApplication.context
                            persistCookies(targetUserId, context)
                        }
                    }
                }
            }
            return response
        }
    }

    private fun getInitLock(userId: String): Any =
        userInitializationLocks.getOrPut(userId) { Any() }

    private fun getPersistenceMutex(userId: String): Mutex =
        userPersistenceLocks.getOrPut(userId) { Mutex() }

    private fun Cookie.hasExpired(): Boolean =
        System.currentTimeMillis() >= expiresAt && expiresAt != Long.MAX_VALUE

    private class ClearableCookieJar : CookieJar {
        private val allCookies = mutableListOf<Cookie>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (cookies.isEmpty()) return

            // 使用 synchronized 保证线程安全
            synchronized(allCookies) {
                cookies.forEach { cookie ->
                    // 检查是否已有相同 name/domain/path 的 Cookie 如果有则移除
                    val existingIndex = allCookies.indexOfFirst {
                        it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path
                    }
                    if (existingIndex >= 0) {
                        allCookies.removeAt(existingIndex)
                    }
                    // 添加新Cookie
                    if (!cookie.hasExpired()) {
                        allCookies.add(cookie)
                    }
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            // 使用 synchronized 保证线程安全
            return synchronized(allCookies) {
                allCookies.filter { !it.hasExpired() && it.matches(url) }
            }
        }

        fun clear() {
            synchronized(allCookies) {
                allCookies.clear()
            }
        }
    }

    // 从磁盘加载指定用户的 Cookie
    private suspend fun restoreCookiesFromDisk(
        userId: String,
        cookieJar: CookieJar,
        context: Context
    ) {
        val prefs = context.cookieDataStore.data.first()
        val cookiesJson = prefs[stringPreferencesKey("cookies_$userId")] ?: return

        try {
            val type = object : TypeToken<List<SerializableCookie>>() {}.type
            val serializableCookies: List<SerializableCookie> = gson.fromJson(cookiesJson, type)
            val url = CHAOXING_BASE_URI.toHttpUrl()

            serializableCookies.forEach { serialized ->
                val cookie = SerializableCookie.toCookie(serialized, url)
                if (!cookie.hasExpired()) {
                    cookieJar.saveFromResponse(url, listOf(cookie))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 获取或创建指定用户的 CookieJar
    fun getOrLoadCookieJar(userId: String?): CookieJar {
        if (userId == null) return contextCookieJar

        // 先检查内存
        _userCookieJars[userId]?.let { return it }

        // 获取用户专属锁对象
        val lock = getInitLock(userId)

        // 内存没有则进行同步控制 防止重复加载
        return synchronized(lock) {
            // 双检锁 防止等待锁期间其他协程已经创建了 CookieJar
            _userCookieJars[userId]?.let { return@synchronized it }

            val jar = ClearableCookieJar()

            runBlocking(Dispatchers.IO) { // 确保在返回 Jar 给拦截器前 数据已经从磁盘加载完毕
                restoreCookiesFromDisk(userId, jar, context)
            }
            _userCookieJars[userId] = jar
            jar
        }
    }

    // 保存指定用户的 Cookie
    suspend fun persistCookies(userId: String, context: Context, newCookies: List<Cookie>? = null) {
        // 获取特定用户持久化互斥锁
        val mutex = getPersistenceMutex(userId)

        // 同一时间只有一个协程在为该用户写磁盘
        mutex.withLock {
            val jar = getOrLoadCookieJar(userId)

            if (newCookies != null) {
                val url = CHAOXING_BASE_URI.toHttpUrl()
                jar.saveFromResponse(url, newCookies)
            }

            val url = CHAOXING_BASE_URI.toHttpUrl()
            val cookies = jar.loadForRequest(url)

            // 序列化为 JSON
            val serializableCookies = cookies.map { SerializableCookie.fromCookie(it) }
            val cookiesJson = gson.toJson(serializableCookies)

            val prefsKey = stringPreferencesKey("cookies_$userId")
            context.cookieDataStore.edit { preferences ->
                preferences[prefsKey] = cookiesJson
            }
        }
    }

    // 将上下文Cookie转移到指定用户账户下
    suspend fun transferLoginCookiesToUser(userId: String, context: Context) {
        // 获取上下文CookieJar中的Cookie
        val contextCookies = contextCookieJar.loadForRequest("https://chaoxing.com".toHttpUrl())

        if (contextCookies.isNotEmpty()) {
            // 获取目标用户CookieJar
            val userCookieJar = getOrLoadCookieJar(userId)

            // 将上下文Cookie保存到用户CookieJar中
            userCookieJar.saveFromResponse("https://chaoxing.com".toHttpUrl(), contextCookies)
            // 持久化到磁盘
            persistCookies(userId, context, contextCookies)
        }
    }

    // 清除指定用户的所有 Cookie
    suspend fun clearCookiesForUser(userId: String) {
        _userCookieJars.remove(userId)

        userInitializationLocks.remove(userId)
        userPersistenceLocks.remove(userId)

        val prefsKey = stringPreferencesKey("cookies_$userId")
        context.cookieDataStore.edit { preferences ->
            preferences.remove(prefsKey)
        }
    }

    // 清除所有用户的 Cookie (用于数据库重置等极端情况)
    suspend fun clearAllCookies() {
        contextCookieJar.clear()
        _userCookieJars.clear()
        userInitializationLocks.clear()
        userPersistenceLocks.clear()
        context.cookieDataStore.edit { it.clear() }
    }
}
