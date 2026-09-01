package com.cookieshax.coursehelper.feature.webview

import android.util.Log
import androidx.navigation.NavController
import com.cookieshax.coursehelper.app.navigation.CameraRoute
import com.cookieshax.coursehelper.app.navigation.CourseTaskRoute
import com.cookieshax.coursehelper.core.location.GeoCodeService
import com.cookieshax.coursehelper.core.location.LocationService
import com.cookieshax.coursehelper.core.network.ApiManager
import com.cookieshax.coursehelper.core.network.ApiResult
import com.cookieshax.coursehelper.core.utils.EncryptionUtils
import com.cookieshax.coursehelper.core.utils.StringUtils
import com.cookieshax.coursehelper.core.utils.getAsJsonObjectOrNull
import com.cookieshax.coursehelper.core.utils.getStringOrEmpty
import com.cookieshax.coursehelper.core.utils.showToast
import com.cookieshax.coursehelper.feature.account.model.AccountRepository
import com.cookieshax.coursehelper.feature.course.model.Course
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

class JsBridgeHandlers(
    private val jsBridgeInterface: JsBridgeInterface,
    private val navController: NavController?,
    private val scope: CoroutineScope,
    private val onOpenUrl: (String) -> Unit,
    private val onCloseWebView: () -> Unit,
    private val onChooseImage: () -> Unit,
    private val onLocationRequested: () -> Unit = {}
) {
    // 处理所有 JS Bridge 回调
    fun handle(notificationName: String, paramsJson: String) {
        when (notificationName) {
            "CLIENT_GET_USERINFO" -> handleGetUserInfo()
            "CLIENT_BARCODE_SCANNER" -> handleBarcodeScanner()
            "CLIENT_CHOOSE_IMAGE" -> handleChooseImage()
            "CLIENT_OPEN_URL" -> handleOpenUrl(paramsJson)
            "CLIENT_USER_LOCATION" -> handleUserLocation()
            "CLIENT_DEVICE_FLAG" -> handleDeviceFlag()
            "CLIENT_FACE_RECOGNITION_BLINK" -> handleFaceRecognition()
            "CLIENT_LOGIN_STATUS" -> handleLoginStatus()
            "CLIENT_DISPLAY_MESSAGE" -> handleClientDisplayMessage(paramsJson)
            "CLIENT_OPEN_RES" -> handleClientOpenRes(paramsJson)
            "CLIENT_EXIT_LEVEL" -> handleClientExitLevel()
        }
    }

    private fun handleGetUserInfo() {
        val currentAccount = AccountRepository.activeAccountFlow.value

        if (currentAccount != null) {
            scope.launch(Dispatchers.IO) {
                var schoolName = ""
                var className = ""
                var fid = ""

                when (val coursesResult = ApiManager.getCourses()) {
                    is ApiResult.Success -> {
                        val courses = Course.fromApiResponse(
                            StringUtils.parseJson(coursesResult.data),
                            true
                        )
                        if (courses.isNotEmpty()) {
                            schoolName = courses[0].schools ?: ""
                            className = courses[0].note ?: ""
                            fid = courses[0].classId
                        }
                    }

                    else -> {}
                }

                val userInfoJson = JsonObject().apply {
                    addProperty("uid", currentAccount.uid)
                    addProperty("fid", fid)
                    addProperty("name", currentAccount.name)
                    addProperty("schoolName", schoolName)
                    addProperty("className", className)
                    addProperty("role", "student")
                    addProperty("avatar", currentAccount.avatarUrl)
                }

                jsBridgeInterface.sendMessageToWebView(
                    "CLIENT_GET_USERINFO",
                    userInfoJson.toString()
                )
            }
        }
    }

    private fun handleBarcodeScanner() {
        navController?.navigate(CameraRoute) {
            launchSingleTop = true
        }
    }

    private fun handleChooseImage() {
        onChooseImage()
    }

    private fun handleOpenUrl(paramsJson: String) {
        try {
            val params = StringUtils.parseJson(paramsJson)
            val webUrl = params?.getStringOrEmpty("webUrl") ?: ""

            if (webUrl.isNotEmpty()) {
                onOpenUrl(webUrl)
                Log.d("WebViewScreen", "Loading URL in current WebView: $webUrl")
            } else {
                Log.e("WebViewScreen", "CLIENT_OPEN_URL: webUrl is empty")
            }
        } catch (e: Exception) {
            Log.e("WebViewScreen", "CLIENT_OPEN_URL parse error: ${e.message}", e)
        }
    }

    private fun handleUserLocation() {
        onLocationRequested()
        scope.launch(Dispatchers.IO) {
            try {
                // 等待有效的定位数据
                val location = withTimeoutOrNull(10L.seconds) {
                    LocationService.locationUpdates
                        .filterNotNull()
                        .first { it.latitude != 0.0 && it.longitude != 0.0 }
                }

                if (location == null) {
                    Log.e("WebViewScreen", "handleUserLocation: Get location timeout")
                    jsBridgeInterface.sendMessageToWebView(
                        "CLIENT_USER_LOCATION",
                        "{\"result\": 0}"
                    )
                    return@launch
                }

                val latitude = location.latitude
                val longitude = location.longitude

                val result = GeoCodeService.reverseGeoCodeSuspend(latitude, longitude)
                val rawAddress = result?.address ?: "null"
                val safeGlobalAddress = rawAddress.replace("\"", "\\\"")

                val poiListJson = result?.poiList?.joinToString(",\n") { poi ->
                    val safePoiName = (poi.name ?: "").replace("\"", "\\\"")
                    val safePoiAddress = (poi.address ?: "").replace("\"", "\\\"")
                    """
                    {
                      "name": "$safePoiName",
                      "address": "$safePoiAddress",
                      "longitude": ${poi.location?.longitude ?: 0.0},
                      "latitude": ${poi.location?.latitude ?: 0.0}
                    }
                    """.trimIndent()
                } ?: ""

                val locationJson = """
                    {
                      "result": 1,
                      "longitude": $longitude,
                      "latitude": $latitude,
                      "address": "$safeGlobalAddress",
                      "poiList": [$poiListJson],
                      "mockData": {
                        "probability": 0,
                        "strategy": "GPS_NATIVE"
                      }
                    }
                """.trimIndent()

                jsBridgeInterface.sendMessageToWebView("CLIENT_USER_LOCATION", locationJson)
            } catch (e: Exception) {
                Log.e("WebViewScreen", "handleUserLocation error: ${e.message}", e)
                jsBridgeInterface.sendMessageToWebView("CLIENT_USER_LOCATION", "{\"result\": 0}")
            }
        }
    }

    private fun handleDeviceFlag() {
        EncryptionUtils.getDeviceCode().let { deviceCode ->
            // 设备码是纯字符串 用 JSON 字符串包装
            val json = JsonObject().apply {
                addProperty("flagInfo", deviceCode)
            }
            jsBridgeInterface.sendMessageToWebView("CLIENT_DEVICE_FLAG", json.toString())
        }
    }

    private fun handleFaceRecognition() {
        scope.launch(Dispatchers.IO) {
            var faceId = ""
            when (val result = ApiManager.getUserFaceId()) {
                is ApiResult.Success -> {
                    try {
                        faceId = result.data
                        Log.d("Face", faceId)
                    } catch (e: Exception) {
                        Log.e("WebViewScreen", "Parse faceId error: ${e.message}", e)
                    }
                }

                else -> {}
            }

            val faceJson = JsonObject().apply {
                addProperty("result", 1)
                addProperty("LiveDetectionStatus", 1)
                addProperty("collectStatus", 1)
                addProperty("currentFaceId", faceId)
            }

            jsBridgeInterface.sendMessageToWebView(
                "CLIENT_FACE_RECOGNITION_BLINK",
                faceJson.toString()
            )
        }
    }

    private fun handleLoginStatus() {
        val json = JsonObject()

        val currentAccount = AccountRepository.activeAccountFlow.value
        if (currentAccount == null) {
            json.apply {
                addProperty("status", "0")
                addProperty("message", "not logged in")
            }
        } else {
            json.apply {
                addProperty("status", "1")
                addProperty("message", "success")
                add("data", JsonObject().apply {
                    addProperty("uid", currentAccount.uid)
                    addProperty("name", currentAccount.name)
                })
            }
        }

        jsBridgeInterface.sendMessageToWebView(
            "CLIENT_LOGIN_STATUS",
            json.toString()
        )
    }

    private fun handleClientDisplayMessage(paramsJson: String) {
        val params = StringUtils.parseJson(paramsJson)
        val message = params?.getStringOrEmpty("message") ?: ""
        if (message.isNotBlank()) {
            message.showToast()
        }
    }

    private fun handleClientOpenRes(paramsJson: String) {
        try {
            val params = StringUtils.parseJson(paramsJson)

            val content = params?.getAsJsonObjectOrNull("content")
            val course = content?.getAsJsonObjectOrNull("course")
            val dataArray = course?.get("data") as? JsonArray

            val firstCourse = dataArray?.get(0) as? JsonObject
            val courseId = firstCourse?.getStringOrEmpty("id") ?: ""

            if (courseId.isNotEmpty()) {
                navController?.navigate(CourseTaskRoute(courseId = courseId))
            } else {
                Log.e("WebViewScreen", "CLIENT_OPEN_RES: courseId is empty in params: $paramsJson")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleClientExitLevel() {
        onCloseWebView()
    }
}
