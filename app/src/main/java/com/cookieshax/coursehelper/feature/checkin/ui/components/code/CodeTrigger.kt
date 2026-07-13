package com.cookieshax.coursehelper.feature.checkin.ui.components.code

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.feature.checkin.model.CheckInParams
import com.cookieshax.coursehelper.feature.checkin.model.CodeCheckInStrategy
import com.cookieshax.coursehelper.feature.checkin.viewmodel.CheckInViewModel
import kotlinx.coroutines.launch

@Composable
fun CodeTrigger(
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
    val isCorrectCode by viewModel.isCorrectCode.collectAsState()
    val code by viewModel.code.collectAsState()

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCheckingIn && selectedIds.isNotEmpty() && isCorrectCode,
            onClick = {
                scope.launch {
                    viewModel.performCheckIn(
                        strategy = CodeCheckInStrategy(),
                        params = CheckInParams.Code(
                            url = url,
                            taskId = taskId,
                            courseId = courseId,
                            signCode = code
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
                Text(text = if (isCorrectCode) "对选中账号签到" else "请先输入正确签到码")
            }
        }
    }
}
