package com.toasterofbread.spectre.ui.modifier

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.core.graphics.applyCanvas
import kotlin.math.roundToInt

class ComposableCaptureState {
    private var bounds: Rect? = null

    fun canCapture(): Boolean = bounds != null

    fun capture(view: View, window: Window, callback: (Bitmap?) -> Unit) {
        val current_bounds = bounds ?: throw NullPointerException("Bounds has not been set")

        val bitmap = Bitmap.createBitmap(
            current_bounds.width.roundToInt(),
            current_bounds.height.roundToInt(),
            Bitmap.Config.ARGB_8888
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PixelCopy.request(
                window,
                current_bounds.toAndroidRect(),
                bitmap,
                { result ->
                    if (result == PixelCopy.SUCCESS) {
                        callback(bitmap)
                    }
                    else {
                        callback(null)
                    }
                },
                Handler(Looper.getMainLooper())
            )
        }
        else {
            bitmap.applyCanvas {
                translate(-current_bounds.left, -current_bounds.top)
                view.draw(this)
            }

            callback(bitmap)
        }
    }

    fun onBoundsChanged(bounds: Rect) {
        this.bounds = bounds
    }
}

@Composable
fun rememberComposableCaptureState(): ComposableCaptureState {
    return remember { ComposableCaptureState() }
}

fun Modifier.capturable(state: ComposableCaptureState): Modifier =
    onGloballyPositioned {
        state.onBoundsChanged(it.boundsInWindow())
    }
