package com.toasterofbread.spectre.ui.layout

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.utils.composable.ShapedIconButton
import com.toasterofbread.spectre.model.ImageProvider
import com.toasterofbread.spectre.ui.component.MediaSessionState
import com.toasterofbread.spectre.ui.component.MediaStatusDisplay
import com.toasterofbread.spectre.ui.component.imageselector.ImageSelector
import com.toasterofbread.spectre.ui.modifier.ComposableCaptureState
import com.toasterofbread.spectre.ui.modifier.rememberComposableCaptureState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

abstract class ImageCapturePage(
    private val image_provider: ImageProvider
): AppPage {
    data class CaptureData(val media_state: MediaSessionState?, val time: Date, val overlay_offset: Offset = Offset.Zero, val base_image_rotation: Int = 0) {
        fun getFilename(): String {
            return SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(time)
        }
    }

    abstract fun saveCapture(base_image: ImageBitmap, overlay_image: ImageBitmap, capture_data: CaptureData)
    abstract fun adjustCapture(base_image: ImageBitmap, capture_data: CaptureData)

    @Composable
    override fun Page(theme: Theme, modifier: Modifier) {
        val context = LocalContext.current
        val coroutine_scope: CoroutineScope = rememberCoroutineScope()

        val media_capture_state: ComposableCaptureState = rememberComposableCaptureState()
        var current_selector: Int by remember { mutableIntStateOf(0) }
        var current_media_state: MediaSessionState? by remember { mutableStateOf(null) }

        Column(modifier) {
            MediaStatusDisplay(
                theme,
                Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                content_padding = PaddingValues(10.dp),
                capture_state = media_capture_state
            ) { session_state ->
                current_media_state = session_state
            }

            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .border(2.dp, Color.White, RoundedCornerShape(3.dp))
            ) {
                Crossfade(current_selector) { index ->
                    val selector: ImageSelector = ImageSelector.ALL[index]
                    selector.Selector(
                        theme,
                        image_provider = image_provider,
                        content_alignment = Alignment.Center,
                        content_padding = PaddingValues(10.dp),
                        content_shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                val next_selector: Int = (current_selector + 1) % ImageSelector.ALL.size
                ShapedIconButton(
                    {
                        current_selector = next_selector
                    },
                    IconButtonDefaults.iconButtonColors(
                        containerColor = theme.accent,
                        contentColor = theme.on_accent
                    ),
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .zIndex(1f)
                ) {
                    Icon(ImageSelector.ALL[next_selector].getIcon(), null)
                }
            }

            Row(
                Modifier.padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                var adjust_on_capture: Boolean by remember { mutableStateOf(false) }
                Switch(
                    adjust_on_capture,
                    { adjust_on_capture = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = theme.accent,
                        checkedTrackColor = theme.vibrant_accent,
                        uncheckedThumbColor = theme.on_accent,
                        uncheckedTrackColor = theme.on_accent.copy(alpha = 0.5f),
                        checkedBorderColor = Color.Transparent,
                        uncheckedBorderColor = Color.Transparent
                    )
                )

                Text("Adjust after capture")

                Spacer(Modifier.fillMaxWidth().weight(1f))

                Button(
                    {
                        coroutine_scope.launch {
                            val selector: ImageSelector = ImageSelector.ALL[current_selector]
                            val capture: ImageSelector.ImageSelectorCapture = selector.captureCurrentImage(context) ?: return@launch
                            val capture_data: CaptureData = CaptureData(current_media_state, Date(), base_image_rotation = capture.rotation)

                            if (adjust_on_capture) {
                                adjustCapture(capture.image, capture_data)
                            }
                            else {
                                val media_capture: Bitmap = media_capture_state.capture()
                                saveCapture(capture.image, media_capture.asImageBitmap(),capture_data)
                            }
                        }
                    },
                    enabled = ImageSelector.ALL[current_selector].canCaptureImage(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.vibrant_accent,
                        contentColor = theme.on_accent,
                        disabledContainerColor = theme.on_accent.copy(alpha = 0.25f),
                        disabledContentColor = theme.accent.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Capture")
                }
            }
        }
    }
}
