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
package com.stormtroopercs.materialreader.reddit.api

import android.content.Context
import com.stormtroopercs.materialreader.account.RedditAccount
import com.stormtroopercs.materialreader.cache.CacheManager
import com.stormtroopercs.materialreader.cache.CacheRequest
import com.stormtroopercs.materialreader.cache.CacheRequest.DownloadQueueType
import com.stormtroopercs.materialreader.cache.CacheRequest.RequestFailureType
import com.stormtroopercs.materialreader.cache.CacheRequestJSONParser
import com.stormtroopercs.materialreader.cache.downloadstrategy.DownloadStrategyAlways
import com.stormtroopercs.materialreader.common.Constants
import com.stormtroopercs.materialreader.common.Constants.Reddit
import com.stormtroopercs.materialreader.common.General.getGeneralErrorForFailure
import com.stormtroopercs.materialreader.common.Optional
import com.stormtroopercs.materialreader.common.Priority
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.TimestampBound
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.common.time.TimestampUTC.Companion.now
import com.stormtroopercs.materialreader.http.FailedRequestBody
import com.stormtroopercs.materialreader.io.CacheDataSource
import com.stormtroopercs.materialreader.io.RequestResponseHandler
import com.stormtroopercs.materialreader.io.WritableHashSet
import com.stormtroopercs.materialreader.jsonwrap.JsonValue
import java.util.UUID
import com.stormtroopercs.materialreader.common.General

class RedditAPIMultiredditListRequester(
    private val context: Context,
    private val user: RedditAccount
) : CacheDataSource<RedditAPIMultiredditListRequester.Key, WritableHashSet, RRError> {
    class Key {
        companion object {
            val INSTANCE: Key = Key()
        }
    }

    override fun performRequest(
        key: Key,
        timestampBound: TimestampBound?,
        handler: RequestResponseHandler<WritableHashSet, RRError>
    ) {
        if (user.isAnonymous) {
            val now = now()

            handler.onRequestSuccess(
                WritableHashSet(
                    HashSet<String>(),
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
        handler: RequestResponseHandler<WritableHashSet, RRError>
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
                        val output = HashSet<String>()

                        val multiredditList = result.asArray()

                        for (multireddit in multiredditList!!) {
                            val name = multireddit.asObject()!!
                                .getObject("data")!!
                                .getString("name")
                            output.add(name!!)
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
        keys: MutableCollection<Key>, timestampBound: TimestampBound?,
        handler: RequestResponseHandler<HashMap<Key, WritableHashSet>, RRError>
    ) {
        throw UnsupportedOperationException()
    }

    override fun performWrite(value: WritableHashSet) {
        throw UnsupportedOperationException()
    }

    override fun performWrite(values: MutableCollection<WritableHashSet>) {
        throw UnsupportedOperationException()
    }
}
