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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.cache.CacheManager
import com.stormtroopercs.materialreader.cache.CacheRequest
import com.stormtroopercs.materialreader.cache.CacheRequestCallbacks
import com.stormtroopercs.materialreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import com.stormtroopercs.materialreader.common.Constants
import com.stormtroopercs.materialreader.common.GenericFactory
import com.stormtroopercs.materialreader.common.Priority
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.common.datastream.SeekableInputStream
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.jsonwrap.JsonValue
import javax.inject.Inject

/**
 * ViewModel for the main screen's "Your subreddits" section.
 *
 * Fetches the signed-in user's subscribed subreddits from the live Reddit
 * API (`/subreddits/mine/subscriber.json`) — the same endpoint the legacy
 * main menu's `MainMenuListingManager` used (via
 * `RedditSubredditSubscriptionManager`). Results are sorted alphabetically,
 * matching the legacy `GROUP_SUBREDDITS_ITEMS` ordering.
 *
 * When signed out the section is hidden entirely (the legacy main menu also
 * suppressed the subscribed group for anonymous accounts — DeepWiki-verified
 * against the upstream `MainMenuListingManager`).
 */
@HiltViewModel
class MainScreenViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    sealed class SubscribedState {
        /** Signed out — nothing to fetch, hide the section. */
        object Idle : SubscribedState()

        object Loading : SubscribedState()

        data class Success(
            val subreddits: List<SubscribedSubreddit>
        ) : SubscribedState()

        data class Error(val message: String) : SubscribedState()
    }

    /** One subscribed-subreddit row (decoupled from the jsonwrap types). */
    data class SubscribedSubreddit(
        val name: String,
        val subscribers: Int?,
        val iconUrl: String?
    ) {
        /** Format the subscriber count for display ("1.2M"). */
        fun subscribersLabel(): String? {
            val count = subscribers ?: return null
            return when {
                count >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", count / 1_000_000.0)
                count >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", count / 1_000.0)
                else -> count.toString()
            }
        }
    }

    private val _state = MutableStateFlow<SubscribedState>(SubscribedState.Idle)
    val state: StateFlow<SubscribedState> = _state.asStateFlow()

    init {
        loadSubscribed()
    }

    /**
     * Load the default account's subscribed subreddits. No-op (stays Idle)
     * when signed out.
     */
    fun loadSubscribed() {
        val account = RedditAccountManager.getInstance(context).getDefaultAccount()
        if (account.username.isBlank()) {
            _state.value = SubscribedState.Idle
            return
        }

        _state.value = SubscribedState.Loading
        viewModelScope.launch {
            try {
                val uriBuilder = Constants.Reddit.getUriBuilder(Constants.Reddit.PATH_SUBREDDITS_MINE_SUBSCRIBER)
                val jsonUri = UriString(uriBuilder.build().toString())

                val callbacks = object : CacheRequestCallbacks {
                    override fun onDataStreamComplete(
                        streamFactory: GenericFactory<SeekableInputStream, IOException>,
                        timestamp: TimestampUTC,
                        session: UUID,
                        fromCache: Boolean,
                        mimetype: String?
                    ) {
                        try {
                            val result = streamFactory.create().use { input ->
                                JsonValue.parse(input)
                            }
                            val children = result.getArrayAtPath("data", "children").get()
                            val items = children.mapNotNull { child ->
                                toSubscribedItem(child)
                            }.sortedBy { it.name.lowercase(java.util.Locale.US) }

                            _state.value = if (items.isEmpty()) {
                                SubscribedState.Idle
                            } else {
                                SubscribedState.Success(items)
                            }
                        } catch (e: Exception) {
                            _state.value = SubscribedState.Error(e.message ?: e.toString())
                        }
                    }

                    override fun onFailure(error: RRError) {
                        _state.value = SubscribedState.Error(
                            error.message ?: "Failed to load subreddits"
                        )
                    }
                }

                val request = CacheRequest(
                    jsonUri,
                    account,
                    null,
                    Priority(Constants.Priority.API_SUBREDDIT_INVIDIVUAL),
                    DownloadStrategyIfNotCached.INSTANCE,
                    Constants.FileType.SUBREDDIT_LIST,
                    CacheRequest.DownloadQueueType.REDDIT_API,
                    context,
                    callbacks
                )
                CacheManager.getInstance(context).makeRequest(request)
            } catch (e: Exception) {
                _state.value = SubscribedState.Error("Failed to load subreddits: ${e.message}")
            }
        }
    }
}

/**
 * Decode one `data.children` entry of a subreddit listing into a
 * [MainScreenViewModel.SubscribedSubreddit]. Delegates to
 * [toSubredditItem] (the search listing's decoder) — the modern listing
 * shape (`name` = thing id `t5_…`, real name in `display_name`, icon in
 * `icon_img`) is identical for search and the subscribed list.
 */
internal fun toSubscribedItem(child: JsonValue): MainScreenViewModel.SubscribedSubreddit? {
    val item = toSubredditItem(child) ?: return null
    return MainScreenViewModel.SubscribedSubreddit(
        name = item.name,
        subscribers = item.subscribers,
        iconUrl = item.iconUrl
    )
}
