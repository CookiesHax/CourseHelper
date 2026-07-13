package com.cookieshax.coursehelper.app.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
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
import com.cookieshax.coursehelper.app.main.MainScreen
import com.cookieshax.coursehelper.feature.account.ui.TagManagerScreen
import com.cookieshax.coursehelper.feature.camera.CameraScreen
import com.cookieshax.coursehelper.feature.course.ui.CourseTaskListScreen
import com.cookieshax.coursehelper.feature.login.LoginScreen
import com.cookieshax.coursehelper.feature.map.MapScreen
import com.cookieshax.coursehelper.feature.settings.ui.SettingsScreen
import com.cookieshax.coursehelper.feature.checkin.ui.CheckInScreen
import com.cookieshax.coursehelper.feature.webview.WebViewScreen

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // 进入此页面
    val enterTransition = slideInHorizontally(
        initialOffsetX = { it }, animationSpec = tween(300)
    ) + fadeIn(
        animationSpec = tween(300)
    )

    // 进入新页面
    val exitTransition = fadeOut(animationSpec = tween(300))

    // 返回到此页面
    val popEnterTransition = slideInHorizontally(
        initialOffsetX = { -it / 3 }, animationSpec = tween(300)
    ) + fadeIn(
        animationSpec = tween(300)
    )

    // 退出此页面
    val popExitTransition = fadeOut(
        animationSpec = tween(300)
    ) + scaleOut(
        targetScale = 0.85f,
        animationSpec = tween(300)
    )

    // 对于不能正确实现 fade 效果的页面
    // 退出此页面
    val popExitTransitionNoFade = slideOutHorizontally(
        targetOffsetX = { it }, animationSpec = tween(300)
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        NavHost(
            navController = navController,
            startDestination = MainRoute
        ) {
            composable<MainRoute> {
                MainScreen(navController = navController)
            }
            composable<SettingsRoute>(
                enterTransition = { enterTransition },
                exitTransition = { exitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransition }
            ) {
                SettingsScreen(navController = navController)
            }
            composable<MapRoute>(
                enterTransition = { enterTransition },
                exitTransition = { exitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransitionNoFade }
            ) {
                MapScreen(onBackClick = { navController.popBackStack() })
            }
            composable<TagManagerRoute>(
                enterTransition = { enterTransition },
                exitTransition = { exitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransition }
            ) {
                TagManagerScreen(navController = navController)
            }
            composable<LoginRoute>(
                enterTransition = { enterTransition },
                exitTransition = { exitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransition }
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<LoginRoute>()
                LoginScreen(
                    onBack = { navController.popBackStack() },
                    initialLoginType = route.loginType
                )
            }
            composable<CourseTaskRoute>(
                enterTransition = { enterTransition },
                exitTransition = { exitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransition }
            ) { backStackEntry ->
                val route = backStackEntry.toRoute<CourseTaskRoute>()
                CourseTaskListScreen(
                    courseId = route.courseId,
                    courseName = route.courseName,
                    navController = navController
                )
            }
            composable<WebViewRoute>(
                enterTransition = { enterTransition },
                exitTransition = { exitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransition }
            ) { backStackEntry ->
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
                enterTransition = { enterTransition },
                exitTransition = { exitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransitionNoFade }
            ) {
                CameraScreen(
                    navController = navController
                )
            }
            composable<CheckInRoute>(
                enterTransition = { enterTransition },
                exitTransition = { exitTransition },
                popEnterTransition = { popEnterTransition },
                popExitTransition = { popExitTransition }
            ) { backStackEntry ->
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
