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

package org.quantumbadger.redreader.navigation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequestCallbacks
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.common.AndroidCommon
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import org.quantumbadger.redreader.jsonwrap.JsonValue
import org.quantumbadger.redreader.reddit.kthings.JsonUtils
import org.quantumbadger.redreader.reddit.kthings.MaybeParseError
import org.quantumbadger.redreader.reddit.kthings.RedditFieldReplies
import org.quantumbadger.redreader.reddit.kthings.RedditThing
import org.quantumbadger.redreader.reddit.kthings.RedditThingResponse
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL
import org.quantumbadger.redreader.common.invokeIf
import org.quantumbadger.redreader.common.time.TimestampUTC
import java.io.IOException
import java.util.UUID
import kotlin.math.max

/**
 * UI state for the comment list screen.
 */
sealed class CommentListUiState {
    object Loading : CommentListUiState()
    data class Success(
        val postTitle: String?,
        val postAuthor: String?,
        val comments: List<CommentItem>,
        val moreCommentsAvailable: Boolean
    ) : CommentListUiState()
    data class Error(val error: RRError) : CommentListUiState()
}

/**
 * A single comment item for display in Compose UI.
 */
data class CommentItem(
    val id: String,
    val author: String?,
    val body: String,
    val score: Int,
    val replyCount: Int,
    val createdUtcTimestamp: Long,
    val authorFlairText: String?,
    val isTopLevel: Boolean,
    val collapsed: Boolean,
    val collapsedReason: String?,
    val replyDepth: Int = 0
)

