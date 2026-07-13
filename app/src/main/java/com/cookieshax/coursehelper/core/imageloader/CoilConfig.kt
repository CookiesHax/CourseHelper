package com.cookieshax.coursehelper.core.imageloader

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.request.CachePolicy
import com.cookieshax.coursehelper.core.network.NetworkClient
import com.cookieshax.coursehelper.core.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

object CoilConfig {
    private var imageLoader: ImageLoader? = null

    fun getImageLoader(context: Context): ImageLoader {
        return imageLoader ?: createImageLoader(context).also { imageLoader = it }
    }

    fun resetImageLoader() {
        imageLoader = null
    }

    private fun createImageLoader(context: Context): ImageLoader {
        val maxCacheSizeMb = runBlocking {
            SettingsRepository(context).maxImageCacheSize.first()
        }
        return ImageLoader.Builder(context)
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(
                        if (maxCacheSizeMb == -1) Long.MAX_VALUE
                        else maxCacheSizeMb.toLong() * 1024 * 1024
                    )
                    .build()
            }
            .okHttpClient {
                // 创建一个与 NetworkClient 具有相同配置的 OkHttpClient 实例
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val originalRequest = chain.request()
                        val requestBuilder = originalRequest.newBuilder()

                        // 添加与 NetworkClient 相同的请求头
                        requestBuilder.addHeader("User-Agent", NetworkClient.getUserAgent())
                        requestBuilder.addHeader("Accept-Language", "zh_CN")
                        requestBuilder.addHeader("Connection", "keep-alive")

                        chain.proceed(requestBuilder.build())
                    }
                    .build()
            }
            .diskCachePolicy(if (maxCacheSizeMb == 0) CachePolicy.DISABLED else CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}
