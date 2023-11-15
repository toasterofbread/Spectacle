package com.toasterofbread.spectre.ui.component.imageselector

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult
import com.toasterofbread.spectre.model.ImageFile
import com.toasterofbread.spectre.model.ImageProvider
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.utils.composable.SubtleLoadingIndicator
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

private const val IMAGE_LOAD_RADIUS: Int = 10
private const val MAX_SIMULTANEOUS_LOADERS: Int = 5

class GalleryImageSelector: ImageSelector {
    override fun getIcon(): ImageVector =
        Icons.Default.Photo

    private var current_image: ImageBitmap? = null
    override suspend fun captureCurrentImage(context: Context): ImageSelector.ImageSelectorCapture? =
        current_image?.let { image ->
            ImageSelector.ImageSelectorCapture(image, 0)
        }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalCoroutinesApi::class)
    @Composable
    override fun Selector(
        theme: Theme,
        image_provider: ImageProvider,
        content_alignment: Alignment,
        content_padding: PaddingValues,
        content_shape: Shape,
        modifier: Modifier
    ) {
        val context = LocalContext.current

        var image_files: List<ImageFile>? by remember { mutableStateOf(null) }
        val pager_state: PagerState = rememberPagerState { image_files?.size ?: 0 }
        LaunchedEffect(Unit) {
            image_files = image_provider.getLocalImages()
        }

        val image_loader: ImageLoader = remember { ImageLoader(context) }
        var image_requests: Array<Disposable?> by remember { mutableStateOf(emptyArray()) }
        LaunchedEffect(image_files) {
            for (loader in image_requests) {
                loader?.dispose()
            }
            image_requests = image_requests.copyOf(image_files?.size ?: 0)
        }

        LaunchedEffect(image_files, pager_state.currentPage) {
            val current: Int = pager_state.currentPage

            for ((index, file) in (image_files ?: emptyList()).withIndex()) {
                val existing_request: Disposable? = image_requests[index]

                if ((index - current).absoluteValue <= IMAGE_LOAD_RADIUS) {
                    if (existing_request != null) {
                        continue
                    }

                    val request: ImageRequest = ImageRequest.Builder(context)
                        .data(file.uri)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build()

                    image_requests[index] = image_loader.enqueue(request)
                }
                else if (existing_request != null) {
                    existing_request.dispose()
                    image_requests[index] = null
                }
            }
        }

        HorizontalPager(
            pager_state,
            modifier,
            pageSpacing = 10.dp
        ) { index ->
            Box(Modifier.fillMaxSize().padding(content_padding), contentAlignment = content_alignment) {
                val job: Deferred<ImageResult>? = image_requests.getOrNull(index)?.job
                var image: ImageBitmap? by remember { mutableStateOf(null) }

                LaunchedEffect(job) {
                    image = null

                    if (job == null) {
                        return@LaunchedEffect
                    }

                    while (!job.isCompleted) {
                        delay(100)
                    }

                    image = job.getCompleted().drawable?.toBitmap()?.asImageBitmap()
                }

                LaunchedEffect(image, index, pager_state.currentPage) {
                    if (index == pager_state.currentPage) {
                        current_image = image
                    }
                }

                image.also {
                    if (it == null) {
                        SubtleLoadingIndicator(container_modifier = Modifier.align(Alignment.Center))
                    }
                    else {
                        Image(it, null, Modifier.clip(content_shape))
                    }
                }
            }
        }
    }
}
