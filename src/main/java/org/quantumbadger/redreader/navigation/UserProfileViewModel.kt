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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequestJSONParser
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.jsonwrap.JsonObject
import org.quantumbadger.redreader.jsonwrap.JsonValue
import org.quantumbadger.redreader.reddit.things.RedditThing
import org.quantumbadger.redreader.reddit.things.RedditUser

/**
 * ViewModel for user profile display.
 * Manages user profile state and provides reactive updates via StateFlow.
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    sealed class UserProfileUiState {
        object Loading : UserProfileUiState()
        data class Ready(
            val username: String,
            val karma: Int,
            val isGold: Boolean,
            val isMod: Boolean,
            val iconUrl: String?,
            val accountType: String
        ) : UserProfileUiState()
        data class Error(val message: String) : UserProfileUiState()
    }

    private val _state = MutableStateFlow<UserProfileUiState>(UserProfileUiState.Loading)
    val state: StateFlow<UserProfileUiState> = _state.asStateFlow()

    private var currentUsername: String = ""

    /**
     * Load the user profile for the given username via the Reddit API
     * (/user/{name}/about.json), mirroring the legacy UserProfileDialog flow.
     */
    fun loadProfile(username: String) {
        if (username.isBlank()) {
            _state.value = UserProfileUiState.Error("No username to load")
            return
        }
        currentUsername = username
        viewModelScope.launch {
            _state.value = UserProfileUiState.Loading
            try {
                val account = RedditAccountManager.getInstance(context).getDefaultAccount()
                    ?: RedditAccountManager.getAnon()

                val listener = object : CacheRequestJSONParser.Listener {
                    override fun onJsonParsed(
                        result: JsonValue,
                        timestamp: TimestampUTC?,
                        session: UUID,
                        fromCache: Boolean
                    ) {
                        try {
                            // /user/{name}/about.json is a raw user object (no {data: ...}
                            // envelope), so parse the JsonObject and cast to RedditUser
                            // directly. (RedditThing.asUser() expects the envelope form.)
                            // JsonObject.asObject uses the non-nullable generic, unlike
                            // the over-nulled base JsonValue.asObject.
                            val user = (result as? JsonObject)?.asObject(RedditUser::class.java)
                            if (user == null) {
                                _state.value = UserProfileUiState.Error("User not found")
                                return
                            }
                            _state.value = UserProfileUiState.Ready(
                                username = user.name ?: username,
                                karma = (user.link_karma ?: 0) + (user.comment_karma ?: 0),
                                isGold = user.is_gold == true,
                                isMod = user.is_mod == true,
                                iconUrl = user.icon_img,
                                accountType = "Reddit User"
                            )
                        } catch (t: Throwable) {
                            _state.value = UserProfileUiState.Error(
                                "Failed to load profile: ${t.message}"
                            )
                        }
                    }

                    override fun onFailure(error: RRError) {
                        _state.value = UserProfileUiState.Error(
                            error.message ?: "Failed to load profile"
                        )
                    }
                }

                val request = CacheRequest(
                    Constants.Reddit.getUri("/user/$username/about.json"),
                    account,
                    null,
                    Priority(Constants.Priority.API_USER_ABOUT),
                    DownloadStrategyIfNotCached.INSTANCE,
                    Constants.FileType.USER_ABOUT,
                    CacheRequest.DownloadQueueType.REDDIT_API,
                    context,
                    CacheRequestJSONParser(context, listener)
                )
                CacheManager.getInstance(context).makeRequest(request)
            } catch (e: Exception) {
                _state.value = UserProfileUiState.Error(
                    "Failed to load profile: ${e.message}"
                )
            }
        }
    }

    /**
     * Block a user.
     *
     * The real API call ([org.quantumbadger.redreader.reddit.RedditAPI.blockUser])
     * requires an [androidx.appcompat.app.AppCompatActivity] for its response
     * handler, so it is invoked from the legacy [UserProfileDialog] activity
     * path. This updates local state optimistically.
     */
    fun blockUser(username: String) {
        viewModelScope.launch {
            _state.value = UserProfileUiState.Ready(
                username = username,
                karma = 0,
                isGold = false,
                isMod = false,
                iconUrl = null,
                accountType = "Blocked User"
            )
        }
    }

    /**
     * Unblock a user (see [blockUser] for why the API call lives in the
     * legacy activity path).
     */
    fun unblockUser(username: String) {
        viewModelScope.launch {
            loadProfile(username)
        }
    }

    /**
     * Mute a user (hide their comments).
     */
    fun muteUser(username: String) {
        viewModelScope.launch {
            try {
                PrefsUtility.pref_appearance_hide_comments_from_blocked_users_set(true)
            } catch (e: Exception) {
                _state.value = UserProfileUiState.Error("Failed to mute user: ${e.message}")
            }
        }
    }
}
