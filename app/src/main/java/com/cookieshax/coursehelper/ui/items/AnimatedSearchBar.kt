package com.cookieshax.coursehelper.ui.items

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.SearchTrigger(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Surface(
        modifier = modifier
            .sharedElement(
                rememberSharedContentState(key = "search_box"),
                animatedVisibilityScope = animatedVisibilityScope
            ),
        color = Color.Transparent,
        shape = CircleShape
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索",
                modifier = Modifier.sharedElement(
                    rememberSharedContentState(key = "search_icon"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.SearchInput(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "搜索...",
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .sharedElement(
                rememberSharedContentState(key = "search_box"),
                animatedVisibilityScope = animatedVisibilityScope
            ),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = CircleShape
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true,
            decorationBox = { innerTextField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .sharedElement(
                                rememberSharedContentState(key = "search_icon"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = hint,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        innerTextField()
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭搜索"
                        )
                    }
                }
            }
        )
    }
}
