package com.evertide.riptide.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.evertide.riptide.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper
@Inject
constructor(
        @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_ID = "library_jobs"
        private const val CONSUME_NOTIFICATION_ID = 1001
        private const val RESYNC_NOTIFICATION_ID = 1002
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel =
                NotificationChannel(
                                CHANNEL_ID,
                                "Library jobs",
                                NotificationManager.IMPORTANCE_DEFAULT,
                        )
                        .apply {
                            description = "Notifies when library ingestion or resync finishes"
                        }
        manager.createNotificationChannel(channel)
    }

    fun notifyConsumeFinished(success: Boolean, processed: Int) =
            notify(
                    id = CONSUME_NOTIFICATION_ID,
                    title =
                            if (success) "Library ingestion complete"
                            else "Library ingestion failed",
                    text =
                            if (success) "$processed song(s) added to your library."
                            else "Something went wrong during ingestion.",
            )

    fun notifyResyncFinished(success: Boolean, processed: Int) =
            notify(
                    id = RESYNC_NOTIFICATION_ID,
                    title = if (success) "Library resync complete" else "Library resync failed",
                    text =
                            if (success) "$processed song(s) resynced."
                            else "Something went wrong during resync.",
            )

    @SuppressLint("MissingPermission")
    private fun notify(id: Int, title: String, text: String) {
        // Pass CHANNEL_ID directly to the builder constructor to satisfy modern Android
        // requirements
        val notification =
                NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build()

        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}
