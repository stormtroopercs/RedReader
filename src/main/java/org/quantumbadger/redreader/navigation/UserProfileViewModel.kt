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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.common.PrefsUtility
import javax.inject.Inject

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

    /**
     * Load user profile for the specified account.
     */
    fun loadProfile(accountId: String? = null) {
        viewModelScope.launch {
            _state.value = UserProfileUiState.Loading
            try {
                val accountManager = RedditAccountManager.getInstance(context)
                val account = accountId?.let {
                    accountManager.getAccountFromId(it)
                } ?: accountManager.getDefaultAccount()

                if (account != null) {
                    _state.value = UserProfileUiState.Ready(
                        username = account.name.decoded ?: "Unknown",
                        karma = account.karma,
                        isGold = account.isGold,
                        isMod = account.isModeratorOfAnySubreddit,
                        iconUrl = account.iconUrl?.toString(),
                        accountType = "Reddit User"
                    )
                } else {
                    _state.value = UserProfileUiState.Error("No account found")
                }
            } catch (e: Exception) {
                _state.value = UserProfileUiState.Error("Failed to load profile: ${e.message}")
            }
        }
    }

    /**
     * Block a user.
     */
    fun blockUser(username: String) {
        viewModelScope.launch {
            try {
                PrefsUtility.prefBlockedUsersSetAdd(context, username)
            } catch (e: Exception) {
                _state.value = UserProfileUiState.Error("Failed to block user: ${e.message}")
            }
        }
    }

    /**
     * Unblock a user.
     */
    fun unblockUser(username: String) {
        viewModelScope.launch {
            try {
                PrefsUtility.prefBlockedUsersSetRemove(context, username)
            } catch (e: Exception) {
                _state.value = UserProfileUiState.Error("Failed to unblock user: ${e.message}")
            }
        }
    }

    /**
     * Mute a user (hide their comments).
     */
    fun muteUser(username: String) {
        viewModelScope.launch {
            try {
                PrefsUtility.prefMutedUsersSetAdd(context, username)
            } catch (e: Exception) {
                _state.value = UserProfileUiState.Error("Failed to mute user: ${e.message}")
            }
        }
    }
}
