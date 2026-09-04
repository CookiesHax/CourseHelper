package com.cookieshax.coursehelper.app

import android.app.Application
import android.content.Context
import android.webkit.WebView
import com.baidu.location.LocationClient
import com.baidu.mapapi.SDKInitializer
import com.cookieshax.coursehelper.BuildConfig
import com.cookieshax.coursehelper.core.info.ChaoXingAppInfo
import com.cookieshax.coursehelper.core.location.LocationMethod
import com.cookieshax.coursehelper.core.location.LocationService
import com.cookieshax.coursehelper.core.network.NetworkClient
import com.cookieshax.coursehelper.core.repository.SettingsRepository
import com.cookieshax.coursehelper.core.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CourseHelperApplication : Application() {
    companion object {
        // 使用 @JvmStatic 让 Java 也能直接通过 CourseHelperApplication.getInstance() 访问
        @JvmStatic
        lateinit var instance: CourseHelperApplication
            private set

        @JvmStatic
        val context: Context get() = instance.applicationContext

        @JvmStatic
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 必须在任何百度 SDK 功能初始化之前同意隐私政策
        SDKInitializer.setAgreePrivacy(applicationContext, true) // 地图 SDK 隐私政策
        LocationClient.setAgreePrivacy(true) // 定位 SDK 隐私政策

        // 异步初始化
        applicationScope.launch(Dispatchers.IO) {
            NetworkClient.preheat()
            val settings = SettingsRepository(applicationContext)

            ChaoXingAppInfo.packageName = settings.packageName.first()

            if (settings.clearCacheOnStartup.first()) {
                val days = settings.cacheExpirationDays.first()
                FileUtils.cleanupCache(applicationContext, days * 86400L)
            }
        }

        applicationScope.launch {
            val settings = SettingsRepository(applicationContext)
            settings.locationMethod.collect { methodStr ->
                val method = try {
                    LocationMethod.valueOf(methodStr)
                } catch (_: Exception) {
                    LocationMethod.BAIDU
                }
                LocationService.setLocationMethod(method)
            }
        }

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
    }
}
