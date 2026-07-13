package com.cookieshax.coursehelper.feature.checkin.ui.components.normal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.feature.checkin.model.CheckInParams
import com.cookieshax.coursehelper.feature.checkin.model.NormalCheckInStrategy
import com.cookieshax.coursehelper.feature.checkin.viewmodel.CheckInViewModel
import kotlinx.coroutines.launch

@Composable
fun NormalTrigger(
    url: String,
    taskId: String,
    courseId: String,
    isNeedCaptcha: Boolean,
    viewModel: CheckInViewModel,
    semaphoreLimit: Int = 6
) {
    val scope = rememberCoroutineScope()
    val isCheckingIn by viewModel.isCheckingIn.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCheckingIn && selectedIds.isNotEmpty(),
            onClick = {
                scope.launch {
                    viewModel.performCheckIn(
                        strategy = NormalCheckInStrategy(),
                        params = CheckInParams.Normal(
                            url = url,
                            taskId = taskId,
                            courseId = courseId,
                            uploadedObjectIds = viewModel.uploadedObjectIds.value
                        ),
                        isNeedCaptcha = isNeedCaptcha,
                        semaphoreLimit = semaphoreLimit
                    )
                }
            }
        ) {
            if (isCheckingIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(text = "对选中账号签到")
            }
        }
    }
}
