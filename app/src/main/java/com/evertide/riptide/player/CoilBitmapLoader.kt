package com.evertide.riptide.player

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Singleton
@OptIn(androidx.media3.common.util.UnstableApi::class)
class CoilBitmapLoader
@Inject
constructor(
        @ApplicationContext private val context: Context,
        private val imageLoader: ImageLoader,
) : BitmapLoader {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun supportsMimeType(mimeType: String): Boolean {
        // Return true for common image mime types supported by Coil
        return mimeType.startsWith("image/")
    }

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        future.setException(UnsupportedOperationException("Raw byte decoding not used"))
        return future
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        scope.launch {
            val request =
                    ImageRequest.Builder(context)
                            .data(uri)
                            .allowHardware(false) // notification bitmaps must be software-backed
                            .build()

            when (val result = imageLoader.execute(request)) {
                is SuccessResult -> future.set(result.image.toBitmap())
                else ->
                        future.setException(
                                IllegalStateException("Failed to load artwork for notification")
                        )
            }
        }
        return future
    }
}
