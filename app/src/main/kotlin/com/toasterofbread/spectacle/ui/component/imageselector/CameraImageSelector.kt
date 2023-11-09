package com.toasterofbread.spectacle.ui.component.imageselector

import android.content.Context
import android.graphics.BitmapFactory
import android.view.Surface
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.spectacle.model.ImageProvider
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

@ExperimentalGetImage
class CameraImageSelector: ImageSelector {
    override fun getIcon(): ImageVector =
        Icons.Default.CameraAlt

    private val image_capture: ImageCapture =
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

    override suspend fun captureCurrentImage(context: Context): ImageBitmap? {
        var result: Result<ImageBitmap>? = null

        image_capture.takePicture(Executors.newSingleThreadExecutor(), object : ImageCapture.OnImageCapturedCallback() {
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

        return result!!.getOrThrow()
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

            image_provider.CameraPreview(
                Modifier.fillMaxSize(),
                front_lens = front_lens,
                image_capture = image_capture,
                content_modifier = Modifier
                    .fillMaxSize()
                    .clip(content_shape),
                content_alignment = Alignment.BottomCenter
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
