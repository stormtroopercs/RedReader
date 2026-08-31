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
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.cache.CacheManager
import com.stormtroopercs.materialreader.cache.CacheRequest
import com.stormtroopercs.materialreader.cache.CacheRequestCallbacks
import com.stormtroopercs.materialreader.cache.downloadstrategy.DownloadStrategyAlways
import com.stormtroopercs.materialreader.common.Constants
import com.stormtroopercs.materialreader.common.GenericFactory
import com.stormtroopercs.materialreader.common.Priority
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.common.datastream.SeekableInputStream
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.jsonwrap.JsonValue
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/**
 * The Explore tab's community directory sub-tab (FINAL-DESIGN 6.5):
 * **Popular** (`/subreddits/popular.json`), **All** (`/subreddits.json`),
 * **New** (`/subreddits/new.json`), **Controversial**
 * (`/subreddits/controversial.json`). All four return the same listing
 * shape (`data.children` of subreddit things), so one fetch path covers
 * them all.
 */
enum class CommunityDirectoryTab {
	POPULAR,
	ALL,
	NEW,
	CONTROVERSIAL,
	;

	val label: String
		get() = when (this) {
			CommunityDirectoryTab.POPULAR -> "Popular"
			CommunityDirectoryTab.ALL -> "All"
			CommunityDirectoryTab.NEW -> "New"
			CommunityDirectoryTab.CONTROVERSIAL -> "Controversial"
		}

	val path: String
		get() = when (this) {
			CommunityDirectoryTab.POPULAR -> "/subreddits/popular.json"
			CommunityDirectoryTab.ALL -> "/subreddits.json"
			CommunityDirectoryTab.NEW -> "/subreddits/new.json"
			CommunityDirectoryTab.CONTROVERSIAL -> "/subreddits/controversial.json"
		}
}

/**
 * ViewModel for the community directory (FINAL-DESIGN Phase 6.5). One
 * request per directory tab, decoded into the shared [SubredditItem]
 * rows (the same row shape the subreddit search uses), so the directory
 * and the search results share a row renderer.
 */
@HiltViewModel
class CommunityDirectoryViewModel @Inject constructor(
	@ApplicationContext private val context: Context,
	private val accountManager: RedditAccountManager,
	private val cacheManager: CacheManager,
) : ViewModel() {

	private val _rows = MutableStateFlow<List<SubredditSearchViewModel.SubredditItem>>(emptyList())
	val rows: StateFlow<List<SubredditSearchViewModel.SubredditItem>> = _rows.asStateFlow()

	private val _loading = MutableStateFlow(false)
	val loading: StateFlow<Boolean> = _loading.asStateFlow()

	private val _error = MutableStateFlow<String?>(null)
	val error: StateFlow<String?> = _error.asStateFlow()

	/** Stale-response guard (a slow earlier tab must not clobber a newer one). */
	private val requestSeq = AtomicInteger(0)

	fun load(tab: CommunityDirectoryTab) {
		_error.value = null
		_loading.value = true
		val seq = requestSeq.incrementAndGet()

		viewModelScope.launch {
			try {
				val account = accountManager.getDefaultAccount()
				val uriBuilder = Constants.Reddit.getUriBuilder(tab.path)
					.appendQueryParameter("limit", "100")
				val jsonUri = UriString(uriBuilder.build().toString())

				val callbacks = object : CacheRequestCallbacks {
					override fun onDataStreamComplete(
						streamFactory: GenericFactory<SeekableInputStream, IOException>,
						timestamp: TimestampUTC,
						session: UUID,
						fromCache: Boolean,
						mimetype: String?,
					) {
						if (seq != requestSeq.get()) return // stale response
						try {
							val result = streamFactory.create().use { input ->
								JsonValue.parse(input)
							}
							val children = result.getArrayAtPath("data", "children").get()
							val items = children.mapNotNull { child -> toSubredditItem(child) }
							_rows.value = items
							_loading.value = false
							if (items.isEmpty()) {
								_error.value = "No communities found"
							}
						} catch (e: Exception) {
							if (seq == requestSeq.get()) {
								_error.value = e.message ?: e.toString()
								_loading.value = false
							}
						}
					}

					override fun onFailure(error: RRError) {
						if (seq != requestSeq.get()) return // stale response
						_error.value = error.message ?: "Failed to load communities"
						_loading.value = false
					}
				}

				val request = CacheRequest(
					jsonUri,
					account,
					null,
					Priority(Constants.Priority.API_SUBREDDIT_LIST),
					DownloadStrategyAlways.INSTANCE,
					Constants.FileType.SUBREDDIT_LIST,
					CacheRequest.DownloadQueueType.REDDIT_API,
					context,
					callbacks,
				)
				cacheManager.makeRequest(request)
			} catch (e: Exception) {
				if (seq == requestSeq.get()) {
					_error.value = "Failed to load communities: ${e.message}"
					_loading.value = false
				}
			}
		}
	}
}
