package com.cookieshax.coursehelper.feature.checkin.model

import com.cookieshax.coursehelper.core.network.ApiManager
import com.cookieshax.coursehelper.core.network.ApiResult
import com.cookieshax.coursehelper.core.utils.Constant
import com.cookieshax.coursehelper.core.utils.EncryptionUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject

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
            val confJson = parseJson(confResp.data)
            val t = confJson.getLong("t")
            val captchaKey = EncryptionUtils.md5Hash("$t$uuid")
            val expirationTime = t + 300000
            val currentToken =
                "${EncryptionUtils.md5Hash("${t}${Constant.CAPTCHA_ID}slide$captchaKey")}:$expirationTime"
            val currentIv =
                EncryptionUtils.md5Hash("${Constant.CAPTCHA_ID}slide$currentTimestamp$uuid")

            val imgResp =
                ApiManager.getCaptchaImageUrl(captchaKey, currentToken, referer, currentIv, uid)
            if (imgResp is ApiResult.Success) {
                val imgJson = parseJson(imgResp.data)
                val vo = imgJson.optJSONObject("imageVerificationVo")
                val bgUrl = vo?.optString("shadeImage", "") ?: ""
                val sliceUrl = vo?.optString("cutoutImage", "") ?: ""
                val finalToken = imgJson.optString("token", currentToken)

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
                val json = parseJson(resultResp.data)
                if (json.optInt("error") == 0 && json.optBoolean("result")) {
                    val extraDataStr = json.optString("extraData")
                    JSONObject(extraDataStr).optString("validate") to this
                } else {
                    val msg = json.optString("msg", "校验未通过")
                    null to this.copy(errorMessage = msg, isLoaded = false)
                }
            } catch (_: Exception) {
                null to this.copy(isLoaded = false)
            }
        } else {
            null to this.copy(isLoaded = false)
        }
    }

    private fun parseJson(data: String): JSONObject {
        val jsonString = data.substringAfter("(").substringBeforeLast(")")
        return JSONObject(jsonString)
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
