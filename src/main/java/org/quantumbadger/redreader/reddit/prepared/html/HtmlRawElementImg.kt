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
package org.quantumbadger.redreader.reddit.prepared.html

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import org.quantumbadger.redreader.RedReader.Companion.getInstance
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequest.DownloadQueueType
import org.quantumbadger.redreader.cache.CacheRequest.RequestFailureType
import org.quantumbadger.redreader.cache.CacheRequestCallbacks
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.General.getGeneralErrorForFailure
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.http.FailedRequestBody
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElement
import org.quantumbadger.redreader.reddit.prepared.bodytext.DynamicSpanned
import java.io.IOException
import java.util.UUID

class HtmlRawElementImg(
    private val mChildren: ArrayList<HtmlRawElement>,
    private val mTitle: String,
    private val mSrc: UriString
) : HtmlRawElement() {
    override fun getPlainText(stringBuilder: StringBuilder) {
        for (element in mChildren) {
            element.getPlainText(stringBuilder)
        }
    }

    @Synchronized
    fun writeTo(
        ssb: SpannableStringBuilder,
        activity: AppCompatActivity,
        dynamicSpanned: DynamicSpanned
    ) {
        val emoteLocationStart = ssb.length

        ssb.append(mTitle)

        CacheManager.Companion.getInstance(activity).makeRequest(
            CacheRequest(
                mSrc,
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
                        stream: GenericFactory<SeekableInputStream, IOException?>,
                        timestamp: TimestampUTC?,
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

                                val textSize = 18
                                val maxImageHeightMultiple = 2.0f

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
                                    val imageAspectRatio =                                         image!!.getHeight().toFloat() / image!!.getWidth()

                                    val newImageWidth = maxHeight / imageAspectRatio

                                    image = Bitmap.createScaledBitmap(
                                        image!!,
                                        Math.round(newImageWidth),
                                        Math.round(maxHeight),
                                        true
                                    )
                                }

                                val span = ImageSpan(
                                    activity.getApplicationContext(),
                                    image!!
                                )
                                dynamicSpanned.addSpanDynamic(
                                    span,
                                    emoteLocationStart,
                                    emoteLocationStart + mTitle.length,
                                    Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                                )
                            }
                        } catch (t: Throwable) {
                            onFailure(
                                getGeneralErrorForFailure(
                                    activity,
                                    RequestFailureType.CONNECTION,
                                    t,
                                    null,
                                    mSrc,
                                    Optional.Companion.empty<FailedRequestBody>()
                                )
                            )
                        }
                    }

                    override fun onFailure(error: RRError) {
                    }
                }
            ))
    }

    override fun reduce(
        activeAttributes: HtmlTextAttributes,
        activity: AppCompatActivity,
        destination: ArrayList<HtmlRawElement?>,
        linkButtons: ArrayList<LinkButtonDetails?>
    ) {
        destination.add(this)
    }

    override fun generate(
        activity: AppCompatActivity,
        destination: ArrayList<BodyElement?>
    ) {
        throw RuntimeException(
            "Attempt to call generate() on inline image: should be inside a block"
        )
    }
}
