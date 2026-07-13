package com.cookieshax.coursehelper.app.main

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cookieshax.coursehelper.app.navigation.CameraRoute
import com.cookieshax.coursehelper.app.navigation.LoginRoute
import com.cookieshax.coursehelper.app.navigation.SettingsRoute
import com.cookieshax.coursehelper.app.navigation.TagManagerRoute
import com.cookieshax.coursehelper.feature.account.model.AccountRepository
import com.cookieshax.coursehelper.feature.account.ui.components.AccountTabContent
import com.cookieshax.coursehelper.feature.course.ui.CourseTabContent
import com.cookieshax.coursehelper.feature.login.LoginType
import com.cookieshax.coursehelper.ui.items.IcTags
import com.cookieshax.coursehelper.ui.items.SearchInput
import com.cookieshax.coursehelper.ui.items.SearchTrigger
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

enum class MainTab {
    COURSE,
    ACCOUNT
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(navController: NavController) {
    val mainViewModel: MainViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val activeAccountId by mainViewModel.activeAccountId.collectAsState()
    val accounts by mainViewModel.accounts.collectAsState()
    val allAccountsId by mainViewModel.accountsId.collectAsState()

    var selectedTab by rememberSaveable { mutableStateOf(MainTab.COURSE) }
    val saveableStateHolder = rememberSaveableStateHolder() // 用于保存和恢复页面状态

    var showLoginMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var selectBackProgress by remember { mutableFloatStateOf(0f) }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    val searchQuery by mainViewModel.searchQuery.collectAsState()
    val debouncedSearchQuery by mainViewModel.debouncedSearchQuery.collectAsState()
    var searchBackProgress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        mainViewModel.syncAccounts()
    }

    LaunchedEffect(Unit) {
        var hasShownToast = false
        AccountRepository.expirationEvent.collect {
            if (!hasShownToast) {
                Toast.makeText(context, "检测到有账号已失效，请及时处理", Toast.LENGTH_SHORT).show()
                hasShownToast = true
            }
        }
    }

