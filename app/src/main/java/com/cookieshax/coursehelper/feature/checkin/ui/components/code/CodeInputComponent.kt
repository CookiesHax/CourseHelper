package com.cookieshax.coursehelper.feature.checkin.ui.components.code

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.core.network.ApiManager
import com.cookieshax.coursehelper.core.network.ApiResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.compose.runtime.rememberCoroutineScope
import com.cookieshax.coursehelper.feature.checkin.viewmodel.CheckInViewModel
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun CodeInputComponent(
    taskId: String,
    codeLength: Int,
    viewModel: CheckInViewModel
) {
    val scope = rememberCoroutineScope()
    val code by viewModel.code.collectAsState()
    val codeInputResult by viewModel.codeInputResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CheckInCodeInput(
            codeLength,
            modifier = Modifier.fillMaxWidth(),
            value = code,
            result = codeInputResult
        ) { newValue ->
            // We don't set correct=true here, we do it in the validation call
            viewModel.updateCode(newValue, false, CodeInputResult.Idle)
            if (newValue.length == codeLength) {
                scope.launch {
                    val result = ApiManager.checkSignCode(taskId, newValue)
                    val isSuccess = (result is ApiResult.Success) &&
                            JSONObject(result.data).optInt("result") == 1

                    if (isSuccess) {
                        viewModel.updateCode(newValue, true, CodeInputResult.Success)
                    } else {
                        viewModel.updateCode(newValue, false, CodeInputResult.Failure)
                        delay(500.milliseconds)
                        viewModel.updateCode("", false, CodeInputResult.Idle)
                    }
                }
            }
        }
    }
}