class CommentListViewModel(
    private val context: Context,
    postId: String
) : ViewModel() {

    @Suppress("PropertyName")
    private val _state = MutableStateFlow<CommentListUiState>(CommentListUiState.Loading)
    val state: StateFlow<CommentListUiState> = _state.asStateFlow()

    @Suppress("PropertyName")
    private val _postId = MutableStateFlow(postId)
    val postId: StateFlow<String> = _postId.asStateFlow()

    init {
        fetchComments(postId)
    }

    fun fetchComments(postId: String) {
        _postId.value = postId
        fetchPostComments(postId)
    }

    fun refresh() {
        fetchComments(_postId.value)
    }

    private fun fetchPostComments(postId: String) {
        viewModelScope.launch {
            _state.value = CommentListUiState.Loading

            try {
                val cacheManager = CacheManager.getInstance(context)
                val account = RedditAccountManager.getInstance(context).getDefaultAccount()

                // Build the URL for this post's comments
                val url = PostCommentListingURL.forPostId(postId)
                val uri = UriString.from(url.generateJsonUri())

                val cacheRequest = CacheRequest(
                    uri,
                    account,
                    null,
                    Priority(Constants.Priority.API_COMMENT_LIST),
                    DownloadStrategyIfNotCached.INSTANCE,
                    Constants.FileType.COMMENT_LIST,
                    CacheRequest.DownloadQueueType.REDDIT_API,
                    null,
                    true,
                    context,
                    object : CacheRequestCallbacks {
                        override fun onFailure(error: RRError) {
                            AndroidCommon.runOnUiThread {
                                _state.value = CommentListUiState.Error(error)
                            }
                        }

                        override fun onDataStreamComplete(
                            streamFactory: org.quantumbadger.redreader.common.GenericFactory<SeekableInputStream?, IOException?>,
                            timestamp: TimestampUTC?,
                            session: java.util.UUID,
                            fromCache: Boolean,
                            mimetype: String?
                        ) {
                            try {
                                val stream = streamFactory.create()
                                val thingResponse = JsonUtils.decodeRedditThingResponseFromStream(stream!!)

                                val (postTitle, postAuthor, comments) = parseThingResponse(thingResponse)

                                AndroidCommon.runOnUiThread {
                                    _state.value = CommentListUiState.Success(
                                        postTitle = postTitle,
                                        postAuthor = postAuthor,
                                        comments = comments,
                                        moreCommentsAvailable = comments.any { it.id.startsWith("more_") }
                                    )
                                }
                            } catch (e: Exception) {
                                val error = General.getGeneralErrorForFailure(
                                    context,
                                    CacheRequest.RequestFailureType.PARSE,
                                    e,
                                    null,
                                    uri,
                                    org.quantumbadger.redreader.common.Optional.empty()
                                )
                                AndroidCommon.runOnUiThread {
                                    _state.value = CommentListUiState.Error(error)
                                }
                            }
                        }
                    }
                )

                cacheManager.makeRequest(cacheRequest)

            } catch (e: Exception) {
                val error = RRError(
                    title = "Error loading comments",
                    message = e.message ?: "Unknown error",
                    reportable = false
                )
                _state.value = CommentListUiState.Error(error)
            }
        }
    }

    private fun parseThingResponse(thingResponse: RedditThingResponse): Triple<String?, String?, List<CommentItem>> {
        val comments = mutableListOf<CommentItem>()

        when (thingResponse) {
            is RedditThingResponse.Multiple -> {
                val first = thingResponse.things[0]
                val second = thingResponse.things[1]

                var postTitle: String?=null
                var postAuthor: String?=null

                if (first is RedditThing.Listing) {
                    val firstListing = first.data
                    if (firstListing.children.isNotEmpty()) {
                        val firstPost = firstListing.children[0].ok() as RedditThing.Post
                        postTitle = firstPost.data.title?.decoded
                        postAuthor = firstPost.data.author?.decoded
                    }
                }

                if (second is RedditThing.Listing) {
                    val commentListing = second.data
                    for (child in commentListing.children) {
                        buildCommentItem(child, 0, comments)
                    }
                }

                return Triple(postTitle, postAuthor, comments)
            }

            is RedditThingResponse.Single -> {
                if (thingResponse.thing is RedditThing.Listing) {
                    val listing = (thingResponse.thing as RedditThing.Listing).data
                    for (child in listing.children) {
                        buildCommentItem(child, 0, comments)
                    }
                }

                return Triple(null, null, comments)
            }
        }

        return Triple(null, null, comments)
    }

    private fun buildCommentItem(
        maybeThing: MaybeParseError<RedditThing>,
        depth: Int,
        output: MutableList<CommentItem>
    ) {
        val thing = maybeThing.ok() ?: return

        when (thing) {
            is RedditThing.More -> {
                output.add(
                    CommentItem(
                        id = "more_${thing.data.count}",
                        author = null,
                        body = "+${thing.data.count} more comments",
                        score = 0,
                        replyCount = thing.data.count,
                        createdUtcTimestamp = System.currentTimeMillis() / 1000,
                        authorFlairText = null,
                        isTopLevel = depth == 0,
                        collapsed = false,
                        collapsedReason = "MORE",
                        replyDepth = depth
                    )
                )
            }

            is RedditThing.Comment -> {
                val comment = thing.data
                val body = comment.body?.decoded ?: ""

                output.add(
                    CommentItem(
                        id = comment.id,
                        author = comment.author?.decoded,
                        body = body,
                        score = if (comment.likes == true) comment.ups else max(0, comment.ups - comment.downs),
                        replyCount = if (comment.replies is RedditFieldReplies.Some) {
                            (comment.replies.value as RedditThing.Listing).data.children.size
                        } else {
                            0
                        },
                        createdUtcTimestamp = comment.created_utc.timestampUTC.timeMs / 1000,
                        authorFlairText = comment.author_flair_text?.decoded,
                        isTopLevel = depth == 0,
                        collapsed = false,
                        collapsedReason = comment.collapsed_reason_code,
                        replyDepth = depth
                    )
                )

                // Recurse into replies
                if (comment.replies is RedditFieldReplies.Some) {
                    val replies = (comment.replies.value as RedditThing.Listing).data
                    for (reply in replies.children) {
                        buildCommentItem(reply, depth + 1, output)
                    }
                }
            }

            else -> {}
        }
    }
}
