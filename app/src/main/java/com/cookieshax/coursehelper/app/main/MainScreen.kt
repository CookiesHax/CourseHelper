package com.cookieshax.coursehelper.app.main

import android.content.res.Configuration
import android.util.Log
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.cookieshax.coursehelper.feature.account.ui.TagManagerScreen
import com.cookieshax.coursehelper.feature.settings.ui.SettingsScreen
import com.cookieshax.coursehelper.app.navigation.CameraRoute
import com.cookieshax.coursehelper.app.navigation.LoginRoute
import com.cookieshax.coursehelper.app.navigation.SettingsRoute
import com.cookieshax.coursehelper.app.navigation.TagManagerRoute
import com.cookieshax.coursehelper.app.navigation.WebViewRoute
import com.cookieshax.coursehelper.core.utils.showToast
import com.cookieshax.coursehelper.feature.account.model.AccountRepository
import com.cookieshax.coursehelper.feature.account.ui.components.AccountTabContent
import com.cookieshax.coursehelper.feature.account.viewmodel.SelectionType
import com.cookieshax.coursehelper.feature.account.viewmodel.TagManagerViewModel
import com.cookieshax.coursehelper.feature.course.ui.CourseTabContent
import com.cookieshax.coursehelper.feature.course.viewmodel.CourseViewModel
import com.cookieshax.coursehelper.feature.login.LoginType
import com.cookieshax.coursehelper.ui.items.IcTags
import com.cookieshax.coursehelper.ui.items.SearchInput
import com.cookieshax.coursehelper.ui.items.SearchTrigger
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

