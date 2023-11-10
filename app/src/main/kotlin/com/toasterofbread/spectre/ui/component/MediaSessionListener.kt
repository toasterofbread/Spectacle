package com.toasterofbread.spectre.ui.component

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.toBitmap
import com.toasterofbread.composekit.utils.common.amplify
import com.toasterofbread.composekit.utils.common.getThemeColour
import com.toasterofbread.spectre.MediaListenerService

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

data class MediaSessionState(
    val title: String,
    val artist_name: String,
    val duration: Long,
    val uri: String,
    val display_icon: ImageBitmap?,
    val album_art: ImageBitmap?,

    val source: MediaSource,
    private var theme_colour: Color? = null
) {

    fun setThemeColour(value: Color) {
        theme_colour = value
    }

    fun getThemeColour(): Color? {
        if (theme_colour == null) {
            theme_colour = display_icon?.getThemeColour()
        }
        return theme_colour
    }

    fun getThemeAccentColour(): Color? =
        getThemeColour()?.amplify(0.2f)
}

class MediaSessionListener(private val context: Context) {
    private val session_manager: MediaSessionManager = context.getSystemService()!!
    private val service_component: ComponentName = ComponentName(context, MediaListenerService::class.java)
    private val package_manager: PackageManager = context.packageManager

    private val media_sources: MutableMap<String, MediaSource> = mutableMapOf()
    private var permission_granted: Boolean = false

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
                }
            ).apply { setThemeColour(Color(158, 36, 93)) },
            MediaSessionState(
                title = "リレイアウター",
                artist_name = "稲葉曇",
                duration = 914000,
                uri = "b56xjtP6Qac",
                display_icon = null,
                album_art = null,

                source = media_sources.getOrPut("com.toasterofbread.spmp") {
                    MediaSource.fromPackageName("com.toasterofbread.spmp", package_manager)
                }
            ).apply { setThemeColour(Color.DarkGray) }
        )

    fun getMediaSessions(): List<MediaSessionState>? {
        if (!permission_granted) {
            val enabled_notification_listeners: String = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            if (!enabled_notification_listeners.contains(context.applicationContext.packageName)) {
                return null
            }
            permission_granted = true
        }

        val active_sessions: MutableList<MediaController> = session_manager.getActiveSessions(service_component)

        val sessions: List<MediaSessionState> = active_sessions.mapNotNull { session ->
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

    fun requestPermission() {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        context.startActivity(intent)
    }
}
