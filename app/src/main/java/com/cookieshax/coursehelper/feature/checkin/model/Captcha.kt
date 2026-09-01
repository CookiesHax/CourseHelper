package com.cookieshax.coursehelper.feature.checkin.model

import com.cookieshax.coursehelper.core.network.ApiManager
import com.cookieshax.coursehelper.core.network.ApiResult
import com.cookieshax.coursehelper.core.utils.Constant
import com.cookieshax.coursehelper.core.utils.EncryptionUtils
import com.cookieshax.coursehelper.core.utils.StringUtils
import com.cookieshax.coursehelper.core.utils.getBooleanOrDefault
import com.cookieshax.coursehelper.core.utils.getIntOrDefault
import com.cookieshax.coursehelper.core.utils.getLongOrDefault
import com.cookieshax.coursehelper.core.utils.getStringOrDefault
import com.cookieshax.coursehelper.core.utils.getStringOrEmpty
import com.cookieshax.coursehelper.core.utils.getStringOrNull
import com.cookieshax.coursehelper.core.utils.getAsJsonObjectOrNull
import com.google.gson.JsonObject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class Captcha(
    val uid: String,
    val referer: String,
    val timestamp: Long = 0,
    val iv: String = "",
    val token: String = "",
    val bgUrl: String = "",
    val sliceUrl: String = "",
    val bgData: ByteArray? = null,
    val sliceData: ByteArray? = null,
    val isLoaded: Boolean = false,
    val errorMessage: String = ""
) {
    suspend fun load(): Captcha = coroutineScope {
        val currentTimestamp = System.currentTimeMillis()
        val uuid = EncryptionUtils.getPlainUuid()

        val confResp = ApiManager.getCptchaConf(uid)
        if (confResp !is ApiResult.Success) {
            return@coroutineScope this@Captcha.copy(
                errorMessage = "获取配置接口请求失败",
                isLoaded = false
            )
        }

        try {
            val confJson = extractPayload(confResp.data) ?: return@coroutineScope this@Captcha.copy(
                errorMessage = "解析配置数据失败",
                isLoaded = false
            )
            val t = confJson.getLongOrDefault("t", 0L)
            val captchaKey = EncryptionUtils.md5Hash("$t$uuid")
            val expirationTime = t + 300000
            val currentToken =
                "${EncryptionUtils.md5Hash("${t}${Constant.CAPTCHA_ID}slide$captchaKey")}:$expirationTime"
            val currentIv =
                EncryptionUtils.md5Hash("${Constant.CAPTCHA_ID}slide$currentTimestamp$uuid")

            val imgResp =
                ApiManager.getCaptchaImageUrl(captchaKey, currentToken, referer, currentIv, uid)
            if (imgResp is ApiResult.Success) {
                val imgJson = extractPayload(imgResp.data) ?: return@coroutineScope this@Captcha.copy(
                    errorMessage = "解析图片数据失败",
                    isLoaded = false
                )
                val vo = imgJson.getAsJsonObjectOrNull("imageVerificationVo")
                val bgUrl = vo?.getStringOrEmpty("shadeImage") ?: ""
                val sliceUrl = vo?.getStringOrEmpty("cutoutImage") ?: ""
                val finalToken = imgJson.getStringOrDefault("token", currentToken)

                val bgDeferred = async { ApiManager.downloadImage(bgUrl, uid) }
                val sliceDeferred = async { ApiManager.downloadImage(sliceUrl, uid) }
                val bgResp = bgDeferred.await()
                val sliceResp = sliceDeferred.await()

                if (bgResp is ApiResult.Success && sliceResp is ApiResult.Success) {
                    this@Captcha.copy(
                        timestamp = currentTimestamp,
                        iv = currentIv,
                        token = finalToken,
                        bgUrl = bgUrl,
                        sliceUrl = sliceUrl,
                        bgData = bgResp.data,
                        sliceData = sliceResp.data,
                        isLoaded = true,
                        errorMessage = ""
                    )
                } else {
                    this@Captcha.copy(errorMessage = "图像下载失败", isLoaded = false)
                }
            } else {
                this@Captcha.copy(errorMessage = "图片获取失败", isLoaded = false)
            }
        } catch (e: Exception) {
            this@Captcha.copy(errorMessage = "解析数据异常: ${e.message}", isLoaded = false)
        }
    }

    suspend fun submit(x: Int): Pair<String?, Captcha> {
        val resultResp = ApiManager.submitCaptcha(
            xValue = x,
            token = token,
            iv = iv,
            timestamp = timestamp + 2,
            referer = referer
        )
        return if (resultResp is ApiResult.Success) {
            try {
                val json = extractPayload(resultResp.data) ?: return null to this.copy(isLoaded = false)
                if (json.getIntOrDefault("error", -1) == 0 && json.getBooleanOrDefault("result", false)) {
                    val extraDataStr = json.getStringOrNull("extraData")
                    val validate = extraDataStr?.let { StringUtils.parseJson(it)?.getStringOrNull("validate") }
                    validate to this
                } else {
                    val msg = json.getStringOrDefault("msg", "校验未通过")
                    null to this.copy(errorMessage = msg, isLoaded = false)
                }
            } catch (_: Exception) {
                null to this.copy(isLoaded = false)
            }
        } else {
            null to this.copy(isLoaded = false)
        }
    }

    private fun extractPayload(data: String): JsonObject? {
        if (data.isBlank()) return null
        val start = data.indexOf('(')
        val end = data.lastIndexOf(')')
        val jsonString = if (start != -1 && end != -1 && start < end) {
            data.substring(start + 1, end)
        } else {
            data
        }
        return StringUtils.parseJson(jsonString)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Captcha

        if (timestamp != other.timestamp) return false
        if (isLoaded != other.isLoaded) return false
        if (uid != other.uid) return false
        if (referer != other.referer) return false
        if (iv != other.iv) return false
        if (token != other.token) return false
        if (bgUrl != other.bgUrl) return false
        if (sliceUrl != other.sliceUrl) return false
        if (!bgData.contentEquals(other.bgData)) return false
        if (!sliceData.contentEquals(other.sliceData)) return false
        if (errorMessage != other.errorMessage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + isLoaded.hashCode()
        result = 31 * result + uid.hashCode()
        result = 31 * result + referer.hashCode()
        result = 31 * result + iv.hashCode()
        result = 31 * result + token.hashCode()
        result = 31 * result + bgUrl.hashCode()
        result = 31 * result + sliceUrl.hashCode()
        result = 31 * result + (bgData?.contentHashCode() ?: 0)
        result = 31 * result + (sliceData?.contentHashCode() ?: 0)
        result = 31 * result + errorMessage.hashCode()
        return result
    }
}
