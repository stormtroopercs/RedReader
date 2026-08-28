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
import android.util.Log
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
import com.stormtroopercs.materialreader.http.FailedRequestBody
import com.stormtroopercs.materialreader.io.CacheDataSource
import com.stormtroopercs.materialreader.io.RequestResponseHandler
import com.stormtroopercs.materialreader.jsonwrap.JsonValue
import com.stormtroopercs.materialreader.reddit.RedditSubredditHistory
import com.stormtroopercs.materialreader.reddit.things.InvalidSubredditNameException
import com.stormtroopercs.materialreader.reddit.things.RedditSubreddit
import com.stormtroopercs.materialreader.reddit.things.RedditThing
import com.stormtroopercs.materialreader.reddit.things.SubredditCanonicalId
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import com.stormtroopercs.materialreader.common.General

class RedditAPIIndividualSubredditDataRequester(
    private val context: Context,
    private val user: RedditAccount
) : CacheDataSource<SubredditCanonicalId, RedditSubreddit, RRError> {
    override fun performRequest(
        subredditCanonicalId: SubredditCanonicalId,
        timestampBound: TimestampBound?,
        handler: RequestResponseHandler<RedditSubreddit, RRError>
    ) {
        val url = Reddit.getUri(subredditCanonicalId.toString() + "/about.json")

        val aboutSubredditCacheRequest = CacheRequest(
            url,
            user,
            null,
            Priority(Constants.Priority.API_SUBREDDIT_INVIDIVUAL),
            DownloadStrategyAlways.Companion.INSTANCE,
            Constants.FileType.SUBREDDIT_ABOUT,
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
                        val subredditThing = result.asObject<RedditThing>(RedditThing::class.java)
                        val subreddit = subredditThing!!.asSubreddit()
                        subreddit.downloadTime = timestamp!!.toUtcMs()
                        handler.onRequestSuccess(subreddit, timestamp)

                        RedditSubredditHistory.addSubreddit(user, subredditCanonicalId)
                    } catch (e: Exception) {
                        handler.onRequestFailed(
                            getGeneralErrorForFailure(
                                context,
                                RequestFailureType.PARSE,
                                e,
                                null,
                                url,
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

        CacheManager.Companion.getInstance(context).makeRequest(aboutSubredditCacheRequest)
    }

    override fun performRequest(
        subredditCanonicalIds: MutableCollection<SubredditCanonicalId>,
        timestampBound: TimestampBound?,
        handler: RequestResponseHandler<HashMap<SubredditCanonicalId, RedditSubreddit>, RRError>
    ) {
        // TODO if there's a bulk API to do this, that would be good... :)

        val result = HashMap<SubredditCanonicalId, RedditSubreddit>()
        val stillOkay = AtomicBoolean(true)
        val requestsToGo = AtomicInteger(subredditCanonicalIds.size)
        val oldestResult = AtomicReference<TimestampUTC?>(null)

        val innerHandler: RequestResponseHandler<RedditSubreddit, RRError> =
            object : RequestResponseHandler<RedditSubreddit, RRError> {
                override fun onRequestFailed(failureReason : RRError) {
                    synchronized(result) {
                        if (stillOkay.get()) {
                            stillOkay.set(false)
                            handler.onRequestFailed(failureReason)
                        }
                    }
                }

                override fun onRequestSuccess(
                    innerResult: RedditSubreddit,
                    timeCached: TimestampUTC?
                ) {
                    synchronized(result) {
                        if (stillOkay.get()) {
                            try {
                                val canonicalId = innerResult.canonicalId

                                result.put(canonicalId, innerResult)

                                synchronized(oldestResult) {
                                    if (oldestResult.get() == null) {
                                        oldestResult.set(timeCached)
                                    } else {
                                        oldestResult.set(
                                            TimestampUTC.oldest(
                                                oldestResult.get()!!,
                                                timeCached!!
                                            )
                                        )
                                    }
                                }

                                RedditSubredditHistory.addSubreddit(user, canonicalId)
                            } catch (e: InvalidSubredditNameException) {
                                Log.e(TAG, "Invalid subreddit name " + innerResult.name, e)
                            }

                            if (requestsToGo.decrementAndGet() == 0) {
                                handler.onRequestSuccess(result, oldestResult.get())
                            }
                        }
                    }
                }
            }

        for (subredditCanonicalId in subredditCanonicalIds) {
            performRequest(subredditCanonicalId, timestampBound, innerHandler)
        }
    }

    override fun performWrite(value: RedditSubreddit) {
        throw UnsupportedOperationException()
    }

    override fun performWrite(values: MutableCollection<RedditSubreddit>) {
        throw UnsupportedOperationException()
    }

    companion object {
        private const val TAG = "IndividualSRDataReq"
    }
}
