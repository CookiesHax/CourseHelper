package com.cookieshax.coursehelper.app.main

import android.util.Log
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.cookieshax.coursehelper.app.main.ui.components.DeleteAccountDialog
import com.cookieshax.coursehelper.app.main.ui.components.InviteCodeDialog
import com.cookieshax.coursehelper.app.main.ui.components.MainBottomBar
import com.cookieshax.coursehelper.app.main.ui.components.MainNavigationRail
import com.cookieshax.coursehelper.app.main.ui.components.MainTopAppBar
import com.cookieshax.coursehelper.app.navigation.CameraRoute
import com.cookieshax.coursehelper.app.navigation.LoginRoute
import com.cookieshax.coursehelper.app.navigation.SettingsRoute
import com.cookieshax.coursehelper.app.navigation.TagManagerRoute
import com.cookieshax.coursehelper.app.navigation.WebViewRoute
import com.cookieshax.coursehelper.core.utils.showToast
import com.cookieshax.coursehelper.feature.account.model.AccountRepository
import com.cookieshax.coursehelper.feature.account.ui.TagManagerScreen
import com.cookieshax.coursehelper.feature.account.ui.components.AccountTabContent
import com.cookieshax.coursehelper.feature.account.viewmodel.SelectionType
import com.cookieshax.coursehelper.feature.account.viewmodel.TagManagerViewModel
import com.cookieshax.coursehelper.feature.course.ui.CourseTabContent
import com.cookieshax.coursehelper.feature.course.viewmodel.CourseViewModel
import com.cookieshax.coursehelper.feature.settings.ui.SettingsScreen
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(navController: NavHostController) {
    val mainViewModel: MainViewModel = viewModel()
    val courseViewModel: CourseViewModel = viewModel()
    val tagViewModel: TagManagerViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val state = rememberMainScreenState(navController)

    val activeAccountId by mainViewModel.activeAccountId.collectAsState()
    val activeAccount by mainViewModel.activeAccount.collectAsState()
    val accounts by mainViewModel.accounts.collectAsState()
    val allAccountsId by mainViewModel.accountsId.collectAsState()

    val saveableStateHolder = rememberSaveableStateHolder()

    val searchQuery by mainViewModel.searchQuery.collectAsState()
    val debouncedSearchQuery by mainViewModel.debouncedSearchQuery.collectAsState()

    // 自适应导航逻辑
    LaunchedEffect(state.useNavRail) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        state.handleAdaptiveNavigation(currentRoute)
    }

    LaunchedEffect(Unit) {
        mainViewModel.syncAccounts()
    }

    LaunchedEffect(Unit) {
        var hasShownToast = false
        AccountRepository.expirationEvent.collect {
            if (!hasShownToast) {
                "检测到有账号已失效，请及时处理".showToast()
                hasShownToast = true
            }
        }
    }

    SharedTransitionLayout {
        Row(modifier = Modifier.fillMaxSize()) {
            if (state.useNavRail) {
                MainNavigationRail(
                    selectedTab = state.selectedTab,
                    onTabSelected = { state.updateSelectedTab(it) },
                    isRailExpanded = state.isRailExpanded,
                    onToggleRail = { state.toggleRail() }
                )
                VerticalDivider()
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                topBar = {
                    val tagSelectionMode by tagViewModel.isSelectionMode.collectAsState()
                    val selectedTagIds by tagViewModel.selectedTagIds.collectAsState()
                    val tags by tagViewModel.tagsWithAccounts.collectAsState()
                    val tagSelectionType by tagViewModel.selectionType.collectAsState()

                    MainTopAppBar(
                        selectedTab = state.selectedTab,
                        isSearching = state.isSearching,
                        onSearchStatusChange = { state.isSearching = it },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { mainViewModel.updateSearchQuery(it) },
                        onClearSearch = { mainViewModel.clearSearchQuery() },
                        searchBackProgress = state.searchBackProgress,
                        isSelectionMode = state.isSelectionMode,
                        selectedAccountCount = state.selectedIds.size,
                        totalAccountCount = accounts.size,
                        onToggleSelectAllAccounts = {
                            state.selectedIds = if (state.selectedIds.size == allAccountsId.size) emptySet() else allAccountsId.toSet()
                        },
                        onDeleteAccounts = { if (state.selectedIds.isNotEmpty()) state.showDeleteDialog = true },
                        isTagSelectionMode = tagSelectionMode,
                        selectedTagCount = selectedTagIds.size,
                        totalTagCount = tags.size,
                        selectionType = tagSelectionType,
                        onToggleSelectAllTags = {
                            tagViewModel.setSelectedTagIds(
                                if (selectedTagIds.size == tags.size) emptySet()
                                else tags.map { it.tag.tagId.toString() }.toSet()
                            )
                        },
                        onDeleteTags = { if (selectedTagIds.isNotEmpty()) tagViewModel.setShowDeleteDialog(true) },
                        onAddTag = { tagViewModel.triggerAddTag() },
                        onToggleTagSelectionType = {
                            tagViewModel.setSelectionType(
                                if (tagSelectionType == SelectionType.TAG) 
                                    SelectionType.ACCOUNT
                                else SelectionType.TAG
                            )
                        },
                        useNavRail = state.useNavRail,
                        onNavigateToTags = { navController.navigate(TagManagerRoute) },
                        onNavigateToSettings = { navController.navigate(SettingsRoute) { launchSingleTop = true } },
                        onNavigateToLogin = { loginType ->
                            navController.navigate(LoginRoute(loginType)) { launchSingleTop = true }
                        },
                        onShowAddCourseDialog = {
                            if (activeAccountId != null) state.showInviteCodeDialog = true
                            else "必须选择一个账号才能添加课程".showToast()
                        }
                    )
                },
                bottomBar = {
                    if (!state.useNavRail) {
                        MainBottomBar(
                            selectedTab = state.selectedTab,
                            onTabSelected = { state.updateSelectedTab(it) }
                        )
                    }
                },
                floatingActionButton = {
                    if (state.selectedTab != MainTab.SETTINGS) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    navController.navigate(CameraRoute) { launchSingleTop = true }
                                },
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "扫码")
                            }
                        }
                    }
                }
            ) { innerPadding ->
                val isTagSelectionMode by tagViewModel.isSelectionMode.collectAsState()
                PredictiveBackHandler(
                    enabled = state.isSearching || 
                             (state.isSelectionMode && state.selectedTab == MainTab.ACCOUNT) ||
                             (isTagSelectionMode && state.selectedTab == MainTab.TAGS)
                ) { progress ->
                    try {
                        progress.collect { backEvent ->
                            if (state.isSearching) {
                                state.searchBackProgress = backEvent.progress
                            } else if (state.isSelectionMode && state.selectedTab == MainTab.ACCOUNT) {
                                state.selectBackProgress = backEvent.progress
                            }
                        }

                        if (state.isSearching) {
                            state.isSearching = false
                            mainViewModel.clearSearchQuery()
                            state.searchBackProgress = 0f
                        } else if (state.isSelectionMode && state.selectedTab == MainTab.ACCOUNT) {
                            state.isSelectionMode = false
                            state.selectedIds = emptySet()
                            state.selectBackProgress = 0f
                        } else if (isTagSelectionMode && state.selectedTab == MainTab.TAGS) {
                            tagViewModel.setSelectionMode(false)
                        }
                    } catch (_: CancellationException) {
                        state.searchBackProgress = 0f
                        state.selectBackProgress = 0f
                    }
                }

                AnimatedContent(
                    targetState = state.selectedTab,
                    transitionSpec = {
                        fadeIn(tween(100)) togetherWith fadeOut(tween(100))
                    },
                    label = "tab_content_transition",
                    modifier = Modifier.padding(innerPadding)
                ) { tab ->
                    when (tab) {
                        MainTab.COURSE -> {
                            saveableStateHolder.SaveableStateProvider(key = "course_tab") {
                                CourseTabContent(
                                    activeAccountId,
                                    navController = navController,
                                    searchQuery = debouncedSearchQuery,
                                    viewModel = courseViewModel
                                )
                            }
                        }

                        MainTab.ACCOUNT -> {
                            saveableStateHolder.SaveableStateProvider(key = "account_tab") {
                                AccountTabContent(
                                    accounts = accounts,
                                    activeAccountId = activeAccountId,
                                    onAccountClick = { id ->
                                        Log.d("MainScreen", "用户点击选择账号 ID: $id")
                                        scope.launch {
                                            AccountRepository.switchActiveAccount(id)
                                            Log.d("MainScreen", "账号选择已更新到 AccountRepository: $id")
                                        }
                                    },
                                    onMove = { fromIndex, toIndex ->
                                        val currentList = accounts.toMutableList()
                                        val movedItem = currentList.removeAt(fromIndex)
                                        currentList.add(toIndex, movedItem)
                                        scope.launch { AccountRepository.reorderAccounts(currentList) }
                                    },
                                    searchQuery = debouncedSearchQuery,
                                    isSelectionMode = state.isSelectionMode,
                                    selectedIds = state.selectedIds,
                                    onSelectionModeChanged = { state.isSelectionMode = it },
                                    onSelectedIdsChanged = { state.selectedIds = it }
                                )
                            }
                        }

                        MainTab.TAGS -> {
                            saveableStateHolder.SaveableStateProvider(key = "tags_tab") {
                                TagManagerScreen(
                                    navController = navController,
                                    showBackButton = false
                                )
                            }
                        }

                        MainTab.SETTINGS -> {
                            saveableStateHolder.SaveableStateProvider(key = "settings_tab") {
                                SettingsScreen(
                                    navController = navController,
                                    showBackButton = false
                                )
                            }
                        }
                    }
                }

                if (state.showDeleteDialog) {
                    DeleteAccountDialog(
                        selectedCount = state.selectedIds.size,
                        onConfirm = {
                            scope.launch {
                                state.selectedIds.forEach { id -> AccountRepository.removeAccount(id) }
                                state.isSelectionMode = false
                                state.selectedIds = emptySet()
                                state.showDeleteDialog = false
                            }
                        },
                        onDismiss = { state.showDeleteDialog = false }
                    )
                }

                if (state.showInviteCodeDialog) {
                    InviteCodeDialog(
                        userName = activeAccount?.name ?: "未知用户",
                        onConfirm = { code ->
                            if (activeAccountId != null) {
                                navController.navigate(
                                    WebViewRoute(
                                        "https://mooc1-api.chaoxing.com/teachingClassPhoneManage/phone/toParticipateCls?inviteCode=$code"
                                    )
                                )
                            }
                            state.showInviteCodeDialog = false
                        },
                        onDismiss = { state.showInviteCodeDialog = false }
                    )
                }
            }
        }
    }
}
