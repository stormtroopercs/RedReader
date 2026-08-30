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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.cache.CacheManager
import com.stormtroopercs.materialreader.cache.CacheRequest
import com.stormtroopercs.materialreader.cache.CacheRequestCallbacks
import com.stormtroopercs.materialreader.cache.downloadstrategy.DownloadStrategyIfTimestampOutsideBounds
import com.stormtroopercs.materialreader.common.Constants
import com.stormtroopercs.materialreader.common.GenericFactory
import com.stormtroopercs.materialreader.common.Priority
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.TimestampBound
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.common.datastream.SeekableInputStream
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.common.time.TimeDuration.Companion.minutes
import com.stormtroopercs.materialreader.database.entities.SubredditEntity
import com.stormtroopercs.materialreader.jsonwrap.JsonValue
import com.stormtroopercs.materialreader.reddit.things.RedditSubreddit
import com.stormtroopercs.materialreader.reddit.things.RedditThing
import com.stormtroopercs.materialreader.repository.SubredditRepository
import javax.inject.Inject

/**
 * ViewModel for subreddit search.
 *
 * The search hits the live Reddit API (`/subreddits/search.json?q=...`)
 * through the same `CacheRequest` read path as the listings — the Room
 * `subreddits` table had no writer, so a Room-local search could only
 * ever return its own (populated) results. Results also seed the Room
 * table as a local cache (REPLACE upsert on the name key).
 *
 * The `SubredditItem` view-model row decodes each child of the search
 * listing's `data.children` array as a `RedditThing` and reads the
 * `RedditSubreddit` fields the search screen displays (name, subscriber
 * count, description, icon).
 */
