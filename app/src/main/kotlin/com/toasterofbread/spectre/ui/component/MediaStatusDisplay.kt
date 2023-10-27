package com.toasterofbread.spectre.ui.component

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import com.toasterofbread.spectre.MediaListenerService
import com.toasterofbread.toastercomposetools.settings.ui.Theme
import com.toasterofbread.toastercomposetools.utils.common.amplify
import com.toasterofbread.toastercomposetools.utils.common.blendWith
import com.toasterofbread.toastercomposetools.utils.common.getContrasted
import com.toasterofbread.toastercomposetools.utils.common.getNeutral
import com.toasterofbread.toastercomposetools.utils.common.getThemeColour
import com.toasterofbread.toastercomposetools.utils.composable.Marquee
import com.toasterofbread.toastercomposetools.utils.composable.WidthShrinkText
import com.toasterofbread.toastercomposetools.utils.composable.pauseableInfiniteRepeatableAnimation
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.sign

data class MediaSessionState(
    val title: String,
    val artist_name: String,
    val duration: Long,
    val uri: String,
    val display_icon: ImageBitmap?,
    val album_art: ImageBitmap?,

    val source: MediaSource,

    // TODO | Temporary, move to body
    private var theme_colour: Color? = null
) {

    fun getThemeColour(): Color {
        if (theme_colour == null) {
            theme_colour = display_icon?.getThemeColour()
        }
        return theme_colour ?: Color.Unspecified
    }
}

data class MediaSource private constructor(
    val package_name: String,
    val name: String,
    val icon: ImageBitmap
) {
    companion object {
        fun fromPackageName(package_name: String, package_manager: PackageManager): MediaSource {
            val info: PackageInfo = package_manager.getPackageInfo(package_name, 0)

            return MediaSource(
                package_name = package_name,
                name = info.applicationInfo.loadLabel(package_manager).toString(),
                icon = info.applicationInfo.loadIcon(package_manager).toBitmap().asImageBitmap()
            )
        }
    }
}

class MediaSessionListener(context: Context) {
    private val session_manager: MediaSessionManager = context.getSystemService()!!
    private val service_component: ComponentName = ComponentName(context, MediaListenerService::class.java)
    private val package_manager: PackageManager = context.packageManager

    private val media_sources: MutableMap<String, MediaSource> = mutableMapOf()

    private val TEST_SESSIONS =
        listOf(
            MediaSessionState(
                title = "カトラリー",
                artist_name = "神山羊",
                duration = 694000,
                uri = "HHhFX9zUV2s",
                display_icon = null,
                album_art = null,

                source = media_sources.getOrPut("com.toasterofbread.spmp") {
                    MediaSource.fromPackageName("com.toasterofbread.spmp", package_manager)
                },
                theme_colour = Color(158, 36, 93)
            ),
            MediaSessionState(
                title = "リレイアウター",
                artist_name = "稲葉曇",
                duration = 914000,
                uri = "b56xjtP6Qac",
                display_icon = null,
                album_art = null,

                source = media_sources.getOrPut("com.toasterofbread.spmp") {
                    MediaSource.fromPackageName("com.toasterofbread.spmp", package_manager)
                },
                theme_colour = Color.DarkGray
            )
        )

