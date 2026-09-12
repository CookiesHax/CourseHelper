package com.cookieshax.coursehelper.feature.course.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cookieshax.coursehelper.core.network.ApiManager
import com.cookieshax.coursehelper.core.network.ApiResult
import com.cookieshax.coursehelper.core.utils.StringUtils
import com.cookieshax.coursehelper.core.utils.getIntOrDefault
import com.cookieshax.coursehelper.core.utils.getAsJsonObjectOrNull
import com.cookieshax.coursehelper.core.utils.getLongOrDefault
import com.cookieshax.coursehelper.core.utils.getStringOrDefault
import com.cookieshax.coursehelper.core.utils.getStringOrNull
import com.cookieshax.coursehelper.core.utils.showToast
import com.cookieshax.coursehelper.feature.course.model.NoticeContent
import com.cookieshax.coursehelper.feature.course.model.NoticeElement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class NoticeState {
    data object Loading : NoticeState()
    data class Success(val content: NoticeContent) : NoticeState()
    data class Error(val message: String) : NoticeState()
}

class NoticeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<NoticeState>(NoticeState.Loading)
    val uiState: StateFlow<NoticeState> = _uiState.asStateFlow()

    fun loadNotice(url: String) {
        viewModelScope.launch {
            _uiState.value = NoticeState.Loading
            when (val initialResult = ApiManager.getNoticeContent(url)) {
                is ApiResult.Success -> {
                    val sn = extractSn(initialResult.data)
                    var content: NoticeContent? = null
                    
                    if (sn != null) {
                        val baseUrl = extractBaseUrl(url)
                        val dataUrl = "$baseUrl/share/notice/$sn/notice_data"
                        val dataResult = ApiManager.getNoticeContent(dataUrl)
                        if (dataResult is ApiResult.Success) {
                            content = parseNoticeJson(dataResult.data, baseUrl)
                        }
                    }

                    if (content == null) {
                        content = parseNoticeHtml(initialResult.data, url)
                    }

                    // 开始异步下载图片
                    downloadImages(content)
                    _uiState.value = NoticeState.Success(content)
                }
                is ApiResult.Error -> {
                    _uiState.value = NoticeState.Error(initialResult.message)
                }
            }
        }
    }

    private suspend fun downloadImages(content: NoticeContent) {
        content.elements.filterIsInstance<NoticeElement.Image>().forEach { imgElement ->
            val result = ApiManager.downloadImage(imgElement.url)
            if (result is ApiResult.Success) {
                imgElement.imageData = result.data
                // 强制触发 UI 更新
                _uiState.value = NoticeState.Success(content.copy())
            }
        }
    }

    private fun extractSn(html: String): String? {
        val regex = Regex("var sn = '([A-Z0-9]+)'")
        return regex.find(html)?.groupValues?.get(1)
    }

    private fun extractBaseUrl(url: String): String {
        return try {
            val uri = java.net.URI(url)
            "${uri.scheme}://${uri.host}"
        } catch (_: Exception) {
            val endIdx = url.indexOf("/", 8)
            if (endIdx != -1) url.substring(0, endIdx) else url
        }
    }

    private fun parseNoticeJson(jsonStr: String, baseUrl: String): NoticeContent? {
        val json = StringUtils.parseJson(jsonStr) ?: return null
        if (json.getIntOrDefault("result", 0) != 1) return null
        
        val data = json.getAsJsonObjectOrNull("data") ?: return null
        val title = data.getStringOrDefault("title", "")
        val sender = data.getStringOrDefault("createrName", "")
        val timeLong = data.getLongOrDefault("insertTime", 0)
        val time = if (timeLong > 0) {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timeLong))
        } else ""

        val elements = mutableListOf<NoticeElement>()
        
        val rtfContent = data.getStringOrNull("rtf_content")
        if (!rtfContent.isNullOrBlank()) {
            val doc = Jsoup.parse(rtfContent, baseUrl)
            val textBuilder = StringBuilder()
            doc.body().childNodes().forEach { node ->
                traverse(node, elements, textBuilder)
            }
            flushText(elements, textBuilder)
        } else {
            val content = data.getStringOrDefault("content", "")
            if (content.isNotBlank()) {
                elements.add(NoticeElement.Text(content))
            }
        }

        val imgs = data.get("imgs")
        if (imgs != null && imgs.isJsonArray) {
            imgs.asJsonArray.forEach { img ->
                if (img.isJsonObject) {
                    val imgUrl = img.asJsonObject.getStringOrNull("imgUrl")
                    if (!imgUrl.isNullOrBlank()) {
                        elements.add(NoticeElement.Image(imgUrl))
                    }
                }
            }
        }

        val attachmentStr = data.getStringOrNull("attachment")
        if (!attachmentStr.isNullOrBlank()) {
            try {
                val attachments = StringUtils.gson.fromJson(attachmentStr, com.google.gson.JsonElement::class.java)
                if (attachments.isJsonArray) {
                    attachments.asJsonArray.forEach { attElement ->
                        if (attElement.isJsonObject) {
                            val att = attElement.asJsonObject
                            val type = att.getIntOrDefault("attachmentType", -1)
                            if (type == 18) {
                                val fileObj = att.getAsJsonObjectOrNull("att_file")
                                    ?: att.getAsJsonObjectOrNull("att_clouddisk")
                                
                                if (fileObj != null) {
                                    val infoJsonStr = fileObj.getStringOrNull("infoJsonStr")
                                    val finalFileObj = if (!infoJsonStr.isNullOrBlank()) {
                                        StringUtils.parseJson(infoJsonStr) ?: fileObj
                                    } else {
                                        fileObj
                                    }

                                    elements.add(NoticeElement.Attachment(
                                        name = finalFileObj.getStringOrDefault("fileName", 
                                            finalFileObj.getStringOrDefault("name", "未知文件")),
                                        size = finalFileObj.getStringOrDefault("fileSize", 
                                            finalFileObj.getStringOrDefault("size", "")),
                                        objectId = finalFileObj.getStringOrDefault("objectId", ""),
                                        resid = finalFileObj.getStringOrDefault("resid", ""),
                                        type = "file"
                                    ))
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return NoticeContent(title, sender, time, elements)
    }

    private fun parseNoticeHtml(html: String, baseUrl: String): NoticeContent {
        val doc = Jsoup.parse(html, baseUrl)
        val title = doc.select(".shareTit, .title, h2, h1").firstOrNull()?.text() ?: doc.title()
        val sender = doc.select(".shareAuthorText, .name, .sender").firstOrNull()?.text() ?: ""
        val time = doc.select(".shareTime, .time, .date").firstOrNull()?.text() ?: ""
        val contentElement = doc.select(".shareCon, .content, #mainContent").firstOrNull() ?: doc.body()

        val elements = mutableListOf<NoticeElement>()
        val textBuilder = StringBuilder()
        contentElement.childNodes().forEach { node ->
            traverse(node, elements, textBuilder)
        }
        flushText(elements, textBuilder)
        return NoticeContent(title, sender, time, elements)
    }

    private fun traverse(node: Node, elements: MutableList<NoticeElement>, textBuilder: StringBuilder) {
        when (node) {
            is TextNode -> {
                val text = node.wholeText
                if (text.isNotBlank()) {
                    textBuilder.append(text.replace(Regex("[ \t\r\\f]+"), " "))
                }
            }
            is Element -> {
                val tagName = node.tagName().lowercase()
                when {
                    tagName == "img" -> {
                        flushText(elements, textBuilder)
                        val src = node.absUrl("data-src").ifEmpty {
                            node.absUrl("src").ifEmpty { node.attr("src") }
                        }
                        if (src.isNotEmpty()) {
                            elements.add(NoticeElement.Image(src))
                        }
                    }
                    tagName == "br" -> {
                        textBuilder.append("\n")
                    }
                    isBlockElement(tagName) -> {
                        flushText(elements, textBuilder)
                        node.childNodes().forEach { child ->
                            traverse(child, elements, textBuilder)
                        }
                        flushText(elements, textBuilder)
                    }
                    else -> {
                        node.childNodes().forEach { child ->
                            traverse(child, elements, textBuilder)
                        }
                    }
                }
            }
        }
    }

    private fun isBlockElement(tag: String): Boolean =
        tag in setOf("p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li", "ul", "ol", "blockquote", "section", "article")

    private fun flushText(elements: MutableList<NoticeElement>, textBuilder: StringBuilder) {
        val text = textBuilder.toString().trim()
        if (text.isNotEmpty()) {
            elements.add(NoticeElement.Text(text))
        }
        textBuilder.setLength(0)
    }

    fun openAttachment(
        attachment: NoticeElement.Attachment,
        download: Boolean = false,
        onUrlReady: (String) -> Unit
    ) {
        if (attachment.objectId.isEmpty()) {
            "无法获取附件标识".showToast()
            return
        }

        viewModelScope.launch {
            val result = if (download) {
                ApiManager.getAttachmentDownloadUrl(
                    objectId = attachment.objectId,
                    resid = attachment.resid
                )
            } else {
                ApiManager.getAttachmentPreviewUrl(
                    objectId = attachment.objectId,
                    fileName = attachment.name,
                    resid = attachment.resid,
                    download = false
                )
            }

            when (result) {
                is ApiResult.Success -> {
                    if (result.data.isNotEmpty()) {
                        onUrlReady(result.data)
                    } else {
                        "获取地址失败".showToast()
                    }
                }
                is ApiResult.Error -> {
                    result.message.showToast()
                }
            }
        }
    }
}