@HiltViewModel
class SubredditSearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subredditRepository: SubredditRepository,
    private val accountManager: RedditAccountManager,
    private val cacheManager: CacheManager
) : ViewModel() {

    sealed class SubredditSearchUiState {
        object Idle : SubredditSearchUiState()
        object Loading : SubredditSearchUiState()
        data class Success(
            val results: List<SubredditItem>,
            val query: String
        ) : SubredditSearchUiState()
        data class Error(val message: String) : SubredditSearchUiState()
    }

    /** One search result row (decoupled from the jsonwrap types). */
    data class SubredditItem(
        val name: String,
        val subscribers: Int?,
        val description: String?,
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

    private val _state = MutableStateFlow<SubredditSearchUiState>(SubredditSearchUiState.Idle)
    val state: StateFlow<SubredditSearchUiState> = _state.asStateFlow()

    /**
     * Query input flow. Each new non-blank value triggers a search via
     * [searchQueryFlow]; [mapLatest] cancels the previous in-flight search when
     * a newer query arrives, so an older response can never clobber a newer
     * one (replaces the old AtomicInteger race-guard).
     */
    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            searchQuery
                .filter { it.isNotBlank() }
                .mapLatest { query -> doSearch(query) }
                .collect { _state.value = it }
        }
    }

    /**
     * Search for subreddits matching the query (live API). Pushes the query
     * into [searchQuery]; the collected flow performs the request.
     */
    fun searchSubreddits(query: String) {
        if (query.isBlank()) {
            _state.value = SubredditSearchUiState.Idle
        } else {
            _state.value = SubredditSearchUiState.Loading
        }
        searchQuery.value = query
    }

    /**
     * Run one subreddit search for [query] and return its terminal
     * [SubredditSearchUiState]. Backed by a CacheRequest bridged into a
     * suspend function; cancellation (from [mapLatest] on a newer query)
     * leaves the stale result un-collected.
     */
    private suspend fun doSearch(query: String): SubredditSearchUiState =
        suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { }
            val account = accountManager.getDefaultAccount()
            val uriBuilder = Constants.Reddit.getUriBuilder("/subreddits/search.json")
                .appendQueryParameter("q", query)
                .appendQueryParameter("limit", "100")
            val jsonUri = UriString(uriBuilder.build().toString())

            val callbacks = object : CacheRequestCallbacks {
                override fun onDataStreamComplete(
                    streamFactory: com.stormtroopercs.materialreader.common.GenericFactory<SeekableInputStream, IOException>,
                    timestamp: TimestampUTC,
                    session: UUID,
                    fromCache: Boolean,
                    mimetype: String?
                ) {
                    if (!cont.isActive) return
                    try {
                        val result = streamFactory.create().use { input ->
                            JsonValue.parse(input)
                        }
                        val children = result.getArrayAtPath("data", "children").get()
                        val items = children.mapNotNull { child ->
                            toSubredditItem(child)
                        }
                        if (items.isEmpty()) {
                            cont.resume(
                                SubredditSearchUiState.Error(
                                    "No subreddits found for \"$query\""
                                )
                            )
                            return
                        }
                        seedLocalCache(items)
                        cont.resume(SubredditSearchUiState.Success(items, query))
                    } catch (e: Exception) {
                        cont.resume(
                            SubredditSearchUiState.Error(e.message ?: e.toString())
                        )
                    }
                }

                override fun onFailure(error: RRError) {
                    if (!cont.isActive) return
                    cont.resume(
                        SubredditSearchUiState.Error(
                            error.message ?: "Subreddit search failed"
                        )
                    )
                }
            }

            val request = CacheRequest(
                jsonUri,
                account,
                null,
                Priority(Constants.Priority.API_SUBREDDIT_SEARCH),
                DownloadStrategyIfTimestampOutsideBounds(
                    TimestampBound.Companion.notOlderThan(minutes(1))
                ),
                Constants.FileType.SUBREDDIT_LIST,
                CacheRequest.DownloadQueueType.REDDIT_API,
                context,
                callbacks
            )
            cacheManager.makeRequest(request)
        }

    /**
     * Upsert the search results into the Room `subreddits` table so the
     * cache is not empty (it was never populated before the 47th — the
     * local search ran against an always-empty table).
     */
    private fun seedLocalCache(items: List<SubredditItem>) {
        viewModelScope.launch {
            items.forEach { item ->
                try {
                    subredditRepository.insertSubreddit(
                        SubredditEntity(
                            name = item.name,
                            displayName = null,
                            subscribers = item.subscribers,
                            description = item.description,
                            iconUrl = item.iconUrl,
                            headerUrl = null,
                            createdUtc = null,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    // The cache is best-effort — never fail the search
                    // because the local write failed.
                }
            }
        }
    }

    /**
     * Clear the current search state.
     */
    fun clearSearch() {
        searchQuery.value = ""
        _state.value = SubredditSearchUiState.Idle
    }

    /**
     * Select a subreddit and navigate to it.
     */
    fun selectSubreddit(name: String): String {
        return name
    }
}

/**
 * Decode one `data.children` entry of a subreddit search listing into a
 * [SubredditSearchViewModel.SubredditItem]. Each child is a `RedditThing`
 * envelope (`{kind: "t5", data: {...}}`) whose `data` is a
 * [RedditSubreddit].
 *
 * Note the modern search shape: `data.name` is the *thing id* (`t5_…`);
 * the subreddit's real name is `display_name` and its community icon is
 * `icon_img` (the `RedditSubreddit` jsonwrap class predates both, so the
 * icon is read straight off the raw `data` object).
 */
internal fun toSubredditItem(child: JsonValue): SubredditSearchViewModel.SubredditItem? {
    return try {
        val thing = child.asObject<RedditThing>(RedditThing::class.java) ?: return null
        val sub = thing.asSubreddit()
        // display_name is the real name ("AskAnAmerican"); fall back to the
        // /r/<name>/ url path if the field is missing.
        val name = sub.display_name
            ?: sub.url?.let { it.removePrefix("/r/").removeSuffix("/").takeIf { n -> n.isNotBlank() } }
        if (name.isNullOrBlank()) return null
        val iconOpt = child.getStringAtPath("data", "icon_img")
        val icon = iconOpt.takeIf { it.isPresent }?.get() ?: sub.header_img
        SubredditSearchViewModel.SubredditItem(
            name = name,
            subscribers = sub.subscribers,
            description = sub.description,
            iconUrl = icon
        )
    } catch (e: Exception) {
        null
    }
}
