/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.quantumbadger.redreader.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.InboxListingActivity
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequest.DownloadQueueType
import org.quantumbadger.redreader.cache.CacheRequest.DownloadStrategyAlways
import org.quantumbadger.redreader.cache.CacheRequest.RequestFailureType
import org.quantumbadger.redreader.cache.CacheRequestCallbacks
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.Constants.Reddit
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.General.getGeneralErrorForFailure
import org.quantumbadger.redreader.common.General.getSharedPrefs
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.http.FailedRequestBody
import org.quantumbadger.redreader.receivers.announcements.AnnouncementDownloader
import org.quantumbadger.redreader.reddit.api.RedditPostActions
import org.quantumbadger.redreader.reddit.kthings.JsonUtils
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import org.quantumbadger.redreader.reddit.kthings.RedditThing
import org.quantumbadger.redreader.reddit.kthings.RedditThing.Listing
import org.quantumbadger.redreader.reddit.kthings.UrlEncodedString
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * WorkManager Worker that checks for new Reddit messages.
 * Replaces the existing NewMessageChecker BroadcastReceiver + AlarmManager pattern.
 */
@HiltWorker
class NewMessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "NewMessageWorker"

    override suspend fun doWork(): Result {
        return try {
            checkForNewMessages(applicationContext)
            AnnouncementDownloader.performDownload(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in NewMessageWorker", e)
            Result.retry()
        }
    }

    companion object {
        private const val PREFS_SAVED_MESSAGE_ID = "LastMessageId"
        private const val PREFS_SAVED_MESSAGE_TIMESTAMP = "LastMessageTimestamp"
        private const val NOTIFICATION_CHANNEL_ID = "RRNewMessageChecker"

        private val sChannelCreated = AtomicBoolean(false)

        fun createNotification(
            title: String?,
            text: String?,
            context: Context
        ) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            synchronized(sChannelCreated) {
                if (!sChannelCreated.getAndSet(true)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (nm.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                            Log.i(TAG, "Creating notification channel")

                            val channel = NotificationChannel(
                                NOTIFICATION_CHANNEL_ID,
                                context.getString(string.notification_channel_name_reddit_messages),
                                NotificationManager.IMPORTANCE_DEFAULT
                            )

                            nm.createNotificationChannel(channel)
                        }
                    }
                }
            }

            val notification = NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.icon_notif)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .setColor(Color.rgb(0xd3, 0x2f, 0x2f))

            val intent = Intent(context, InboxListingActivity::class.java)

            var flags = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags = flags or PendingIntent.FLAG_IMMUTABLE
            }

            notification.setContentIntent(PendingIntent.getActivity(context, 0, intent, flags))

            nm.notify(0, notification.build())
        }
    }
}
