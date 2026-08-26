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
import androidx.appcompat.app.AppCompatActivity
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
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyAlways
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.common.time.TimeFormatHelper
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.jsonwrap.JsonValue
import org.quantumbadger.redreader.reddit.APIResponseHandler.ActionResponseHandler
import org.quantumbadger.redreader.reddit.APIResponseHandler.UserResponseHandler
import org.quantumbadger.redreader.reddit.RedditAPI
import org.quantumbadger.redreader.reddit.things.RedditThing
import org.quantumbadger.redreader.reddit.things.RedditUser

/**
 * ViewModel for user profile display.
 * Manages user profile state and provides reactive updates via StateFlow.
 *
 * In addition to rendering the profile (karma, badges, avatar, account age),
 * this backs the in-app [UserProfileScreen] with the same actions the legacy
 * user-profile dialog offered: blocking / unblocking a user (via
 * [RedditAPI.blockUser] / [RedditAPI.unblockUser]), re-login when the block
 * permission is denied, and the raw [RedditUser] for the "more info" dialog.
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
            val linkKarma: Int,
            val commentKarma: Int,
            val isGold: Boolean,
            val isMod: Boolean,
            val isEmployee: Boolean,
            val isSuspended: Boolean,
            val isFriend: Boolean,
            val isBlocked: Boolean,
            val isSelf: Boolean,
            val canBlock: Boolean,
            val canMessage: Boolean,
            val iconUrl: String?,
            val accountType: String,
            val accountAge: String?,
            val latestUser: RedditUser?
        ) : UserProfileUiState()
        data class Error(val message: String) : UserProfileUiState()
    }

    /**
     * Transient feedback for a block/unblock action. Non-null only until the
     * UI has handled it (then cleared via [clearBlockFeedback]).
     */
    sealed class BlockFeedback {
        /** The account lacks the `block_user` permission (HTTP 403). */
        object PermissionDenied : BlockFeedback()
        /** The action failed for another reason. */
        data class Failure(val error: RRError) : BlockFeedback()
    }

    private val _state = MutableStateFlow<UserProfileUiState>(UserProfileUiState.Loading)
    val state: StateFlow<UserProfileUiState> = _state.asStateFlow()

    private val _blockFeedback = MutableStateFlow<BlockFeedback?>(null)
    val blockFeedback: StateFlow<BlockFeedback?> = _blockFeedback.asStateFlow()

    fun clearBlockFeedback() {
        _blockFeedback.value = null
    }

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
                        timestamp: TimestampUTC,
                        session: UUID,
                        fromCache: Boolean
                    ) {
                        try {
                            // /user/{name}/about.json is a RedditThing envelope
                            // ({kind: "t2", data: {...}}) — parse the envelope and
                            // unwrap the user, the same way RedditAPI.getUser does.
                            val user = result.asObject(RedditThing::class.java)?.asUser()
                            if (user == null || user.name == null) {
                                _state.value = UserProfileUiState.Error("User not found")
                                return
                            }
                            val default = RedditAccountManager.getInstance(context).getDefaultAccount()
                            val isAnonymous = default.isAnonymous
                            val isSelf = user.name != null && !default.isAnonymous &&
                                StringUtils.asciiLowercase(user.name!!) ==
                                StringUtils.asciiLowercase(default.canonicalUsername)

                            val accountAge = user.created_utc?.let { createdUtc ->
                                TimeFormatHelper.format(
                                    TimestampUTC.now().elapsedPeriodSince(
                                        TimestampUTC.fromUtcSecs(createdUtc)
                                    ),
                                    context,
                                    R.string.user_profile_account_age,
                                    1
                                )
                            }

                            _state.value = UserProfileUiState.Ready(
                                username = user.name ?: username,
                                karma = (user.link_karma ?: 0) + (user.comment_karma ?: 0),
                                linkKarma = user.link_karma ?: 0,
                                commentKarma = user.comment_karma ?: 0,
                                isGold = user.is_gold == true,
                                isMod = user.is_mod == true,
                                isEmployee = user.is_employee == true,
                                isSuspended = user.is_suspended == true,
                                isFriend = user.is_friend == true,
                                isBlocked = user.is_blocked == true,
                                isSelf = isSelf,
                                canBlock = !isAnonymous && !isSelf,
                                canMessage = !isAnonymous,
                                iconUrl = user.iconUrl?.value,
                                accountType = "Reddit User",
                                accountAge = accountAge,
                                latestUser = user
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
     * Block a user via [RedditAPI.blockUser]. On success the profile flips to
     * the blocked state; on a 403 the [BlockFeedback.PermissionDenied] event is
     * raised (the UI offers a re-login); otherwise a
     * [BlockFeedback.Failure] is raised.
     *
     * [activity] is the hosting activity (the response handlers post to its UI
     * thread and the API needs a non-application context for the request).
     */
    fun blockUser(activity: AppCompatActivity, username: String) {
        val cm = CacheManager.getInstance(activity)
        val currentUser = RedditAccountManager.getInstance(activity).defaultAccount

        RedditAPI.blockUser(
            cm,
            username,
            object : RedditAPI.BlockUserResponseHandler {
                override fun onSuccess() {
                    activity.runOnUiThread {
                        (state.value as? UserProfileUiState.Ready)?.let { ready ->
                            _state.value = ready.copy(isBlocked = true)
                        }
                    }
                }

                override fun onBlockUserPermissionDenied() {
                    activity.runOnUiThread {
                        _blockFeedback.value = BlockFeedback.PermissionDenied
                    }
                }

                override fun onFailure(error: RRError) {
                    activity.runOnUiThread {
                        _blockFeedback.value = BlockFeedback.Failure(error)
                    }
                }
            },
            currentUser,
            activity
        )
    }

    /**
     * Unblock a user via [RedditAPI.unblockUser]. The unblock endpoint needs the
     * current user's fullname as the `container` field, so this is a two-step
     * call: first [RedditAPI.getUser] for the current account to read its
     * fullname, then [RedditAPI.unblockUser]. Mirrors the legacy user-profile
     * dialog's two-step unblock.
     */
    fun unblockUser(activity: AppCompatActivity, username: String) {
        val cm = CacheManager.getInstance(activity)
        val currentUser = RedditAccountManager.getInstance(activity).defaultAccount

        RedditAPI.getUser(
            cm,
            currentUser.username,
            object : UserResponseHandler(activity) {
                override fun onDownloadStarted() {}

                override fun onSuccess(redditUser: RedditUser, timestamp: TimestampUTC) {
                    val currentUserFullname = redditUser.fullname()
                    RedditAPI.unblockUser(
                        cm,
                        username,
                        currentUserFullname,
                        object : ActionResponseHandler(activity) {
                            override fun onSuccess() {
                                activity.runOnUiThread {
                                    (state.value as? UserProfileUiState.Ready)?.let { ready ->
                                        _state.value = ready.copy(isBlocked = false)
                                    }
                                }
                            }

                            override fun onFailure(error: RRError) {
                                activity.runOnUiThread {
                                    _blockFeedback.value = BlockFeedback.Failure(error)
                                }
                            }

                            override fun onCallbackException(t: Throwable) {
                                activity.runOnUiThread {
                                    _blockFeedback.value = BlockFeedback.Failure(RRError(t = t))
                                }
                            }
                        },
                        currentUser,
                        activity
                    )
                }

                override fun onCallbackException(t: Throwable) {
                    activity.runOnUiThread {
                        _blockFeedback.value = BlockFeedback.Failure(RRError(t = t))
                    }
                }

                override fun onFailure(error: RRError) {
                    activity.runOnUiThread {
                        _blockFeedback.value = BlockFeedback.Failure(error)
                    }
                }
            },
            currentUser,
            DownloadStrategyAlways.INSTANCE,
            activity
        )
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

    /**
     * Sign the current account out: remove it from the local account store
     * ([RedditAccountManager.deleteAccount]). The account manager notifies its
     * listeners, so the main screen's account row flips back to "Sign in to
     * Reddit" and the default account falls back to anonymous. No server call
     * is involved (the refresh token simply stops being used from this
     * device), mirroring the legacy account dialog's remove-account action.
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                val manager = RedditAccountManager.getInstance(context)
                val account = manager.defaultAccount
                if (account.isAnonymous) {
                    return@launch
                }
                manager.deleteAccount(account)
            } catch (e: Exception) {
                _state.value = UserProfileUiState.Error("Failed to sign out: ${e.message}")
            }
        }
    }
}
