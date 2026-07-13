package com.cookieshax.coursehelper.feature.checkin.model

import com.cookieshax.coursehelper.core.network.ApiManager
import com.cookieshax.coursehelper.core.network.ApiResult
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

sealed class CheckInParams {
    data class Normal(
        val url: String,
        val taskId: String,
        val courseId: String,
        val uploadedObjectIds: Map<String, String>,
        val validate: String = ""
    ) : CheckInParams()

    data class QrCode(
        val url: String,
        val taskId: String,
        val courseId: String,
        val enc: String,
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val isNeedFaceCheck: Boolean,
        val enc2: String = "",
        val validate: String = ""
    ) : CheckInParams()

    data class Location(
        val url: String,
        val taskId: String,
        val courseId: String,
        val latitude: Double,
        val longitude: Double,
        val address: String,
        val isNeedFaceCheck: Boolean,
        val validate: String = ""
    ) : CheckInParams()

    data class Gesture(
        val url: String,
        val taskId: String,
        val courseId: String,
        val gestureCode: String,
        val validate: String = ""
    ) : CheckInParams()

    data class Code(
        val url: String,
        val taskId: String,
        val courseId: String,
        val signCode: String,
        val validate: String = ""
    ) : CheckInParams()
}

interface CheckInStrategy {
    suspend fun execute(
        uid: String,
        params: CheckInParams,
        faceCache: FaceCache? = null
    ): ApiResult<String>
}

class FaceCache {
    data class Entry(
        val faceId: String,
        val faceEnc: String,
        val timestamp: Long
    )

    private val cache = ConcurrentHashMap<String, Entry>()

    fun get(uid: String, taskId: String): Pair<String, String>? {
        val entry = cache["${uid}_${taskId}"] ?: return null
        if (System.currentTimeMillis() - entry.timestamp < 60000) {
            return entry.faceId to entry.faceEnc
        }
        return null
    }

    fun put(uid: String, taskId: String, faceId: String, faceEnc: String) {
        cache["${uid}_${taskId}"] = Entry(faceId, faceEnc, System.currentTimeMillis())
    }
}

class NormalCheckInStrategy : CheckInStrategy {
    override suspend fun execute(
        uid: String,
        params: CheckInParams,
        faceCache: FaceCache?
    ): ApiResult<String> {
        val p = params as CheckInParams.Normal
        val objectId = p.uploadedObjectIds[uid] ?: ""
        return ApiManager.checkIn(
            uid = uid,
            activeId = p.taskId,
            courseId = p.courseId,
            objectId = objectId,
            validate = p.validate
        )
    }
}

class QrCodeCheckInStrategy : CheckInStrategy {
    override suspend fun execute(
        uid: String,
        params: CheckInParams,
        faceCache: FaceCache?
    ): ApiResult<String> {
        val p = params as CheckInParams.QrCode
        var faceId = ""
        var faceEnc = ""
        if (p.isNeedFaceCheck) {
            val cached = faceCache?.get(uid, p.taskId)
            if (cached != null) {
                faceId = cached.first
                faceEnc = cached.second
            } else {
                val result = ApiManager.getUserFaceId(uid)
                if (result is ApiResult.Success) {
                    val json = JSONObject(result.data)
                    val data = json.optJSONObject("data")
                    val faceUrl = data?.optString("http", "") ?: ""
                    when (val uploadResult = ApiManager.uploadModifiedFace(faceUrl, uid)) {
                        is ApiResult.Success -> {
                            faceId = uploadResult.data
                        }

                        is ApiResult.Error -> {
                            return uploadResult
                        }
                    }
                } else {
                    return ApiResult.Error("获取人脸信息失败")
                }

                val encResult = ApiManager.getFaceEnc(p.taskId, faceId, asUser = uid)
                if (encResult is ApiResult.Success) {
                    faceEnc = encResult.data
                    faceCache?.put(uid, p.taskId, faceId, faceEnc)
                } else {
                    return encResult
                }
            }
        }

        return ApiManager.checkInQrCode(
            uid = uid,
            activeId = p.taskId,
            courseId = p.courseId,
            enc = p.enc,
            latitude = p.latitude,
            longitude = p.longitude,
            address = p.address,
            enc2 = p.enc2,
            validate = p.validate,
            faceId = faceId,
            faceEnc = faceEnc
        )
    }
}

class LocationCheckInStrategy : CheckInStrategy {
    override suspend fun execute(
        uid: String,
        params: CheckInParams,
        faceCache: FaceCache?
    ): ApiResult<String> {
        val p = params as CheckInParams.Location
        var faceId = ""
        var faceEnc = ""
        if (p.isNeedFaceCheck) {
            val cached = faceCache?.get(uid, p.taskId)
            if (cached != null) {
                faceId = cached.first
                faceEnc = cached.second
            } else {
                val result = ApiManager.getUserFaceId(uid)
                if (result is ApiResult.Success) {
                    val json = JSONObject(result.data)
                    val data = json.optJSONObject("data")
                    val faceUrl = data?.optString("http", "") ?: ""
                    when (val uploadResult = ApiManager.uploadModifiedFace(faceUrl, uid)) {
                        is ApiResult.Success -> {
                            faceId = uploadResult.data
                        }

                        is ApiResult.Error -> {
                            return uploadResult
                        }
                    }
                } else {
                    return ApiResult.Error("获取人脸信息失败")
                }

                val encResult = ApiManager.getFaceEnc(p.taskId, faceId, asUser = uid)
                if (encResult is ApiResult.Success) {
                    faceEnc = encResult.data
                    faceCache?.put(uid, p.taskId, faceId, faceEnc)
                } else {
                    return encResult
                }
            }
        }

        return ApiManager.checkInLocation(
            uid = uid,
            activeId = p.taskId,
            courseId = p.courseId,
            latitude = p.latitude,
            longitude = p.longitude,
            address = p.address,
            validate = p.validate,
            faceId = faceId,
            faceEnc = faceEnc
        )
    }
}

class GestureCheckInStrategy : CheckInStrategy {
    override suspend fun execute(
        uid: String,
        params: CheckInParams,
        faceCache: FaceCache?
    ): ApiResult<String> {
        val p = params as CheckInParams.Gesture
        return ApiManager.checkInCode(
            uid = uid,
            activeId = p.taskId,
            courseId = p.courseId,
            signCode = p.gestureCode,
            validate = p.validate
        )
    }
}

class CodeCheckInStrategy : CheckInStrategy {
    override suspend fun execute(
        uid: String,
        params: CheckInParams,
        faceCache: FaceCache?
    ): ApiResult<String> {
        val p = params as CheckInParams.Code
        return ApiManager.checkInCode(
            uid = uid,
            activeId = p.taskId,
            courseId = p.courseId,
            signCode = p.signCode,
            validate = p.validate
        )
    }
}
