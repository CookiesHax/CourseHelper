package com.cookieshax.coursehelper.core.utils

import android.widget.Toast
import com.cookieshax.coursehelper.app.CourseHelperApplication
import com.google.gson.Gson
import com.google.gson.JsonObject

object StringUtils {
    // 全局单例
    val gson: Gson = Gson()
    val prettyGson: Gson = Gson().newBuilder().setPrettyPrinting().create()

    // 解析 JSON 响应为 JsonObject
    fun parseJson(json: String): JsonObject? {
        return try {
            gson.fromJson(json, JsonObject::class.java)
        } catch (_: Exception) {
            null
        }
    }

    // 从 JSON 中提取字符串字段
    fun getString(json: JsonObject, key: String, default: String = ""): String {
        return try {
            json.get(key)?.asString ?: default
        } catch (_: Exception) {
            default
        }
    }

    // 从 JSON 中提取布尔字段
    fun getBoolean(json: JsonObject, key: String, default: Boolean = false): Boolean {
        return try {
            json.get(key)?.asBoolean ?: default
        } catch (_: Exception) {
            default
        }
    }
}

val String.maskedPhone: String
    get() = if (this.length == 11) {
        "${this.substring(0, 3)}****${this.substring(7)}"
    } else {
        this
    }

fun String.showToast(duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(CourseHelperApplication.context, this, duration).show()
}

fun JsonObject.getStringOrNull(key: String): String? {
    val element = get(key) ?: return null
    if (element.isJsonNull || !element.isJsonPrimitive) return null
    return element.asString
}

fun JsonObject.getStringOrDefault(key: String, default: String): String =
    getStringOrNull(key) ?: default

fun JsonObject.getStringOrEmpty(key: String): String =
    getStringOrDefault(key, "")

fun JsonObject.getIntOrDefault(key: String, default: Int): Int =
    getStringOrNull(key)?.trim()?.toIntOrNull() ?: default

fun JsonObject.getLongOrDefault(key: String, default: Long): Long =
    getStringOrNull(key)?.trim()?.toLongOrNull() ?: default

fun JsonObject.getDoubleOrDefault(key: String, default: Double): Double =
    getStringOrNull(key)?.trim()?.toDoubleOrNull() ?: default

fun JsonObject.getBooleanOrDefault(key: String, default: Boolean): Boolean {
    val str = getStringOrNull(key)?.trim() ?: return default
    return str.toBooleanStrictOrNull() ?: default
}

fun JsonObject.getAsJsonObjectOrNull(key: String): JsonObject? {
    val element = get(key) ?: return null
    return if (element.isJsonObject) element.asJsonObject else null
}
