package com.cookieshax.coursehelper.core.database.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cookieshax.coursehelper.core.utils.Constant
import com.cookieshax.coursehelper.core.utils.EncryptionUtils
import com.cookieshax.coursehelper.core.utils.StringUtils
import com.cookieshax.coursehelper.core.database.model.AccountStatus
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

@Keep
@Entity(tableName = "accounts")
data class Account(
    @SerializedName("pic") val avatarUrl: String = "",
    val name: String = "未知用户名",
    @PrimaryKey
    @SerializedName("puid") val uid: String = "",
    val phone: String = "未知手机号",
    var deviceInfo: JsonObject? = null,
    val status: AccountStatus = AccountStatus.UNKNOWN,
    val order: Int = 0 // 用于排序
) {
    companion object {
        fun fromJsonObject(json: JsonObject): Account {
            val account = Account(
                avatarUrl = json.get("pic")?.asString ?: "",
                name = json.get("name")?.asString ?: "未知用户名",
                uid = json.get("puid")?.asString ?: "",
                phone = json.get("phone")?.asString ?: "未知手机号",
                // status 和 order 在 JSON 中不存在 使用构造函数的默认值
                status = json.get("status")?.let {
                    try {
                        AccountStatus.valueOf(it.asString)
                    } catch (_: Exception) {
                        AccountStatus.UNKNOWN
                    }
                } ?: AccountStatus.UNKNOWN,
                order = json.get("order")?.asInt ?: 0
            )

            val clientIdCipher = json.get("clientId")?.asString
            if (!clientIdCipher.isNullOrBlank()) {
                val deviceInfoJson =
                    EncryptionUtils.rsaDecrypt(clientIdCipher, Constant.DEVICE_INFO_KEY)
                if (deviceInfoJson.isNotEmpty()) {
                    account.deviceInfo = StringUtils.parseJson(deviceInfoJson)
                }
            }

            return account.copy(
                avatarUrl = account.avatarUrl.let {
                    when {
                        it.isBlank() -> ""
                        it.startsWith("https://", ignoreCase = true) -> it
                        it.startsWith("http://", ignoreCase = true) -> it.replaceFirst(
                            "http://", "https://", ignoreCase = true
                        )

                        else -> it
                    }
                }
            )
        }
    }
}
