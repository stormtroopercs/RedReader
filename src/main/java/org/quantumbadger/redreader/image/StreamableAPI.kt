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
package org.quantumbadger.redreader.image

import android.content.Context
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequest.DownloadQueueType
import org.quantumbadger.redreader.cache.CacheRequest.RequestFailureType
import org.quantumbadger.redreader.cache.CacheRequestJSONParser
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.General.getGeneralErrorForFailure
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.http.FailedRequestBody
import org.quantumbadger.redreader.jsonwrap.JsonValue
import java.util.UUID
import org.quantumbadger.redreader.common.General

object StreamableAPI {
    fun getImageInfo(
        context: Context,
        imageId: String?,
        priority: Priority,
        listener: GetImageInfoListener
    ) {
        val apiUrl = UriString("https://api.streamable.com/videos/" + imageId)

        CacheManager.Companion.getInstance(context).makeRequest(
            CacheRequest(
                apiUrl,
                RedditAccountManager.Companion.getAnon(),
                null,
                priority,
                DownloadStrategyIfNotCached.Companion.INSTANCE,
                Constants.FileType.IMAGE_INFO,
                DownloadQueueType.IMMEDIATE,
                context,
                CacheRequestJSONParser(context, object : CacheRequestJSONParser.Listener {
                    override fun onJsonParsed(
                        result: JsonValue,
                        timestamp: TimestampUTC,
                        session: UUID,
                        fromCache: Boolean
                    ) {
                        try {
                            val outer = result.asObject()
                            listener.onSuccess(ImageInfo.parseStreamable(outer!!))
                        } catch (t: Throwable) {
                            listener.onFailure(
                                getGeneralErrorForFailure(
                                    context,
                                    RequestFailureType.PARSE,
                                    t,
                                    null,
                                    apiUrl,
                                    Optional.Companion.of<FailedRequestBody>(
                                        FailedRequestBody(
                                            result
                                        )
                                    )
                                )
                            )
                        }
                    }

                    override fun onFailure(error: RRError) {
                        listener.onFailure(error)
                    }
                })
            )
        )
    }
}
