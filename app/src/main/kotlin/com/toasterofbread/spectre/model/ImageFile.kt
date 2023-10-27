package com.toasterofbread.spectre.model

import android.net.Uri

data class ImageFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val mime_type: String
)
