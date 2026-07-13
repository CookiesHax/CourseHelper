package com.cookieshax.coursehelper.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cookieshax.coursehelper.feature.settings.ui.components.AboutSection
import com.cookieshax.coursehelper.feature.settings.viewmodel.SettingsDialogOpen
import com.cookieshax.coursehelper.feature.settings.viewmodel.SettingsViewModel
import com.cookieshax.coursehelper.feature.settings.ui.components.AppSection
import com.cookieshax.coursehelper.feature.settings.ui.components.CourseSection
import com.cookieshax.coursehelper.feature.settings.ui.components.WebSection
import com.cookieshax.coursehelper.feature.settings.ui.components.PersonalizationSection
import com.cookieshax.coursehelper.feature.settings.ui.dialogs.AppThemeDialog
import com.cookieshax.coursehelper.feature.settings.ui.dialogs.CacheExpirationDaysDialog
import com.cookieshax.coursehelper.feature.settings.ui.dialogs.LoginEndpointDialog
import com.cookieshax.coursehelper.feature.settings.ui.dialogs.ThemeColorDialog
import com.cookieshax.coursehelper.feature.settings.ui.dialogs.UserAgentDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val isNavigating = remember { mutableStateOf(false) }
    val viewModel: SettingsViewModel = viewModel()
    val isReady by viewModel.isReady.collectAsState()
    val activeDialog by viewModel.activeDialog.collectAsState()
    val isDynamicColorEnabled by viewModel.isDynamicColorEnabled.collectAsState()
    val preferOkHttpOverWebView by viewModel.preferOkHttpOverWebView.collectAsState()
    val clearCacheOnStartup by viewModel.clearCacheOnStartup.collectAsState()
    val cacheExpirationDays by viewModel.cacheExpirationDays.collectAsState()
    val loginEndpoint by viewModel.loginEndpoint.collectAsState()
    val checkInSemaphoreLimit by viewModel.checkInSemaphoreLimit.collectAsState()
    val isOpencvEnabledForCaptcha by viewModel.isOpencvEnabledForCaptcha.collectAsState()
    val maxCaptchaRetries by viewModel.maxCaptchaRetries.collectAsState()
    val isCheckInDefaultSelectAll by viewModel.isCheckInDefaultSelectAll.collectAsState()
    val cacheSize by viewModel.cacheSize.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val themeColor by viewModel.themeColor.collectAsState()
    val showUnsupportedTasks by viewModel.showUnsupportedTasks.collectAsState()
    val showUnnecessaryCourses by viewModel.showUnnecessaryCourses.collectAsState()
    val maxImageCacheSize by viewModel.maxImageCacheSize.collectAsState()
    val userAgent by viewModel.userAgent.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshCacheSize()
    }

    if (!isReady) return

    when (activeDialog) {
        SettingsDialogOpen.CACHE_EXPIRATION_DAYS -> {
            CacheExpirationDaysDialog(
                currentDays = cacheExpirationDays,
                onDismissRequest = { viewModel.setActiveDialog(null) },
                onConfirm = {
                    viewModel.setCacheExpirationDays(it)
                    viewModel.setActiveDialog(null)
                }
            )
        }

        SettingsDialogOpen.LOGIN_ENDPOINT -> {
            LoginEndpointDialog(
                currentEndpoint = loginEndpoint,
                onDismissRequest = { viewModel.setActiveDialog(null) },
                onConfirm = {
                    viewModel.setLoginEndpoint(it)
                    viewModel.setActiveDialog(null)
                }
            )
        }

        SettingsDialogOpen.APP_THEME -> {
            AppThemeDialog(
                currentTheme = appTheme,
                onDismissRequest = { viewModel.setActiveDialog(null) },
                onConfirm = {
                    viewModel.setAppTheme(it)
                    viewModel.setActiveDialog(null)
                }
            )
        }

        SettingsDialogOpen.THEME_COLOR -> {
            ThemeColorDialog(
                currentThemeColor = themeColor,
                onDismissRequest = { viewModel.setActiveDialog(null) },
                onConfirm = {
                    viewModel.setThemeColor(it)
                    viewModel.setActiveDialog(null)
                }
            )
        }

        SettingsDialogOpen.USER_AGENT -> {
            UserAgentDialog(
                currentUA = userAgent,
                onDismissRequest = { viewModel.setActiveDialog(null) },
                onConfirm = {
                    viewModel.setUserAgent(it)
                    viewModel.setActiveDialog(null)
                }
            )
        }

        else -> {}
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("设置") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (!isNavigating.value) {
                                    isNavigating.value = true
                                    navController.popBackStack()
                                }
                            },
                            enabled = !isNavigating.value
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 个性化
                PersonalizationSection(
                    isDynamicColorEnabled = isDynamicColorEnabled,
                    appTheme = appTheme,
                    themeColor = themeColor,
                    onToggleDynamicColor = { viewModel.toggleDynamicColor(it) },
                    onOpenDialog = { viewModel.setActiveDialog(it) }
                )

                // 课程
                CourseSection(
                    preferOkHttpOverWebView = preferOkHttpOverWebView,
                    isCheckInDefaultSelectAll = isCheckInDefaultSelectAll,
                    checkInSemaphoreLimit = checkInSemaphoreLimit,
                    isOpencvEnabledForCaptcha = isOpencvEnabledForCaptcha,
                    maxCaptchaRetries = maxCaptchaRetries,
                    showUnsupportedTasks = showUnsupportedTasks,
                    showUnnecessaryCourses = showUnnecessaryCourses,
                    onTogglePreferOkHttp = { viewModel.togglePreferOkHttp(it) },
                    onToggleCheckInDefaultSelectAll = { viewModel.toggleCheckInDefaultSelectAll(it) },
                    onSetCheckInSemaphoreLimit = { viewModel.setCheckInSemaphoreLimit(it) },
                    onToggleOpencvForCaptcha = { viewModel.toggleOpencvForCaptcha(it) },
                    onSetMaxCaptchaRetries = { viewModel.setMaxCaptchaRetries(it) },
                    onToggleShowUnsupportedTasks = { viewModel.toggleShowUnsupportedTasks(it) },
                    onToggleShowUnnecessaryCourses = { viewModel.toggleShowUnnecessaryCourses(it) }
                )

                // 网络
                WebSection(
                    userAgent = userAgent,
                    loginEndpoint = loginEndpoint,
                    onOpenDialog = { viewModel.setActiveDialog(it) }
                )

                // 应用
                AppSection(
                    clearCacheOnStartup = clearCacheOnStartup,
                    cacheExpirationDays = cacheExpirationDays,
                    cacheSize = cacheSize,
                    maxImageCacheSize = maxImageCacheSize,
                    onOpenDialog = { viewModel.setActiveDialog(it) },
                    onToggleClearCacheOnStartup = { viewModel.toggleClearCacheOnStartup(it) },
                    onClearCache = { viewModel.clearCache() },
                    onSetMaxImageCacheSize = { viewModel.setMaxImageCacheSize(it) }
                )

                // 关于
                AboutSection()

                Spacer(
                    modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding() + 16.dp)
                )
            }
        }
    }
}
