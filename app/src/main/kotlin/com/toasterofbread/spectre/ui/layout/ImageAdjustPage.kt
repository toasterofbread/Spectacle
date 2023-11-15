package com.toasterofbread.spectre.ui.layout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.utils.common.thenIf
import com.toasterofbread.spectre.model.ImageSaver
import com.toasterofbread.spectre.ui.component.MediaStatusDisplay

class ImageAdjustPage(
    private val base_image: ImageBitmap,
    capture_data: ImageCapturePage.CaptureData,
    private val image_saver: ImageSaver
): AppPage {
    private var capture_data: ImageCapturePage.CaptureData by mutableStateOf(capture_data)

    @Composable
    override fun Page(theme: Theme, modifier: Modifier) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            Column(modifier) {
                val rotated = capture_data.base_image_rotation % 2 != 0
                val aspect_ratio =
                    if (!rotated)
                        base_image.height.toFloat() / base_image.width
                    else
                        base_image.width.toFloat() / base_image.height

                Canvas(
                    Modifier.fillMaxWidth().requiredHeight(this@BoxWithConstraints.maxWidth * aspect_ratio).border(2.dp, Color.White)
                ) {
                    val image_size: IntSize =
                        if (!rotated) IntSize(size.width.toInt(), size.height.toInt())
                        else IntSize(size.height.toInt(), size.width.toInt())

                    translate(left = image_size.height.toFloat()) {
                        rotate(capture_data.base_image_rotation * 90f, pivot = Offset.Zero) {
                            drawImage(
                                base_image,
                                dstSize = image_size
                            )
                        }
                    }
                }

//                Image(
//                    base_image,
//                    null,
//                    Modifier
////                        .fillMaxWidth()
////                        .border(1.dp, Color.Green)
//                        .border(1.dp, Color.White)
//                        .thenIf(capture_data.base_image_rotation % 2 != 0) {
//                            requiredSize(
//                                this@BoxWithConstraints.maxWidth * (base_image.height.toFloat() / base_image.width),
//                                this@BoxWithConstraints.maxWidth * (base_image.width.toFloat() / base_image.height)
//                            )
//                        }
//                        .rotate(capture_data.base_image_rotation * 90f)
//                        .border(1.dp, Color.Blue)
//                )

                capture_data.media_state?.also { media_state ->
                    MediaStatusDisplay(
                        theme,
                        Modifier
                            .height(100.dp)
                            .fillMaxWidth(),
                        interactive = false,
                        media_sessions_override = listOf(media_state)
                    )
                }
            }
        }
    }
}
