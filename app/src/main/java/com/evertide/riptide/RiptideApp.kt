package com.evertide.riptide

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.evertide.riptide.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class RiptideApp : Application(), SingletonImageLoader.Factory {

    @Inject lateinit var imageLoader: ImageLoader
    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        notificationHelper.ensureChannel()
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader = imageLoader
}
