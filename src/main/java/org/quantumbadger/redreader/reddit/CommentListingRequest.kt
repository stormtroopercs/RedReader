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
package org.quantumbadger.redreader.reddit

import android.content.Context
import android.util.Log
import androidx.annotation.UiThread
import org.apache.commons.text.StringEscapeUtils
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.activities.SessionChangeListener
import org.quantumbadger.redreader.activities.SessionChangeListener.SessionChangeType
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequest.DownloadQueueType
import org.quantumbadger.redreader.cache.CacheRequest.RequestFailureType
import org.quantumbadger.redreader.cache.CacheRequestCallbacks
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategy
import org.quantumbadger.redreader.common.AndroidCommon.runOnUiThread
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.General.getGeneralErrorForFailure
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.UriString.Companion.from
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.fragments.CommentListingFragment
import org.quantumbadger.redreader.http.FailedRequestBody
import org.quantumbadger.redreader.reddit.api.RedditPostActions.ActionDescriptionPair.Companion.from
import org.quantumbadger.redreader.reddit.kthings.JsonUtils.decodeRedditThingResponseFromStream
import org.quantumbadger.redreader.reddit.kthings.MaybeParseError
import org.quantumbadger.redreader.reddit.kthings.RedditFieldReplies.Some
import org.quantumbadger.redreader.reddit.kthings.RedditListing
import org.quantumbadger.redreader.reddit.kthings.RedditMediaMetadata
import org.quantumbadger.redreader.reddit.kthings.RedditThing
import org.quantumbadger.redreader.reddit.kthings.RedditThing.Listing
import org.quantumbadger.redreader.reddit.kthings.RedditThing.More
import org.quantumbadger.redreader.reddit.kthings.RedditThing.Post
import org.quantumbadger.redreader.reddit.kthings.RedditThingResponse
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager
import org.quantumbadger.redreader.reddit.prepared.RedditParsedComment
import org.quantumbadger.redreader.reddit.prepared.RedditParsedPost
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost
import org.quantumbadger.redreader.reddit.prepared.RedditRenderableComment
import org.quantumbadger.redreader.reddit.url.RedditURLParser
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL
import java.io.IOException
import java.util.Locale
import java.util.UUID
import org.quantumbadger.redreader.common.General

