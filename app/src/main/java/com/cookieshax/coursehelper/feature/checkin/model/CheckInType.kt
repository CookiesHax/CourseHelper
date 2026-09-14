package com.cookieshax.coursehelper.feature.checkin.model

sealed class CheckInType {
    object Normal : CheckInType()
    object QRCode : CheckInType()
    object Gesture : CheckInType()
    object Location : CheckInType()
    object Code : CheckInType()
    object Unknown : CheckInType()
}
