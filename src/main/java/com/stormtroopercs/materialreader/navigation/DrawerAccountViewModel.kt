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
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.cache.CacheManager
import com.stormtroopercs.materialreader.cache.CacheRequest
import com.stormtroopercs.materialreader.cache.CacheRequestJSONParser
import com.stormtroopercs.materialreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import com.stormtroopercs.materialreader.common.Constants
import com.stormtroopercs.materialreader.common.Priority
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.jsonwrap.JsonValue
import com.stormtroopercs.materialreader.reddit.things.RedditThing

/**
 * Backs the navigation drawer's account header: the signed-in user's total
 * karma and avatar, fetched from `/user/{name}/about.json` (the same
 * endpoint + RedditThing-envelope unwrap as [UserProfileViewModel]). One
 * fetch per username per process; the cache strategy keeps repeat drawer
 * opens cheap. The header renders a default avatar until data lands.
 */
@HiltViewModel
class DrawerAccountViewModel @Inject constructor(
	@ApplicationContext private val context: Context,
	private val accountManager: RedditAccountManager,
	private val cacheManager: CacheManager,
) : ViewModel() {

	private var loadedUsername: String? = null

	private val _karma = MutableStateFlow<Int?>(null)
	val karma: StateFlow<Int?> = _karma.asStateFlow()

	private val _iconUrl = MutableStateFlow<String?>(null)
	val iconUrl: StateFlow<String?> = _iconUrl.asStateFlow()

	fun loadUser(username: String?) {
		val name = username?.takeIf { it.isNotBlank() }
		if (name == null) return
		if (name.equals(loadedUsername, ignoreCase = true)) return
		loadedUsername = name

		viewModelScope.launch {
			try {
				val account = accountManager.getDefaultAccount()

				val listener = object : CacheRequestJSONParser.Listener {
					override fun onJsonParsed(
						result: JsonValue,
						timestamp: TimestampUTC,
						session: UUID,
						fromCache: Boolean
					) {
						try {
							// /user/{name}/about.json is a RedditThing envelope
							// ({kind: "t2", data: {...}}) — parse the envelope and
							// unwrap the user, the same way UserProfileViewModel does.
							val user = result.asObject(RedditThing::class.java)?.asUser()
							if (user != null) {
								_karma.value = (user.link_karma ?: 0) + (user.comment_karma ?: 0)
								_iconUrl.value = user.iconUrl?.value
							}
						} catch (t: Throwable) {
							// Header stays on the default avatar; not fatal.
						}
					}

					override fun onFailure(error: RRError) {
						// No header data; the default avatar renders.
					}
				}

				val request = CacheRequest(
					Constants.Reddit.getUri("/user/$name/about.json"),
					account,
					null,
					Priority(Constants.Priority.API_USER_ABOUT),
					DownloadStrategyIfNotCached.INSTANCE,
					Constants.FileType.USER_ABOUT,
					CacheRequest.DownloadQueueType.REDDIT_API,
					context,
					CacheRequestJSONParser(context, listener)
				)
				cacheManager.makeRequest(request)
			} catch (e: Exception) {
				// No header data; the default avatar renders.
			}
		}
	}
}
