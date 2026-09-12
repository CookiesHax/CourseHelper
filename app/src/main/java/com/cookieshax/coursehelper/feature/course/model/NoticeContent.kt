package com.cookieshax.coursehelper.feature.course.model

data class NoticeContent(
    val title: String = "",
    val sender: String = "",
    val time: String = "",
    val elements: List<NoticeElement> = emptyList()
)

sealed class NoticeElement {
    data class Text(val text: String) : NoticeElement()
    data class Image(
        val url: String,
        var imageData: ByteArray? = null
    ) : NoticeElement() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Image

            if (url != other.url) return false
            if (!imageData.contentEquals(other.imageData)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + (imageData?.contentHashCode() ?: 0)
            return result
        }
    }

    data class Attachment(
        val name: String,
        val size: String,
        val objectId: String = "",
        val resid: String = "",
        val type: String
    ) : NoticeElement()
}
