package com.cookieshax.coursehelper.feature.checkin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.feature.account.model.Account
import com.cookieshax.coursehelper.feature.account.model.TagWithAccounts
import com.cookieshax.coursehelper.ui.items.IcTag

@Composable
fun AccountTagBar(
    allAccounts: List<Account>,
    tagsWithAccounts: List<TagWithAccounts>,
    selectedIds: Set<String>,
    onToggleIds: (List<String>, Boolean) -> Unit, // (uids, shouldSelect)
    modifier: Modifier = Modifier
) {
    val isAllSelected = remember(allAccounts, selectedIds) {
        allAccounts.isNotEmpty() && allAccounts.all { it.uid in selectedIds }
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),

        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "全选" tag
        item {
            FilterChip(
                selected = isAllSelected,
                onClick = {
                    val allUids = allAccounts.map { it.uid }
                    onToggleIds(allUids, !isAllSelected)
                },
                label = { Text("全选") },
                leadingIcon = {
                    Icon(
                        imageVector = IcTag,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            )
        }

        // 其他 tag
        items(tagsWithAccounts, key = { it.tag.tagId }) { tagWithAccounts ->
            val tag = tagWithAccounts.tag
            val tagUids = remember(tagWithAccounts) { tagWithAccounts.accounts.map { it.uid } }
            val isTagSelected = remember(tagUids, selectedIds) {
                tagUids.isNotEmpty() && tagUids.all { it in selectedIds }
            }

            val tagColor = remember(tag.color) { Color(tag.color) }

            FilterChip(
                selected = isTagSelected,
                onClick = {
                    onToggleIds(tagUids, !isTagSelected)
                },
                label = { Text(tag.name) },
                leadingIcon = {
                    Icon(
                        imageVector = IcTag,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                        tint = tagColor
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = tagColor,
                    selectedContainerColor = tagColor.copy(alpha = 0.2f),
                    selectedLabelColor = tagColor,
                    selectedLeadingIconColor = tagColor
                )
            )
        }
    }
}
