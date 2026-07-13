package com.cookieshax.coursehelper.ui.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Round
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val IcTags: ImageVector
    get() {
        if (_icTags != null) {
            return _icTags!!
        }
        _icTags = Builder(
            name = "IcTags", defaultWidth = 24.0.dp, defaultHeight = 24.0.dp,
            viewportWidth = 24.0f, viewportHeight = 24.0f
        ).apply {
            path(
                fill = SolidColor(Color(0x00000000)), stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2.0f, strokeLineCap = Round, strokeLineJoin =
                    StrokeJoin.Round, strokeLineMiter = 4.0f, pathFillType = NonZero
            ) {
                moveTo(13.172f, 2.0f)
                arcToRelative(
                    2.0f, 2.0f, 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 1.414f,
                    dy1 = 0.586f
                )
                lineToRelative(6.71f, 6.71f)
                arcToRelative(
                    2.4f, 2.4f, 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 0.0f,
                    dy1 = 3.408f
                )
                lineToRelative(-4.592f, 4.592f)
                arcToRelative(
                    2.4f, 2.4f, 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = -3.408f,
                    dy1 = 0.0f
                )
                lineToRelative(-6.71f, -6.71f)
                arcTo(
                    2.0f, 2.0f, 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    x1 = 6.0f,
                    y1 = 9.172f
                )
                verticalLineTo(3.0f)
                arcToRelative(
                    1.0f, 1.0f, 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = true,
                    dx1 = 1.0f,
                    dy1 = -1.0f
                )
                close()
                moveTo(2.0f, 7.0f)
                verticalLineToRelative(6.172f)
                arcToRelative(
                    2.0f, 2.0f, 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.586f,
                    dy1 = 1.414f
                )
                lineToRelative(6.71f, 6.71f)
                arcToRelative(
                    2.4f, 2.4f, 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 3.191f,
                    dy1 = 0.193f
                )
            }
            path(
                fill = SolidColor(Color(0xFF000000)), stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2.0f, strokeLineCap = Round, strokeLineJoin =
                    StrokeJoin.Round, strokeLineMiter = 4.0f, pathFillType = NonZero
            ) {
                moveTo(10.5f, 6.5f)
                moveToRelative(-0.5f, 0.0f)
                arcToRelative(
                    0.5f, 0.5f, 0.0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    dx1 = 1.0f,
                    dy1 = 0.0f
                )
                arcToRelative(
                    0.5f, 0.5f, 0.0f,
                    isMoreThanHalf = true,
                    isPositiveArc = true,
                    dx1 = -1.0f,
                    dy1 = 0.0f
                )
            }
        }
            .build()
        return _icTags!!
    }

private var _icTags: ImageVector? = null

@Preview
@Composable
private fun Preview() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(IcTags, contentDescription = "")
    }
}
