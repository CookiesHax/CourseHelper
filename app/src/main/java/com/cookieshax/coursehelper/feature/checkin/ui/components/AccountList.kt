package com.cookieshax.coursehelper.feature.checkin.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.feature.account.model.Account
import com.cookieshax.coursehelper.feature.account.model.TagWithAccounts
import com.cookieshax.coursehelper.feature.checkin.ui.items.CheckInAccountItem

@Composable
fun AccountList(
    accounts: List<Account>,
    tagsWithAccounts: List<TagWithAccounts>,
    selectedIds: Set<String>,
    isNeedPhoto: Boolean,
    activeUploadAccountId: String?,
    uploadedObjectIds: Map<String, String>?,
    onSelectionChange: (String, Boolean) -> Unit,
    onToggleIds: (List<String>, Boolean) -> Unit,
    onUploadImage: ((String) -> Unit)?,
    onOpenCamera: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        AccountTagBar(
            allAccounts = accounts,
            tagsWithAccounts = tagsWithAccounts,
            selectedIds = selectedIds,
            onToggleIds = onToggleIds
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(accounts, key = { it.uid }) { account ->
                CheckInAccountItem(
                    account = account,
                    isSelected = selectedIds.contains(account.uid),
                    onSelectionChange = { isSelected ->
                        onSelectionChange(account.uid, isSelected)
                    },
                    isNeedPhoto = isNeedPhoto,
                    onUploadImage = {
                        if (activeUploadAccountId == null) {
                            onUploadImage?.let { it(account.uid) }
                        }
                    },
                    onCameraClick = {
                        if (activeUploadAccountId == null) {
                            onOpenCamera?.let { it(account.uid) }
                        }
                    },
                    isUploadSuccess = uploadedObjectIds?.containsKey(account.uid) == true
                )
            }
        }
    }
}
