package com.cookieshax.coursehelper.feature.course.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cookieshax.coursehelper.app.navigation.CheckInRoute
import com.cookieshax.coursehelper.app.navigation.MapRoute
import com.cookieshax.coursehelper.app.navigation.WebViewRoute
import com.cookieshax.coursehelper.core.location.LocationService
import com.cookieshax.coursehelper.feature.course.ui.items.TaskItem
import com.cookieshax.coursehelper.feature.course.model.CourseTask
import com.cookieshax.coursehelper.feature.course.viewmodel.CourseTaskViewModel
import com.cookieshax.coursehelper.feature.course.viewmodel.TaskState
import com.cookieshax.coursehelper.feature.settings.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseTaskListScreen(
    courseId: String,
    courseName: String? = null,
    navController: NavController
) {
    val viewModel: CourseTaskViewModel = viewModel()
    val isNavigating = remember { mutableStateOf(false) }
    val lastClickTime = remember { mutableLongStateOf(0L) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val settingsViewModel: SettingsViewModel = viewModel()
    val preferOkHttpOverWebView by settingsViewModel.preferOkHttpOverWebView.collectAsStateWithLifecycle()
    val showUnsupportedTasks = settingsViewModel.showUnsupportedTasks.collectAsState().value

    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isMockLocationFlow by LocationService.isMockLocationFlow.collectAsState() // 模拟定位状态

    // 初始加载
    LaunchedEffect(courseId) {
        viewModel.loadTasks(courseId)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (uiState is TaskState.Success) {
                                (uiState as TaskState.Success).course.name
                            } else {
                                courseName ?: "加载中..."
                            }
                        )
                    },
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
            val scrollState = rememberScrollState()
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    viewModel.refreshTasks(courseId)
                },
                modifier = Modifier
                    .fillMaxSize()
            ) {
                when (val state = uiState) {
                    is TaskState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }

                    is TaskState.Success -> {
                        if (state.tasks.isEmpty()) {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .padding(horizontal = 16.dp)
                                    .verticalScroll(scrollState),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                Text(text = "暂无活动", fontSize = 16.sp)
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    top = innerPadding.calculateTopPadding(), // 顶栏留白
                                    bottom = innerPadding.calculateBottomPadding() + 16.dp, // 导航栏留白 + 额外间距
                                    start = 16.dp,
                                    end = 16.dp
                                )
                            ) {
                                items(state.tasks) { task ->
                                    if (!showUnsupportedTasks) {
                                        if (CourseTask.isUnsupportedTask(task)) {
                                            return@items
                                        }
                                    }

                                    TaskItem(
                                        courseTask = task,
                                        onItemClick = { clickedTask ->
                                            val currentTime = System.currentTimeMillis()
                                            if (currentTime - lastClickTime.longValue < 500) {
                                                return@TaskItem
                                            }
                                            lastClickTime.longValue = currentTime
                                            if (
                                                (clickedTask.activeType == 2 || clickedTask.activeType == 74) // 签到 / 签退
                                                && preferOkHttpOverWebView
                                                && clickedTask.otherId != ""
                                            ) {
                                                navController.navigate(
                                                    CheckInRoute(
                                                        url = clickedTask.url,
                                                        taskId = clickedTask.id.toString(),
                                                        courseId = courseId
                                                    )
                                                )
                                            } else {
                                                if (CourseTask.isUnsupportedTask(task)) {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("不支持的活动，请前往学习通客户端查看")
                                                    }
                                                } else {
                                                    navController.navigate(
                                                        WebViewRoute(clickedTask.url)
                                                    ) {
                                                        launchSingleTop = true
                                                    }
                                                }
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }

                    is TaskState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .verticalScroll(scrollState),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            Text(text = "错误: ${state.msg}", fontSize = 16.sp)
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                if (!preferOkHttpOverWebView) {
                    FloatingActionButton(
                        onClick = {
                            navController.navigate(MapRoute) {
                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(
                                end = 24.dp,
                                bottom = innerPadding.calculateBottomPadding() + 24.dp
                            ),
                        containerColor = if (isMockLocationFlow) {
                            MaterialTheme.colorScheme.error // 红色表示正在模拟位置
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = "虚拟定位")
                    }
                }
            }
        }
    }
}
