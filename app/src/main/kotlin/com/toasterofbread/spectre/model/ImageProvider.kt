package com.toasterofbread.spectre.model

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.View.OnTouchListener
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraX
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.MeteringPoint
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.LifecycleOwner
import com.toasterofbread.composekit.utils.composable.AlignableCrossfade
import com.toasterofbread.spectre.ui.component.PermissionRequestButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class ImageProvider(private val activity: ComponentActivity) {
    private val permission_requester: ActivityResultLauncher<Array<String>>
    private var permission_result_callback: ((Boolean) -> Unit)? = null

    init {
        permission_requester =
            activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                val result: Boolean = results.values.firstOrNull() ?: return@registerForActivityResult

                permission_result_callback?.invoke(result)
                permission_result_callback = null
            }
    }

    suspend fun getLocalImages(): List<ImageFile>? {
        var permission_granted: Boolean? = null
        requestGalleryPermission { granted ->
            permission_granted = granted
        }

        while (permission_granted == null) {
            delay(100)
        }

        if (permission_granted != true) {
            return null
        }

        return getLocalImages(activity.contentResolver)
    }

    private suspend fun getLocalImages(content_resolver: ContentResolver): List<ImageFile> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
        )

        val collection_uri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val images = mutableListOf<ImageFile>()

        content_resolver.query(
            collection_uri,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val id_column = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val name_column = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val size_column = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val mime_type_column = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val uri = ContentUris.withAppendedId(collection_uri, cursor.getLong(id_column))
                val name = cursor.getString(name_column)
                val size = cursor.getLong(size_column)
                val mime_type = cursor.getString(mime_type_column)

                images.add(ImageFile(uri, name, size, mime_type))
            }
        }

        return@withContext images
    }

    @SuppressLint("ComposableNaming", "ClickableViewAccessibility")
    @Composable
    fun CameraPreview(
        modifier: Modifier,
        front_lens: Boolean = false,
        content_modifier: Modifier = Modifier,
        content_alignment: Alignment = Alignment.Center,
        onPointFocused: (Offset) -> Unit = {}
    ): CameraController? {
        var permission_granted: Boolean? by remember { mutableStateOf(null) }
        LaunchedEffect(Unit) {
            requestCameraPermission { granted ->
                permission_granted = granted
            }
        }

        val context: Context = LocalContext.current
        val lifecycle_owner: LifecycleOwner = LocalLifecycleOwner.current

        val controller: LifecycleCameraController = remember { LifecycleCameraController(context) }
        var controller_bound: Boolean by remember { mutableStateOf(false) }
        val camera_view: PreviewView = remember { PreviewView(context) }

        DisposableEffect(Unit) {
            controller.bindToLifecycle(lifecycle_owner)
            camera_view.controller = controller

            camera_view.performClick()
            camera_view.setOnTouchListener { _, event ->
                if (event.action == ACTION_UP) {
                    onPointFocused(Offset(event.x, event.y))
                }
                return@setOnTouchListener false
            }

            onDispose {
                controller.unbind()
            }
        }

        AlignableCrossfade(
            permission_granted,
            modifier,
            contentAlignment = content_alignment
        ) { granted ->
            if (granted == null) {
                return@AlignableCrossfade
            }

            if (granted == false) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    PermissionRequestButton("camera") {
                        permission_granted = null
                        requestCameraPermission { granted ->
                            permission_granted = granted
                        }
                    }
                }
                return@AlignableCrossfade
            }

            DisposableEffect(Unit) {
                controller_bound = true

                onDispose {
                    controller_bound = false
                }
            }

            val facing: Int =
                if (front_lens) CameraSelector.LENS_FACING_FRONT
                else CameraSelector.LENS_FACING_BACK

            LaunchedEffect(facing) {
                camera_view.performClick()
                controller.cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(facing)
                    .build()
            }

            AndroidView(
                { camera_view },
                content_modifier
            )
        }

        if (controller_bound) {
            return controller
        }
        else {
            return null
        }
    }

    private fun requestGalleryPermission(callback: (Boolean) -> Unit) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) == PermissionChecker.PERMISSION_GRANTED
        ) {
            callback(true)
        }
        else if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PermissionChecker.PERMISSION_GRANTED
        ) {
            callback(true)
        }
        else if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PermissionChecker.PERMISSION_GRANTED) {
            callback(true)
        }
        else {
            permission_result_callback = callback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                permission_requester.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED))
            }
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permission_requester.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
            }
            else {
                permission_requester.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            }
        }
    }

    private fun requestCameraPermission(callback: (Boolean) -> Unit) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PermissionChecker.PERMISSION_GRANTED) {
            callback(true)
            return
        }

        permission_result_callback = callback
        permission_requester.launch(arrayOf(Manifest.permission.CAMERA))
    }
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    val provider = ProcessCameraProvider.getInstance(this)
    provider.addListener(
        { continuation.resume(provider.get()) },
        ContextCompat.getMainExecutor(this)
    )
}
