package com.cookieshax.coursehelper.feature.checkin.model

import androidx.annotation.Keep

@Keep
data class CheckInState(
    // 通用状态
    var otherId: String = "",
    var ifNeedVCode: Int = 0,
    var openCheckFaceFlag: Int = 0,
    var starttime: Long = 0L,
    var endTime: Long = 0L,
    var signInId: Long = 0L, // 存疑
    var signOutId: Long = 0L, // 存疑
    var signOutPublishTimeStamp: Long = 0L,

    // 位置签到
    var locationLatitude: Double = .0,
    var locationLongitude: Double = .0,
    var locationRange: Double = .0,
    var locationText: String = "",

    // 拍照签到
    var ifphoto: Int = 0,

    // 二维码签到
    var ifopenAddress: Int = 0,
    var ifrefreshewm: Int = 0,

    // 签到码签到
    var numberCount: Int = 0
)

fun mapToCheckInType(id: String?): CheckInType = when (id) {
    "0" -> CheckInType.Normal
    "2" -> CheckInType.QRCode
    "3" -> CheckInType.Gesture
    "4" -> CheckInType.Location
    "5" -> CheckInType.Code
    else -> CheckInType.Unknown
}
