package com.toasterofbread.spectre.ui.layout

import android.app.Activity
import androidx.compose.animation.Crossfade
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.toasterofbread.spectre.model.ImageProvider
import com.toasterofbread.spectre.ui.component.MediaStatusDisplay
import com.toasterofbread.spectre.ui.component.imageselector.ImageSelector
import com.toasterofbread.spectre.ui.modifier.rememberComposableCaptureState
import com.toasterofbread.toastercomposetools.settings.ui.Theme
import com.toasterofbread.toastercomposetools.utils.composable.ShapedIconButton

abstract class ImageCapturePage(
    private val image_provider: ImageProvider
): AppPage {
    abstract fun onCaptured(adjust: Boolean, base_image: ImageBitmap, overlay_image: ImageBitmap)

    @Composable
    override fun Page(theme: Theme, modifier: Modifier) {
        var current_selector: Int by remember { mutableIntStateOf(0) }
        var selected_image: ImageBitmap? by remember { mutableStateOf(null) }

        Box(modifier) {
            val next_selector: Int = (current_selector + 1) % ImageSelector.ALL.size
            ShapedIconButton(
                {
                    current_selector = next_selector
                    selected_image = null
                },
                IconButtonDefaults.iconButtonColors(
                    containerColor = theme.accent,
                    contentColor = theme.on_accent
                ),
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .zIndex(1f)
            ) {
                Icon(ImageSelector.ALL[next_selector].getIcon(), null)
            }

            Column {
                Crossfade(
                    current_selector,
                    Modifier
                        .fillMaxHeight()
                        .weight(1f)
                ) { index ->
                    val selector: ImageSelector = ImageSelector.ALL[index]
                    selector.Selector(
                        theme,
                        image_provider = image_provider,
                        content_alignment = Alignment.BottomCenter,
                        content_padding = PaddingValues(10.dp),
                        content_shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) { image ->
                        if (index == current_selector) {
                            selected_image = image
                        }
                    }
                }

                val view = LocalView.current
                val window = (LocalContext.current as Activity).window
                val status_capture_state = rememberComposableCaptureState()

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

                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f))

                    Button(
                        {
                            val selected = selected_image ?: return@Button
                            if (!status_capture_state.canCapture()) {
                                return@Button
                            }

                            status_capture_state.capture(view, window) { capture ->
                                if (capture == null) {
                                    return@capture
                                }

                                onCaptured(adjust_on_capture, selected, capture.asImageBitmap())
                            }
                        },
                        enabled = selected_image != null,
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

                MediaStatusDisplay(
                    theme,
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    content_padding = PaddingValues(10.dp)
                )
            }
        }
    }
}
