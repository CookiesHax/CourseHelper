package com.cookieshax.coursehelper.app.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cookieshax.coursehelper.app.navigation.AppNavigation
import com.cookieshax.coursehelper.core.permission.PermissionManager
import com.cookieshax.coursehelper.feature.settings.viewmodel.SettingsViewModel
import com.cookieshax.coursehelper.ui.theme.CourseHelperTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PermissionManager.init(this)

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        setContent {
            val viewModel: SettingsViewModel = viewModel()
            val isDynamicColor by viewModel.isDynamicColorEnabled.collectAsState()
            val appTheme by viewModel.appTheme.collectAsState()

            val darkTheme = when (appTheme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            CourseHelperTheme(
                darkTheme = darkTheme,
                dynamicColor = isDynamicColor,
                themeColor = viewModel.themeColor
            ) {
                AppNavigation()
            }
        }
    }
}
