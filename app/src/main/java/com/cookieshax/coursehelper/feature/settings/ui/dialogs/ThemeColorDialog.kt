package com.cookieshax.coursehelper.feature.settings.ui.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cookieshax.coursehelper.ui.items.HctColorPickerDialog
import com.cookieshax.coursehelper.ui.items.HctColorState

@Composable
fun ThemeColorDialog(
    currentThemeColor: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var draftColor by remember(currentThemeColor) {
        mutableStateOf(HctColorState.fromHex(currentThemeColor))
    }

    HctColorPickerDialog(
        state = draftColor,
        onStateChange = { draftColor = it },
        onDismissRequest = onDismissRequest,
        onApply = {
            onConfirm(draftColor.hex)
        }
    )
}
