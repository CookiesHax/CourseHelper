package com.cookieshax.coursehelper.feature.account.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.feature.account.model.AccountWithTags
import com.cookieshax.coursehelper.feature.account.ui.items.AccountWithTagsItem
import com.cookieshax.coursehelper.ui.items.Placeholder

@Composable
fun AccountWithTagsListContent(
    accountsWithTags: List<AccountWithTags>,
    filteredAccounts: List<AccountWithTags>,
    onAccountClick: (AccountWithTags) -> Unit,
    onEditTag: (AccountWithTags) -> Unit,
    modifier: Modifier = Modifier
) {
    if (accountsWithTags.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            Placeholder("暂无账号", "请先在账号页面添加账号")
        }
    } else if (filteredAccounts.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("未找到账号")
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(
                items = filteredAccounts,
                key = { _, accountWithTags -> accountWithTags.account.uid }
            ) { _, accountWithTags ->
                AccountWithTagsItem(
                    account = accountWithTags.account,
                    tags = accountWithTags.tags,
                    isSelected = false,
                    onClick = { onAccountClick(accountWithTags) },
                    onLongClick = { },
                    onEditTag = { onEditTag(accountWithTags) }
                )
            }
        }
    }
}
