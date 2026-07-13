package com.cookieshax.coursehelper.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.rememberDynamicColorScheme
import kotlinx.coroutines.flow.StateFlow

@Composable
fun CourseHelperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    themeColor: StateFlow<String>?,
    content: @Composable () -> Unit
) {
    val colorHex by (themeColor ?: remember {
        kotlinx.coroutines.flow.MutableStateFlow("#769CDF")
    }).collectAsState()

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> {
            val cleanHex = colorHex.removePrefix("#").trim()
            val argbColor = when (cleanHex.length) {
                6 -> "FF$cleanHex".toLong(16)
                8 -> cleanHex.toLong(16)
                else -> 0xFF769CDF
            }

            rememberDynamicColorScheme(
                seedColor = Color(argbColor),
                isDark = darkTheme
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
