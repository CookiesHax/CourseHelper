package com.cookieshax.coursehelper.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import androidx.navigation.NavDestination.Companion.hasRoute
import com.cookieshax.coursehelper.app.main.MainScreen
import com.cookieshax.coursehelper.feature.account.ui.TagManagerScreen
import com.cookieshax.coursehelper.feature.camera.CameraScreen
import com.cookieshax.coursehelper.feature.course.ui.CourseTaskListScreen
import com.cookieshax.coursehelper.feature.login.LoginScreen
import com.cookieshax.coursehelper.feature.map.MapScreen
import com.cookieshax.coursehelper.feature.settings.ui.SettingsScreen
import com.cookieshax.coursehelper.feature.checkin.ui.CheckInScreen
import com.cookieshax.coursehelper.feature.webview.WebViewScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = MainRoute,
            // 前进进入 - 新页面从右侧 2/3 位置向左划入
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it * 2 / 3 },
                    animationSpec = tween(400)
                ) + fadeIn(tween(400))
            },
            // 前进退出 - 旧页面原地淡出
            exitTransition = {
                fadeOut(tween(400))
            },
            // 后退进入 - 上一个页面从左侧 2/3 位置向右划入 (LTR)
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it * 2 / 3 }, // 负坐标代表从左侧开始
                    animationSpec = tween(400)
                ) + fadeIn(tween(400))
            },
            // 后退退出 - 当前关闭的页面原地淡出
            popExitTransition = {
                fadeOut(tween(400))
            },
            // 预测性返回保持一致
            predictivePopEnterTransition = { _ ->
                slideInHorizontally(
                    initialOffsetX = { -it * 2 / 3 },
                    animationSpec = tween(400)
                ) + fadeIn(tween(400))
            },
            predictivePopExitTransition = {
                val isSlideRoute = initialState.destination.hasRoute<MapRoute>() ||
                        initialState.destination.hasRoute<CameraRoute>()
                if (isSlideRoute) {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(400)
                    )
                } else {
                    fadeOut(tween(400))
                }
            }
        ) {
            composable<MainRoute> {
                MainScreen(navController = navController)
            }
            composable<SettingsRoute> {
                SettingsScreen(navController = navController)
            }
            composable<MapRoute>(
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(400)
                    )
                }
            ) {
                MapScreen(onBackClick = { navController.popBackStack() })
            }
            composable<TagManagerRoute> {
                TagManagerScreen(navController = navController)
            }
            composable<LoginRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<LoginRoute>()
                LoginScreen(
                    onBack = { navController.popBackStack() },
                    initialLoginType = route.loginType
                )
            }
            composable<CourseTaskRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<CourseTaskRoute>()
                CourseTaskListScreen(
                    courseId = route.courseId,
                    courseName = route.courseName,
                    navController = navController
                )
            }
            composable<WebViewRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<WebViewRoute>()
                WebViewScreen(
                    url = route.url,
                    onBackPressed = { navController.popBackStack() },
                    navController = navController
                )
            }
            composable<CameraRoute>(
                deepLinks = listOf(
                    navDeepLink<CameraRoute>(basePath = "coursehelper://scan")
                ),
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(400)
                    )
                }
            ) {
                CameraScreen(navController = navController)
            }
            composable<CheckInRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<CheckInRoute>()
                CheckInScreen(
                    url = route.url,
                    taskId = route.taskId,
                    navController = navController,
                    courseId = route.courseId
                )
            }
        }
    }
}
