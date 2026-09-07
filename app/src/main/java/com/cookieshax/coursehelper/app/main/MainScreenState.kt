package com.cookieshax.coursehelper.app.main

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavHostController
import com.cookieshax.coursehelper.app.navigation.SettingsRoute
import com.cookieshax.coursehelper.app.navigation.TagManagerRoute

@Stable
class MainScreenState(
    val navController: NavHostController,
    val configuration: Configuration,
    initialSelectedTab: MainTab = MainTab.COURSE,
    initialIsSearching: Boolean = false,
    initialIsSelectionMode: Boolean = false,
    initialIsRailExpanded: Boolean = false
) {
    // Tab 状态
    var selectedTab by mutableStateOf(initialSelectedTab)
    var lastMainTab by mutableStateOf(initialSelectedTab)

    // 搜索状态
    var isSearching by mutableStateOf(initialIsSearching)
    var searchBackProgress by mutableFloatStateOf(0f)

    // 选择模式状态 (账号)
    var isSelectionMode by mutableStateOf(initialIsSelectionMode)
    var selectedIds by mutableStateOf(setOf<String>())
    var selectBackProgress by mutableFloatStateOf(0f)

    // UI 状态
    var isRailExpanded by mutableStateOf(initialIsRailExpanded)
    var showDeleteDialog by mutableStateOf(false)
    var showInviteCodeDialog by mutableStateOf(false)

    // 屏幕适配逻辑
    val isLandscape get() = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet get() = configuration.smallestScreenWidthDp >= 600
    val useNavRail get() = isLandscape && isTablet

    fun updateSelectedTab(tab: MainTab) {
        selectedTab = tab
        if (tab == MainTab.COURSE || tab == MainTab.ACCOUNT) {
            lastMainTab = tab
        }
    }

    fun toggleRail() {
        isRailExpanded = !isRailExpanded
    }

    fun handleAdaptiveNavigation(currentRoute: String?) {
        if (useNavRail) {
            if (currentRoute?.contains("TagManagerRoute") == true) {
                navController.popBackStack()
                selectedTab = MainTab.TAGS
            } else if (currentRoute?.contains("SettingsRoute") == true) {
                navController.popBackStack()
                selectedTab = MainTab.SETTINGS
            }
        } else {
            if (selectedTab == MainTab.TAGS) {
                selectedTab = lastMainTab
                navController.navigate(TagManagerRoute)
            } else if (selectedTab == MainTab.SETTINGS) {
                selectedTab = lastMainTab
                navController.navigate(SettingsRoute)
            }
        }
    }
}

@Composable
fun rememberMainScreenState(
    navController: NavHostController,
    configuration: Configuration = LocalConfiguration.current
): MainScreenState {
    return rememberSaveable(
        navController, configuration,
        saver = listSaver(
            save = {
                listOf(
                    it.selectedTab.name,
                    it.isSearching,
                    it.isSelectionMode,
                    it.isRailExpanded,
                    it.lastMainTab.name
                )
            },
            restore = {
                MainScreenState(
                    navController = navController,
                    configuration = configuration,
                    initialSelectedTab = MainTab.valueOf(it[0] as String),
                    initialIsSearching = it[1] as Boolean,
                    initialIsSelectionMode = it[2] as Boolean,
                    initialIsRailExpanded = it[3] as Boolean
                ).apply {
                    lastMainTab = MainTab.valueOf(it[4] as String)
                }
            }
        )
    ) {
        MainScreenState(navController, configuration)
    }
}
