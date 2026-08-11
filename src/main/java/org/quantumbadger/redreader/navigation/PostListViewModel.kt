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
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.UriString.Companion.from
import org.quantumbadger.redreader.reddit.kthings.RedditListing
import org.quantumbadger.redreader.reddit.kthings.RedditThing
import org.quantumbadger.redreader.reddit.url.PostListingURL
import org.quantumbadger.redreader.reddit.url.RedditURLParser

sealed class PostListUiState {
    object Loading : PostListUiState()
    data class Success(val posts: List<PostItem>) : PostListUiState()
    data class Error(val message: String) : PostListUiState()
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
    val linkFlairText: String?,
    val authorFlairText: String?,
    val thumbnail: String?,
    val selftext: String?,
    val createdUtc: Long
)

class PostListViewModel(
    private val context: Context,
    subreddit: String
) : ViewModel() {

    @Suppress("PropertyName")
    private val _state = MutableStateFlow<PostListUiState>(PostListUiState.Loading)
    val state: StateFlow<PostListUiState> = _state.asStateFlow()

    @Suppress("PropertyName")
    private val _subreddit = MutableStateFlow(subreddit)
    val subreddit: StateFlow<String> = _subreddit.asStateFlow()

    init {
        fetchPosts(subreddit)
    }

    fun fetchPosts(subreddit: String) {
        _subreddit.value = subreddit
        fetchPostListing(subreddit)
    }

    private fun fetchPostListing(subreddit: String) {
        viewModelScope.launch {
            _state.value = PostListUiState.Loading

            try {
                val cacheManager = CacheManager.getInstance(context)
                val account = RedditAccountManager.getInstance(context).getDefaultAccount()

                // Parse the URL for this subreddit
                val postListingUrl = if (subreddit.isBlank()) {
                    RedditURLParser.parse("https://www.reddit.com/")
                } else {
                    RedditURLParser.parse("https://www.reddit.com/r/$subreddit/")
                }

                if (postListingUrl !is PostListingURL) {
                    _state.value = PostListUiState.Error("Invalid post listing URL")
                    return@launch
                }

                val cacheRequest = CacheRequest(
                    from(postListingUrl.generateJsonUri()),
                    account,
                    null,
                    DownloadStrategyIfNotCached.INSTANCE,
                    true,
                    null,
                    Priority.HIGH,
                    null
                )

                cacheManager.makeRequest(cacheRequest) { result ->
                    when (result) {
                        is CacheRequest.Result.Success -> {
                            try {
                                val listing = result.cacheFile.use { stream ->
                                    stream.inputStream.use {
                                        RedditListing.parse(stream)
                                    }
                                }

                                val posts = when (listing) {
                                    is RedditThing.Listing -> listing.children
                                        .filterIsInstance<RedditThing.Post>()
                                        .map { it.toPostItem() }
                                    else -> emptyList()
                                }

                                _state.value = PostListUiState.Success(posts)
                            } catch (e: Exception) {
                                _state.value = PostListUiState.Error("Parse error: ${e.message}")
                            }
                        }
                        is CacheRequest.Result.Failed -> {
                            _state.value = PostListUiState.Error(
                                "Failed to load: ${result.failureReason.userMessage}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.value = PostListUiState.Error("Error: ${e.message}")
            }
        }
    }
}

private fun RedditThing.Post.toPostItem(): PostItem {
    return PostItem(
        id = id,
        title = title?.decoded,
        author = author?.decoded,
        subreddit = subreddit.decoded,
        score = score,
        numComments = num_comments,
        url = findUrl()?.toString(),
        permalink = permalink.decoded,
        isSelf = is_self,
        isOver18 = over_18,
        isSpoiler = spoiler,
        isStickied = stickied,
        isLocked = locked,
        linkFlairText = link_flair_text?.decoded,
        authorFlairText = author_flair_text?.decoded,
        thumbnail = thumbnail?.decoded,
        selftext = selftext?.decoded,
        createdUtc = created_utc.timestampUTC.timeMs
    )
}
