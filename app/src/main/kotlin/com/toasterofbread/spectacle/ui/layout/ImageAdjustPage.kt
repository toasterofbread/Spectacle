package com.toasterofbread.spectacle.ui.layout

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.spectacle.model.ImageSaver

class ImageAdjustPage(
    private val base_image: ImageBitmap,
    private val capture_data: ImageCapturePage.CaptureData,
    private val image_saver: ImageSaver
): AppPage {
    @Composable
    override fun Page(theme: Theme, modifier: Modifier) {
        Column(modifier) {
//            Image(overlay_image, null)
            Image(base_image, null)
        }
    }
}
