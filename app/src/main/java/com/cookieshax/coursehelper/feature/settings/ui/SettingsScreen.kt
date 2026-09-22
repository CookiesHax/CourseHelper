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
import com.cookieshax.coursehelper.feature.settings.ui.components.AppSection
import com.cookieshax.coursehelper.feature.settings.ui.components.CourseSection
import com.cookieshax.coursehelper.feature.settings.ui.components.PersonalizationSection
import com.cookieshax.coursehelper.feature.settings.ui.components.StorageSection
import com.cookieshax.coursehelper.feature.settings.ui.dialogs.AppThemeDialog
import com.cookieshax.coursehelper.feature.settings.ui.dialogs.CacheExpirationDaysDialog
import com.cookieshax.coursehelper.feature.settings.ui.dialogs.CheckInAccountSelectionModeDialog
import com.cookieshax.coursehelper.feature.settings.ui.dialogs.DeviceIdDialog
import com.cookieshax.coursehelper.feature.settings.ui.dialogs.LocationMethodDialog
import com.cookieshax.coursehelper.feature.settings.ui.dialogs.LoginEndpointDialog
import com.cookieshax.coursehelper.feature.settings.ui.dialogs.PackageNameDialog
import com.cookieshax.coursehelper.feature.settings.ui.dialogs.SliderEditDialog
import com.cookieshax.coursehelper.feature.settings.ui.dialogs.StepSliderEditDialog
import com.cookieshax.coursehelper.feature.settings.ui.dialogs.ThemeColorDialog
import com.cookieshax.coursehelper.feature.settings.ui.dialogs.UserAgentDialog
import com.cookieshax.coursehelper.feature.settings.viewmodel.SettingsDialogOpen
import com.cookieshax.coursehelper.feature.settings.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    showBackButton: Boolean = true
) {
    val isNavigating = remember { mutableStateOf(false) }
    val viewModel: SettingsViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val activeDialog by viewModel.activeDialog.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshCacheSize()
    }

    if (!uiState.isReady) return

    when (activeDialog) {
        SettingsDialogOpen.CACHE_EXPIRATION_DAYS -> {
            CacheExpirationDaysDialog(
                currentDays = uiState.cacheExpirationDays,
                onDismissRequest = { viewModel.setActiveDialog(null) },
                onConfirm = {
                    viewModel.setCacheExpirationDays(it)
                    viewModel.setActiveDialog(null)
                }
            )
        }

        SettingsDialogOpen.LOGIN_ENDPOINT -> {
            LoginEndpointDialog(
                currentEndpoint = uiState.loginEndpoint,
                onDismissRequest = { viewModel.setActiveDialog(null) },
                onConfirm = {
                    viewModel.setLoginEndpoint(it)
                    viewModel.setActiveDialog(null)
                }
            )
        }

        SettingsDialogOpen.APP_THEME -> {
            AppThemeDialog(
                currentTheme = uiState.appTheme,
                onDismissRequest = { viewModel.setActiveDialog(null) },
                onConfirm = {
                    viewModel.setAppTheme(it)
                    viewModel.setActiveDialog(null)
                }
            )
        }

        SettingsDialogOpen.THEME_COLOR -> {
            ThemeColorDialog(
                currentThemeColor = uiState.themeColor,
                onDismissRequest = { viewModel.setActiveDialog(null) },
                onConfirm = {
                    viewModel.setThemeColor(it)
                    viewModel.setActiveDialog(null)
                }
            )
        }

        SettingsDialogOpen.USER_AGENT -> {
            UserAgentDialog(
                currentUA = uiState.userAgent,
                onDismissRequest = { viewModel.setActiveDialog(null) },
                onConfirm = {
                    viewModel.setUserAgent(it)
                    viewModel.setActiveDialog(null)
                }
            )
        }

        SettingsDialogOpen.PACKAGE_NAME -> {
            PackageNameDialog(
                currentPackageName = uiState.packageName,
                onDismissRequest = { viewModel.setActiveDialog(null) },
                onConfirm = {
                    viewModel.setPackageName(it)
                    viewModel.setActiveDialog(null)
                }
            )
        }

        SettingsDialogOpen.CHECK_IN_SEMAPHORE -> {
            SliderEditDialog(
                title = "签到并发数",
                value = uiState.checkInSemaphoreLimit,
                onDismissRequest = { viewModel.setActiveDialog(null) },
                onConfirm = {
                    viewModel.setCheckInSemaphoreLimit(it)
                    viewModel.setActiveDialog(null)
                }
            )
        }

        SettingsDialogOpen.MAX_CAPTCHA_RETRIES -> {
            SliderEditDialog(
                title = "自动Captcha最大重试次数",
                value = uiState.maxCaptchaRetries,
                onDismissRequest = { viewModel.setActiveDialog(null) },
                onConfirm = {
                    viewModel.setMaxCaptchaRetries(it)
                    viewModel.setActiveDialog(null)
                }
            )
        }

        SettingsDialogOpen.MAX_IMAGE_CACHE_SIZE -> {
            StepSliderEditDialog(
                title = "图片缓存上限",
                value = uiState.maxImageCacheSize,
                steps = listOf(0, 8, 16, 32, 64, 128, 256, -1),
                valueLabel = { valMb ->
                    when (valMb) {
                        -1 -> "无限制"
                        0 -> "不缓存"
                        else -> "$valMb MB"
                    }
                },
                onDismissRequest = { viewModel.setActiveDialog(null) },
                onConfirm = {
                    viewModel.setMaxImageCacheSize(it)
                    viewModel.setActiveDialog(null)
                }
            )
        }

        SettingsDialogOpen.DEVICE_ID -> {
            DeviceIdDialog(
                currentDeviceId = uiState.deviceId,
                onDismissRequest = { viewModel.setActiveDialog(null) },
                onConfirm = {
                    viewModel.updateDeviceId(it)
                    viewModel.setActiveDialog(null)
                }
            )
        }

        SettingsDialogOpen.LOCATION_METHOD -> {
            LocationMethodDialog(
                currentMethod = uiState.locationMethod,
                onDismissRequest = { viewModel.setActiveDialog(null) },
                onConfirm = {
                    viewModel.setLocationMethod(it)
                    viewModel.setActiveDialog(null)
                }
            )
        }

        SettingsDialogOpen.CHECK_IN_ACCOUNT_SELECTION_MODE -> {
            CheckInAccountSelectionModeDialog(
                currentMode = uiState.checkInAccountSelectionMode,
                onDismissRequest = { viewModel.setActiveDialog(null) },
                onConfirm = {
                    viewModel.setCheckInAccountSelectionMode(it)
                    viewModel.setActiveDialog(null)
                }
            )
        }

        else -> {}
    }

    val content = @Composable { modifier: Modifier ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PersonalizationSection(
                uiState = uiState,
                onToggleDynamicColor = { viewModel.toggleDynamicColor(it) },
                onOpenDialog = { viewModel.setActiveDialog(it) }
            )

            CourseSection(
                uiState = uiState,
                onTogglePreferOkHttp = { viewModel.togglePreferOkHttp(it) },
                onToggleCheckInSelectAllOnScan = { viewModel.toggleCheckInSelectAllOnScan(it) },
                onSetCheckInSemaphoreLimit = { viewModel.setCheckInSemaphoreLimit(it) },
                onToggleOpencvForCaptcha = { viewModel.toggleOpencvForCaptcha(it) },
                onSetMaxCaptchaRetries = { viewModel.setMaxCaptchaRetries(it) },
                onToggleShowUnsupportedTasks = { viewModel.toggleShowUnsupportedTasks(it) },
                onToggleShowUnnecessaryCourses = { viewModel.toggleShowUnnecessaryCourses(it) },
                onToggleCacheAllAccountsOnStartup = { viewModel.toggleCacheAllAccountsOnStartup(it) },
                onToggleExcludeCheckedInAccounts = { viewModel.toggleExcludeCheckedInAccounts(it) },
                onOpenDialog = { viewModel.setActiveDialog(it) }
            )

            AppSection(
                uiState = uiState,
                onOpenDialog = { viewModel.setActiveDialog(it) }
            )

            StorageSection(
                uiState = uiState,
                onOpenDialog = { viewModel.setActiveDialog(it) },
                onToggleClearCacheOnStartup = { viewModel.toggleClearCacheOnStartup(it) },
                onClearCache = { viewModel.clearCache() },
                onSetMaxImageCacheSize = { viewModel.setMaxImageCacheSize(it) }
            )

            AboutSection()

            Spacer(
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        if (!showBackButton) {
            content(Modifier)
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("设置") },
                        navigationIcon = {
                            if (showBackButton) {
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
                        }
                    )
                }
            ) { innerPadding ->
                content(Modifier.padding(top = innerPadding.calculateTopPadding()))
            }
        }
    }
}
