package com.toasterofbread.spectre.ui.component.imageselector

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spectre.model.ImageProvider
import com.toasterofbread.composekit.settings.ui.Theme

interface ImageSelector {
    fun getIcon(): ImageVector

    data class ImageSelectorCapture(val image: ImageBitmap, val rotation: Int)

    suspend fun captureCurrentImage(context: Context): ImageSelectorCapture?
    fun canCaptureImage(): Boolean = true

    @Composable
    fun Selector(
        theme: Theme,
        image_provider: ImageProvider,
        content_alignment: Alignment,
        content_padding: PaddingValues,
        content_shape: Shape,
        modifier: Modifier
    )

    companion object {
        val ALL: List<ImageSelector> =
            listOf(CameraImageSelector(), GalleryImageSelector())
    }
}
