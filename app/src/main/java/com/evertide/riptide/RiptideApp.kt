package com.evertide.riptide

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.evertide.riptide.data.image.ImageCacheManager
import com.evertide.riptide.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltAndroidApp
class RiptideApp : Application(), SingletonImageLoader.Factory {

    @Inject lateinit var imageLoader: ImageLoader
    @Inject lateinit var imageCacheManager: ImageCacheManager
    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            imageCacheManager.evictExpired(imageLoader)
            cacheDir.resolve("thumbnail_cache").deleteRecursively()
        }
        notificationHelper.ensureChannel()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader
}
