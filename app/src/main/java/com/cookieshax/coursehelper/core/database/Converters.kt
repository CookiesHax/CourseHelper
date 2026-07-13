package com.cookieshax.coursehelper.core.database

import androidx.room.TypeConverter
import com.cookieshax.coursehelper.core.utils.StringUtils
import com.cookieshax.coursehelper.feature.account.model.AccountStatus
import com.google.gson.JsonObject

class Converters {
    @TypeConverter
    fun fromAccountStatus(status: AccountStatus): String = status.name

    @TypeConverter
    fun toAccountStatus(name: String): AccountStatus = try {
        AccountStatus.valueOf(name)
    } catch (_: Exception) {
        AccountStatus.UNKNOWN
    }

    @TypeConverter
    fun fromJsonObject(jsonObject: JsonObject?): String? = jsonObject?.toString()

    @TypeConverter
    fun toJsonObject(json: String?): JsonObject? = json?.let { StringUtils.parseJson(it) }
}
