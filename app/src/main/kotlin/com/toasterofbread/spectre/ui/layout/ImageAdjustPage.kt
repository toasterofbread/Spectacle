package com.toasterofbread.spectre.ui.layout

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.toasterofbread.toastercomposetools.settings.ui.Theme

class ImageAdjustPage(
    private val base_image: ImageBitmap,
    private val overlay_image: ImageBitmap
): AppPage {
    @Composable
    override fun Page(theme: Theme, modifier: Modifier) {
        Column(modifier) {
            Image(overlay_image, null)
            Image(base_image, null)
        }
    }
}