    // UI
    SharedTransitionLayout {
        Scaffold(
            // TopAppBar
            topBar = {
                AnimatedContent(
                    targetState = isSearching,
                    transitionSpec = {
                        fadeIn(tween(100)) togetherWith fadeOut(tween(100))
                    },
                    label = "top_bar_transition"
                ) { searching ->
                    if (searching) {
                        TopAppBar(
                            title = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(end = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SearchInput(
                                        query = searchQuery,
                                        onQueryChange = { mainViewModel.updateSearchQuery(it) },
                                        onClose = {
                                            isSearching = false // 关闭搜索状态
                                            mainViewModel.clearSearchQuery() // 并清除搜索框文字
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .graphicsLayer {
                                                val scale = 1f - (searchBackProgress * 0.08f)
                                                scaleX = scale
                                                scaleY = scale
                                                alpha =
                                                    1f - (searchBackProgress * 2f).coerceAtMost(1f)
                                            },
                                        hint = if (selectedTab == MainTab.COURSE) "搜索课程..." else "搜索账号...",
                                        animatedVisibilityScope = this@AnimatedContent
                                    )
                                }
                            }
                        )
                    } else {
                        TopAppBar(
                            title = {
                                if (isSelectionMode && selectedTab == MainTab.ACCOUNT) {
                                    Text(
                                        text = "${selectedIds.size} / ${accounts.size}",
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                } else {
                                    Text(
                                        text = "CourseHelper",
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                }
                            },
                            actions = {
                                // 标签管理
                                IconButton(
                                    onClick = { navController.navigate(TagManagerRoute) }
                                ) {
                                    Icon(
                                        imageVector = IcTags,
                                        contentDescription = "标签"
                                    )
                                }

                                // 搜索
                                SearchTrigger(
                                    onClick = { isSearching = true },
                                    animatedVisibilityScope = this@AnimatedContent
                                )

                                if (selectedTab == MainTab.COURSE || !isSelectionMode) {
                                    Box {
                                        IconButton(onClick = { showLoginMenu = true }) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "添加"
                                            )
                                        }

                                        DropdownMenu(
                                            expanded = showLoginMenu,
                                            onDismissRequest = { showLoginMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("密码登录") },
                                                onClick = {
                                                    showLoginMenu = false
                                                    navController.navigate(LoginRoute(LoginType.PASSWORD)) {
                                                        launchSingleTop = true
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("验证码登录") },
                                                onClick = {
                                                    showLoginMenu = false
                                                    navController.navigate(LoginRoute(LoginType.VERIFICATION_CODE)) {
                                                        launchSingleTop = true
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("二维码登录") },
                                                onClick = {
                                                    showLoginMenu = false
                                                    navController.navigate(LoginRoute(LoginType.QRCODE)) {
                                                        launchSingleTop = true
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                if (isSelectionMode && selectedTab == MainTab.ACCOUNT) {
                                    if (selectedIds.size == AccountRepository.accountsSizeFlow.collectAsState().value) {
                                        IconButton(onClick = {
                                            selectedIds = setOf()
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Deselect,
                                                contentDescription = "取消全选"
                                            )
                                        }
                                    } else {
                                        IconButton(onClick = {
                                            selectedIds = allAccountsId.toSet()
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.SelectAll,
                                                contentDescription = "全选"
                                            )
                                        }
                                    }
                                }

                                if (isSelectionMode && selectedTab == MainTab.ACCOUNT) {
                                    // 删除按钮
                                    IconButton(onClick = {
                                        if (selectedIds.isNotEmpty()) showDeleteDialog = true
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "删除",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else {
                                    // 设置
                                    IconButton(
                                        onClick = {
                                            navController.navigate(SettingsRoute) {
                                                launchSingleTop = true
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = "设置")
                                    }
                                }
                            }
                        )
                    }
                }
            },
            // 底部导航栏
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == MainTab.COURSE,
                        onClick = { selectedTab = MainTab.COURSE },
                        icon = {
                            Icon(
                                Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null
                            )
                        },
                        label = { Text("课程") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == MainTab.ACCOUNT,
                        onClick = { selectedTab = MainTab.ACCOUNT },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("账号") }
                    )
                }
            },
            // FloatingActionButton
            floatingActionButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    FloatingActionButton(
                        onClick = {
                            navController.navigate(CameraRoute) {
                                launchSingleTop = true
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "扫码")
                    }
                }
            }) { innerPadding ->
            PredictiveBackHandler(
                enabled = isSearching || (isSelectionMode && selectedTab == MainTab.ACCOUNT)
            ) { progress ->
                try {
                    progress.collect { backEvent ->
                        if (isSearching) {
                            searchBackProgress = backEvent.progress
                        } else if (isSelectionMode && selectedTab == MainTab.ACCOUNT) {
                            selectBackProgress = backEvent.progress
                        }
                    }

                    if (isSearching) {
                        // 如果正在搜索 先退出搜索
                        isSearching = false
                        mainViewModel.clearSearchQuery()
                        searchBackProgress = 0f
                    } else if (isSelectionMode && selectedTab == MainTab.ACCOUNT) {
                        // 关闭搜索状态下 如果在账号页选择模式 则退出选择模式
                        isSelectionMode = false
                        selectedIds = emptySet()
                        selectBackProgress = 0f
                    }
                } catch (_: CancellationException) {
                    searchBackProgress = 0f
                    selectBackProgress = 0f
                }
            }

            // 根据选中的Tab显示不同内容
            AnimatedContent(
                targetState = selectedTab,
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
                                searchQuery = debouncedSearchQuery
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
                                        AccountRepository.switchActiveAccount(id) // 更新 AccountRepository 中的当前选中账号
                                        Log.d(
                                            "MainScreen",
                                            "账号选择已更新到 AccountRepository: $id"
                                        )
                                    }
                                },
                                onMove = { fromIndex, toIndex ->
                                    val currentList = accounts.toMutableList()
                                    val movedItem = currentList.removeAt(fromIndex)
                                    currentList.add(toIndex, movedItem)
                                    scope.launch { AccountRepository.reorderAccounts(currentList) }
                                },
                                searchQuery = debouncedSearchQuery,
                                isSelectionMode = isSelectionMode,
                                selectedIds = selectedIds,
                                onSelectionModeChanged = { isSelectionMode = it },
                                onSelectedIdsChanged = { selectedIds = it }
                            )
                        }
                    }
                }
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("删除账号") },
                    text = { Text("确定要删除选中的 ${selectedIds.size} 个账号吗？此操作无法撤销。") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    selectedIds.forEach { id ->
                                        AccountRepository.removeAccount(id)
                                    }
                                    isSelectionMode = false
                                    selectedIds = emptySet()
                                    showDeleteDialog = false
                                }
                            }
                        ) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("取消")
                        }
                    }
                )
            }
        }
    }
}
