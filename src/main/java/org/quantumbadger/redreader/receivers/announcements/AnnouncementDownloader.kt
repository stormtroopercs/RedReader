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
package org.quantumbadger.redreader.receivers.announcements

import android.content.Context
import android.util.Log
import org.quantumbadger.redreader.RedReader.Companion.getInstance
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequest.DownloadQueueType
import org.quantumbadger.redreader.cache.CacheRequestJSONParser
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyAlways
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.Constants.Reddit
import org.quantumbadger.redreader.common.General.getSharedPrefs
import org.quantumbadger.redreader.common.HexUtils
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.SharedPrefsWrapper
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.jsonwrap.JsonValue
import java.io.IOException
import java.util.UUID

object AnnouncementDownloader {
    private const val TAG = "AnnouncementDownloader"

    @Suppress("PropertyName")
    private val PUBLIC_KEY = ("3059301306072A8648CE3D020106082A8648CE3D0301070342000"
            + "4F74D436746282E6080F0EE9FB80DCDCA06667F701A0266F2F14C15C204B6E48414444BD9D0C1170E6B0"
            + "C257B3DE1AE23F4BA965D8CEB055A3C374DA927415C5D")

    const val PREF_KEY_PAYLOAD_STORAGE_HEX: String = "AnnouncementDownloaderPayload"
    const val PREF_KEY_LAST_READ_ID: String = "AnnouncementDownloaderLastReadId"

    fun performDownload(context: Context) {
        val announcementsEnabled = PrefsUtility.pref_menus_mainmenu_dev_announcements()

        if (!announcementsEnabled) {
            return
        }

        CacheManager.Companion.getInstance(context).makeRequest(
            CacheRequest(
                Reddit.getUri("/r/rr_announcements/new.json?limit=1"),
                RedditAccountManager.Companion.getAnon(),
                null,
                Priority(Constants.Priority.DEV_ANNOUNCEMENTS),
                DownloadStrategyAlways.Companion.INSTANCE,
                Constants.FileType.POST_LIST,
                DownloadQueueType.REDDIT_API,
                false,
                context,
                CacheRequestJSONParser(context, object : CacheRequestJSONParser.Listener {
                    override fun onJsonParsed(
                        result: JsonValue,
                        timestamp: TimestampUTC?,
                        session: UUID,
                        fromCache: Boolean
                    ) {
                        onJsonRetrieved(context, result)
                    }

                    override fun onFailure(error: RRError) {
                        Log.e(
                            TAG,
                            "Error downloading announcements: " + error,
                            error.t
                        )
                    }
                })
            )
        )
    }

    private fun onJsonRetrieved(
        context: Context,
        root: JsonValue
    ) {
        try {
            val selfText = root.getStringAtPath(
                "data",
                "children",
                0,
                "data",
                "selftext"
            )

            if (selfText.isEmpty()) {
                throw IOException("Couldn't find self text in response")
            }

            // This verifies the signature
            val payloadData = SignedDataSerializer.deserialize(
                SignatureHandler.stringToPublicKey(PUBLIC_KEY),
                selfText.get()
            )

            getSharedPrefs(context).edit()
                .putString(PREF_KEY_PAYLOAD_STORAGE_HEX, HexUtils.toHex(payloadData))
                .apply()

            Log.i(TAG, "Announcement stored in shared prefs")
        } catch (t: Throwable) {
            Log.e(TAG, "Error parsing announcements", t)
        }
    }

    fun getMostRecentUnreadAnnouncement(
        prefs: SharedPrefsWrapper
    ): Optional<Announcement?> {
        try {
            val hex = prefs.getString(PREF_KEY_PAYLOAD_STORAGE_HEX, "")

            if (hex == null || hex.isEmpty()) {
                Log.i(TAG, "No announcement found in shared prefs")
                return Optional.Companion.empty<Announcement?>()
            }

            val announcement: Announcement=                Announcement.Companion.fromPayload(Payload.Companion.fromBytes(HexUtils.fromHex(hex)))

            if (announcement.isExpired()) {
                Log.i(TAG, "Announcement is expired: " + announcement.id)
                return Optional.Companion.empty<Announcement?>()
            }

            val lastReadId = prefs.getString(PREF_KEY_LAST_READ_ID, "")

            if (announcement.id == lastReadId) {
                Log.i(TAG, "Announcement is already read: " + announcement.id)
                return Optional.Companion.empty<Announcement?>()
            }

            Log.i(TAG, "Got unread announcement: " + announcement.id)

            return Optional.Companion.of<Announcement?>(announcement)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to parse stored announcement", t)
            return Optional.Companion.empty<Announcement?>()
        }
    }

    fun markAsRead(
        context: Context,
        announcement: Announcement
    ) {
        getSharedPrefs(context).edit()
            .putString(PREF_KEY_LAST_READ_ID, announcement.id)
            .apply()

        Log.i(TAG, "Marked announcement as read: " + announcement.id)
    }
}
