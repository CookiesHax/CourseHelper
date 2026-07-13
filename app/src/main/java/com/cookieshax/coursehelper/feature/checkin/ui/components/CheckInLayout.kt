package com.cookieshax.coursehelper.feature.checkin.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.feature.account.model.Account
import com.cookieshax.coursehelper.feature.account.model.TagWithAccounts
import com.cookieshax.coursehelper.feature.checkin.viewmodel.CheckInViewModel
import kotlinx.coroutines.launch

@Composable
fun CheckInLayout(
    viewModel: CheckInViewModel,
    accounts: List<Account>,
    tagsWithAccounts: List<TagWithAccounts>,
    isNeedPhoto: Boolean,
    inputComponent: @Composable (CheckInViewModel, ((String) -> Unit) -> Unit, ((String) -> Unit) -> Unit) -> Unit,
    triggerComponent: @Composable (CheckInViewModel) -> Unit
) {
    var onUploadImage by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var onOpenCamera by remember { mutableStateOf<((String) -> Unit)?>(null) }

    val selectedIds by viewModel.selectedIds.collectAsState()
    val resultsMap by viewModel.resultsMap.collectAsState()
    val uploadedObjectIds by viewModel.uploadedObjectIds.collectAsState()
    val activeUploadAccountId by viewModel.activeUploadAccountId.collectAsState()
    val manualCaptchaQueue by viewModel.manualCaptchaQueue.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Input area
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                inputComponent(
                    viewModel,
                    { onUploadImage = it },
                    { onOpenCamera = it }
                )
            }

            // Account list
            AccountList(
                modifier = Modifier.weight(1f),
                accounts = accounts,
                tagsWithAccounts = tagsWithAccounts,
                selectedIds = selectedIds,
                isNeedPhoto = isNeedPhoto,
                activeUploadAccountId = activeUploadAccountId,
                uploadedObjectIds = uploadedObjectIds,
                onSelectionChange = { uid, isSelected ->
                    viewModel.setAccountSelected(uid, isSelected)
                },
                onToggleIds = { uids, shouldSelect ->
                    viewModel.toggleAccountsSelection(uids, shouldSelect)
                },
                onUploadImage = { uid ->
                    onUploadImage?.invoke(uid)
                },
                onOpenCamera = { uid ->
                    onOpenCamera?.invoke(uid)
                }
            )

            // Action area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                triggerComponent(viewModel)
            }
        }

        if (resultsMap.isNotEmpty()) {
            CheckInResultDialog(
                results = resultsMap,
                accounts = accounts.filter { it.uid in selectedIds },
                onDismiss = {
                    viewModel.clearResults()
                }
            )
        }

        manualCaptchaQueue.firstOrNull()?.let { request ->
            val scope = rememberCoroutineScope()
            val currentIndex = 1
            val totalRemaining = manualCaptchaQueue.size
            val displayTitle = "验证码验证 ($currentIndex / $totalRemaining)"

            CaptchaDialog(
                captcha = request.captcha,
                onVerify = { offset ->
                    scope.launch {
                        viewModel.submitManualCaptchaOffset(offset)
                    }
                },
                onDismiss = {
                    viewModel.cancelManualCaptcha()
                },
                title = displayTitle
            )
        }
    }
}