    fun getMediaSessions(): List<MediaSessionState> {
        // TODO | Ask for notification permission
        val active_sessions = session_manager.getActiveSessions(service_component)

        val sessions = active_sessions.mapNotNull { session ->
            val metadata = session.metadata ?: return@mapNotNull null
            if (metadata.size() == 0) {
                return@mapNotNull null
            }

            val media_source = media_sources.getOrPut(session.packageName) {
                MediaSource.fromPackageName(session.packageName, package_manager)
            }

            return@mapNotNull MediaSessionState(
                title = metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
                artist_name = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST),
                duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION),
                uri =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_URI)
                else metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI),
                display_icon = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)?.asImageBitmap(),
                album_art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)?.asImageBitmap(),

                source = media_source
            )
        }

        return sessions + TEST_SESSIONS
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaStatusDisplay(
    theme: Theme,
    modifier: Modifier = Modifier,
    content_padding: PaddingValues = PaddingValues()
) {
    val context = LocalContext.current
    var media_sessions: List<MediaSessionState> by remember { mutableStateOf(emptyList()) }

    LaunchedEffect(Unit) {
        val session_listener = MediaSessionListener(context)

        while (true) {
            media_sessions = session_listener.getMediaSessions()
            delay(1000)
        }
    }

    val pager_state = rememberPagerState { media_sessions.size }

    LaunchedEffect(media_sessions) {
        theme.currentThumbnnailColourChanged(media_sessions[pager_state.currentPage].getThemeColour())
    }

    LaunchedEffect(pager_state.currentPageOffsetFraction) {
        val current_colour = media_sessions[pager_state.currentPage].getThemeColour()
        val next_colour = media_sessions[
            pager_state.currentPage + pager_state.currentPageOffsetFraction.sign.toInt()
        ].getThemeColour()

        theme.currentThumbnnailColourChanged(
            next_colour.blendWith(
                other = current_colour,
                ratio = pager_state.currentPageOffsetFraction.absoluteValue
            ),
            snap = true
        )
    }

    HorizontalPager(
        pager_state,
        modifier,
        pageSpacing = 0.dp
    ) {
        val session: MediaSessionState = media_sessions[it]
        val shape = RoundedCornerShape(10.dp)
        val theme_colour = session.getThemeColour()

        val wave_offset: Float by pauseableInfiniteRepeatableAnimation(
            start = 0f,
            end = 1f,
            period = 30000,
            getPlaying = {
                true
            }
        )

        val line_colour = theme_colour.amplify(-0.1f).copy(alpha = 0.3f)

        Box(
            Modifier
                .padding(content_padding)
                .clip(shape)
                .background(theme_colour)
                .border(2.dp, line_colour, shape)
        ) {
            CompositionLocalProvider(LocalContentColor provides theme_colour.getContrasted()) {
                if (session.display_icon != null) {
                    Image(
                        session.display_icon,
                        null,
                        Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Canvas(Modifier.fillMaxSize()) {
                    val h_wave_count = 10
                    val wave_height = 15.dp
                    val wave_spacing = 25.dp
                    val wave_stroke = Stroke(2.dp.toPx())

                    rotate(-15f) {
                        val path = Path()

                        fun drawWave(position: Float, offset: Float) {
                            wavePath(path, -1, wave_height.toPx(), h_wave_count) { (offset - wave_offset) * size.width }
                            path.translate(Offset(0f, position))
                            drawPath(path, line_colour, style = wave_stroke)

                            wavePath(path, 1, wave_height.toPx(), h_wave_count) { (offset - wave_offset) * size.width }
                            path.translate(Offset(0f, position))
                            drawPath(path, line_colour, style = wave_stroke)
                        }

                        for (wave in 0 until (maxOf(size.width, size.height) / wave_spacing.toPx()).toInt()) {
                            drawWave(((wave_spacing * wave) - 150.dp).toPx(), wave * 0.5f)
                        }
                    }
                }

                Row(
                    Modifier
                        .background(theme_colour.copy(alpha = 0.5f))
                        .fillMaxSize()
                        .padding(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Marquee {
                            Text(session.title, style = MaterialTheme.typography.titleLarge, softWrap = false)
                        }
                        Marquee {
                            Text(session.artist_name, style = MaterialTheme.typography.bodySmall, softWrap = false)
                        }
                    }

                    Image(
                        session.source.icon, null,
                        Modifier
                            .align(Alignment.Bottom)
                            .size(24.dp)
                    )
                }
            }
        }
    }
}

private fun DrawScope.wavePath(
    path: Path,
    direction: Int,
    height: Float,
    waves: Int,
    getOffset: (DrawScope.() -> Float)? = null
): Path {
    path.reset()

    val y_offset = height / 2
    val half_period = (size.width / (waves - 1)) / 2
    val offset_px = getOffset?.invoke(this)?.let { offset ->
        offset % size.width - (if (offset > 0f) size.width else 0f)
    } ?: 0f

    path.moveTo(x = -half_period / 2 + offset_px, y = y_offset)

    for (i in 0 until ceil((size.width * 2) / half_period + 1).toInt()) {
        if ((i % 2 == 0) != (direction == 1)) {
            path.relativeMoveTo(half_period, 0f)
            continue
        }

        path.relativeQuadraticBezierTo(
            dx1 = half_period / 2,
            dy1 = height / 2 * direction,
            dx2 = half_period,
            dy2 = 0f,
        )
    }

    return path
}
