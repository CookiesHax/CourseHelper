package com.cookieshax.coursehelper.feature.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.baidu.mapapi.model.LatLng

@Composable
fun CoordinateInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (LatLng) -> Unit,
    initialLocation: LatLng? = null
) {
    var latStr by remember { mutableStateOf(initialLocation?.latitude?.toString() ?: "") }
    var lngStr by remember { mutableStateOf(initialLocation?.longitude?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("输入精确坐标") },
        text = {
            Column {
                OutlinedTextField(
                    value = latStr,
                    onValueChange = { latStr = it },
                    label = { Text("纬度 (Latitude)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = lngStr,
                    onValueChange = { lngStr = it },
                    label = { Text("经度 (Longitude)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val lat = latStr.toDoubleOrNull()
                val lng = lngStr.toDoubleOrNull()
                if (lat != null && lng != null) {
                    onConfirm(LatLng(lat, lng))
                    onDismiss()
                }
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
