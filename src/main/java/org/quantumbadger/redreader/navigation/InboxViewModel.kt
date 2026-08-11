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
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.reddit.url.InboxListingURL
import org.quantumbadger.redreader.reddit.url.RedditURLParser
import javax.inject.Inject

/**
 * ViewModel for inbox listing (messages, comments, likes).
 * Manages inbox state and provides reactive updates via StateFlow.
 */
@HiltViewModel
class InboxViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    sealed class InboxUiState {
        object Loading : InboxUiState()
        data class Success(
            val unreadCount: Int,
            val messages: List<InboxItem>,
            val hasMore: Boolean
        ) : InboxUiState()
        data class Error(val message: String) : InboxUiState()
    }

    data class InboxItem(
        val id: String,
        val subject: String?,
        val body: String?,
        val sender: String?,
        val recipient: String?,
        val subreddit: String?,
        val timestamp: Long,
        val isRead: Boolean,
        val messageType: MessageType
    )

    enum class MessageType {
        MESSAGE,
        COMMENT_REPLY,
        POST_REPLY,
        LIKE,
        OTHER
    }

    private val _state = MutableStateFlow<InboxUiState>(InboxUiState.Loading)
    val state: StateFlow<InboxUiState> = _state.asStateFlow()

    init {
        loadInbox()
    }

    /**
     * Load inbox messages.
     */
    fun loadInbox() {
        viewModelScope.launch {
            _state.value = InboxUiState.Loading
            try {
                val account = RedditAccountManager.getInstance(context).getDefaultAccount()
                if (account != null) {
                    // TODO: Implement actual inbox loading via CacheRequest
                    // For now, return empty state
                    _state.value = InboxUiState.Success(
                        unreadCount = 0,
                        messages = emptyList(),
                        hasMore = false
                    )
                } else {
                    _state.value = InboxUiState.Error("No account available")
                }
            } catch (e: Exception) {
                _state.value = InboxUiState.Error("Failed to load inbox: ${e.message}")
            }
        }
    }

    /**
     * Mark a message as read.
     */
    fun markAsRead(messageId: String) {
        viewModelScope.launch {
            try {
                // TODO: Implement actual mark-as-read via CacheRequest
            } catch (e: Exception) {
                _state.value = InboxUiState.Error("Failed to mark as read: ${e.message}")
            }
        }
    }

    /**
     * Mark all messages as read.
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                // TODO: Implement mark-all-as-read via CacheRequest
            } catch (e: Exception) {
                _state.value = InboxUiState.Error("Failed to mark all as read: ${e.message}")
            }
        }
    }

    /**
     * Send a message to a user.
     */
    fun sendMessage(recipient: String, subject: String, body: String) {
        viewModelScope.launch {
            try {
                // TODO: Implement message sending via CacheRequest
            } catch (e: Exception) {
                _state.value = InboxUiState.Error("Failed to send message: ${e.message}")
            }
        }
    }
}
