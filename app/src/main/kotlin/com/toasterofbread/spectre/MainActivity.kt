package com.toasterofbread.spectre

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.toasterofbread.composekit.platform.ApplicationContext
import com.toasterofbread.composekit.platform.PlatformContext
import com.toasterofbread.spectre.model.ImageProvider
import com.toasterofbread.spectre.ui.layout.AppPage
import com.toasterofbread.spectre.ui.layout.ImageAdjustPage
import com.toasterofbread.spectre.ui.layout.ImageCapturePage
import com.toasterofbread.spectre.ui.theme.SpectreTheme
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.settings.ui.ThemeData
import com.toasterofbread.spectre.model.ImageSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AppTheme: Theme("System theme") {
    private lateinit var platform_context: PlatformContext

    fun init(context: PlatformContext) {
        platform_context = context
    }

    override fun saveThemes(themes: List<ThemeData>) {}
    override fun loadThemes(): List<ThemeData> = emptyList()

    override fun selectAccentColour(theme_data: ThemeData, thumbnail_colour: Color?): Color {
        return thumbnail_colour ?: theme_data.accent
    }

    override fun getLightColorScheme(): ColorScheme =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicLightColorScheme(platform_context.ctx)
        else lightColorScheme()

    override fun getDarkColorScheme(): ColorScheme =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(platform_context.ctx)
        else darkColorScheme()

    override fun onAccentColourChanged(colour: Color) {
        platform_context.setStatusBarColour(colour)
    }
}

val LocalTheme: ProvidableCompositionLocal<AppTheme> = staticCompositionLocalOf { AppTheme() }

class MainActivity: ComponentActivity() {
    private lateinit var image_provider: ImageProvider

    private val permission_callbacks: MutableList<(Boolean) -> Unit> = mutableListOf()
    private lateinit var permission_launcher: ActivityResultLauncher<String>
    private val coroutine_scope = CoroutineScope(Job())

    fun requestPermission(permission: String, callback: (Boolean) -> Unit) {
        synchronized(permission_callbacks) {
            permission_callbacks.add(callback)

            if (permission_callbacks.size == 1) {
                permission_launcher.launch(permission)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = PlatformContext(this, coroutine_scope, ApplicationContext(this))
        image_provider = ImageProvider(this)

        permission_launcher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            synchronized(permission_callbacks) {
                for (callback in permission_callbacks) {
                    callback(granted)
                }
                permission_callbacks.clear()
            }
        }

        setContent {
            SpectreTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Test(context)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutine_scope.cancel()
    }

    @Composable
    fun Test(platform_context: PlatformContext) {
        val coroutine_scope = rememberCoroutineScope()
        val theme: AppTheme = LocalTheme.current

        theme.init(platform_context)
        theme.Update()

        var current_page: AppPage? by remember { mutableStateOf(null) }
        val capture_page: AppPage = remember {
            object : ImageCapturePage(image_provider) {
                private val image_saver: ImageSaver = ImageSaver(platform_context)

                override fun saveCapture(base_image: ImageBitmap, overlay_image: ImageBitmap, capture_data: CaptureData) {
                    coroutine_scope.launch {
                        image_saver.saveImage(base_image, overlay_image, capture_data)
                    }
                }

                override fun adjustCapture(base_image: ImageBitmap, capture_data: CaptureData) {
                    current_page = ImageAdjustPage(base_image, capture_data, image_saver)
                }
            }.also {
                current_page = it
            }
        }

        Crossfade(current_page) { page ->
            CompositionLocalProvider(LocalContentColor provides theme.on_accent) {
                page?.Page(
                    theme,
                    Modifier
                        .fillMaxSize()
                        .background(theme.accent)
                        .padding(bottom = 10.dp)
                )
            }

            BackHandler(page != capture_page) {
                current_page = capture_page
            }
        }
    }
}
