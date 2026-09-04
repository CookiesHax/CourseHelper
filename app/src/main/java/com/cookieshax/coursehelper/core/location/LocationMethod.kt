package com.cookieshax.coursehelper.core.location

enum class LocationMethod(val description: String) {
    BAIDU("百度定位 SDK (无效则 fallback GPS)"),
    GPS_ONLY("仅 GPS"),
    GPS_ALWAYS("仅 GPS (始终运行定位)")
}
