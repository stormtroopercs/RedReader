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
 * along with RedReader.  If not, see <http://www.gnu.org/licenses>.\
 ******************************************************************************/

package org.quantumbadger.redreader.navigation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequestCallbacks
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfTimestampOutsideBounds
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.TimestampBound
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.common.time.TimeDuration.Companion.minutes
import org.quantumbadger.redreader.database.entities.SubredditEntity
import org.quantumbadger.redreader.jsonwrap.JsonValue
import org.quantumbadger.redreader.reddit.things.RedditSubreddit
import org.quantumbadger.redreader.reddit.things.RedditThing
import org.quantumbadger.redreader.repository.SubredditRepository
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
    private val subredditRepository: SubredditRepository
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
     * In-flight request counter: a response is only applied if it is still
     * the latest request (fast typing otherwise lets an older query's
     * response clobber a newer one — the classic race).
     */
    private val requestSeq = AtomicInteger(0)

    /**
     * Search for subreddits matching the query (live API).
     */
    fun searchSubreddits(query: String) {
        if (query.isBlank()) {
            requestSeq.incrementAndGet() // invalidate any in-flight request
            _state.value = SubredditSearchUiState.Idle
            return
        }

        _state.value = SubredditSearchUiState.Loading
        val seq = requestSeq.incrementAndGet()

        viewModelScope.launch {
            try {
                val account = RedditAccountManager.getInstance(context).getDefaultAccount()
                val uriBuilder = Constants.Reddit.getUriBuilder("/subreddits/search.json")
                    .appendQueryParameter("q", query)
                    .appendQueryParameter("limit", "100")
                val jsonUri = UriString(uriBuilder.build().toString())

                val callbacks = object : CacheRequestCallbacks {
                    override fun onDataStreamComplete(
                        streamFactory: GenericFactory<SeekableInputStream, IOException>,
                        timestamp: TimestampUTC,
                        session: UUID,
                        fromCache: Boolean,
                        mimetype: String?
                    ) {
                        if (seq != requestSeq.get()) return // stale response
                        try {
                            val result = streamFactory.create().use { input ->
                                JsonValue.parse(input)
                            }
                            val children = result.getArrayAtPath("data", "children").get()
                            val items = children.mapNotNull { child ->
                                toSubredditItem(child)
                            }
                            if (items.isEmpty()) {
                                _state.value = SubredditSearchUiState.Error(
                                    "No subreddits found for \"$query\""
                                )
                                return
                            }
                            _state.value = SubredditSearchUiState.Success(items, query)
                            seedLocalCache(items)
                        } catch (e: Exception) {
                            _state.value = SubredditSearchUiState.Error(
                                e.message ?: e.toString()
                            )
                        }
                    }

                    override fun onFailure(error: RRError) {
                        if (seq != requestSeq.get()) return // stale response
                        _state.value = SubredditSearchUiState.Error(
                            error.message ?: "Subreddit search failed"
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
                CacheManager.getInstance(context).makeRequest(request)
            } catch (e: Exception) {
                if (seq != requestSeq.get()) return@launch
                _state.value = SubredditSearchUiState.Error(
                    "Failed to search subreddits: ${e.message}"
                )
            }
        }
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
