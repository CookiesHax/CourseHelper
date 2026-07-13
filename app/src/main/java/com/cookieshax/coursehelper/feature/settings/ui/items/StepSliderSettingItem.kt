package com.cookieshax.coursehelper.feature.settings.ui.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun StepSliderSettingItem(
    title: String,
    value: Int,
    steps: List<Int>,
    valueLabel: (Int) -> String,
    onValueChange: (Int) -> Unit
) {
    val currentIndex = steps.indexOf(value).coerceAtLeast(0)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = valueLabel(value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = currentIndex.toFloat(),
            onValueChange = { index ->
                onValueChange(steps[index.roundToInt()])
            },
            valueRange = 0f..(steps.size - 1).toFloat(),
            steps = if (steps.size > 2) steps.size - 2 else 0
        )
    }
}
