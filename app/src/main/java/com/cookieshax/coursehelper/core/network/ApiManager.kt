package com.cookieshax.coursehelper.core.network

import com.cookieshax.coursehelper.app.CourseHelperApplication
import com.cookieshax.coursehelper.core.info.ChaoXingAppInfo
import com.cookieshax.coursehelper.core.info.DeviceInfo
import com.cookieshax.coursehelper.core.utils.Constant
import com.cookieshax.coursehelper.core.utils.EncryptionUtils
import com.cookieshax.coursehelper.core.utils.ImageUtils
import com.cookieshax.coursehelper.core.utils.StringUtils
import com.cookieshax.coursehelper.feature.account.model.AccountRepository
import com.cookieshax.coursehelper.feature.course.model.Course
import org.json.JSONObject
import java.io.File

object ApiManager {
    data class QRCodeData(val uuid: String, val enc: String)

    // Login
    // 获取用户信息
    suspend fun getUserInfo(asUser: String? = AccountRepository.activeAccountIdFlow.value): ApiResult<String> {
        val deviceInfo = mapOf(
            "app_name" to ChaoXingAppInfo.PACKAGE_NAME,
            "app_ver" to ChaoXingAppInfo.VERSION,
            "board" to DeviceInfo.hardware,
            "brand" to DeviceInfo.brand,
            "cdid" to EncryptionUtils.getPlainUuid(),
            "cdtype" to "${DeviceInfo.manufacturer} ${DeviceInfo.model}",
            "cpu_ar" to DeviceInfo.cpuArch,
            "device_id" to DeviceInfo.deviceId,
            "dpi" to DeviceInfo.dpi,
            "hardware" to DeviceInfo.hardware,
            "mediaDrmId" to DeviceInfo.mediaDrmId,
            "oaid" to DeviceInfo.oaid,
            "os_lang" to DeviceInfo.language,
            "os_name" to DeviceInfo.osName,
            "os_ver" to DeviceInfo.osVersionName,
            "platform" to "android",
            "resolution" to DeviceInfo.resolution,
            "time_stamp" to System.currentTimeMillis().toString(),
        )

        val formData = mapOf(
            "data" to EncryptionUtils.rsaEncrypt(
                StringUtils.gson.toJson(deviceInfo),
                Constant.DEVICE_INFO_KEY
            )
        )

        return NetworkClient.post(
            "https://sso.chaoxing.com/apis/login/userLogin4Uname.do",
            bodyMap = formData,
            asUser = asUser
        )
    }

    // 密码登录
    suspend fun loginByPassword(username: String, password: String): ApiResult<String> {
        val loginData = mapOf(
            "uname" to username,
            "code" to password
        )
        val loginInfo = EncryptionUtils.aesEcbEncrypt(
            StringUtils.gson.toJson(loginData),
            Constant.APP_LOGIN_KEY
        )

        val formData = mapOf(
            "logininfo" to loginInfo,
            "loginType" to "1",  // 密码登录类型
            "roleSelect" to "true",
            "entype" to "1",
        )

        val result = NetworkClient.post(
            "https://passport2-api.chaoxing.com/v11/loginregister?cx_xxt_passport=json",
            formData,
            asUser = null
        )

        return result
    }

    suspend fun loginByPasswordWeb(username: String, password: String): ApiResult<String> {
        val formData = mapOf(
            "fid" to "-1",
            "uname" to EncryptionUtils.aesCbcEncrypt(username, Constant.WEB_LOGIN_KEY),
            "password" to EncryptionUtils.aesCbcEncrypt(password, Constant.WEB_LOGIN_KEY),
            "t" to "true",
            "forbidotherlogin" to "0",
            "validate" to ""
        )

        val result = NetworkClient.post(
            "https://passport2.chaoxing.com/fanyalogin",
            formData,
            asUser = null
        )

        return result
    }

    // 发送验证码
    suspend fun sendVerificationCode(phone: String): ApiResult<String> {
        val timestampMS = System.currentTimeMillis().toString()
        val enc = EncryptionUtils.md5Hash(phone + Constant.SEND_CAPTCHA_KEY + timestampMS)

        val formData = mapOf(
            "to" to phone,
            "countrycode" to "86",
            "time" to timestampMS,
            "enc" to enc
        )

        return NetworkClient.post(
            "https://passport2-api.chaoxing.com/api/sendcaptcha",
            formData,
            asUser = null
        )
    }

