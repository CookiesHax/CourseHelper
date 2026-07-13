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

val IcTag: ImageVector
    get() {
        if (_icTag != null) {
            return _icTag!!
        }
        _icTag = Builder(
            name = "IcTag", defaultWidth = 12.0.dp, defaultHeight = 12.0.dp,
            viewportWidth = 24.0f, viewportHeight = 24.0f
        ).apply {
            path(
                fill = SolidColor(Color(0x00000000)), stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2.0f, strokeLineCap = Round, strokeLineJoin =
                    StrokeJoin.Round, strokeLineMiter = 4.0f, pathFillType = NonZero
            ) {
                moveTo(12.586f, 2.586f)
                arcTo(
                    2.0f, 2.0f, 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    x1 = 11.172f,
                    y1 = 2.0f
                )
                horizontalLineTo(4.0f)
                arcToRelative(
                    2.0f, 2.0f, 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = -2.0f,
                    dy1 = 2.0f
                )
                verticalLineToRelative(7.172f)
                arcToRelative(
                    2.0f, 2.0f, 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.586f,
                    dy1 = 1.414f
                )
                lineToRelative(8.704f, 8.704f)
                arcToRelative(
                    2.426f, 2.426f, 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 3.42f,
                    dy1 = 0.0f
                )
                lineToRelative(6.58f, -6.58f)
                arcToRelative(
                    2.426f, 2.426f, 0.0f,
                    isMoreThanHalf = false,
                    isPositiveArc = false,
                    dx1 = 0.0f,
                    dy1 = -3.42f
                )
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000)), stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 2.0f, strokeLineCap = Round, strokeLineJoin =
                    StrokeJoin.Round, strokeLineMiter = 4.0f, pathFillType = NonZero
            ) {
                moveTo(7.5f, 7.5f)
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
        return _icTag!!
    }

private var _icTag: ImageVector? = null

@Preview
@Composable
private fun Preview() {
    Box(modifier = Modifier.padding(12.dp)) {
        Image(IcTag, contentDescription = "")
    }
}
