package com.cookieshax.coursehelper.feature.course.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cookieshax.coursehelper.app.navigation.CourseTaskRoute
import com.cookieshax.coursehelper.feature.course.ui.items.CourseItem
import com.cookieshax.coursehelper.feature.course.viewmodel.CourseState
import com.cookieshax.coursehelper.feature.course.viewmodel.CourseViewModel
import com.cookieshax.coursehelper.ui.items.Placeholder

// 课程 Tab
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseTabContent(
    selectedAccountId: String?,
    modifier: Modifier = Modifier,
    navController: NavController? = null,
    searchQuery: String = "",
    viewModel: CourseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val loadedAccountId by viewModel.loadedAccountId.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // 数据过期 当前选中账号与数据所属账号不一致
    val effectiveState = if (selectedAccountId != loadedAccountId) {
        CourseState.Loading
    } else {
        uiState
    }

    val pullToRefreshState = rememberPullToRefreshState()

    // 账号切换时立即清除过期数据 避免切回课程页时闪一帧旧数据
    LaunchedEffect(selectedAccountId) {
        viewModel.onAccountChanged(selectedAccountId)
    }

    // UI
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        state = pullToRefreshState,
        onRefresh = {
            viewModel.refreshCourses(selectedAccountId)
        },
        modifier = modifier.fillMaxSize()
    ) {
        val scrollState = rememberScrollState()

        when (effectiveState) {
            is CourseState.Idle -> {
                Placeholder("请先选择一个账号", "在'账号'页面点击账号即可切换")
            }

            is CourseState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is CourseState.Success -> {
                if (effectiveState.list.isEmpty()) {
                    // 支持滚动 否则无法下滑刷新
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp), // 给左右留点边距
                        horizontalAlignment = Alignment.CenterHorizontally // 水平居中
                    ) {
                        // 上方的占位块
                        Spacer(modifier = Modifier.weight(1f))
                        // 居中内容
                        Placeholder("暂无课程", "当前账号下没有找到课程信息")
                        // 下方的占位块
                        Spacer(modifier = Modifier.weight(1f))
                    }
                } else {
                    val filteredCourses = remember(effectiveState.list, searchQuery) {
                        if (searchQuery.isEmpty()) {
                            effectiveState.list
                        } else {
                            effectiveState.list.filter {
                                it.name.contains(
                                    searchQuery,
                                    ignoreCase = true
                                )
                            }
                        }
                    }

                    if (filteredCourses.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("未找到课程")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredCourses) { course ->
                                CourseItem(course) { clickedCourse ->
                                    viewModel.cacheCourse(clickedCourse)
                                    navController?.navigate(
                                        CourseTaskRoute(
                                            courseId = clickedCourse.courseId,
                                            courseName = clickedCourse.name
                                        )
                                    ) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        }
                    }
                }
            }

            is CourseState.Error -> {
                // 支持滚动 否则无法下滑刷新
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp), // 给左右留点边距
                    horizontalAlignment = Alignment.CenterHorizontally // 水平居中
                ) {
                    // 上方的占位块
                    Spacer(modifier = Modifier.weight(1f))
                    // 居中内容
                    Placeholder("加载失败", effectiveState.msg)
                    // 下方的占位块
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