    // 验证码登录
    suspend fun loginByVerificationCode(phone: String, code: String): ApiResult<String> {
        val loginData = mapOf(
            "uname" to phone,
            "code" to code
        )
        val loginInfo = EncryptionUtils.aesEcbEncrypt(
            StringUtils.gson.toJson(loginData),
            Constant.APP_LOGIN_KEY
        )

        val formData = mapOf(
            "logininfo" to loginInfo,
            "loginType" to "2",  // 验证码登录类型
            "roleSelect" to "true",
            "entype" to "1",
            "countrycode" to "86"
        )

        val result = NetworkClient.post(
            "https://passport2-api.chaoxing.com/v11/loginregister?cx_xxt_passport=json",
            formData,
            asUser = null
        )

        return result
    }

    // 二维码登录部分
    // 获取二维码数据
    suspend fun getQRCodeData(): ApiResult<QRCodeData> {
        val result = NetworkClient.get("https://passport2.chaoxing.com/login", asUser = null)

        return when (result) {
            is ApiResult.Success -> {
                try {
                    val uuidRegex = Regex("""value="(.+?)" id="uuid"""")
                    val uuidMatch = uuidRegex.find(result.data)
                    val uuid = uuidMatch?.groupValues?.get(1)

                    val encRegex = Regex("""value="(.+?)" id="enc"""")
                    val encMatch = encRegex.find(result.data)
                    val enc = encMatch?.groupValues?.get(1)

                    if (uuid != null && enc != null) {
                        ApiResult.Success(QRCodeData(uuid, enc))
                    } else {
                        ApiResult.Error("无法解析二维码参数")
                    }
                } catch (e: Exception) {
                    ApiResult.Error("解析二维码参数失败: ${e.message}")
                }
            }

            is ApiResult.Error -> result
        }
    }

    // 检查二维码是否过期
    suspend fun checkQRAuthStatus(uuid: String, enc: String): ApiResult<String> {
        val formData = mapOf(
            "enc" to enc,
            "uuid" to uuid,
            "doubleFactorLogin" to "0",
            "forbidotherlogin" to "0"
        )

        return NetworkClient.post(
            "https://passport2.chaoxing.com/getauthstatus/v2",
            formData,
            asUser = null
        )
    }

    // 获取网页登录二维码图片
    suspend fun getQRCodeImage(uuid: String): ApiResult<ByteArray> {
        val url = "https://passport2.chaoxing.com/createqr?uuid=$uuid&fid=-1"
        return downloadImage(url, asUser = null)
    }

    // Course
    suspend fun getCourses(): ApiResult<String> {
        return NetworkClient.get("https://mooc1-api.chaoxing.com/mycourse/backclazzdata?view=json&getTchClazzType=1&mcode=")
    }

    suspend fun getJoinClassTime(course: Course): String? {
        val uid = AccountRepository.activeAccountIdFlow.value ?: return null

        val params = mapOf(
            "courseid" to course.courseId,
            "clazzid" to course.classId,
            "userid" to uid,
            "personid" to course.cpi,
            "view" to "json",
            "fields" to "clazzid,popupagreement,personid,clazzname,createtime"
        )

        val result = NetworkClient.get("https://mooc1-api.chaoxing.com/gas/clazzperson", params)

        return when (result) {
            is ApiResult.Success -> {
                val dataArray = StringUtils.parseJson(result.data)
                    ?.getAsJsonArray("data")

                if (dataArray != null && dataArray.size() > 0) {
                    dataArray.get(0).asJsonObject
                        .get("createtime")?.asString
                } else {
                    null
                }
            }

            is ApiResult.Error -> null
        }
    }

    suspend fun getTaskList(course: Course): ApiResult<String> {
        val uid = AccountRepository.activeAccountIdFlow.value ?: return ApiResult.Error("未登录")

        // 获取加入班级时间
        val joinClassTime = getJoinClassTime(course) ?: ""

        val params = mapOf(
            "courseId" to course.courseId,
            "classId" to course.classId,
            "uid" to uid,
            "cpi" to course.cpi,
            "joinclasstime" to joinClassTime
        )

        // 加密参数
        val encParams = EncryptionUtils.getEncParams(params)

        return NetworkClient.get(
            "https://mobilelearn.chaoxing.com/ppt/activeAPI/taskactivelist",
            encParams
        )
    }

    suspend fun getTaskListWeb(course: Course): ApiResult<String> {
        AccountRepository.activeAccountIdFlow.value ?: return ApiResult.Error("未登录")

        val timestampMS = System.currentTimeMillis().toString()

        val params = mapOf(
            "fid" to "0",
            "courseId" to course.courseId,
            "classId" to course.classId,
            "showNotStartedActive" to "0",
            "_" to timestampMS
        )

        return NetworkClient.get(
            "https://mobilelearn.chaoxing.com/v2/apis/active/student/activelist",
            params
        )
    }

    // 上传文件
    suspend fun uploadFile(file: File, uid: String? = null): String? {
        val userId = uid ?: (AccountRepository.activeAccountIdFlow.value ?: return null)

        // 获取 Token
        val tokenResult =
            NetworkClient.get("https://pan-yz.chaoxing.com/api/token/uservalid", asUser = userId)
        val token = when (tokenResult) {
            is ApiResult.Success -> JSONObject(tokenResult.data).optString("_token")
            else -> return null
        }

        // CRC 秒传校验
        val crc = EncryptionUtils.getFileCRC32(file)
        val crcParams = mapOf(
            "puid" to userId,
            "crc" to crc,
            "_token" to token
        )
        val crcResult = NetworkClient.get(
            "https://pan-yz.chaoxing.com/api/crcStorageStatus",
            params = crcParams,
            asUser = userId
        )

        // 秒传命中
        if (crcResult is ApiResult.Success) {
            val crcJson = JSONObject(crcResult.data)
            if (crcJson.optBoolean("result") && crcJson.optBoolean("exist")) {
                return crcJson.optJSONObject("data")?.optString("objectid")
            }
        }

        // 秒传未命中
        val parts = mapOf(
            "file" to file,
            "puid" to userId
        )
        val uploadParams = mapOf(
            "_from" to "mobilelearn",
            "_token" to token
        )

        val uploadResult = NetworkClient.postMultipart(
            url = "https://pan-yz.chaoxing.com/upload",
            parts = parts,
            params = uploadParams,
            asUser = userId
        )

        return when (uploadResult) {
            is ApiResult.Success -> {
                JSONObject(uploadResult.data).optJSONObject("data")?.optString("objectId")
            }

            else -> null
        }
    }

    // 签到
    // 获取具体签到配置信息
    suspend fun getCheckInTaskInfo(activeId: String): ApiResult<String> {
        val params = mapOf(
            "activeId" to activeId,
        )
        return NetworkClient.get(
            "https://mobilelearn.chaoxing.com/v2/apis/active/getPPTActiveInfo",
            params
        )
    }

    suspend fun getUserFaceId(userId: String? = null): ApiResult<String> {
        var uid = userId
        if (userId == null) {
            uid =
                AccountRepository.activeAccountIdFlow.value.let {
                    it ?: return ApiResult.Error("未登录")
                }
        }

        val params = mapOf(
            "enc" to EncryptionUtils.md5Hash(uid + Constant.FACE_ID)
        )

        return NetworkClient.get(
            "https://passport2-api.chaoxing.com/api/getUserFaceid",
            params,
            asUser = uid
        )
    }

    suspend fun getFaceEnc(activeId: String, faceId: String, asUser: String): ApiResult<String> {
        val account = AccountRepository.getCurrentListSnapshot().find { it.uid == asUser }

        val cid = account?.deviceInfo?.get("cid")?.asString ?: ""
        val sc = account?.deviceInfo?.get("sc")?.asString ?: ""

        val faceResult = mutableMapOf(
            "LiveDetectionStatus" to "1",
            "collectStatus" to "1",
            "currentFaceId" to faceId,
            "cxcid" to cid,
            "cxtime" to System.currentTimeMillis().toString()
        )

        val buffer = StringBuilder()
        for (key in faceResult.keys) {
            val value = faceResult[key] ?: ""
            buffer.append(key).append(value)
        }
        // 拼接 sc 字段
        buffer.append(sc)

        // 计算 md5 并塞回 faceResult
        val signToken = EncryptionUtils.md5Hash(buffer.toString())
        faceResult["signToken"] = signToken

        val params = mapOf(
            "DB_STRATEGY" to "PRIMARY_KEY",
            "STRATEGY_PARA" to "activeId",
            "activeId" to activeId,
            "faceResult" to StringUtils.gson.toJson(faceResult)
        )

        val resp = NetworkClient.get(
            "https://mobilelearn.chaoxing.com/pptSign/check-face-result",
            params = params,
            asUser = asUser
        )

        when (resp) {
            is ApiResult.Success -> {
                try {
                    val json = JSONObject(resp.data)
                    val status = json.optInt("status")
                    return if (status == 1) {
                        ApiResult.Success(json.optString("enc"))
                    } else {
                        ApiResult.Error("获取 faceEnc 失败: ${resp.data}")
                    }
                } catch (e: Exception) {
                    return ApiResult.Error("解析 faceEnc 失败: ${e.message}")
                }
            }

            is ApiResult.Error -> {
                return ApiResult.Error(resp.message)
            }
        }
    }

    suspend fun uploadModifiedFace(faceUrl: String, userId: String? = null): ApiResult<String> {
        var uid = userId
        if (userId == null) {
            uid =
                AccountRepository.activeAccountIdFlow.value.let {
                    it ?: return ApiResult.Error("未登录")
                }
        }

        if (faceUrl.isBlank()) {
            return ApiResult.Error("人脸图片地址为空")
        }

        return when (val imgResp =
            downloadImage(faceUrl.replace("http://", "https://"), asUser = uid)) {
            is ApiResult.Success -> {
                val file = File(
                    CourseHelperApplication.context.cacheDir,
                    "${uid}_${System.currentTimeMillis()}_modified.jpg"
                )

                try {
                    val modifiedBytes =
                        ImageUtils.modifyRandomPixelWithSimilarColor(imgResp.data)

                    ImageUtils.saveByteArrayToFile(modifiedBytes, file)

                    val objectId = uploadFile(file, uid)
                    if (objectId.isNullOrBlank()) ApiResult.Error("上传人脸图片失败") else ApiResult.Success(
                        objectId
                    )
                } finally {
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }

            is ApiResult.Error -> ApiResult.Error("下载人脸图片失败: ${imgResp.message}")
        }
    }

    suspend fun getCptchaConf(asUser: String): ApiResult<String> {
        val params = mapOf(
            "callback" to "cx_captcha_function",
            "captchaId" to Constant.CAPTCHA_ID,
            "_" to System.currentTimeMillis().toString()
        )
        return NetworkClient.get(
            "https://captcha.chaoxing.com/captcha/get/conf",
            params = params,
            asUser = asUser
        )
    }

    suspend fun getCaptchaImageUrl(
        captchaKey: String,
        token: String,
        referer: String,
        iv: String,
        asUser: String
    ): ApiResult<String> {
        val params = mapOf(
            "callback" to "cx_captcha_function",
            "captchaId" to Constant.CAPTCHA_ID,
            "type" to "slide",
            "version" to "1.1.20",
            "captchaKey" to captchaKey,
            "token" to token,
            "referer" to referer,
            "iv" to iv,
            "_" to System.currentTimeMillis().toString()
        )
        return NetworkClient.get(
            "https://captcha.chaoxing.com/captcha/get/verification/image",
            params = params,
            asUser = asUser
        )
    }

    suspend fun downloadImage(url: String, asUser: String? = null): ApiResult<ByteArray> =
        NetworkClient.getBytes(url, asUser = asUser)

    suspend fun submitCaptcha(
        xValue: Int,
        token: String,
        iv: String,
        timestamp: Long,
        referer: String
    ): ApiResult<String> {
        val formData = mapOf(
            "callback" to "cx_captcha_function",
            "captchaId" to Constant.CAPTCHA_ID,
            "type" to "slide",
            "token" to token,
            "textClickArr" to "[{\"x\":${xValue}}]",
            "coordinate" to "[]",
            "runEnv" to "10",
            "version" to "1.1.20",
            "t" to "a",
            "iv" to iv,
            "_" to timestamp
        )

        val headers = mapOf(
            "Referer" to referer
        )
        return NetworkClient.post(
            "https://captcha.chaoxing.com/captcha/check/verification/result",
            formData,
            headers = headers
        )
    }

    suspend fun checkSignCode(activeId: String, signCode: String): ApiResult<String> {
        val params = mapOf(
            "activeId" to activeId,
            "signCode" to signCode
        )
        return NetworkClient.get(
            "https://mobilelearn.chaoxing.com/widget/sign/pcStuSignController/checkSignCode",
            params
        )
    }

    suspend fun checkIn(
        uid: String,
        activeId: String,
        courseId: String,
        objectId: String,
        validate: String
    ): ApiResult<String> {
        val accounts = AccountRepository.getCurrentListSnapshot()
        val user = accounts.find { it.uid == uid }
        if (user == null) {
            return ApiResult.Error("未找到用户")
        }

        val params = mutableMapOf(
            "activeId" to activeId,
            "courseId" to courseId,
            "uid" to uid,
            "clientip" to "",
            "useragent" to "",
            "latitude" to "-1",
            "longitude" to "-1",
            "appType" to "15",
            "fid" to "0",
            "objectId" to objectId,
            "name" to user.name,
            "validate" to validate,
            "deviceCode" to EncryptionUtils.getDeviceCode()
        )

        if (params["objectId"] == "") params.remove("objectId")
        if (params["validate"] == "") params.remove(params["validate"])

        return NetworkClient.get(
            "https://mobilelearn.chaoxing.com/pptSign/stuSignajax",
            params = params,
            asUser = uid
        )
    }

    suspend fun checkInQrCode(
        uid: String,
        activeId: String,
        courseId: String,
        enc: String,
        latitude: Double = .0,
        longitude: Double = .0,
        address: String = "",
        enc2: String = "",
        validate: String = "",
        faceId: String = "",
        faceEnc: String = ""
    ): ApiResult<String> {
        val accounts = AccountRepository.getCurrentListSnapshot()
        val user = accounts.find { it.uid == uid }
        if (user == null) {
            return ApiResult.Error("未找到用户")
        }

        val locationJson = """
            {
                "result": 1,
                "latitude":$latitude,
                "longitude":$longitude,
                "mockData": {
                    "strategy": 0,
                    "probability": -1
                },
                "address": "$address"
            }
        """.trimIndent()

        val params = mutableMapOf(
            "enc" to enc,
            "name" to user.name,
            "activeId" to activeId,
            "uid" to uid,
            "clientip" to "",
            "location" to locationJson,
            "latitude" to "-1",
            "longitude" to "-1",
            "fid" to "0",
            "appType" to "15",
            "deviceCode" to EncryptionUtils.getDeviceCode(),
            "vpProbability" to "",
            "vpStrategy" to "",
            "enc2" to enc2,
            "validate" to validate,
            "currentFaceId" to faceId,
            "ifCFP" to "0",
            "courseId" to courseId,
            "faceEnc" to faceEnc
        )

        if (params["enc2"] == "") params.remove("enc2")
        if (params["validate"] == "") params.remove("validate")
        if (params["faceEnc"] == "") params.remove("faceEnc")

        return NetworkClient.get(
            "https://mobilelearn.chaoxing.com/pptSign/stuSignajax",
            params = params,
            asUser = uid
        )
    }

    suspend fun checkInCode(
        uid: String,
        activeId: String,
        courseId: String,
        signCode: String,
        validate: String
    ): ApiResult<String> {
        val accounts = AccountRepository.getCurrentListSnapshot()
        val user = accounts.find { it.uid == uid }
        if (user == null) {
            return ApiResult.Error("未找到用户")
        }

        val params = mutableMapOf(
            "activeId" to activeId,
            "courseId" to courseId,
            "uid" to uid,
            "clientip" to "",
            "latitude" to "-1",
            "longitude" to "-1",
            "appType" to "15",
            "fid" to "0",
            "name" to user.name,
            "signCode" to signCode,
            "validate" to validate,
            "deviceCode" to EncryptionUtils.getDeviceCode()
        )

        if (params["validate"] == "") params.remove("validate")

        return NetworkClient.get(
            "https://mobilelearn.chaoxing.com/pptSign/stuSignajax",
            params = params,
            asUser = uid
        )
    }

    suspend fun checkInLocation(
        uid: String,
        activeId: String,
        courseId: String,
        latitude: Double,
        longitude: Double,
        address: String = "",
        validate: String = "",
        faceId: String = "",
        faceEnc: String = ""
    ): ApiResult<String> {
        val accounts = AccountRepository.getCurrentListSnapshot()
        val user = accounts.find { it.uid == uid }
        if (user == null) {
            return ApiResult.Error("未找到用户")
        }

        val params = mutableMapOf(
            "name" to user.name,
            "address" to address,
            "activeId" to activeId,
            "courseId" to courseId,
            "uid" to uid,
            "clientip" to "",
            "latitude" to latitude.toString(),
            "longitude" to longitude.toString(),
            "fid" to "0",
            "appType" to "15",
            "ifTiJiao" to "1",
            "validate" to validate,
            "deviceCode" to EncryptionUtils.getDeviceCode(),
            "vpProbability" to "-1",
            "vpStrategy" to "",
            "currentFaceId" to faceId,
            "ifCFP" to "0",
            "faceEnc" to faceEnc
        )

        if (params["validate"] == "") params.remove("validate")
        if (params["faceEnc"] == "") params.remove("faceEnc")

        return NetworkClient.get(
            "https://mobilelearn.chaoxing.com/pptSign/stuSignajax",
            params = params,
            asUser = uid
        )
    }
}
