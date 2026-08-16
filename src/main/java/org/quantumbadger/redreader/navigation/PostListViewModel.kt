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
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
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
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.reddit.PostSort
import org.quantumbadger.redreader.reddit.kthings.JsonUtils.decodeRedditThingFromStream
import org.quantumbadger.redreader.reddit.kthings.RedditThing
import org.quantumbadger.redreader.reddit.url.PostListingURL
import org.quantumbadger.redreader.reddit.url.RedditURLParser

sealed class PostListUiState {
    data class Loading(val isInitialLoad: Boolean) : PostListUiState()
    data class Success(val posts: List<PostItem>) : PostListUiState()
    data class Error(val error: RRError) : PostListUiState()
}

data class PostItem(
    val id: String,
    val title: String?,
    val author: String?,
    val subreddit: String,
    val score: Int,
    val numComments: Int,
    val url: String?,
    val permalink: String,
    val isSelf: Boolean,
    val isOver18: Boolean,
    val isSpoiler: Boolean,
    val isStickied: Boolean,
    val isLocked: Boolean,
    val isVideo: Boolean,
    val isCrosspost: Boolean,
    val linkFlairText: String?,
    val authorFlairText: String?,
    val thumbnail: String?,
    val selftext: String?,
    val createdUtc: Long
)

@HiltViewModel
class PostListViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<PostListUiState>(PostListUiState.Loading(true))
    val state: StateFlow<PostListUiState> = _state.asStateFlow()

    private val _posts = MutableStateFlow<List<PostItem>>(emptyList())
    val posts: StateFlow<List<PostItem>> = _posts.asStateFlow()

    private val _sortBy = MutableStateFlow(PrefsUtility.pref_behaviour_postsort())
    val sortBy: StateFlow<PostSort> = _sortBy.asStateFlow()

    private var currentSubreddit: String = ""

    fun fetchPosts(subreddit: String) {
        currentSubreddit = subreddit
        _state.value = PostListUiState.Loading(_state.value !is PostListUiState.Success)
        fetchPostListing(subreddit)
    }

    fun refresh() {
        if (currentSubreddit.isEmpty()) return
        _state.value = PostListUiState.Loading(false)
        fetchPostListing(currentSubreddit)
    }

    fun setSortBy(sort: PostSort) {
        if (sort == _sortBy.value) return
        _sortBy.value = sort
        PrefsUtility.pref_behaviour_postsort_set(sort)
        refresh()
    }

    private fun fetchPostListing(subreddit: String) {
        viewModelScope.launch {
            try {
                val account = RedditAccountManager.getInstance(context).getDefaultAccount()
                if (account == null) {
                    _state.value = PostListUiState.Error(
                        RRError(title = "Not signed in", message = "Sign in to view post listings")
                    )
                    return@launch
                }

                val rawUri = if (subreddit.isBlank()) {
                    "https://www.reddit.com/"
                } else {
                    "https://www.reddit.com/r/$subreddit/"
                }
                val postListingUrl = RedditURLParser.parseProbablePostListing(Uri.parse(rawUri))
                if (postListingUrl !is PostListingURL) {
                    _state.value = PostListUiState.Error(
                        RRError(title = "Invalid listing", message = "Invalid post listing URL")
                    )
                    return@launch
                }

                val jsonUri = postListingUrl.generateJsonUri()
                if (jsonUri == null) {
                    _state.value = PostListUiState.Error(
                        RRError(title = "Invalid listing", message = "Could not build JSON URI")
                    )
                    return@launch
                }

                val callbacks = object : CacheRequestCallbacks {
                    override fun onFailure(error: RRError) {
                        _state.value = PostListUiState.Error(
                            RRError(
                                title = "Failed to load posts",
                                message = error.message ?: error.toString()
                            )
                        )
                    }

                    override fun onDataStreamComplete(
                        streamFactory: GenericFactory<SeekableInputStream, IOException?>,
                        timestamp: TimestampUTC?,
                        session: UUID,
                        fromCache: Boolean,
                        mimetype: String?
                    ) {
                        try {
                            val thing = decodeRedditThingFromStream(streamFactory.create())
                            val listing = (thing as? RedditThing.Listing)?.data
                                ?: throw RuntimeException(
                                    "Expected listing, got " + thing.javaClass.name
                                )

                            val posts = listing.children
                                .mapNotNull { it.ok() as? RedditThing.Post }
                                .map { it.toPostItem() }

                            _posts.value = posts
                            _state.value = PostListUiState.Success(posts)
                        } catch (e: Exception) {
                            _state.value = PostListUiState.Error(
                                RRError(
                                    title = "Parse error",
                                    message = e.message,
                                    t = e
                                )
                            )
                        }
                    }
                }

                val request = CacheRequest(
                    UriString(jsonUri.toString()),
                    account,
                    null,
                    Priority(Constants.Priority.API_POST_LIST),
                    DownloadStrategyIfNotCached.INSTANCE,
                    Constants.FileType.POST_LIST,
                    CacheRequest.DownloadQueueType.REDDIT_API,
                    context,
                    callbacks
                )
                CacheManager.getInstance(context).makeRequest(request)
            } catch (e: Exception) {
                _state.value = PostListUiState.Error(
                    RRError(title = "Error", message = e.message, t = e)
                )
            }
        }
    }
}

private fun RedditThing.Post.toPostItem(): PostItem {
    val p = data
    return PostItem(
        id = p.name.toString(),
        title = p.title?.decoded,
        author = p.author?.decoded,
        subreddit = p.subreddit.decoded,
        score = p.score,
        numComments = p.num_comments,
        url = p.findUrl()?.value,
        permalink = p.permalink.decoded,
        isSelf = p.is_self,
        isOver18 = p.over_18,
        isSpoiler = p.spoiler,
        isStickied = p.stickied,
        isLocked = p.locked,
        isVideo = p.is_video,
        isCrosspost = p.crosspost_parent != null,
        linkFlairText = p.link_flair_text?.decoded,
        authorFlairText = p.author_flair_text?.decoded,
        thumbnail = p.thumbnail?.decoded,
        selftext = p.selftext?.decoded,
        createdUtc = p.created_utc.value.toUtcSecs()
    )
}