enum class MainTab {
    COURSE,
    ACCOUNT,
    TAGS,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(navController: NavHostController) {
    val mainViewModel: MainViewModel = viewModel()
    val courseViewModel: CourseViewModel = viewModel()
    val tagViewModel: TagManagerViewModel = viewModel()
    val scope = rememberCoroutineScope()

    val activeAccountId by mainViewModel.activeAccountId.collectAsState()
    val activeAccount by mainViewModel.activeAccount.collectAsState()
    val accounts by mainViewModel.accounts.collectAsState()
    val allAccountsId by mainViewModel.accountsId.collectAsState()

    var selectedTab by rememberSaveable { mutableStateOf(MainTab.COURSE) }
    var lastMainTab by rememberSaveable { mutableStateOf(MainTab.COURSE) } // 记录上一个主 Tab

    // 追踪主 Tab
    LaunchedEffect(selectedTab) {
        if (selectedTab == MainTab.COURSE || selectedTab == MainTab.ACCOUNT) {
            lastMainTab = selectedTab
        }
    }

    val saveableStateHolder = rememberSaveableStateHolder() // 用于保存和恢复页面状态

    var showLoginMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInviteCodeDialog by remember { mutableStateOf(false) }

    var isSelectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedIds by rememberSaveable { mutableStateOf(setOf<String>()) }
    var selectBackProgress by remember { mutableFloatStateOf(0f) }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    val searchQuery by mainViewModel.searchQuery.collectAsState()
    val debouncedSearchQuery by mainViewModel.debouncedSearchQuery.collectAsState()
    var searchBackProgress by remember { mutableFloatStateOf(0f) }

    var isRailExpanded by rememberSaveable { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = configuration.smallestScreenWidthDp >= 600
    val useNavRail = isLandscape && isTablet

    // 自适应导航逻辑 旋转屏幕时自动切换 Tab 或 独立页面
    LaunchedEffect(useNavRail) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (useNavRail) {
            // 如果切换到横屏/平板 且当前在独立的标签/设置页面 则返回主页并选中对应 Tab
            if (currentRoute?.contains("TagManagerRoute") == true) {
                navController.popBackStack()
                selectedTab = MainTab.TAGS
            } else if (currentRoute?.contains("SettingsRoute") == true) {
                navController.popBackStack()
                selectedTab = MainTab.SETTINGS
            }
        } else {
            // 如果切换到竖屏/手机 且当前在集成的标签/设置 Tab 则退回到主 Tab 并导航到独立页面
            if (selectedTab == MainTab.TAGS) {
                selectedTab = lastMainTab
                navController.navigate(TagManagerRoute)
            } else if (selectedTab == MainTab.SETTINGS) {
                selectedTab = lastMainTab
                navController.navigate(SettingsRoute)
            }
        }
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

    // UI
    SharedTransitionLayout {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val isTablet = configuration.smallestScreenWidthDp >= 600
        val useNavRail = isLandscape && isTablet

        val railWidth by animateDpAsState(
            targetValue = if (isRailExpanded) 200.dp else 80.dp,
            label = "rail_width_animation"
        )

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavRail) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(railWidth),
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            IconButton(onClick = { isRailExpanded = !isRailExpanded }) {
                                Icon(Icons.Default.Menu, contentDescription = "切换菜单模式")
                            }
                        }
                    }
                ) {
                    Spacer(Modifier.height(8.dp))
                    AnimatedContent(
                        targetState = isRailExpanded,
                        transitionSpec = {
                            fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                        },
                        label = "rail_content_transition"
                    ) { expanded ->
                        if (expanded) {
                            // 水平排列布局
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RailHorizontalItem(
                                    selected = selectedTab == MainTab.COURSE,
                                    onClick = { selectedTab = MainTab.COURSE },
                                    icon = Icons.AutoMirrored.Filled.MenuBook,
                                    label = "课程"
                                )
                                RailHorizontalItem(
                                    selected = selectedTab == MainTab.ACCOUNT,
                                    onClick = { selectedTab = MainTab.ACCOUNT },
                                    icon = Icons.Default.Person,
                                    label = "账号"
                                )

                                Spacer(Modifier.height(8.dp))

                                RailHorizontalItem(
                                    selected = selectedTab == MainTab.TAGS,
                                    onClick = { selectedTab = MainTab.TAGS },
                                    icon = IcTags,
                                    label = "标签管理"
                                )
                                RailHorizontalItem(
                                    selected = selectedTab == MainTab.SETTINGS,
                                    onClick = { selectedTab = MainTab.SETTINGS },
                                    icon = Icons.Default.Settings,
                                    label = "系统设置"
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                NavigationRailItem(
                                    selected = selectedTab == MainTab.COURSE,
                                    onClick = { selectedTab = MainTab.COURSE },
                                    icon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = null
                                        )
                                    },
                                    label = { Text("课程") },
                                    alwaysShowLabel = true
                                )
                                NavigationRailItem(
                                    selected = selectedTab == MainTab.ACCOUNT,
                                    onClick = { selectedTab = MainTab.ACCOUNT },
                                    icon = {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null
                                        )
                                    },
                                    label = { Text("账号") },
                                    alwaysShowLabel = true
                                )

                                NavigationRailItem(
                                    selected = selectedTab == MainTab.TAGS,
                                    onClick = { selectedTab = MainTab.TAGS },
                                    icon = { Icon(IcTags, contentDescription = null) },
                                    label = { Text("标签") },
                                    alwaysShowLabel = true
                                )
                                NavigationRailItem(
                                    selected = selectedTab == MainTab.SETTINGS,
                                    onClick = { selectedTab = MainTab.SETTINGS },
                                    icon = {
                                        Icon(
                                            Icons.Default.Settings,
                                            contentDescription = null
                                        )
                                    },
                                    label = { Text("设置") },
                                    alwaysShowLabel = true
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                }
                VerticalDivider()
            }

            Scaffold(
                modifier = Modifier.weight(1f),
                // TopAppBar
                topBar = {
                    AnimatedContent(
                        targetState = isSearching to selectedTab,
                        transitionSpec = {
                            fadeIn(tween(100)) togetherWith fadeOut(tween(100))
                        },
                        label = "top_bar_transition"
                    ) { (searching, tab) ->
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
                                                isSearching = false
                                                mainViewModel.clearSearchQuery()
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .graphicsLayer {
                                                    val scale = 1f - (searchBackProgress * 0.08f)
                                                    scaleX = scale
                                                    scaleY = scale
                                                    alpha =
                                                        1f - (searchBackProgress * 2f).coerceAtMost(
                                                            1f
                                                        )
                                                },
                                            hint = when (tab) {
                                                MainTab.COURSE -> "搜索课程..."
                                                MainTab.ACCOUNT -> "搜索账号..."
                                                MainTab.TAGS -> "搜索标签..."
                                                else -> "搜索..."
                                            },
                                            animatedVisibilityScope = this@AnimatedContent
                                        )
                                    }
                                }
                            )
                        } else {
                            TopAppBar(
                                title = {
                                    val title = when (tab) {
                                        MainTab.COURSE -> "CourseHelper"
                                        MainTab.ACCOUNT -> if (isSelectionMode) "${selectedIds.size} / ${accounts.size}" else "账号管理"
                                        MainTab.TAGS -> {
                                            val isTagSelectionMode by tagViewModel.isSelectionMode.collectAsState()
                                            val selectedTagIds by tagViewModel.selectedTagIds.collectAsState()
                                            val tags by tagViewModel.tagsWithAccounts.collectAsState()
                                            if (isTagSelectionMode) "${selectedTagIds.size} / ${tags.size}" else "标签管理"
                                        }
                                        MainTab.SETTINGS -> "系统设置"
                                    }
                                    Text(
                                        text = title,
                                        modifier = Modifier.padding(start = 2.dp)
                                    )
                                },
                                actions = {
                                    when (tab) {
                                        MainTab.COURSE, MainTab.ACCOUNT -> {
                                            // 手机端显示标签入口
                                            if (!useNavRail) {
                                                IconButton(onClick = {
                                                    navController.navigate(
                                                        TagManagerRoute
                                                    )
                                                }) {
                                                    Icon(IcTags, contentDescription = "标签")
                                                }
                                            }

                                            SearchTrigger(
                                                onClick = { isSearching = true },
                                                animatedVisibilityScope = this@AnimatedContent
                                            )

                                            // 添加按钮逻辑
                                            if (tab == MainTab.COURSE || !isSelectionMode) {
                                                Box {
                                                    IconButton(onClick = { showLoginMenu = true }) {
                                                        Icon(
                                                            Icons.Default.Add,
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
                                                                navController.navigate(
                                                                    LoginRoute(
                                                                        LoginType.PASSWORD
                                                                    )
                                                                ) { launchSingleTop = true }
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("验证码登录") },
                                                            onClick = {
                                                                showLoginMenu = false
                                                                navController.navigate(
                                                                    LoginRoute(
                                                                        LoginType.VERIFICATION_CODE
                                                                    )
                                                                ) { launchSingleTop = true }
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("二维码登录") },
                                                            onClick = {
                                                                showLoginMenu = false
                                                                navController.navigate(
                                                                    LoginRoute(
                                                                        LoginType.QRCODE
                                                                    )
                                                                ) { launchSingleTop = true }
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("添加课程") },
                                                            onClick = {
                                                                showLoginMenu = false
                                                                if (activeAccountId != null) showInviteCodeDialog =
                                                                    true
                                                                else "必须选择一个账号才能添加课程".showToast()
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            // 账号多选逻辑
                                            if (isSelectionMode && tab == MainTab.ACCOUNT) {
                                                IconButton(onClick = {
                                                    selectedIds =
                                                        if (selectedIds.size == allAccountsId.size) emptySet() else allAccountsId.toSet()
                                                }) {
                                                    Icon(
                                                        if (selectedIds.size == allAccountsId.size) Icons.Default.Deselect else Icons.Default.SelectAll,
                                                        contentDescription = "选择"
                                                    )
                                                }
                                                IconButton(onClick = {
                                                    if (selectedIds.isNotEmpty()) showDeleteDialog =
                                                        true
                                                }) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "删除",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }

                                            if (!useNavRail && !isSelectionMode) {
                                                IconButton(onClick = {
                                                    navController.navigate(
                                                        SettingsRoute
                                                    ) { launchSingleTop = true }
                                                }) {
                                                    Icon(
                                                        Icons.Default.Settings,
                                                        contentDescription = "设置"
                                                    )
                                                }
                                            }
                                        }

                                        MainTab.TAGS -> {
                                            val selectionType by tagViewModel.selectionType.collectAsState()
                                            val isSelectionMode by tagViewModel.isSelectionMode.collectAsState()
                                            val selectedIds by tagViewModel.selectedTagIds.collectAsState()
                                            val tags by tagViewModel.tagsWithAccounts.collectAsState()

                                            SearchTrigger(
                                                onClick = { isSearching = true },
                                                animatedVisibilityScope = this@AnimatedContent
                                            )

                                            if (isSelectionMode) {
                                                IconButton(onClick = {
                                                    tagViewModel.setSelectedTagIds(
                                                        if (selectedIds.size == tags.size) emptySet()
                                                        else tags.map { it.tag.tagId.toString() }
                                                            .toSet()
                                                    )
                                                }) {
                                                    Icon(
                                                        if (selectedIds.size == tags.size) Icons.Default.Deselect else Icons.Default.SelectAll,
                                                        contentDescription = "选择"
                                                    )
                                                }
                                                IconButton(onClick = {
                                                    if (selectedIds.isNotEmpty()) tagViewModel.setShowDeleteDialog(
                                                        true
                                                    )
                                                }) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "删除",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            } else {
                                                IconButton(onClick = { tagViewModel.triggerAddTag() }) {
                                                    Icon(
                                                        Icons.Default.Add,
                                                        contentDescription = "新建标签"
                                                    )
                                                }
                                                IconButton(onClick = {
                                                    tagViewModel.setSelectionType(
                                                        if (selectionType == SelectionType.TAG) SelectionType.ACCOUNT
                                                        else SelectionType.TAG
                                                    )
                                                }) {
                                                    Icon(
                                                        if (selectionType == SelectionType.TAG) Icons.Default.Person else IcTags,
                                                        contentDescription = "切换视图"
                                                    )
                                                }
                                            }
                                        }

                                        MainTab.SETTINGS -> {
                                            // 设置页不需要额外顶栏按钮
                                        }
                                    }
                                }
                            )
                        }
                    }
                },
                // 底部导航栏
                bottomBar = {
                    if (!useNavRail) {
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
                    }
                },
                // FloatingActionButton
                floatingActionButton = {
                    if (selectedTab != MainTab.SETTINGS) {
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
                    }
                }) { innerPadding ->
                val isTagSelectionMode by tagViewModel.isSelectionMode.collectAsState()
                PredictiveBackHandler(
                    enabled = isSearching || 
                             (isSelectionMode && selectedTab == MainTab.ACCOUNT) ||
                             (isTagSelectionMode && selectedTab == MainTab.TAGS)
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
                        } else if (isTagSelectionMode && selectedTab == MainTab.TAGS) {
                            // 退出标签选择模式
                            tagViewModel.setSelectionMode(false)
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

                if (showInviteCodeDialog) {
                    var text by remember { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = { showInviteCodeDialog = false },
                        title = { Text("为 ${activeAccount?.name ?: "未知用户"} 添加课程") },
                        text = {
                            Column {
                                OutlinedTextField(
                                    value = text,
                                    onValueChange = {
                                        if (it.all { char -> char.isDigit() }) text = it
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("邀请码") },
                                    placeholder = { Text("输入邀请码...") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (activeAccountId != null) {
                                    navController.navigate(
                                        WebViewRoute(
                                            "https://mooc1-api.chaoxing.com/teachingClassPhoneManage/phone/toParticipateCls?inviteCode=$text"
                                        )
                                    )
                                }
                                showInviteCodeDialog = false
                            }) {
                                Text("确定")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showInviteCodeDialog = false }) {
                                Text("取消")
                            }
                        }
                    )
                }
            }
        }
    }
}
