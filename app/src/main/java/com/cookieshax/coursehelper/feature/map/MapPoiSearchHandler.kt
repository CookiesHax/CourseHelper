package com.cookieshax.coursehelper.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.baidu.mapapi.search.core.PoiInfo
import com.baidu.mapapi.search.core.SearchResult
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener
import com.baidu.mapapi.search.poi.PoiCitySearchOption
import com.baidu.mapapi.search.poi.PoiDetailResult
import com.baidu.mapapi.search.poi.PoiDetailSearchResult
import com.baidu.mapapi.search.poi.PoiIndoorResult
import com.baidu.mapapi.search.poi.PoiResult
import com.baidu.mapapi.search.poi.PoiSearch
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * POI 搜索逻辑处理器
 * 封装 PoiSearch 实例 搜索防抖和结果监听
 */
@Composable
fun rememberPoiSearchHandler(
    searchDelayMs: Long = 300L
): PoiSearchHandler {
    val poiSearch = remember { PoiSearch.newInstance() }
    var searchResults by remember { mutableStateOf<List<PoiInfo>>(emptyList()) }

    DisposableEffect(Unit) {
        val listener = object : OnGetPoiSearchResultListener {
            override fun onGetPoiResult(result: PoiResult?) {
                if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
                    searchResults = emptyList()
                    return
                }
                searchResults = result.allPoi ?: emptyList()
            }

            @Deprecated("Deprecated in Java")
            override fun onGetPoiDetailResult(result: PoiDetailResult?) {
            }

            override fun onGetPoiDetailResult(result: PoiDetailSearchResult?) {}

            override fun onGetPoiIndoorResult(result: PoiIndoorResult?) {}
        }

        poiSearch.setOnGetPoiSearchResultListener(listener)
        onDispose {
            poiSearch.destroy()
        }
    }

    return remember(poiSearch, searchResults) {
        PoiSearchHandler(
            poiSearch = poiSearch,
            results = searchResults,
            searchDelayMs = searchDelayMs
        )
    }
}

/**
 * POI 搜索处理器
 * 提供搜索方法和结果状态
 */
class PoiSearchHandler internal constructor(
    private val poiSearch: PoiSearch,
    val results: List<PoiInfo>,
    val searchDelayMs: Long
) {
    fun searchInCity(keyword: String) {
        if (keyword.isNotEmpty()) {
            poiSearch.searchInCity(
                PoiCitySearchOption()
                    .city("全国")
                    .keyword(keyword)
                    .pageNum(0)
                    .pageCapacity(10)
            )
        }
    }
}

/**
 * 搜索防抖效果
 * 当查询关键词变化时 延迟执行搜索
 */
@Composable
fun LaunchedSearchEffect(
    query: String,
    handler: PoiSearchHandler
) {
    LaunchedEffect(query) {
        delay(handler.searchDelayMs.milliseconds)
        handler.searchInCity(query)
    }
}
