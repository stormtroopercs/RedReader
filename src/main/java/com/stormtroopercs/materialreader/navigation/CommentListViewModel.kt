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

package com.stormtroopercs.materialreader.navigation

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.common.BugReporter
import com.stormtroopercs.materialreader.cache.CacheManager
import com.stormtroopercs.materialreader.cache.CacheRequest
import com.stormtroopercs.materialreader.cache.CacheRequestCallbacks
import com.stormtroopercs.materialreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import com.stormtroopercs.materialreader.common.Constants
import com.stormtroopercs.materialreader.common.General
import com.stormtroopercs.materialreader.common.Priority
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.common.AndroidCommon
import com.stormtroopercs.materialreader.common.datastream.SeekableInputStream
import com.stormtroopercs.materialreader.jsonwrap.JsonValue
import com.stormtroopercs.materialreader.reddit.RedditAPI
import com.stormtroopercs.materialreader.reddit.APIResponseHandler.ActionResponseHandler
import com.stormtroopercs.materialreader.reddit.kthings.JsonUtils
import com.stormtroopercs.materialreader.reddit.kthings.MaybeParseError
import com.stormtroopercs.materialreader.reddit.kthings.RedditFieldReplies
import com.stormtroopercs.materialreader.reddit.kthings.RedditIdAndType
import com.stormtroopercs.materialreader.reddit.kthings.RedditThing
import com.stormtroopercs.materialreader.reddit.kthings.RedditThingResponse
import com.stormtroopercs.materialreader.reddit.url.CommentListingURL
import com.stormtroopercs.materialreader.reddit.url.RedditURLParser
import android.net.Uri
import com.stormtroopercs.materialreader.common.invokeIf
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
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
    val replyDepth: Int = 0,
    /** The full `t1_…` name — needed for vote / report (RedditAPI expects the
     *  full id, not the bare one in [id]). */
    val fullName: String = "",
    /** The comment's subreddit (for the report flow). */
    val subreddit: String? = null,
    /** The comment's permalink (a `UrlEncodedString` path), used to build a
     *  shareable URL. */
    val permalink: String? = null
)

/**
 * A comment action the user can invoke from the list. Maps onto the legacy
 * [RedditAPI] endpoints: the vote actions hit `api/vote` (dir +1/0/-1).
 * [COPY_LINK] and [REPORT] are handled by the screen (clipboard / report
 * dialog), not by an endpoint call here.
 */
enum class CommentAction {
	UPVOTE,
	DOWNVOTE,
	COPY_LINK,
	REPORT
}

