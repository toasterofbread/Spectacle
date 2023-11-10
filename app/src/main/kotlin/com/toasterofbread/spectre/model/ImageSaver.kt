package com.toasterofbread.spectre.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.os.Environment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.toasterofbread.composekit.platform.PlatformContext
import com.toasterofbread.spectre.R
import com.toasterofbread.spectre.ui.layout.ImageCapturePage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

class ImageSaver(private val context: PlatformContext) {
    suspend fun saveImage(base_image: ImageBitmap, overlay_image: ImageBitmap, capture_data: ImageCapturePage.CaptureData) = withContext(Dispatchers.IO) {
        val final_image: Bitmap = Bitmap.createBitmap(base_image.height, base_image.width, Bitmap.Config.RGB_565)
        val canvas: Canvas = Canvas(final_image)

        val matrix: Matrix = Matrix()

        matrix.postRotate(90f)
        matrix.postTranslate(base_image.height.toFloat(), 0f)
        canvas.drawBitmap(base_image.asAndroidBitmap(), matrix, Paint())

        val overlay_height: Float = canvas.width * (overlay_image.height.toFloat() / overlay_image.width.toFloat())
        val overlay_offset: Offset = capture_data.overlay_offset + Offset(0f, canvas.height - overlay_height)

        canvas.drawBitmap(
            overlay_image.asAndroidBitmap(),
            Rect(0, 0, overlay_image.width, overlay_image.height),
            Rect(
                overlay_offset.x.toInt(),
                overlay_offset.y.toInt(),
                canvas.width + overlay_offset.x.toInt(),
                overlay_height.toInt() + overlay_offset.y.toInt()
            ),
            Paint()
        )

        val directory: File = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            context.ctx.getString(R.string.app_name)
        )
        directory.mkdirs()

        val filename: String = capture_data.getFilename() + ".jpg"
        val file: File = directory.resolve(filename)

        file.outputStream().use { stream ->
            println("BEGIN WRITE")
            final_image.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            println("END WRITE")
        }

        context.sendToast("Image saved ${file.absolutePath}")
    }
}
