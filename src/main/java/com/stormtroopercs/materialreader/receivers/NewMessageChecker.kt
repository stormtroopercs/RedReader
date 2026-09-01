/*******************************************************************************
 * This file is part of MaterialReader.
 *
 * MaterialReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MaterialReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MaterialReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.stormtroopercs.materialreader.receivers

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
import com.stormtroopercs.materialreader.R
import com.stormtroopercs.materialreader.R.string
import com.stormtroopercs.materialreader.account.RedditAccount
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.activities.MainActivityCompose
import com.stormtroopercs.materialreader.cache.CacheManager
import com.stormtroopercs.materialreader.cache.CacheRequest
import com.stormtroopercs.materialreader.cache.CacheRequest.DownloadQueueType
import com.stormtroopercs.materialreader.cache.CacheRequest.RequestFailureType
import com.stormtroopercs.materialreader.cache.CacheRequestCallbacks
import com.stormtroopercs.materialreader.cache.downloadstrategy.DownloadStrategyAlways
import com.stormtroopercs.materialreader.common.Constants
import com.stormtroopercs.materialreader.common.Constants.Reddit
import com.stormtroopercs.materialreader.common.General
import com.stormtroopercs.materialreader.common.General.getGeneralErrorForFailure
import com.stormtroopercs.materialreader.common.General.getSharedPrefs
import com.stormtroopercs.materialreader.common.General.isSensitiveDebugLoggingEnabled
import com.stormtroopercs.materialreader.common.GenericFactory
import com.stormtroopercs.materialreader.common.PrefsUtility
import com.stormtroopercs.materialreader.common.Priority
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.UriString.Companion.from
import com.stormtroopercs.materialreader.common.datastream.SeekableInputStream
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.http.FailedRequestBody
import com.stormtroopercs.materialreader.receivers.announcements.AnnouncementDownloader
import com.stormtroopercs.materialreader.reddit.kthings.JsonUtils.decodeRedditThingFromStream
import com.stormtroopercs.materialreader.reddit.kthings.RedditIdAndType
import com.stormtroopercs.materialreader.reddit.kthings.RedditThing
import com.stormtroopercs.materialreader.reddit.kthings.RedditThing.Listing
import com.stormtroopercs.materialreader.reddit.kthings.UrlEncodedString
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

			Log.i("MaterialReader", "Checking for new messages.")

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
						mimetype: String?,
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

							val unknownUser = (
								"[" +
									context.getString(string.general_unknown) +
									"]"
								)

							if (thing is RedditThing.Comment) {
								val comment = thing.data

								title = context.getString(
									string.notification_comment,
									General.nullAlternative<String>(
										com.stormtroopercs.materialreader.common.General.mapIfNotNull<UrlEncodedString?, String>(
											comment.author,
											{ it?.decoded },
										)!!,
										unknownUser,
									),
								)

								messageID = comment.name
								messageTimestamp = comment.created_utc.value
							} else if (thing is RedditThing.Message) {
								val message = thing.data

								title = context.getString(
									string.notification_message,
									General.nullAlternative<String>(
										com.stormtroopercs.materialreader.common.General.mapIfNotNull<UrlEncodedString?, String>(
											message.author,
											{ it?.decoded },
										)!!,
										com.stormtroopercs.materialreader.common.General.mapIfNotNull<UrlEncodedString?, String>(
											message.subreddit_name_prefixed,
											{ it?.decoded },
										)!!,
										unknownUser,
									),
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
								"",
							)
							val oldMessageTimestamp = prefs.getLong(
								PREFS_SAVED_MESSAGE_TIMESTAMP,
								0,
							)

							if (oldMessageId == null ||
								(
									messageID.value != oldMessageId &&
										(
											oldMessageTimestamp
												<= messageTimestamp.toUtcSecs()
											)
									)
							) {
								Log.e(TAG, "New messages detected. Showing notification.")

								prefs.edit()
									.putString(PREFS_SAVED_MESSAGE_ID, messageID.value)
									.putLong(
										PREFS_SAVED_MESSAGE_TIMESTAMP,
										messageTimestamp.toUtcSecs(),
									)
									.apply()

								if (messageCount > 1) {
									title = context.getString(
										string.notification_message_multiple,
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
									FailedRequestBody.Companion.from(streamFactory),
								),
							)
						}
					}
				},
			)

			cm.makeRequest(request)
		}

		private val sChannelCreated = AtomicBoolean(false)

		fun createNotification(
			title: String?,
			text: String?,
			context: Context,
		) {
			val nm = context.getSystemService(
				Context.NOTIFICATION_SERVICE,
			) as NotificationManager

			synchronized(sChannelCreated) {
				if (!sChannelCreated.getAndSet(true)) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						if (nm.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
							Log.i(TAG, "Creating notification channel")

							val channel = NotificationChannel(
								NOTIFICATION_CHANNEL_ID,
								context.getString(
									string.notification_channel_name_reddit_messages,
								),
								NotificationManager.IMPORTANCE_DEFAULT,
							)

							nm.createNotificationChannel(channel)
						} else {
							Log.i(
								TAG,
								"Not creating notification channel as it already exists",
							)
						}
					} else {
						Log.i(
							TAG,
							"Not creating notification channel due to old Android version",
						)
					}
				}
			}

			val notification = NotificationCompat.Builder(
				context,
				NOTIFICATION_CHANNEL_ID,
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

			nm.notify(0, notification.build())
		}
	}
}