@HiltViewModel
class CommentListViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    @Suppress("PropertyName")
    private val _state = MutableStateFlow<CommentListUiState>(CommentListUiState.Loading)
    val state: StateFlow<CommentListUiState> = _state.asStateFlow()

    @Suppress("PropertyName")
    private val _postId = MutableStateFlow("")
    val postId: StateFlow<String> = _postId.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    /**
     * Fetch the comment listing identified by [listingPath]: a bare post id
     * (a post's comment listing) or a `u/<user>/comments` path (a user's
     * comment listing). The path is mapped to the matching Reddit
     * comment-listing URL and the screen title is derived from it.
     */
    fun fetchComments(listingPath: String) {
        _postId.value = listingPath
        _title.value = if (listingPath.startsWith("u/")) listingPath else "Comments"
        fetchList(listingPath)
    }

    fun refresh() {
        fetchComments(_postId.value)
    }

    /** A transient result for the last comment action (success/failure text)
     *  to surface as a Snackbar. Null when nothing to show. */
    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult.asStateFlow()

    /**
     * Vote on [comment] against the Reddit API, as the legacy
     * `RedditCommentActions` menu did: `api/vote` with dir +1 (up) / −1
     * (down). The default account is used and the hosting [activity] builds
     * the [ActionResponseHandler]. [COPY_LINK] and [REPORT] are not endpoint
     * calls (handled by the screen), so they are not processed here.
     */
    fun performAction(activity: AppCompatActivity, comment: CommentItem, action: CommentAction) {
        val account = RedditAccountManager.getInstance(context).getDefaultAccount()
        if (account == null) {
            _actionResult.value = "Not signed in"
            return
        }

        val apiAction = when (action) {
            CommentAction.UPVOTE -> RedditAPI.ACTION_UPVOTE
            CommentAction.DOWNVOTE -> RedditAPI.ACTION_DOWNVOTE
            else -> return
        }

        val idAndType = RedditIdAndType(comment.fullName)

        val handler = object : ActionResponseHandler(activity) {
            override fun onSuccess() {
                AndroidCommon.runOnUiThread {
                    _actionResult.value = if (action == CommentAction.UPVOTE) "Upvoted" else "Downvoted"
                }
            }

            override fun onFailure(error: RRError) {
                AndroidCommon.runOnUiThread {
                    _actionResult.value = error.message ?: "Action failed"
                }
            }

            override fun onCallbackException(t: Throwable) {
                BugReporter.handleGlobalError(activity, t)
            }
        }

        RedditAPI.action(
            CacheManager.getInstance(context),
            handler,
            account,
            idAndType,
            apiAction,
            activity
        )
    }

    /** Clear a shown action-result message (called after the Snackbar). */
    fun clearActionResult() {
        _actionResult.value = null
    }

    private fun fetchList(listingPath: String) {
        viewModelScope.launch {
            _state.value = CommentListUiState.Loading

            try {
                val cacheManager = CacheManager.getInstance(context)
                val account = RedditAccountManager.getInstance(context).getDefaultAccount()
                if (account == null) {
                    AndroidCommon.runOnUiThread {
                        _state.value = CommentListUiState.Error(
                            RRError(title = "Not signed in", message = "Sign in to view comments")
                        )
                    }
                    return@launch
                }

                // A bare post id is a post's comment listing; a u/<user>/comments
                // path is a user's comment listing.
                val rawUri = if (listingPath.startsWith("u/")) {
                    "https://www.reddit.com/$listingPath/"
                } else {
                    "https://www.reddit.com/comments/$listingPath/"
                }
                val url = RedditURLParser.parseProbableCommentListing(Uri.parse(rawUri))
                if (url !is CommentListingURL) {
                    AndroidCommon.runOnUiThread {
                        _state.value = CommentListUiState.Error(
                            RRError(title = "Invalid listing", message = "Invalid comment listing URL")
                        )
                    }
                    return@launch
                }
                val uri = UriString(url.generateJsonUri().toString())

                val cacheRequest = CacheRequest(
                    uri,
                    account,
                    null,
                    Priority(Constants.Priority.API_COMMENT_LIST),
                    DownloadStrategyIfNotCached.INSTANCE,
                    Constants.FileType.COMMENT_LIST,
                    CacheRequest.DownloadQueueType.REDDIT_API,
                    context,
                    object : CacheRequestCallbacks {
                        override fun onFailure(error: RRError) {
                            AndroidCommon.runOnUiThread {
                                _state.value = CommentListUiState.Error(error)
                            }
                        }

                        override fun onDataStreamComplete(
                            streamFactory: com.stormtroopercs.materialreader.common.GenericFactory<SeekableInputStream, IOException>,
                            timestamp: TimestampUTC,
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
                                    com.stormtroopercs.materialreader.common.Optional.empty()
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
                        createdUtcTimestamp = comment.created_utc.value.toUtcSecs(),
                        authorFlairText = comment.author_flair_text?.decoded,
                        isTopLevel = depth == 0,
                        collapsed = false,
                        collapsedReason = comment.collapsed_reason_code,
                        replyDepth = depth,
                        fullName = comment.name.toString(),
                        subreddit = comment.subreddit?.decoded,
                        permalink = comment.permalink?.decoded
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
