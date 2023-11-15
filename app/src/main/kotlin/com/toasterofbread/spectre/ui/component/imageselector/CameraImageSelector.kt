package com.toasterofbread.spectre.ui.component.imageselector

import android.content.Context
import android.graphics.BitmapFactory
import android.view.Surface
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.spectre.model.ImageProvider
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.math.roundToInt

private const val FOCUS_INDICATOR_SIZE_DP: Float = 75f
private const val FOCUS_INDICATOR_DISPLAY_DURATION_MS: Long = 500

@ExperimentalGetImage
class CameraImageSelector: ImageSelector {
    override fun getIcon(): ImageVector =
        Icons.Default.CameraAlt

    private var camera_controller: CameraController? by mutableStateOf(null)

    override fun canCaptureImage(): Boolean = camera_controller != null

    override suspend fun captureCurrentImage(context: Context): ImageSelector.ImageSelectorCapture? {
        val controller: CameraController = camera_controller!!
        var result: Result<ImageBitmap>? = null

        controller.takePicture(Executors.newSingleThreadExecutor(), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val buffer = image.image!!.planes[0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)

                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
                result = Result.success(bitmap.asImageBitmap())
            }

            override fun onError(exception: ImageCaptureException) {
                result = Result.failure(exception)
            }
        })

        while (result == null) {
            delay(100)
        }

        return ImageSelector.ImageSelectorCapture(result!!.getOrThrow(), 1)
    }

    @Composable
    override fun Selector(
        theme: Theme,
        image_provider: ImageProvider,
        content_alignment: Alignment,
        content_padding: PaddingValues,
        content_shape: Shape,
        modifier: Modifier
    ) {
        Box(modifier, contentAlignment = content_alignment) {
            var front_lens: Boolean by remember { mutableStateOf(false) }
            var focus_point: Offset? by remember { mutableStateOf(null) }
            val focus_point_opacity = remember { Animatable(0f) }

            camera_controller = image_provider.CameraPreview(
                Modifier.fillMaxSize(),
                front_lens = front_lens,
                content_modifier = Modifier
                    .fillMaxSize()
                    .clip(content_shape),
                content_alignment = Alignment.BottomCenter
            ) {
                focus_point = it
            }

            LaunchedEffect(focus_point) {
                if (focus_point == null) {
                    focus_point_opacity.snapTo(0f)
                    return@LaunchedEffect
                }

                focus_point_opacity.snapTo(1f)
                delay(FOCUS_INDICATOR_DISPLAY_DURATION_MS)
                focus_point_opacity.animateTo(0f)
            }

            Box(
                Modifier
                    .offset {
                        IntOffset(focus_point?.x?.roundToInt() ?: 0, focus_point?.y?.roundToInt() ?: 0)
                    }
                    .offset(-FOCUS_INDICATOR_SIZE_DP.dp / 2, -FOCUS_INDICATOR_SIZE_DP.dp / 2)
                    .size(FOCUS_INDICATOR_SIZE_DP.dp)
                    .graphicsLayer { alpha = focus_point_opacity.value }
                    .border(1.dp, Color.White, CircleShape)
                    .align(Alignment.TopStart)
            )

            ShapedIconButton(
                { front_lens = !front_lens },
                IconButtonDefaults.iconButtonColors(
                    containerColor = theme.accent,
                    contentColor = theme.on_accent
                ),
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(content_padding)
            ) {
                val icon_rotation: Float by animateFloatAsState(
                    if (front_lens) 180f else 0f,
                    spring(Spring.DampingRatioLowBouncy, Spring.StiffnessVeryLow)
                )
                Icon(Icons.Default.FlipCameraAndroid, null, Modifier.graphicsLayer { rotationZ = icon_rotation })
            }
        }
    }
}
