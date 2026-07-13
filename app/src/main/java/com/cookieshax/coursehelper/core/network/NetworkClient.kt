package com.cookieshax.coursehelper.core.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import com.cookieshax.coursehelper.BuildConfig
import com.cookieshax.coursehelper.app.CourseHelperApplication
import com.cookieshax.coursehelper.core.info.ChaoXingAppInfo
import com.cookieshax.coursehelper.core.info.DeviceInfo
import com.cookieshax.coursehelper.feature.account.model.AccountRepository
import com.cookieshax.coursehelper.core.utils.Constant
import com.cookieshax.coursehelper.core.utils.EncryptionUtils
import java.net.URLEncoder
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Request
import okhttp3.Response
import com.cookieshax.coursehelper.core.repository.SettingsRepository
import java.io.File

private val Context.networkDataStore: DataStore<Preferences> by preferencesDataStore(name = "network_config")

data class RequestAsUser(val userId: String?)

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
}

object NetworkClient {
    private val context by lazy { CourseHelperApplication.context.applicationContext }

    @Volatile
    private var cachedId: String? = null

    @Volatile
    private var cachedUserAgent: String? = null

    private val config: Map<String, String>
        get() = mapOf(
            "systemHttpAgent" to "Dalvik/2.1.0 (Linux; U; Android ${DeviceInfo.osVersionName}; ${DeviceInfo.model} Build/${DeviceInfo.buildId})",
            "device" to DeviceInfo.model,
            "productId" to "3",
            "version" to ChaoXingAppInfo.VERSION,
            "versionCode" to ChaoXingAppInfo.VERSION_CODE,
            "apiVersion" to ChaoXingAppInfo.API_VERSION
        )

    private class CommonHeaderInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()

            // 构建全局 Header Map
            val globalHeaders = mapOf(
                "User-Agent" to getOrGenerateUserAgent(),
                "Accept-Language" to "zh_CN",
                "Connection" to "keep-alive"
            )

            // 遍历 Map 并添加到请求中
            globalHeaders.forEach { (key, value) ->
                if (originalRequest.header(key) == null) {
                    requestBuilder.addHeader(key, value)
                }
            }

