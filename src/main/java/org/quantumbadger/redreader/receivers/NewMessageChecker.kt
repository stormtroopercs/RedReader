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
 * along with RedReader.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package org.quantumbadger.redreader.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.MainActivityCompose
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequest.DownloadQueueType
import org.quantumbadger.redreader.cache.CacheRequest.RequestFailureType
import org.quantumbadger.redreader.cache.CacheRequestCallbacks
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyAlways
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.Constants.Reddit
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.General.getGeneralErrorForFailure
import org.quantumbadger.redreader.common.General.getSharedPrefs
import org.quantumbadger.redreader.common.General.isSensitiveDebugLoggingEnabled
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.UriString.Companion.from
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.http.FailedRequestBody
import org.quantumbadger.redreader.receivers.announcements.AnnouncementDownloader
import org.quantumbadger.redreader.reddit.kthings.JsonUtils.decodeRedditThingFromStream
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import org.quantumbadger.redreader.reddit.kthings.RedditThing
import org.quantumbadger.redreader.reddit.kthings.RedditThing.Listing
import org.quantumbadger.redreader.reddit.kthings.UrlEncodedString
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class NewMessageChecker : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        checkForNewMessages(context)
        AnnouncementDownloader.performDownload(context)
    }

    companion object {
        private const val TAG = "NewMessageChecker"

        private const val NOTIFICATION_CHANNEL_ID = "RRNewMessageChecker"

        const val PREFS_SAVED_MESSAGE_ID: String = "LastMessageId"
        const val PREFS_SAVED_MESSAGE_TIMESTAMP: String = "LastMessageTimestamp"


        fun checkForNewMessages(rawContext: Context) {
            // Ensure notification strings respect the app's language setting

            val context = PrefsUtility.getLocalisedContext(rawContext)

            Log.i("RedReader", "Checking for new messages.")

            val notificationsEnabled = PrefsUtility.pref_behaviour_notifications()
            if (!notificationsEnabled) {
                return
            }

            val user: RedditAccount

            try {
                user = RedditAccountManager.Companion.getInstance(context).getDefaultAccount()
            } catch (e: SQLiteDatabaseCorruptException) {
                // Avoid background crash
                Log.e(TAG, "Accounts database corrupt", e)
                return
            }

            if (user.isAnonymous) {
                return
            }

            val cm: CacheManager = CacheManager.Companion.getInstance(context)

            val url = Reddit.getUri("/message/unread.json?limit=2")

            val request = CacheRequest(
                url,
                user,
                null,
                Priority(Constants.Priority.API_INBOX_LIST),
                DownloadStrategyAlways.Companion.INSTANCE,
                Constants.FileType.INBOX_LIST,
                DownloadQueueType.REDDIT_API,
                false,
                context,
                object : CacheRequestCallbacks {
                    override fun onFailure(error: RRError) {
                        Log.e(TAG, "Request failed: " + error, error.t)
                    }

                    override fun onDataStreamComplete(
                        streamFactory: GenericFactory<SeekableInputStream, IOException>,
                        timestamp: TimestampUTC,
                        session: UUID,
                        fromCache: Boolean,
                        mimetype: String?
                    ) {
                        try {
                            val listingThing = decodeRedditThingFromStream(streamFactory.create())

                            val listing = (listingThing as Listing).data

                            val messageCount = listing.children.size

                            if (isSensitiveDebugLoggingEnabled) {
                                Log.i(TAG, "Got response. Message count = " + messageCount)
                            }

                            if (messageCount < 1) {
                                return
                            }

                            val thing = listing.children.get(0).ok()

                            var title: String?
                            val text = context.getString(string.notification_message_action)

                            val messageID: RedditIdAndType
                            val messageTimestamp: TimestampUTC

                            val unknownUser = ("["
                                    + context.getString(string.general_unknown)
                                    + "]")

                            if (thing is RedditThing.Comment) {
                                val comment = thing.data

                                title = context.getString(
                                    string.notification_comment,
                                    General.nullAlternative<String>(
                                        org.quantumbadger.redreader.common.General.mapIfNotNull<UrlEncodedString?, String>(
                                            comment.author,
                                            { it?.decoded }
                                        )!!,
                                        unknownUser
                                    )
                                )

                                messageID = comment.name
                                messageTimestamp = comment.created_utc.value
                            } else if (thing is RedditThing.Message) {
                                val message = thing.data

                                title = context.getString(
                                    string.notification_message,
                                    General.nullAlternative<String>(
                                        org.quantumbadger.redreader.common.General.mapIfNotNull<UrlEncodedString?, String>(
                                            message.author,
                                            { it?.decoded }
                                        )!!,
                                        org.quantumbadger.redreader.common.General.mapIfNotNull<UrlEncodedString?, String>(
                                            message.subreddit_name_prefixed,
                                            { it?.decoded }
                                        )!!,
                                        unknownUser
                                    )
                                )

                                messageID = message.name
                                messageTimestamp = message.created_utc.value
                            } else {
                                throw RuntimeException("Unknown item in list.")
                            }

                            // Check if the previously saved message is the same as the one we
                            // just received
                            val prefs = getSharedPrefs(context)
                            val oldMessageId = prefs.getString(
                                PREFS_SAVED_MESSAGE_ID,
                                ""
                            )
                            val oldMessageTimestamp = prefs.getLong(
                                PREFS_SAVED_MESSAGE_TIMESTAMP,
                                0
                            )

                            if (oldMessageId == null || (messageID.value != oldMessageId && (oldMessageTimestamp
                                        <= messageTimestamp.toUtcSecs()))
                            ) {
                                Log.e(TAG, "New messages detected. Showing notification.")

                                prefs.edit()
                                    .putString(PREFS_SAVED_MESSAGE_ID, messageID.value)
                                    .putLong(
                                        PREFS_SAVED_MESSAGE_TIMESTAMP,
                                        messageTimestamp.toUtcSecs()
                                    )
                                    .apply()

                                if (messageCount > 1) {
                                    title = context.getString(
                                        string.notification_message_multiple
                                    )
                                }

                                createNotification(title, text, context)
                            } else {
                                Log.e(TAG, "All messages have been previously seen.")
                            }
                        } catch (e: Exception) {
                            onFailure(
                                getGeneralErrorForFailure(
                                    context,
                                    RequestFailureType.PARSE,
                                    e,
                                    null,
                                    url,
                                    FailedRequestBody.Companion.from(streamFactory)
                                )
                            )
                        }
                    }
                })

            cm.makeRequest(request)
        }

        private val sChannelCreated = AtomicBoolean(false)

        fun createNotification(
            title: String?,
            text: String?,
            context: Context
        ) {
            val nm = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

            synchronized(sChannelCreated) {
                if (!sChannelCreated.getAndSet(true)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (nm.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
                            Log.i(TAG, "Creating notification channel")

                            val channel = NotificationChannel(
                                NOTIFICATION_CHANNEL_ID,
                                context.getString(
                                    string.notification_channel_name_reddit_messages
                                ),
                                NotificationManager.IMPORTANCE_DEFAULT
                            )

                            nm.createNotificationChannel(channel)
                        } else {
                            Log.i(
                                TAG,
                                "Not creating notification channel as it already exists"
                            )
                        }
                    } else {
                        Log.i(
                            TAG,
                            "Not creating notification channel due to old Android version"
                        )
                    }
                }
            }

            val notification = NotificationCompat.Builder(
                context
            )
                .setSmallIcon(R.drawable.icon_notif)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .setColor(Color.rgb(0xd3, 0x2f, 0x2f))

            val intent = Intent(context, MainActivityCompose::class.java).apply {
                putExtra(MainActivityCompose.EXTRA_DEEP_LINK, MainActivityCompose.DEEP_LINK_INBOX)
            }

            var flags = 0

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags = flags or PendingIntent.FLAG_IMMUTABLE
            }

            notification.setContentIntent(PendingIntent.getActivity(context, 0, intent, flags))

            nm.notify(0, notification.getNotification())
        }
    }
}
