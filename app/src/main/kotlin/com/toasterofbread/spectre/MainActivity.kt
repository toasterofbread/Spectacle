package com.toasterofbread.spectre

import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.toasterofbread.spectre.model.ImageProvider
import com.toasterofbread.spectre.ui.layout.AppPage
import com.toasterofbread.spectre.ui.layout.ImageAdjustPage
import com.toasterofbread.spectre.ui.layout.ImageCapturePage
import com.toasterofbread.spectre.ui.theme.SpectreTheme
import com.toasterofbread.toastercomposetools.settings.ui.Theme
import com.toasterofbread.toastercomposetools.settings.ui.ThemeData
import com.toasterofbread.toastercomposetools.utils.common.getContrasted


class MainActivity: ComponentActivity() {
    private lateinit var image_provider: ImageProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        image_provider = ImageProvider(this)

        setContent {
            SpectreTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Test()
                }
            }
        }
    }

    @Composable
    fun Test() {
        val coroutine_scope = rememberCoroutineScope()
        val theme = remember {
            object : Theme("System theme", coroutine_scope) {
                override fun saveThemes(themes: List<ThemeData>) {}
                override fun loadThemes(): List<ThemeData> = emptyList()

                override fun selectAccentColour(theme_data: ThemeData, thumbnail_colour: Color?): Color {
                    return thumbnail_colour ?: theme_data.accent
                }

                override fun getLightColorScheme(): ColorScheme =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicLightColorScheme(this@MainActivity)
                    else lightColorScheme()

                override fun getDarkColorScheme(): ColorScheme =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(this@MainActivity)
                    else darkColorScheme()
            }
        }

        theme.Update()

        var current_page: AppPage? by remember { mutableStateOf(null) }
        val capture_page: AppPage = remember {
            object : ImageCapturePage(image_provider) {
                override fun onCaptured(adjust: Boolean, base_image: ImageBitmap, overlay_image: ImageBitmap) {
                    println("AA $adjust")
                    if (adjust) {
                        current_page = ImageAdjustPage(base_image, overlay_image)
                    }
                }
            }.also {
                current_page = it
            }
        }

        Crossfade(current_page) { page ->
            CompositionLocalProvider(LocalContentColor provides theme.on_accent) {
                page?.Page(theme, Modifier.fillMaxSize().background(theme.accent))
            }

            BackHandler(page != capture_page) {
                current_page = capture_page
            }
        }
    }
}
