package com.cookieshax.coursehelper.feature.checkin.ui.components.gesture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cookieshax.coursehelper.core.network.ApiManager
import com.cookieshax.coursehelper.core.network.ApiResult
import com.cookieshax.coursehelper.feature.checkin.viewmodel.CheckInViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun GestureInputComponent(
    taskId: String,
    viewModel: CheckInViewModel
) {
    val scope = rememberCoroutineScope()
    val gesturePoints by viewModel.gesturePoints.collectAsState()
    val isCorrectGesture by viewModel.isCorrectGesture.collectAsState()
    val gestureResult by viewModel.gestureResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GestureCanvas(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth(),
            selectedPoints = gesturePoints,
            gestureResult = gestureResult,
            onPointsChange = { newPoints ->
                if (newPoints.isNotEmpty() && gesturePoints.isEmpty()) {
                    viewModel.updateGesture(newPoints, false, GestureResult.Idle)
                } else {
                    viewModel.updateGesture(newPoints, isCorrectGesture, GestureResult.Idle)
                }
            },
            onGestureComplete = { code ->
                scope.launch {
                    viewModel.updateGesture(gesturePoints, false, GestureResult.Idle)
                    val result = ApiManager.checkSignCode(taskId, code)
                    val isSuccess = (result is ApiResult.Success)
                            && JSONObject(result.data).optInt("result") == 1

                    if (!isSuccess) {
                        viewModel.updateGesture(gesturePoints, false, GestureResult.Failure)
                        delay(500.milliseconds)
                        viewModel.updateGesture(emptyList(), false, GestureResult.Idle)
                    } else {
                        viewModel.updateGesture(gesturePoints, true, GestureResult.Success)
                    }
                }
            }
        )
    }
}