class CommentListingRequest(
    private val mContext: Context,
    private val mFragment: CommentListingFragment?,
    private val mActivity: BaseActivity,
    private val mCommentListingURL: RedditURL?,
    private val mParsePostSelfText: Boolean,
    private val mUrl: RedditURL,
    private val mUser: RedditAccount,
    private val mSession: UUID?,
    private val mDownloadStrategy: DownloadStrategy,
    private val mListener: Listener
) {
    private val mCacheManager: CacheManager

    init {
        mCacheManager = CacheManager.Companion.getInstance(mContext)

        mCacheManager.makeRequest(createCommentListingCacheRequest())
    }

    @UiThread
    interface Listener {
        fun onCommentListingRequestDownloadNecessary()

        fun onCommentListingRequestFailure(error: RRError?)

        fun onCommentListingRequestCachedCopy(timestamp: TimestampUTC?)

        fun onCommentListingRequestParseStart()

        fun onCommentListingRequestPostDownloaded(post: RedditPreparedPost?)

        fun onCommentListingRequestAllItemsDownloaded(items: ArrayList<RedditCommentListItem>?)
    }

    private fun onThingDownloaded(
        thingResponse: RedditThingResponse,
        session: UUID,
        timestamp: TimestampUTC?,
        fromCache: Boolean
    ) {
        var parentPostAuthor: String?=null

        if (mActivity is SessionChangeListener) {
            (mActivity as SessionChangeListener).onSessionChanged(
                session,
                SessionChangeType.COMMENTS,
                timestamp
            )
        }

        val minimumCommentScore = PrefsUtility.pref_behaviour_comment_min()

        if (fromCache) {
            runOnUiThread(Runnable { mListener.onCommentListingRequestCachedCopy(timestamp) })
        }

        runOnUiThread(Runnable { mListener.onCommentListingRequestParseStart() })

        val commentListing: RedditListing

        if (thingResponse is RedditThingResponse.Single) {
            commentListing = (thingResponse
                .thing as Listing).data
        } else {
            val multiple = thingResponse as RedditThingResponse.Multiple

            if (multiple.things.size != 2) {
                throw RuntimeException(
                    "Expecting 2 items in array response, got "
                            + multiple.things.size
                )
            }

            val post = ((multiple.things.get(0) as Listing)
                .data
                .children
                .get(0)
                .ok() as Post).data

            val parsedPost =                 RedditParsedPost(mActivity, post, mParsePostSelfText)

            val preparedPost = RedditPreparedPost(
                mContext,
                mCacheManager,
                0,
                parsedPost,
                timestamp,
                true,
                false,
                false,
                false
            )

            runOnUiThread(Runnable {
                mListener.onCommentListingRequestPostDownloaded(
                    preparedPost
                )
            })

            parentPostAuthor = parsedPost.author

            commentListing = (thingResponse
                .things.get(1) as Listing).data
        }

        // Download comments
        val topLevelComments: ArrayList<MaybeParseError<RedditThing?>> = commentListing.children

        val items = ArrayList<RedditCommentListItem>(200)

        for (commentThingValue in topLevelComments) {
            buildCommentTree(
                commentThingValue,
                null,
                items,
                minimumCommentScore,
                parentPostAuthor
            )
        }

        val changeDataManager: RedditChangeDataManager = RedditChangeDataManager.Companion.getInstance(mUser)

        for (item in items) {
            if (item.isComment()) {
                changeDataManager.update(
                    timestamp,
                    item.asComment().getParsedComment().getRawComment()
                )
            }
        }

        runOnUiThread(Runnable { mListener.onCommentListingRequestAllItemsDownloaded(items) })
    }

    private fun createCommentListingCacheRequest(): CacheRequest {
        val url = from(mUrl.generateJsonUri())

        return CacheRequest(
            url,
            mUser,
            mSession,
            Priority(Constants.Priority.API_COMMENT_LIST),
            mDownloadStrategy,
            Constants.FileType.COMMENT_LIST,
            DownloadQueueType.REDDIT_API,
            mContext,
            object : CacheRequestCallbacks {
                override fun onFailure(error: RRError) {
                    runOnUiThread(Runnable { mListener.onCommentListingRequestFailure(error) })
                }

                override fun onDownloadNecessary() {
                    runOnUiThread(
                        Runnable { mListener.onCommentListingRequestDownloadNecessary() })
                }

                override fun onDataStreamAvailable(
                    streamFactory: GenericFactory<SeekableInputStream, IOException?>,
                    timestamp: TimestampUTC?,
                    session: UUID,
                    fromCache: Boolean,
                    mimetype: String?
                ) {
                    Thread(null, Runnable {
                        try {
                            val thingResponse = decodeRedditThingResponseFromStream(
                                streamFactory.create()
                            )

                            onThingDownloaded(thingResponse, session, timestamp, fromCache)
                        } catch (e: Exception) {
                            onFailure(
                                getGeneralErrorForFailure(
                                    mContext,
                                    RequestFailureType.PARSE,
                                    e,
                                    null,
                                    url,
                                    FailedRequestBody.Companion.from(streamFactory)
                                )
                            )
                        }
                    }, "Comment parsing", 1000000).start()
                }
            })
    }

    private fun buildCommentTree(
        maybeThing: MaybeParseError<RedditThing>,
        parent: RedditCommentListItem?,
        output: ArrayList<RedditCommentListItem>,
        minimumCommentScore: Int?,
        parentPostAuthor: String?
    ) {
        // TODO handle gracefully by showing error message

        val thing = maybeThing.ok()

        if (thing is More
            && mUrl.pathType() == RedditURLParser.POST_COMMENT_LISTING_URL
        ) {
            output.add(
                RedditCommentListItem(
                    thing.data,
                    parent,
                    mFragment,
                    mActivity,
                    mCommentListingURL
                )
            )
        } else if (thing is RedditThing.Comment) {
            var comment = thing.data

            if (comment.media_metadata != null && comment.body_html != null) {
                try {
                    for (entry in comment.media_metadata.entries) {
                        if (entry.value !is MaybeParseError.Ok<*>) {
                            continue
                        }

                        val emoteMetadata =                             (entry.value as MaybeParseError.Ok<RedditMediaMetadata>).value

                        // id is always structured as emote|{subreddit_id}|{emote_id}
                        // for subreddit emotes
                        if (emoteMetadata.id.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()[0].equals("emote", ignoreCase = true)
                            && emoteMetadata.s.u != null) {
                            val subredditId = emoteMetadata.id.split("\\|".toRegex())
                                .dropLastWhile { it.isEmpty() }.toTypedArray()[1]

                            // These are default reddit emotes (i think).
                            // They already have an img tag in the body html
                            // so no processing is required for these
                            if (subredditId == "free_emotes_pack") {
                                continue
                            }

                            val emoteId = emoteMetadata.id.split("\\|".toRegex())
                                .dropLastWhile { it.isEmpty() }.toTypedArray()[2]

                            val emotePlaceholder = String.format(
                                Locale.getDefault(),
                                ":%s:", emoteId
                            )

                            val imgTag = String.format(
                                Locale.getDefault(),
                                "<emote src=\"%s\" title=\"%s\"></emote>",
                                StringEscapeUtils.escapeHtml4(
                                    emoteMetadata.s.u.decoded
                                ),
                                emotePlaceholder
                            )

                            comment = comment.copyWithNewBodyHtml(
                                comment.body_html!!.decoded
                                    .replace(emotePlaceholder, imgTag)
                            )
                        }
                    }
                } catch (e: Exception) {
                    // Including this try-catch to cover for edge cases where reddit might send
                    // different values under media_metadata
                    Log.e(
                        TAG,
                        "Exception while processing media metadata for "
                                + comment.getIdAndType(),
                        e
                    )
                }
            }

            val currentCanonicalUserName: String = RedditAccountManager.Companion.getInstance(mContext)
                    .getDefaultAccount().canonicalUsername
            val showSubredditName = !(mCommentListingURL != null
                    && mCommentListingURL.pathType() == RedditURLParser.POST_COMMENT_LISTING_URL)
            val neverAutoCollapse = mCommentListingURL != null
                    && mCommentListingURL.pathType() == RedditURLParser.USER_COMMENT_LISTING_URL

            val item: RedditCommentListItem
            val renderableComment = RedditRenderableComment(
                RedditParsedComment(comment, mActivity),
                parentPostAuthor,
                minimumCommentScore,
                currentCanonicalUserName,
                true,
                showSubredditName,
                neverAutoCollapse
            )

            if (comment.isBlockedByUser()
                && !PrefsUtility.pref_appearance_hide_comments_from_blocked_users()
            ) {
                renderableComment.setBlockedUser(true)
            }

            item = RedditCommentListItem(
                renderableComment,
                parent,
                mFragment,
                mActivity,
                mCommentListingURL
            )

            // hide comment if user is blocked
            if (comment.isBlockedByUser()
                && PrefsUtility.pref_appearance_hide_comments_from_blocked_users()
            ) {
                return
            }

            output.add(item)

            if (comment.replies is Some) {
                val listing = (comment.replies.value as Listing).data

                for (reply in listing.children) {
                    buildCommentTree(
                        reply,
                        item,
                        output,
                        minimumCommentScore,
                        parentPostAuthor
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "CommentListingRequest"
    }
}
