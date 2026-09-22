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
import com.cookieshax.coursehelper.feature.account.model.AccountRepository
import com.cookieshax.coursehelper.feature.course.model.CourseRepository
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

        // 初始化 SDK 隐私政策
        SDKInitializer.setAgreePrivacy(applicationContext, true)
        LocationClient.setAgreePrivacy(true)

        val settings = SettingsRepository(this)

        // 异步初始化任务
        applicationScope.launch(Dispatchers.IO) {
            // 基础组件预热
            NetworkClient.preheat()
            ChaoXingAppInfo.packageName = settings.packageName.first()

            // 启动时缓存清理
            if (settings.clearCacheOnStartup.first()) {
                val days = settings.cacheExpirationDays.first()
                FileUtils.cleanupCache(applicationContext, days * 86400L)
            }

            // 课程数据预加载
            val showUnnecessary = settings.showUnnecessaryCourses.first()
            if (settings.cacheAllAccountsOnStartup.first()) {
                AccountRepository.accountList.value.forEach { account ->
                    CourseRepository.fetchCourses(account.uid, showUnnecessary)
                }
            } else {
                AccountRepository.activeAccountIdFlow.value?.let { activeId ->
                    CourseRepository.fetchCourses(activeId, showUnnecessary)
                }
            }
        }

        // 响应式配置监听
        applicationScope.launch {
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
