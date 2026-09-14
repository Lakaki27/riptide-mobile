package com.evertide.riptide.util

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap

suspend fun getDominantColor(context: Context, imageLoader: ImageLoader, url: String?): Color? {
    if (url == null) return null
    val request =
            ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false) // Palette needs CPU-readable pixels
                    .build()
    val result = imageLoader.execute(request)
    if (result !is SuccessResult) return null
    val bitmap = result.image.toBitmap()
    val swatch = Palette.from(bitmap).generate().dominantSwatch ?: return null
    return Color(swatch.rgb)
}
