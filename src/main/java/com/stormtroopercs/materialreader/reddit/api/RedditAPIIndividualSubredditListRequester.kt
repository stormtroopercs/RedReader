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
import android.net.Uri
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
import com.stormtroopercs.materialreader.common.UnexpectedInternalStateException
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.common.UriString.Companion.from
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.common.time.TimestampUTC.Companion.now
import com.stormtroopercs.materialreader.http.FailedRequestBody
import com.stormtroopercs.materialreader.io.CacheDataSource
import com.stormtroopercs.materialreader.io.RequestResponseHandler
import com.stormtroopercs.materialreader.io.WritableHashSet
import com.stormtroopercs.materialreader.jsonwrap.JsonValue
import com.stormtroopercs.materialreader.reddit.RedditSubredditManager
import com.stormtroopercs.materialreader.reddit.RedditSubredditManager.SubredditListType
import com.stormtroopercs.materialreader.reddit.things.InvalidSubredditNameException
import com.stormtroopercs.materialreader.reddit.things.RedditSubreddit
import com.stormtroopercs.materialreader.reddit.things.RedditThing
import java.util.UUID
import com.stormtroopercs.materialreader.common.General

class RedditAPIIndividualSubredditListRequester(
    private val context: Context,
    private val user: RedditAccount
) : CacheDataSource<SubredditListType, WritableHashSet, RRError> {
    override fun performRequest(
        type: SubredditListType,
        timestampBound: TimestampBound?,
        handler: RequestResponseHandler<WritableHashSet, RRError>
    ) {
        if (type == SubredditListType.DEFAULTS) {
            val now = now()

            val data =                 HashSet<String>(Reddit.DEFAULT_SUBREDDITS.size + 1)

            for (id in Reddit.DEFAULT_SUBREDDITS) {
                data.add(id.toString())
            }

            data.add("/r/redreader")

            val result = WritableHashSet(data, now, "DEFAULTS")
            handler.onRequestSuccess(result, now)

            return
        }

        if (type == SubredditListType.MOST_POPULAR) {
            doSubredditListRequest(
                SubredditListType.MOST_POPULAR,
                handler,
                null
            )
        } else if (user.isAnonymous) {
            when (type) {
                SubredditListType.SUBSCRIBED -> {
                    performRequest(
                        SubredditListType.DEFAULTS,
                        timestampBound,
                        handler
                    )
                    return
                }

                SubredditListType.MODERATED -> {
                    val curTime = now()
                    handler.onRequestSuccess(
                        WritableHashSet(
                            HashSet<String>(),
                            curTime,
                            SubredditListType.MODERATED.name
                        ),
                        curTime
                    )
                    return
                }

                SubredditListType.MULTIREDDITS -> {
                    val curTime = now()
                    handler.onRequestSuccess(
                        WritableHashSet(
                            HashSet<String>(),
                            curTime,
                            SubredditListType.MULTIREDDITS.name
                        ),
                        curTime
                    )
                    return
                }

                else -> throw RuntimeException(
                    ("Internal error: unknown subreddit list type '"
                            + type.name
                            + "'")
                )
            }
        } else {
            doSubredditListRequest(type, handler, null)
        }
    }

    private fun doSubredditListRequest(
        type: SubredditListType,
        handler: RequestResponseHandler<WritableHashSet, RRError>,
        after: String?
    ) {
        val uri: UriString

        run {
            val baseUri: UriString
            when (type) {
                SubredditListType.SUBSCRIBED -> baseUri = Reddit.getUri(
                    Reddit.PATH_SUBREDDITS_MINE_SUBSCRIBER
                )

                SubredditListType.MODERATED -> baseUri = Reddit.getUri(
                    Reddit.PATH_SUBREDDITS_MINE_MODERATOR
                )

                SubredditListType.MOST_POPULAR -> baseUri = Reddit.getUri(
                    Reddit.PATH_SUBREDDITS_POPULAR
                )

                else -> throw UnexpectedInternalStateException(type.name)
            }
            if (after == null) {
                uri = baseUri
            } else {
                val builder = Uri.parse(baseUri.toString()).buildUpon()
                builder.appendQueryParameter("after", after)
                uri = from(builder)
            }
        }

        val aboutSubredditCacheRequest = CacheRequest(
            uri,
            user,
            null,
            Priority(Constants.Priority.API_SUBREDDIT_INVIDIVUAL),
            DownloadStrategyAlways.Companion.INSTANCE,
            Constants.FileType.SUBREDDIT_LIST,
            DownloadQueueType.REDDIT_API,
            context,
            CacheRequestJSONParser(context, object : CacheRequestJSONParser.Listener {
                override fun onJsonParsed(
                    result: JsonValue,
                    timestamp: TimestampUTC,
                    session: UUID, fromCache: Boolean
                ) {
                    try {
                        val output = HashSet<String>()
                        val toWrite = ArrayList<RedditSubreddit>()

                        val redditListing =                             result.asObject()!!.getObject("data")

                        val subreddits =                             redditListing!!.getArray("children")

                        if (type == SubredditListType.SUBSCRIBED && subreddits!!.size() == 0 && after == null) {
                            performRequest(
                                SubredditListType.DEFAULTS,
                                TimestampBound.Companion.ANY,
                                handler
                            )
                            return
                        }

                        for (v in subreddits!!) {
                            val thing = v!!.asObject<RedditThing>(RedditThing::class.java)
                            val subreddit = thing!!.asSubreddit()

                            subreddit.downloadTime = timestamp!!.toUtcMs()

                            try {
                                output.add(subreddit.canonicalId.toString())
                                toWrite.add(subreddit)
                            } catch (e: InvalidSubredditNameException) {
                                Log.e(
                                    "SubredditListRequester",
                                    "Ignoring invalid subreddit",
                                    e
                                )
                            }
                        }

                        RedditSubredditManager.Companion.getInstance(context, user)
                            .offerRawSubredditData(toWrite, timestamp)
                        val receivedAfter = redditListing.getString("after")
                        if (receivedAfter != null && type !=
                            SubredditListType.MOST_POPULAR
                        ) {
                            doSubredditListRequest(
                                type,
                                object : RequestResponseHandler<WritableHashSet, RRError> {
                                    override fun onRequestFailed(
                                        failureReason: RRError
                                    ) {
                                        handler.onRequestFailed(failureReason)
                                    }

                                    override fun onRequestSuccess(
                                        result: WritableHashSet,
                                        timeCached: TimestampUTC?
                                    ) {
                                        output.addAll(result.toHashset())
                                        handler.onRequestSuccess(
                                            WritableHashSet(
                                                output,
                                                timeCached!!,
                                                type.name
                                            ), timeCached
                                        )

                                        if (after == null) {
                                            Log.i(
                                                "SubredditListRequester", ("Got "
                                                        + output.size
                                                        + " subreddits in multiple requests")
                                            )
                                        }
                                    }
                                },
                                receivedAfter
                            )
                        } else {
                            handler.onRequestSuccess(
                                WritableHashSet(
                                    output,
                                    timestamp!!,
                                    type.name
                                ), timestamp
                            )

                            if (after == null) {
                                Log.i(
                                    "SubredditListRequester", ("Got "
                                            + output.size + " subreddits in 1 request")
                                )
                            }
                        }
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

        CacheManager.Companion.getInstance(context).makeRequest(aboutSubredditCacheRequest)
    }

    override fun performRequest(
        keys: MutableCollection<SubredditListType>,
        timestampBound: TimestampBound?,
        handler: RequestResponseHandler<HashMap<SubredditListType, WritableHashSet>, RRError>
    ) {
        // TODO batch API? or just make lots of requests and build up a hash map?
        throw UnsupportedOperationException()
    }

    override fun performWrite(value: WritableHashSet) {
        throw UnsupportedOperationException()
    }

    override fun performWrite(values: MutableCollection<WritableHashSet>) {
        throw UnsupportedOperationException()
    }
}
