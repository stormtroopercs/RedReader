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
package org.quantumbadger.redreader.reddit.prepared

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.text.style.ImageSpan
import android.util.Log
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequest.DownloadQueueType
import org.quantumbadger.redreader.cache.CacheRequestCallbacks
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import org.quantumbadger.redreader.common.AndroidCommon.runOnUiThread
import org.quantumbadger.redreader.common.BetterSSB
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.General.mapIfNotNull
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.reddit.kthings.MaybeParseError
import org.quantumbadger.redreader.reddit.kthings.RedditComment
import org.quantumbadger.redreader.reddit.kthings.RedditComment.FlairEmoteData
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import org.quantumbadger.redreader.reddit.kthings.UrlEncodedString
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElement
import org.quantumbadger.redreader.reddit.prepared.html.HtmlReader
import org.quantumbadger.redreader.reddit.things.RedditThingWithIdAndType
import java.io.IOException
import java.util.UUID
import org.quantumbadger.redreader.common.General

class RedditParsedComment(
    val rawComment: RedditComment,
    activity: AppCompatActivity
) : RedditThingWithIdAndType {
    val body: BodyElement

    val flair: BetterSSB?

    init {
        this.body = HtmlReader.Companion.parse(
            rawComment.body_html!!.decoded,  // TODO nullable?
            activity
        )

        val flair = mapIfNotNull<UrlEncodedString?, String>(
            rawComment.author_flair_text,
            { it?.decoded }
        )

        if (flair != null) {
            this.flair = BetterSSB()
            this.flair!!.append(flair)

            if (rawComment.author_flair_richtext != null) {
                getFlairEmotes(rawComment.author_flair_richtext, activity)
            }
        } else {
            this.flair = null
        }
    }

    override val idAlone: String get() = rawComment.idAlone

    override val idAndType: RedditIdAndType get() = rawComment.idAndType

    private fun getFlairEmotes(
        flairRichtext: List<MaybeParseError<FlairEmoteData>>,
        activity: AppCompatActivity
    ) {
        val alignment: Int

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            alignment = ImageSpan.ALIGN_CENTER
        } else {
            alignment = ImageSpan.ALIGN_BASELINE
        }

        for (flairEmoteData in flairRichtext) {
            if (flairEmoteData !is MaybeParseError.Ok<*>) {
                continue
            }

            val flairEmoteObject =                 (flairEmoteData as MaybeParseError.Ok<FlairEmoteData>)
                    .value

            val objectType = flairEmoteObject.e

            if (objectType == "emoji") {
                val placeholder = flairEmoteObject.a
                val url = flairEmoteObject.u

                CacheManager.Companion.getInstance(activity).makeRequest(
                    CacheRequest(
                        UriString(url),
                        RedditAccountManager.Companion.getAnon(),
                        null,
                        Priority(Constants.Priority.API_COMMENT_LIST),
                        DownloadStrategyIfNotCached.Companion.INSTANCE,
                        Constants.FileType.IMAGE,
                        DownloadQueueType.IMMEDIATE,
                        activity,
                        object : CacheRequestCallbacks {
                            var image: Bitmap?=null

                            override fun onDataStreamComplete(
                                stream: GenericFactory<SeekableInputStream, IOException>,
                                timestamp: TimestampUTC,
                                session: UUID,
                                fromCache: Boolean,
                                mimetype: String?
                            ) {
                                try {
                                    stream.create().use { `is` ->
                                        image = BitmapFactory.decodeStream(`is`)
                                        if (image == null) {
                                            throw IOException("Failed to decode bitmap")
                                        }

                                        val textSize = 11
                                        val maxImageHeightMultiple = 1.0f

                                        val maxHeight = TypedValue.applyDimension(
                                            TypedValue.COMPLEX_UNIT_SP,
                                            (PrefsUtility.appearance_fontscale_comment_headers()
                                                    * textSize
                                                    * maxImageHeightMultiple),
                                            activity.getApplicationContext()
                                                .getResources()
                                                .getDisplayMetrics()
                                        )

                                        if (image!!.getHeight() > maxHeight) {
                                            val imageAspectRatio =                                                 image!!.getHeight().toFloat() / image!!.getWidth()

                                            val newImageWidth = maxHeight / imageAspectRatio

                                            image = Bitmap.createScaledBitmap(
                                                image!!,
                                                Math.round(newImageWidth),
                                                Math.round(maxHeight),
                                                true
                                            )
                                        }

                                        if (image == null) {
                                            throw IOException("Failed to decode bitmap")
                                        }

                                        val span = ImageSpan(
                                            activity.getApplicationContext(),
                                            image!!,
                                            alignment
                                        )
                                        runOnUiThread(Runnable {
                                            if (this@RedditParsedComment.flair != null) {
                                                this@RedditParsedComment.flair!!.replace(placeholder, span)
                                            }
                                        })
                                    }
                                } catch (t: Throwable) {
                                    onFailure(
                                        RRError(
                                            "Exception while downloading emote",
                                            null,
                                            true,
                                            t
                                        )
                                    )
                                }
                            }

                            override fun onFailure(error: RRError) {
                                Log.e(
                                    "RedditParsedComment",
                                    "Failed to download emote: " + error.message,
                                    error.t
                                )
                            }
                        }
                    ))
            }
        }
    }
}
