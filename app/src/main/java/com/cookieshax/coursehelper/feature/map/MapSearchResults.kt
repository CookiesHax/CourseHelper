package com.cookieshax.coursehelper.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.baidu.mapapi.search.core.PoiInfo

/**
 * 地图搜索结果列表组件
 * 显示 POI 搜索结果 支持点击选择
 */
@Composable
fun MapSearchResults(
    results: List<PoiInfo>,
    onPoiSelected: (PoiInfo) -> Unit,
    modifier: Modifier = Modifier,
    topOffset: Dp = 0.dp
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = topOffset,
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "搜索结果 (${results.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results.size) { index ->
                    val poi = results[index]
                    PoiResultItem(
                        poi = poi,
                        onClick = { onPoiSelected(poi) }
                    )
                }
            }
        }
    }
}

/**
 * 单个 POI 结果 item
 */
@Composable
private fun PoiResultItem(
    poi: PoiInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = poi.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = poi.address.takeUnless { it.isNullOrEmpty() } ?: poi.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
