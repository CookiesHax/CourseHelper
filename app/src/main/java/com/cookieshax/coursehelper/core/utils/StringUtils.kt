package com.cookieshax.coursehelper.core.utils

import com.google.gson.Gson
import com.google.gson.JsonObject

object StringUtils {
    // 全局单例
    val gson: Gson = Gson()

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

fun JsonObject.getStringOrEmpty(key: String): String =
    if (has(key) && get(key).isJsonPrimitive) get(key).asString else ""

fun JsonObject.getStringOrDefault(key: String, default: String): String =
    if (has(key) && get(key).isJsonPrimitive) get(key).asString else default

fun JsonObject.getStringOrNull(key: String): String? =
    if (has(key) && get(key).isJsonPrimitive) get(key).asString else null

fun JsonObject.getIntOrDefault(key: String, default: Int): Int =
    if (has(key) && get(key).isJsonPrimitive) get(key).asInt else default

fun JsonObject.getBooleanOrDefault(key: String, default: Boolean): Boolean =
    if (has(key) && get(key).isJsonPrimitive) get(key).asBoolean else default
