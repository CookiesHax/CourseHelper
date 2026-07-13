package com.cookieshax.coursehelper.app.navigation

import com.cookieshax.coursehelper.feature.login.LoginType
import kotlinx.serialization.Serializable

@Serializable
object MainRoute

@Serializable
object SettingsRoute

@Serializable
object MapRoute

@Serializable
object TagManagerRoute

@Serializable
data class LoginRoute(val loginType: LoginType = LoginType.PASSWORD)

@Serializable
data class CourseTaskRoute(val courseId: String, val courseName: String? = null)

@Serializable
data class WebViewRoute(val url: String)

@Serializable
object CameraRoute

@Serializable
data class CheckInRoute(val url: String, val taskId: String, val courseId: String? = null)
