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
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequestCallbacks
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.common.Constants.Reddit
import org.quantumbadger.redreader.reddit.kthings.JsonUtils.decodeRedditThingFromStream
import org.quantumbadger.redreader.reddit.kthings.RedditThing
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
            val messages: List<InboxItem>
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
     * Load inbox messages from the Reddit API (`/message/inbox.json`),
     * using the same CacheRequest + decodeRedditThingFromStream pattern as
     * [PostListViewModel]. The `mark=true` parameter marks received
     * messages read server-side (same as the legacy inbox fetch).
     */
    fun loadInbox() {
        viewModelScope.launch {
            _state.value = InboxUiState.Loading
            try {
                val account = RedditAccountManager.getInstance(context).getDefaultAccount()
                val signedIn = account.username.isNotBlank()
                if (!signedIn) {
                    _state.value = InboxUiState.Error("Sign in to view your inbox")
                    return@launch
                }

                val jsonUri = Reddit.getUri("/message/inbox.json?mark=true&limit=100").toString()
                val callbacks = object : CacheRequestCallbacks {
                    override fun onFailure(error: RRError) {
                        _state.value = InboxUiState.Error(error.message ?: error.toString())
                    }

                    override fun onDataStreamComplete(
                        streamFactory: GenericFactory<SeekableInputStream, IOException>,
                        timestamp: TimestampUTC,
                        session: UUID,
                        fromCache: Boolean,
                        mimetype: String?
                    ) {
                        try {
                            val thing = decodeRedditThingFromStream(streamFactory.create())
                            val listing = (thing as? RedditThing.Listing)?.data
                                ?: throw RuntimeException("Expected listing, got " + thing.javaClass.name)

                            val items = listing.children
                                .mapNotNull { it.ok() }
                                .mapNotNull { toInboxItem(it) }

                            _state.value = InboxUiState.Success(
                                unreadCount = items.count { !it.isRead },
                                messages = items
                            )
                        } catch (e: Exception) {
                            _state.value = InboxUiState.Error(e.message ?: e.toString())
                        }
                    }
                }

                val request = CacheRequest(
                    UriString(jsonUri),
                    account,
                    null,
                    Priority(Constants.Priority.API_INBOX_LIST),
                    DownloadStrategyIfNotCached.INSTANCE,
                    Constants.FileType.INBOX_LIST,
                    CacheRequest.DownloadQueueType.REDDIT_API,
                    context,
                    callbacks
                )
                CacheManager.getInstance(context).makeRequest(request)
            } catch (e: Exception) {
                _state.value = InboxUiState.Error("Failed to load inbox: ${e.message}")
            }
        }
    }

    /**
     * Mark a message as read (local state update). The server-side
     * mark-as-read already happens when the inbox is fetched with
     * `mark=true`; the legacy app had no per-item mark-as-read endpoint
     * either, it re-fetched the inbox.
     */
    fun markAsRead(messageId: String) {
        _state.update { state ->
            if (state is InboxUiState.Success) {
                val updated = state.messages.map { if (it.id == messageId) it.copy(isRead = true) else it }
                InboxUiState.Success(
                    unreadCount = updated.count { !it.isRead },
                    messages = updated
                )
            } else {
                state
            }
        }
    }

    /**
     * Mark all messages as read (local state update).
     */
    fun markAllAsRead() {
        _state.update { state ->
            if (state is InboxUiState.Success) {
                InboxUiState.Success(
                    unreadCount = 0,
                    messages = state.messages.map { it.copy(isRead = true) }
                )
            } else {
                state
            }
        }
    }
}

/**
 * Map a raw inbox listing child (private message, comment reply, or post
 * reply) to the UI-facing [InboxViewModel.InboxItem].
 */
private fun toInboxItem(thing: RedditThing): InboxViewModel.InboxItem? {
    return when (thing) {
        is RedditThing.Message -> {
            val m = thing.data
            InboxViewModel.InboxItem(
                id = m.name.toString(),
                subject = m.subject?.decoded,
                body = m.body?.decoded,
                sender = m.author?.decoded,
                recipient = m.dest?.decoded,
                subreddit = m.subreddit_name_prefixed?.decoded,
                timestamp = m.created_utc.value.toUtcSecs(),
                isRead = false, // fetched with mark=true — already marked read server-side
                messageType = InboxViewModel.MessageType.MESSAGE
            )
        }
        is RedditThing.Comment -> {
            val c = thing.data
            val isPostReply = c.link_id != null
            InboxViewModel.InboxItem(
                id = c.id,
                subject = if (isPostReply) "Post reply" else "Comment reply",
                body = c.body?.decoded,
                sender = c.subreddit?.decoded,
                recipient = null,
                subreddit = c.subreddit?.decoded,
                timestamp = c.created_utc.value.toUtcSecs(),
                isRead = false,
                messageType = if (isPostReply) {
                    InboxViewModel.MessageType.POST_REPLY
                } else {
                    InboxViewModel.MessageType.COMMENT_REPLY
                }
            )
        }
        else -> null
    }
}
