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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.cookieshax.coursehelper.feature.account.model.Account
import com.cookieshax.coursehelper.feature.account.ui.items.MainAccountItem
import com.cookieshax.coursehelper.ui.items.Placeholder
import kotlinx.coroutines.launch

// 账号 Tab
@Composable
fun AccountTabContent(
    accounts: List<Account>,
    activeAccountId: String?,
    onAccountClick: (String) -> Unit,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    isSelectionMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onSelectionModeChanged: (Boolean) -> Unit = {},
    onSelectedIdsChanged: (Set<String>) -> Unit = {}
) {
    if (accounts.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Placeholder("暂无账号", "点击右上角 + 添加账号")
        }
    } else {
        val filteredAccounts = remember(accounts, searchQuery) {
            if (searchQuery.isEmpty()) {
                accounts
            } else {
                accounts.filter {
                    (it.name.contains(searchQuery, ignoreCase = true)
                            || it.uid.contains(searchQuery, ignoreCase = true))
                            || it.phone.contains(searchQuery, ignoreCase = true)
                }
            }
        }

        if (filteredAccounts.isEmpty()) {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("未找到账号")
            }
        } else {
            val density = LocalDensity.current
            val scope = rememberCoroutineScope()
            val lazyListState = rememberLazyListState()

            val currentIsSelectionMode by rememberUpdatedState(isSelectionMode)

            var isDragging by remember { mutableStateOf(false) }
            var draggedIndex by remember { mutableStateOf<Int?>(null) }
            var targetIndex by remember { mutableStateOf<Int?>(null) }
            var dragOffset by remember { mutableFloatStateOf(0f) }
            var draggedItemHeight by remember { mutableFloatStateOf(0f) }

            var prevAccounts by remember { mutableStateOf(accounts) }
            var skipAnimateOnce by remember { mutableStateOf(false) }

            if (prevAccounts != accounts) {
                prevAccounts = accounts
                if (draggedIndex != null) {
                    skipAnimateOnce = true
                }
                draggedIndex = null
                targetIndex = null
                dragOffset = 0f
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
                    .pointerInput(filteredAccounts) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { initialOffset ->
                                isDragging = true
                                val layoutInfo = lazyListState.layoutInfo
                                val touchedItem = layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                    initialOffset.y.toInt() in item.offset .. (item.offset + item.size)
                                }
                                touchedItem?.let { item ->
                                    if (item.index in filteredAccounts.indices) {
                                        if (!currentIsSelectionMode) {
                                            onSelectionModeChanged(true)
                                            onSelectedIdsChanged(setOf(filteredAccounts[item.index].uid))
                                        }
                                        draggedIndex = item.index
                                        targetIndex = item.index
                                        draggedItemHeight =
                                            item.size + with(density) { 8.dp.toPx() }
                                        dragOffset = 0f
                                    }
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount.y

                                val startIndex =
                                    draggedIndex ?: return@detectDragGesturesAfterLongPress
                                val layoutInfo = lazyListState.layoutInfo
                                val draggedItemInfo =
                                    layoutInfo.visibleItemsInfo.firstOrNull { it.index == startIndex }
                                        ?: return@detectDragGesturesAfterLongPress

                                val currentDraggedCenter =
                                    draggedItemInfo.offset + draggedItemInfo.size / 2 + dragOffset

                                val hoverItem = layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                    currentDraggedCenter.toInt() in item.offset .. (item.offset + item.size)
                                }

                                if (hoverItem != null && hoverItem.index in filteredAccounts.indices) {
                                    targetIndex = hoverItem.index
                                }
                            },
                            onDragEnd = {
                                isDragging = false
                                val start = draggedIndex
                                val end = targetIndex
                                if (start != null && end != null && start != end) {
                                    val targetOffset = (end - start) * draggedItemHeight
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
                    items = filteredAccounts,
                    key = { index, account ->
                        account.uid.takeIf { it.isNotEmpty() } ?: "fallback_empty_key_$index"
                    }
                ) { index, account ->
                    val isDragged = index == draggedIndex
                    val isSelected = if (isSelectionMode) {
                        selectedIds.contains(account.uid)
                    } else {
                        account.uid == activeAccountId
                    }

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
                        label = "AccountDisplaceAnimation"
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

                    MainAccountItem(
                        account = account,
                        isSelected = isSelected,
                        isActiveAccount = account.uid == activeAccountId,
                        modifier = itemModifier,
                        onClick = {
                            if (isSelectionMode) {
                                val nextSet = if (selectedIds.contains(account.uid)) {
                                    selectedIds - account.uid
                                } else {
                                    selectedIds + account.uid
                                }
                                onSelectedIdsChanged(nextSet)
                            } else {
                                onAccountClick(account.uid)
                            }
                        },
                        isSelectionMode = isSelectionMode,
                        isDragging = isDragging
                    )
                }
            }
        }
    }
}
