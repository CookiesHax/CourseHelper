package com.cookieshax.coursehelper.core.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_manager")

class SettingsRepository(private val context: Context) {
    private val dynamicColorKey = booleanPreferencesKey("use_dynamic_color")
    private val preferOkHttpKey = booleanPreferencesKey("prefer_okhttp_over_webview")
    private val clearCacheOnStartupKey = booleanPreferencesKey("clear_cache_on_startup")
    private val cacheExpirationDaysKey = intPreferencesKey("cache_expiration_days")
    private val loginEndpointKey = stringPreferencesKey("login_endpoint")
    private val checkInSemaphoreLimitKey = intPreferencesKey("check_in_semaphore_limit")
    private val checkInDefaultSelectAllKey = booleanPreferencesKey("check_in_default_select_all")
    private val appThemeKey = stringPreferencesKey("app_theme")
    private val themeColorKey = stringPreferencesKey("theme_color")
    private val isOpencvEnabledForCaptchaKey =
        booleanPreferencesKey("is_opencv_enabled_for_captcha")
    private val maxCaptchaRetriesKey = intPreferencesKey("max_captcha_retries")
    private val showUnsupportedTasksKey = booleanPreferencesKey("show_unsupported_activities")
    private val showUnnecessaryCoursesKey = booleanPreferencesKey("show_unnecessary_courses")
    private val maxImageCacheSizeKey = intPreferencesKey("max_image_cache_size")
    private val userAgentKey = stringPreferencesKey("user_agent")

    val userAgent: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[userAgentKey] ?: ""
        }

    val isDynamicColorEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[dynamicColorKey] != false
        }

    val preferOkHttpOverWebView: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[preferOkHttpKey] != false
        }

    val clearCacheOnStartup: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[clearCacheOnStartupKey] == true
        }

    val cacheExpirationDays: Flow<Int> = context.settingsDataStore.data
        .map { preferences ->
            preferences[cacheExpirationDaysKey] ?: 7
        }

    val loginEndpoint: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[loginEndpointKey] ?: "web"
        }

    val checkInSemaphoreLimit: Flow<Int> = context.settingsDataStore.data
        .map { preferences ->
            preferences[checkInSemaphoreLimitKey] ?: 6
        }

    val isOpencvEnabledForCaptcha: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[isOpencvEnabledForCaptchaKey] != false
        }

    val maxCaptchaRetries: Flow<Int> = context.settingsDataStore.data
        .map { preferences ->
            preferences[maxCaptchaRetriesKey] ?: 3
        }

    val isCheckInDefaultSelectAll: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[checkInDefaultSelectAllKey] != false
        }

    val appTheme: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[appThemeKey] ?: "system"
        }

    val themeColor: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[themeColorKey] ?: "#769CDF"
        }

    val showUnsupportedTasks: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[showUnsupportedTasksKey] != false
        }

    val showUnnecessaryCourses: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[showUnnecessaryCoursesKey] == true
        }

    val maxImageCacheSize: Flow<Int> = context.settingsDataStore.data
        .map { preferences ->
            preferences[maxImageCacheSizeKey] ?: 64
        }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[dynamicColorKey] = enabled
        }
    }

    suspend fun setPreferOkHttpOverWebView(prefer: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[preferOkHttpKey] = prefer
        }
    }

    suspend fun setClearCacheOnStartup(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[clearCacheOnStartupKey] = enabled
        }
    }

    suspend fun setCacheExpirationDays(days: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[cacheExpirationDaysKey] = days
        }
    }

    suspend fun setLoginEndpoint(endpoint: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[loginEndpointKey] = endpoint
        }
    }

    suspend fun setCheckInSemaphoreLimit(limit: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[checkInSemaphoreLimitKey] = limit
        }
    }

    suspend fun setOpencvEnabledForCaptcha(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[isOpencvEnabledForCaptchaKey] = enabled
        }
    }

    suspend fun setMaxCaptchaRetries(retries: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[maxCaptchaRetriesKey] = retries
        }
    }

    suspend fun setCheckInDefaultSelectAll(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[checkInDefaultSelectAllKey] = enabled
        }
    }

    suspend fun setAppTheme(theme: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[appThemeKey] = theme
        }
    }

    suspend fun setThemeColor(color: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[themeColorKey] = color
        }
    }

    suspend fun setShowUnsupportedTasks(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[showUnsupportedTasksKey] = enabled
        }
    }

    suspend fun setShowUnnecessaryCourses(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[showUnnecessaryCoursesKey] = enabled
        }
    }

    suspend fun setMaxImageCacheSize(size: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[maxImageCacheSizeKey] = size
        }
    }

    suspend fun setUserAgent(userAgent: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[userAgentKey] = userAgent
        }
    }
}
