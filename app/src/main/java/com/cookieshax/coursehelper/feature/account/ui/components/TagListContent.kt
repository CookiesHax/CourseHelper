package com.cookieshax.coursehelper.feature.account.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cookieshax.coursehelper.feature.account.model.TagWithAccounts
import com.cookieshax.coursehelper.feature.account.ui.items.TagItem
import com.cookieshax.coursehelper.ui.items.Placeholder
import kotlinx.coroutines.launch

@Composable
fun TagListContent(
    tags: List<TagWithAccounts>,
    filteredTags: List<TagWithAccounts>,
    searchQuery: String,
    isSelectionMode: Boolean,
    selectedTagIds: Set<String>,
    onTagClick: (TagWithAccounts) -> Unit,
    onTagEdit: (TagWithAccounts) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onSelectionModeChanged: (Boolean) -> Unit,
    onSelectedIdsChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tags.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            Placeholder("暂无标签", "点击右上角 + 创建标签")
        }
    } else if (filteredTags.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("未找到标签")
        }
    } else {
        val density = LocalDensity.current
        val scope = rememberCoroutineScope()
        val lazyListState = rememberLazyListState()

        val currentIsSelectionMode by rememberUpdatedState(isSelectionMode)
        val currentFilteredTags by rememberUpdatedState(filteredTags)
        val currentSearchQuery by rememberUpdatedState(searchQuery)
        val currentOnSelectionModeChanged by rememberUpdatedState(onSelectionModeChanged)
        val currentOnSelectedIdsChanged by rememberUpdatedState(onSelectedIdsChanged)

        var isDragging by remember { mutableStateOf(false) }
        var draggedIndex by remember { mutableStateOf<Int?>(null) }
        var targetIndex by remember { mutableStateOf<Int?>(null) }
        var dragOffset by remember { mutableFloatStateOf(0f) }
        var draggedItemHeight by remember { mutableFloatStateOf(0f) }

        var prevTags by remember { mutableStateOf(tags) }
        var prevIsSelectionMode by remember { mutableStateOf(isSelectionMode) }
        var skipAnimateOnce by remember { mutableStateOf(false) }

        if (prevTags != tags) {
            prevTags = tags
            // 如果是在拖拽结束后触发的变化 跳过一次 Compose 自带的项动画
            if (draggedIndex != null) {
                skipAnimateOnce = true
            }
            // 重置拖拽状态
            draggedIndex = null
            targetIndex = null
            dragOffset = 0f
        }

        if (prevIsSelectionMode != isSelectionMode) {
            prevIsSelectionMode = isSelectionMode
            skipAnimateOnce = true
        }

        if (skipAnimateOnce) {
            SideEffect {
                skipAnimateOnce = false
            }
        }

        LazyColumn(
            state = lazyListState,
            modifier = modifier
                .fillMaxSize()
                .pointerInput(filteredTags) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { initialOffset ->
                            if (currentSearchQuery.isNotBlank()) return@detectDragGesturesAfterLongPress

                            val layoutInfo = lazyListState.layoutInfo
                            val touchedItem = layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                initialOffset.y.toInt() in item.offset .. (item.offset + item.size)
                            }
                            touchedItem?.let { item ->
                                if (item.index in currentFilteredTags.indices) {
                                    isDragging = true
                                    if (!currentIsSelectionMode) {
                                        currentOnSelectionModeChanged(true)
                                        currentOnSelectedIdsChanged(setOf(currentFilteredTags[item.index].tag.tagId.toString()))
                                    }
                                    draggedIndex = item.index
                                    targetIndex = item.index
                                    draggedItemHeight = item.size + with(density) { 8.dp.toPx() }
                                    dragOffset = 0f
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (currentSearchQuery.isNotBlank()) return@detectDragGesturesAfterLongPress
                            change.consume()
                            dragOffset += dragAmount.y

                            val startIndex = draggedIndex ?: return@detectDragGesturesAfterLongPress
                            val layoutInfo = lazyListState.layoutInfo
                            val draggedItemInfo =
                                layoutInfo.visibleItemsInfo.firstOrNull { it.index == startIndex }
                                    ?: return@detectDragGesturesAfterLongPress

                            val draggedTop = draggedItemInfo.offset + dragOffset
                            val draggedBottom =
                                draggedItemInfo.offset + draggedItemInfo.size + dragOffset

                            // 针对大小不一的项优化 向下用底部判断 向上用顶部判断
                            val hoverItem = if (dragOffset > 0) {
                                layoutInfo.visibleItemsInfo.lastOrNull { item ->
                                    item.index in currentFilteredTags.indices &&
                                            draggedBottom > (item.offset + item.size / 2)
                                }
                            } else {
                                layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                    item.index in currentFilteredTags.indices &&
                                            draggedTop < (item.offset + item.size / 2)
                                }
                            }

                            targetIndex = if (hoverItem != null) {
                                hoverItem.index
                            } else {
                                draggedIndex
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            val start = draggedIndex
                            val end = targetIndex
                            if (start != null && end != null && start != end) {
                                val layoutInfo = lazyListState.layoutInfo
                                val startItem =
                                    layoutInfo.visibleItemsInfo.firstOrNull { it.index == start }
                                val endItem =
                                    layoutInfo.visibleItemsInfo.firstOrNull { it.index == end }

                                val targetOffset = if (startItem != null && endItem != null) {
                                    if (start < end) {
                                        // 向下移动 目标位置应为目标项的底部减去被拖动项的大小 确保紧贴下方
                                        (endItem.offset + endItem.size) - (startItem.offset + startItem.size)
                                    } else {
                                        // 向上移动 目标位置即为目标项的起始位置偏移
                                        endItem.offset - startItem.offset
                                    }
                                } else {
                                    (end - start) * draggedItemHeight
                                }.toFloat()

                                scope.launch {
                                    animate(
                                        initialValue = dragOffset,
                                        targetValue = targetOffset,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    ) { value, _ ->
                                        dragOffset = value
                                    }
                                    onMove(start, end)
                                }
                            } else {
                                scope.launch {
                                    animate(
                                        initialValue = dragOffset,
                                        targetValue = 0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    ) { value, _ ->
                                        dragOffset = value
                                    }
                                    draggedIndex = null
                                    targetIndex = null
                                    dragOffset = 0f
                                }
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            scope.launch {
                                animate(
                                    initialValue = dragOffset,
                                    targetValue = 0f,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ) { value, _ ->
                                    dragOffset = value
                                }
                                draggedIndex = null
                                targetIndex = null
                                dragOffset = 0f
                            }
                        }
                    )
                },
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = filteredTags,
                key = { _, tagWithAccounts -> tagWithAccounts.tag.tagId }
            ) { index, tagWithAccounts ->
                val tagId = tagWithAccounts.tag.tagId.toString()
                val isDragged = index == draggedIndex

                val currentStart = draggedIndex
                val currentEnd = targetIndex

                val targetTranslationY = when {
                    currentStart == null || currentEnd == null -> 0f
                    index == currentStart -> dragOffset
                    currentStart < currentEnd && index in (currentStart + 1) .. currentEnd -> -draggedItemHeight
                    currentStart > currentEnd && index in currentEnd ..< currentStart -> draggedItemHeight
                    else -> 0f
                }

                val animatedTranslationY by animateFloatAsState(
                    targetValue = targetTranslationY,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "TagDisplaceAnimation"
                )

                val itemModifier = Modifier
                    .animateItem(
                        placementSpec = if (skipAnimateOnce) null else spring(stiffness = Spring.StiffnessMediumLow)
                    )
                    .graphicsLayer {
                        translationY = when {
                            currentStart == null -> 0f
                            isDragged -> dragOffset
                            else -> animatedTranslationY
                        }

                        if (isDragged && isDragging) {
                            scaleX = 1.02f
                            scaleY = 1.02f
                            shadowElevation = 8.dp.toPx()
                        }
                    }
                    .zIndex(if (isDragged) 1f else 0f)

                TagItem(
                    tagName = tagWithAccounts.tag.name,
                    color = Color(tagWithAccounts.tag.color),
                    accounts = tagWithAccounts.accounts,
                    onClick = { onTagClick(tagWithAccounts) },
                    onEdit = { onTagEdit(tagWithAccounts) },
                    modifier = itemModifier,
                    isSelected = selectedTagIds.contains(tagId),
                    isDragging = isDragging
                )
            }
        }
    }
}
