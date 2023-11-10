package com.toasterofbread.spectre.ui.component

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import com.toasterofbread.composekit.platform.composable.platformClickable
import com.toasterofbread.composekit.settings.ui.Theme
import com.toasterofbread.composekit.utils.common.amplify
import com.toasterofbread.composekit.utils.common.blendWith
import com.toasterofbread.composekit.utils.common.getContrasted
import com.toasterofbread.composekit.utils.common.thenWith
import com.toasterofbread.composekit.utils.composable.NullCrossfade
import com.toasterofbread.composekit.utils.composable.pauseableInfiniteRepeatableAnimation
import com.toasterofbread.spectre.LocalTheme
import com.toasterofbread.spectre.ui.modifier.ComposableCaptureState
import com.toasterofbread.spectre.ui.modifier.capturable
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue
import kotlin.math.sign


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaStatusDisplay(
    theme: Theme,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    media_sessions_override: List<MediaSessionState>? = null,
    content_padding: PaddingValues = PaddingValues(),
    capture_state: ComposableCaptureState? = null,
    onSelectedSessionChanged: (MediaSessionState?) -> Unit = {}
) {
    val context: Context = LocalContext.current
    val session_listener: MediaSessionListener = remember { MediaSessionListener(context) }

    var media_sessions: List<MediaSessionState>? by remember { mutableStateOf(media_sessions_override ?: emptyList()) }
    val pager_state: PagerState = rememberPagerState { media_sessions?.size ?: 0 }
    var editing_session: MediaSessionState? by remember { mutableStateOf(null) }

    val edited_sessions: MutableMap<Pair<String, String>, MediaSessionState> = remember { mutableStateMapOf() }

    editing_session?.also { session ->
        SessionStateEditor(session) { edited ->
            if (edited != null) {
                edited_sessions[Pair(session.title, session.artist_name)] = edited
                onSelectedSessionChanged(edited)
            }
            editing_session = null
        }
    }

    fun List<MediaSessionState>.getSession(index: Int): MediaSessionState? =
        getOrNull(index)?.let { session ->
            edited_sessions[Pair(session.title, session.artist_name)] ?: session
        }

    LaunchedEffect(media_sessions_override) {
        if (media_sessions_override != null) {
            media_sessions = media_sessions_override
            return@LaunchedEffect
        }

        while (true) {
            media_sessions = session_listener.getMediaSessions()
            delay(1000)
        }
    }

    LaunchedEffect(media_sessions) {
        theme.currentThumbnnailColourChanged(media_sessions?.getOrNull(pager_state.currentPage)?.getThemeAccentColour())
    }

    LaunchedEffect(pager_state.currentPageOffsetFraction, editing_session) {
        val sessions: List<MediaSessionState> = media_sessions ?: return@LaunchedEffect

        val current_colour: Color =
            sessions.getSession(
                pager_state.currentPage
            )?.getThemeAccentColour() ?: return@LaunchedEffect

        val next_colour: Color =
            sessions.getSession(
                pager_state.currentPage + pager_state.currentPageOffsetFraction.sign.toInt()
            )?.getThemeAccentColour() ?: return@LaunchedEffect

        theme.currentThumbnnailColourChanged(
            next_colour.blendWith(
                other = current_colour,
                ratio = pager_state.currentPageOffsetFraction.absoluteValue
            ),
            snap = true
        )
    }

    NullCrossfade(media_sessions, modifier) { sessions ->
        if (sessions == null) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PermissionRequestButton("notification access") {
                    session_listener.requestPermission()
                }
            }
        }
        else {
            HorizontalPager(
                pager_state,
                Modifier.platformClickable(
                    enabled = interactive,
                    onAltClick = {
                        editing_session = sessions.getSession(pager_state.currentPage)
                    }
                ),
                pageSpacing = 0.dp,
                userScrollEnabled = interactive
            ) {
                val session: MediaSessionState? = sessions.getSession(it)

                onSelectedSessionChanged(session)

                if (session == null) {
                    return@HorizontalPager
                }

                val shape: Shape = RoundedCornerShape(10.dp)
                val theme_colour: Color = session.getThemeColour() ?: Color.Unspecified

                val wave_offset: Float by pauseableInfiniteRepeatableAnimation(
                    start = 0f,
                    end = 1f,
                    period = 30000,
                    getPlaying = {
                        true
                    }
                )

                val line_colour: Color = theme_colour.amplify(-0.1f).copy(alpha = 0.3f)

                Row(
                    Modifier
                        .padding(content_padding)
                        .thenWith(capture_state) { capturable(it) },
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CompositionLocalProvider(LocalContentColor provides theme_colour.getContrasted()) {
                        if (session.display_icon != null) {
                            Image(
                                session.display_icon,
                                null,
                                Modifier
                                    .aspectRatio(1f)
                                    .clip(shape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(shape)
                                .border(2.dp, line_colour, shape)
                        ) {
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
//                                    .background(theme_colour.copy(alpha = 0.1f))
                                    .fillMaxSize()
                                    .padding(7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Box(Modifier.fillMaxHeight(0.5f), contentAlignment = Alignment.BottomStart) {
                                        Text(
                                            session.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            softWrap = false
                                        )
                                    }

                                    Row(Modifier.fillMaxHeight()) {
                                        Text(
                                            session.artist_name,
                                            Modifier
                                                .alpha(0.7f)
                                                .align(Alignment.Top),
                                            style = MaterialTheme.typography.bodySmall,
                                            softWrap = false
                                        )

                                        Spacer(
                                            Modifier
                                                .fillMaxWidth()
                                                .weight(1f))

                                        Image(
                                            session.source.icon, null,
                                            Modifier
                                                .size(20.dp)
                                                .align(Alignment.Bottom)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionStateEditor(state: MediaSessionState, modifier: Modifier = Modifier, onCompleted: (MediaSessionState?) -> Unit) {
    val theme: Theme = LocalTheme.current

    var title: String by remember(state) { mutableStateOf(state.title) }
    var artist_name: String by remember(state) { mutableStateOf(state.artist_name) }
    var theme_colour: Color? by remember(state) { mutableStateOf(state.getThemeColour()) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = { onCompleted(null) },
        confirmButton = {
            Button(
                {
                    onCompleted(
                        state.copy(
                            title = title,
                            artist_name = artist_name,
                            theme_colour = theme_colour
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.accent,
                    contentColor = theme.on_accent
                )
            ) {
                Text("Done")
            }
        },
        title = {
            Text("Edit media")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(
                    title,
                    { title = it },
                    label = { Text("Title") }
                )
                TextField(
                    artist_name,
                    { artist_name = it },
                    label = { Text("Artist") }
                )

                ClassicColorPicker(
                    Modifier.fillMaxWidth().aspectRatio(1f),
                    HsvColor.from(theme_colour ?: Color.Unspecified),
                    showAlphaBar = false
                ) { colour ->
                    theme_colour = colour.toColor()
                }
            }
        }
    )
}
