package com.toasterofbread.spectre.ui.component

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.ceil

fun DrawScope.wavePath(
    path: Path,
    direction: Int,
    height: Float,
    waves: Int,
    getOffset: (DrawScope.() -> Float)? = null
): Path {
    path.reset()

    val y_offset = height / 2
    val half_period = (size.width / (waves - 1)) / 2
    val offset_px = getOffset?.invoke(this)?.let { offset ->
        offset % size.width - (if (offset > 0f) size.width else 0f)
    } ?: 0f

    path.moveTo(x = -half_period / 2 + offset_px, y = y_offset)

    for (i in 0 until ceil((size.width * 2) / half_period + 1).toInt()) {
        if ((i % 2 == 0) != (direction == 1)) {
            path.relativeMoveTo(half_period, 0f)
            continue
        }

        path.relativeQuadraticBezierTo(
            dx1 = half_period / 2,
            dy1 = height / 2 * direction,
            dx2 = half_period,
            dy2 = 0f,
        )
    }

    return path
}