            return chain.proceed(requestBuilder.build())
        }
    }

    // 初始化 OkHttpClient
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(CommonHeaderInterceptor())
        .addInterceptor(CookieManager.CookieInterceptor())
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        })
        .build()

    fun getUserAgent(): String = getOrGenerateUserAgent()

    fun clearUserAgentCache() {
        cachedUserAgent = null
    }

    // 公共请求方法
    suspend fun get(
        url: String,
        params: Map<String, String>? = null,
        headers: Map<String, String>? = null,
        asUser: String? = AccountRepository.activeAccountIdFlow.value
    ): ApiResult<String> {
        // 构建带参数的 URL
        val requestUrl = if (!params.isNullOrEmpty()) {
            val existingUrl =
                url.toHttpUrlOrNull() ?: throw IllegalArgumentException("Invalid URL: $url")
            val newUrlBuilder = existingUrl.newBuilder()
            params.forEach { (key, value) ->
                newUrlBuilder.addQueryParameter(key, value)
            }
            newUrlBuilder.build().toString()
        } else {
            url
        }

        return performRequest(requestUrl, headers, asUser) { builder -> builder.get() }
    }

    suspend fun getBytes(
        url: String,
        params: Map<String, String>? = null,
        headers: Map<String, String>? = null,
        asUser: String? = AccountRepository.activeAccountIdFlow.value
    ): ApiResult<ByteArray> {
        // 构建带参数的URL
        val requestUrl = if (!params.isNullOrEmpty()) {
            val existingUrl = url.toHttpUrl()
            val newUrlBuilder = existingUrl.newBuilder()
            params.forEach { (key, value) ->
                newUrlBuilder.addQueryParameter(key, value)
            }
            newUrlBuilder.build().toString()
        } else {
            url
        }

        return performRequest(
            requestUrl,
            headers,
            asUser,
            isByteRequest = true
        ) { builder -> builder.get() }
    }

    suspend fun post(
        url: String,
        bodyMap: Map<String, Any>,
        params: Map<String, String>? = null,
        headers: Map<String, String>? = null,
        asUser: String? = AccountRepository.activeAccountIdFlow.value
    ): ApiResult<String> {
        // 构建带参数的 URL
        val requestUrl = if (!params.isNullOrEmpty()) {
            val existingUrl = url.toHttpUrl()
            val newUrlBuilder = existingUrl.newBuilder()
            params.forEach { (key, value) ->
                newUrlBuilder.addQueryParameter(key, value)
            }
            newUrlBuilder.build().toString()
        } else {
            url
        }

        return performRequest(requestUrl, headers, asUser) { builder ->
            val formBody = bodyMap.map { (key, value) ->
                "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value.toString(), "UTF-8")}"
            }.joinToString("&")
            val mediaType = "application/x-www-form-urlencoded".toMediaType()
            builder.post(formBody.toRequestBody(mediaType))
        }
    }

    suspend fun postMultipart(
        url: String,
        parts: Map<String, Any>,
        params: Map<String, String>? = null,
        headers: Map<String, String>? = null,
        asUser: String? = AccountRepository.activeAccountIdFlow.value
    ): ApiResult<String> {
        val requestUrl = if (!params.isNullOrEmpty()) {
            val urlBuilder = url.toHttpUrl().newBuilder()
            params.forEach { (k, v) -> urlBuilder.addQueryParameter(k, v) }
            urlBuilder.build().toString()
        } else {
            url
        }

        return performRequest(requestUrl, headers, asUser) { builder ->
            val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)

            parts.forEach { (key, value) ->
                when (value) {
                    is File -> {
                        val requestFile = value.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        multipartBuilder.addFormDataPart(key, value.name, requestFile)
                    }

                    else -> {
                        multipartBuilder.addFormDataPart(key, value.toString())
                    }
                }
            }
            builder.post(multipartBuilder.build())
        }
    }

    private fun getOrGenerateId(): String {
        cachedId?.let { return it }
        return synchronized(this) {
            cachedId?.let { return@synchronized it }

            val key = stringPreferencesKey("unique_device_id")
            // 此时在请求流或者具体调用中 用 runBlocking 也是在 OkHttp 的子线程 安全可控
            val savedId = runBlocking { context.networkDataStore.data.first()[key] }

            val id = if (savedId != null) {
                savedId
            } else {
                val newId = EncryptionUtils.getPlainUuid()
                runBlocking {
                    context.networkDataStore.edit { preferences ->
                        preferences[key] = newId
                    }
                }
                newId
            }
            cachedId = id
            id
        }
    }

    private fun getOrGenerateUserAgent(): String {
        cachedUserAgent?.let { return it }
        return synchronized(this) {
            cachedUserAgent?.let { return@synchronized it }

            val settingsRepository = SettingsRepository(context)
            val savedUa = runBlocking { settingsRepository.userAgent.first() }

            val ua = savedUa.ifEmpty {
                val defaultUa = generateDefaultUserAgent()
                runBlocking { settingsRepository.setUserAgent(defaultUa) }
                defaultUa
            }

            cachedUserAgent = ua
            ua
        }
    }

    private fun generateDefaultUserAgent(): String {
        val device = config["device"]!!
        val productId = config["productId"]!!
        val version = config["version"]!!
        val versionCode = config["versionCode"]!!
        val apiVersion = config["apiVersion"]!!
        val systemAgent = config["systemHttpAgent"]!!

        // 获取安全的唯一标识 ID
        val currentId = getOrGenerateId()

        val userAgentTemp =
            "(device:$device) Language/zh_CN com.chaoxing.mobile/ChaoXingStudy_${productId}_${version}_android_phone_${versionCode}_${apiVersion} (@Kalimdor)_$currentId"

        val schildInput = "(schild:${Constant.SCHILD_SALT}) $userAgentTemp"
        val schild = EncryptionUtils.md5Hash(schildInput)

        return "$systemAgent (schild:$schild) $userAgentTemp"
    }

    // 内部通用请求处理器
    private suspend fun <T> performRequest(
        url: String,
        headers: Map<String, String>?,
        asUser: String?,
        isByteRequest: Boolean = false,
        requestAction: (Request.Builder) -> Request.Builder
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder().url(url)

            requestBuilder.tag(RequestAsUser::class.java, RequestAsUser(asUser))

            headers?.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }

            val finalRequest = requestAction(requestBuilder).build()
            val response = client.newCall(finalRequest).execute()

            if (!response.isSuccessful) {
                return@withContext ApiResult.Error("HTTP Error: ${response.code}", response.code)
            }

            @Suppress("UNCHECKED_CAST")
            val result = if (isByteRequest) {
                response.body.bytes() as? T
            } else {
                response.body.string() as? T
            }

            if (result != null) {
                ApiResult.Success(result)
            } else {
                ApiResult.Error("Empty response body")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Network error")
        }
    }
}
