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
package org.quantumbadger.redreader.reddit.api

import android.content.Context
import org.quantumbadger.redreader.RedReader.Companion.getInstance
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequest.DownloadQueueType
import org.quantumbadger.redreader.cache.CacheRequest.RequestFailureType
import org.quantumbadger.redreader.cache.CacheRequestJSONParser
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyAlways
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.Constants.Reddit
import org.quantumbadger.redreader.common.General.getGeneralErrorForFailure
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.TimestampBound
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.common.time.TimestampUTC.Companion.now
import org.quantumbadger.redreader.http.FailedRequestBody
import org.quantumbadger.redreader.io.CacheDataSource
import org.quantumbadger.redreader.io.RequestResponseHandler
import org.quantumbadger.redreader.io.WritableHashSet
import org.quantumbadger.redreader.jsonwrap.JsonValue
import java.util.UUID

class RedditAPIMultiredditListRequester(
    private val context: Context,
    private val user: RedditAccount
) : CacheDataSource<Key?, WritableHashSet?, RRError?> {
    object Key {
        val INSTANCE: Key = Key()
    }

    override fun performRequest(
        key: Key?,
        timestampBound: TimestampBound?,
        handler: RequestResponseHandler<WritableHashSet?, RRError?>
    ) {
        if (user.isAnonymous) {
            val now = now()

            handler.onRequestSuccess(
                WritableHashSet(
                    HashSet<String?>(),
                    now,
                    user.canonicalUsername
                ),
                now
            )
        } else {
            doRequest(handler)
        }
    }

    private fun doRequest(
        handler: RequestResponseHandler<WritableHashSet?, RRError?>
    ) {
        val uri = Reddit.getUri(Reddit.PATH_MULTIREDDITS_MINE)

        val request = CacheRequest(
            uri,
            user,
            null,
            Priority(Constants.Priority.API_SUBREDDIT_LIST),
            DownloadStrategyAlways.Companion.INSTANCE,
            Constants.FileType.MULTIREDDIT_LIST,
            DownloadQueueType.REDDIT_API,
            context,
            CacheRequestJSONParser(context, object : CacheRequestJSONParser.Listener {
                override fun onJsonParsed(
                    result: JsonValue,
                    timestamp: TimestampUTC,
                    session: UUID,
                    fromCache: Boolean
                ) {
                    try {
                        val output = HashSet<String?>()

                        val multiredditList = result.asArray()

                        for (multireddit in multiredditList!!) {
                            val name = multireddit.asObject()!!
                                .getObject("data")!!
                                .getString("name")
                            output.add(name)
                        }

                        handler.onRequestSuccess(
                            WritableHashSet(
                                output,
                                timestamp,
                                user.canonicalUsername
                            ), timestamp
                        )
                    } catch (e: Exception) {
                        handler.onRequestFailed(
                            getGeneralErrorForFailure(
                                context,
                                RequestFailureType.PARSE,
                                e,
                                null,
                                uri,
                                Optional.Companion.of<FailedRequestBody>(FailedRequestBody(result))
                            )
                        )
                    }
                }

                override fun onFailure(error: RRError) {
                    handler.onRequestFailed(error)
                }
            })
        )

        CacheManager.Companion.getInstance(context).makeRequest(request)
    }

    override fun performRequest(
        keys: MutableCollection<Key?>?, timestampBound: TimestampBound?,
        handler: RequestResponseHandler<HashMap<Key?, WritableHashSet?>?, RRError?>?
    ) {
        throw UnsupportedOperationException()
    }

    override fun performWrite(value: WritableHashSet?) {
        throw UnsupportedOperationException()
    }

    override fun performWrite(values: MutableCollection<WritableHashSet?>?) {
        throw UnsupportedOperationException()
    }
}
