package com.toasterofbread.spectre.ui.component.imageselector

import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.toasterofbread.spectre.model.ImageFile
import com.toasterofbread.spectre.model.ImageProvider
import com.toasterofbread.toastercomposetools.settings.ui.Theme
import com.toasterofbread.toastercomposetools.utils.composable.SubtleLoadingIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.math.absoluteValue

private const val IMAGE_LOAD_RADIUS: Int = 10
private const val MAX_SIMULTANEOUS_LOADERS: Int = 5

class GalleryImageSelector: ImageSelector {
    override fun getIcon(): ImageVector =
        Icons.Default.Photo

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Selector(
        theme: Theme,
        image_provider: ImageProvider,
        content_alignment: Alignment,
        content_padding: PaddingValues,
        content_shape: Shape,
        modifier: Modifier,
        onSelectedImageChanged: (ImageBitmap?) -> Unit
    ) {
        var image_files: List<ImageFile>? by remember { mutableStateOf(null) }

        LaunchedEffect(Unit) {
            image_files = image_provider.getLocalImages()
        }

        val context = LocalContext.current
        val coroutine_scope = rememberCoroutineScope()

        var image_loaders: Array<Job?> by remember { mutableStateOf(emptyArray()) }
        val loaded_images: MutableList<ImageBitmap?> = remember { mutableStateListOf() }

        val pager_state: PagerState = rememberPagerState { image_files?.size ?: 0 }
        val load_mutex = remember { Semaphore(permits = MAX_SIMULTANEOUS_LOADERS) }

        LaunchedEffect(image_files) {
            for (loader in image_loaders) {
                loader?.cancel()
            }
            image_loaders = image_loaders.copyOf(image_files?.size ?: 0)
        }

        LaunchedEffect(image_files, pager_state.currentPage) {
            val current = pager_state.currentPage

            for ((index, file) in (image_files ?: emptyList()).withIndex()) {
                val loading: Job? = image_loaders.getOrNull(index)

                if ((index - current).absoluteValue > IMAGE_LOAD_RADIUS) {
                    if (loading != null) {
                        image_loaders[index] = null
                        synchronized(loaded_images) {
                            if (index in loaded_images.indices) {
                                loaded_images[index] = null
                            }
                        }
                    }
                }
                else if (loading == null || loading.isCancelled) {
                    val loader: Job = coroutine_scope.launch(Dispatchers.IO) {
                        load_mutex.withPermit {
                            val bitmap: ImageBitmap =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, file.uri)).asImageBitmap()
                                }
                                else {
                                    @Suppress("DEPRECATION")
                                    MediaStore.Images.Media.getBitmap(context.contentResolver, file.uri).asImageBitmap()
                                }

                            synchronized(loaded_images) {
                                if (loaded_images.size <= index) {
                                    while (loaded_images.size < index) {
                                        loaded_images.add(null)
                                    }
                                    loaded_images.add(bitmap)
                                }
                                else {
                                    loaded_images[index] = bitmap
                                }
                            }
                        }
                    }

                    image_loaders[index] = loader
                }
            }
        }

        HorizontalPager(
            pager_state,
            modifier,
            pageSpacing = 10.dp
        ) { index ->
            Box(Modifier.fillMaxSize().padding(content_padding), contentAlignment = content_alignment) {
                val image: ImageBitmap? = loaded_images.getOrNull(index)
                LaunchedEffect(image, index, pager_state.currentPage) {
                    if (index == pager_state.currentPage) {
                        onSelectedImageChanged(image)
                    }
                }

                if (image == null) {
                    SubtleLoadingIndicator(container_modifier = Modifier.align(Alignment.Center))
                    return@HorizontalPager
                }

                Image(image, null, Modifier.clip(content_shape))
            }
        }
    }
}
