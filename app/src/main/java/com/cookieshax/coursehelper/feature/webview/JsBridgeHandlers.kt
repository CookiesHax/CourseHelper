package com.cookieshax.coursehelper.feature.webview

import android.util.Log
import android.widget.Toast
import androidx.navigation.NavController
import com.cookieshax.coursehelper.app.navigation.CameraRoute
import com.cookieshax.coursehelper.app.navigation.CourseTaskRoute
import com.cookieshax.coursehelper.core.location.GeoCodeService
import com.cookieshax.coursehelper.core.location.LocationService
import com.cookieshax.coursehelper.core.network.ApiManager
import com.cookieshax.coursehelper.core.network.ApiResult
import com.cookieshax.coursehelper.core.utils.EncryptionUtils
import com.cookieshax.coursehelper.core.utils.StringUtils
import com.cookieshax.coursehelper.feature.account.model.AccountRepository
import com.cookieshax.coursehelper.feature.course.model.Course
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

class JsBridgeHandlers(
    private val jsBridgeInterface: JsBridgeInterface,
    private val navController: NavController?,
    private val scope: CoroutineScope,
    private val onOpenUrl: (String) -> Unit,
    private val onCloseWebView: () -> Unit,
    private val onChooseImage: () -> Unit
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
        val activeAccountId = AccountRepository.activeAccountIdFlow.value
        val accounts = AccountRepository.getCurrentListSnapshot()
        val currentAccount = accounts.find { it.uid == activeAccountId }

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

                val userInfoJson = JSONObject().apply {
                    put("uid", currentAccount.uid)
                    put("fid", fid)
                    put("name", currentAccount.name)
                    put("schoolName", schoolName)
                    put("className", className)
                    put("role", "student")
                    put("avatar", currentAccount.avatarUrl)
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
            val params = JSONObject(paramsJson)
            val webUrl = params.optString("webUrl", "")

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
        val currentLocation = LocationService.getCurrentLocation()
        val latitude = currentLocation.latitude
        val longitude = currentLocation.longitude

        GeoCodeService.reverseGeoCode(latitude, longitude) { result ->
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
        }
    }

    private fun handleDeviceFlag() {
        EncryptionUtils.getDeviceCode().let { deviceCode ->
            // 设备码是纯字符串 用 JSON 字符串包装
            val json = JSONObject().apply {
                put("flagInfo", deviceCode)
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

            val faceJson = JSONObject().apply {
                put("result", 1)
                put("LiveDetectionStatus", 1)
                put("collectStatus", 1)
                put("currentFaceId", faceId)
            }

            jsBridgeInterface.sendMessageToWebView(
                "CLIENT_FACE_RECOGNITION_BLINK",
                faceJson.toString()
            )
        }
    }

    private fun handleLoginStatus() {
        val json = JSONObject()

        val activeAccountId = AccountRepository.activeAccountIdFlow.value
        if (activeAccountId == null) {
            json.apply {
                put("status", "0")
                put("message", "not logged in")
            }
        }

        val name =
            AccountRepository.getCurrentListSnapshot().find { it.uid == activeAccountId }?.name
        if (name.isNullOrEmpty()) {
            Log.e("WebViewScreen", "Active account name is null or empty for uid: $activeAccountId")
        }

        json.apply {
            put("status", "1")
            put("message", "success")
            put("data", JSONObject().apply {
                put("uid", activeAccountId)
                put("name", name)
            })
        }

        jsBridgeInterface.sendMessageToWebView(
            "CLIENT_LOGIN_STATUS",
            json.toString()
        )
    }

    private fun handleClientDisplayMessage(paramsJson: String) {
        val params = JSONObject(paramsJson)
        val message = params.optString("message", "")
        if (message.isNotBlank()) {
            Toast.makeText(
                jsBridgeInterface.webView?.context,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleClientOpenRes(paramsJson: String) {
        try {
            val params = JSONObject(paramsJson)

            val content = params.optJSONObject("content")
            val course = content?.optJSONObject("course")
            val dataArray = course?.optJSONArray("data")

            val firstCourse = dataArray?.optJSONObject(0)
            val courseId = firstCourse?.optString("id").orEmpty()

            if (courseId.isNotEmpty()) {
                navController?.navigate(CourseTaskRoute(courseId = courseId))
            } else {
                Log.e("WebViewScreen", "CLIENT_OPEN_RES: courseId is empty in params: $paramsJson")
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }
    
    private fun handleClientExitLevel() {
        onCloseWebView()
    }
}
