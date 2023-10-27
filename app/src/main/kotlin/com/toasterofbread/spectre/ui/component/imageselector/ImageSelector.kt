package com.toasterofbread.spectre.ui.component.imageselector

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import com.toasterofbread.spectre.model.ImageProvider
import com.toasterofbread.toastercomposetools.settings.ui.Theme

interface ImageSelector {
    fun getIcon(): ImageVector

    @Composable
    fun Selector(
        theme: Theme,
        image_provider: ImageProvider,
        content_alignment: Alignment,
        content_padding: PaddingValues,
        content_shape: Shape,
        modifier: Modifier,
        onSelectedImageChanged: (ImageBitmap?) -> Unit
    )

    companion object {
        val ALL: List<ImageSelector> =
            listOf(CameraImageSelector(), GalleryImageSelector())
    }
}
