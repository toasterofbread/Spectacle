package com.toasterofbread.spectre.ui.modifier

import android.app.Activity
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.core.graphics.applyCanvas
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

class ComposableCaptureState(private val view: View, private val window: Window) {
    private var bounds: Rect? = null

    fun canCapture(): Boolean = bounds != null

    suspend fun capture(): Bitmap {
        val current_bounds = bounds ?: throw NullPointerException("Bounds has not been set")

        val bitmap: Bitmap = Bitmap.createBitmap(
            current_bounds.width.roundToInt(),
            current_bounds.height.roundToInt(),
            Bitmap.Config.RGB_565
        )
        var result: Boolean? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PixelCopy.request(
                window,
                current_bounds.toAndroidRect(),
                bitmap,
                { copy_result ->
                    result = copy_result == PixelCopy.SUCCESS
                },
                Handler(Looper.getMainLooper())
            )
        }
        else {
            bitmap.applyCanvas {
                translate(-current_bounds.left, -current_bounds.top)
                view.draw(this)
            }

            result = true
        }

        while (result == null) {
            delay(100)
        }

        return bitmap
    }

    fun onBoundsChanged(bounds: Rect) {
        this.bounds = bounds
    }
}

@Composable
fun rememberComposableCaptureState(): ComposableCaptureState {
    val view = LocalView.current
    val window = (LocalContext.current as Activity).window
    return remember(view, window) { ComposableCaptureState(view, window) }
}

fun Modifier.capturable(state: ComposableCaptureState): Modifier =
    onGloballyPositioned {
        state.onBoundsChanged(it.boundsInWindow())
    }
