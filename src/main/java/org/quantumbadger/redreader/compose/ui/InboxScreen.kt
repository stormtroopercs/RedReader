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

package org.quantumbadger.redreader.compose.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.quantumbadger.redreader.navigation.InboxViewModel

/**
 * Compose Inbox Screen.
 * Displays messages, comments, and likes with read/unread status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onNavigateBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onSendMessage: () -> Unit,
    viewModel: InboxViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Inbox")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onMarkAllRead) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Mark all as read"
                        )
                    }
                    IconButton(onClick = onSendMessage) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Send message"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val uiState = state) {
            is InboxViewModel.InboxUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is InboxViewModel.InboxUiState.Success -> {
                InboxContent(
                    uiState = uiState,
                    onMarkAsRead = { viewModel.markAsRead(it.id) },
                    onSendMessage = { viewModel.sendMessage(it.recipient ?: "", "Re: ${it.subject ?: ""}", "") },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            is InboxViewModel.InboxUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadInbox() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxContent(
    uiState: InboxViewModel.InboxUiState.Success,
    onMarkAsRead: (InboxViewModel.InboxItem) -> Unit,
    onSendMessage: (InboxViewModel.InboxItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        if (uiState.unreadCount > 0) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "${uiState.unreadCount} unread message${if (uiState.unreadCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (uiState.messages.isEmpty()) {
            item {
                EmptyInboxState()
            }
        } else {
            items(uiState.messages, key = { it.id }) { message ->
                InboxMessageCard(
                    message = message,
                    onClick = { onMarkAsRead(message) },
                    onReply = { onSendMessage(message) }
                )
            }
        }

        if (uiState.hasMore) {
            item {
                LoadMoreButton()
            }
        }
    }
}

@Composable
private fun EmptyInboxState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Default.Message,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No messages yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "When someone sends you a message or replies to you,\nit will appear here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun InboxMessageCard(
    message: InboxViewModel.InboxItem,
    onClick: () -> Unit,
    onReply: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (message.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with sender and type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = getSenderText(message),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!message.isRead) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "New",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Message type icon
                Icon(
                    imageVector = getMessageTypeIcon(message.messageType),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Subject
            message.subject?.let { subject ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subject,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Body preview
            message.body?.let { body ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (!message.isRead) TextDecoration.None else TextDecoration.LineThrough
                )
            }

            // Footer with timestamp
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (message.messageType == InboxViewModel.MessageType.MESSAGE) {
                    TextButton(onClick = onReply) {
                        Text("Reply")
                    }
                }
            }
        }
    }
}

private fun getSenderText(message: InboxViewModel.InboxItem): String {
    return when (message.messageType) {
        InboxViewModel.MessageType.MESSAGE -> "From: ${message.sender ?: "Unknown"}"
        InboxViewModel.MessageType.COMMENT_REPLY -> "Comment reply on ${message.subreddit ?: "a subreddit"}"
        InboxViewModel.MessageType.POST_REPLY -> "Post reply on ${message.subreddit ?: "a subreddit"}"
        InboxViewModel.MessageType.LIKE -> "New like"
        InboxViewModel.MessageType.OTHER -> "Notification"
    }
}

private fun getMessageTypeIcon(messageType: InboxViewModel.MessageType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (messageType) {
        InboxViewModel.MessageType.MESSAGE -> Icons.AutoMirrored.Default.Message
        InboxViewModel.MessageType.COMMENT_REPLY -> Icons.Default.Comment
        InboxViewModel.MessageType.POST_REPLY -> Icons.Default.PostAdd
        InboxViewModel.MessageType.LIKE -> Icons.Default.Favorite
        InboxViewModel.MessageType.OTHER -> Icons.Default.Notifications
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> "${diff / (604_800_000 * 4)} weeks ago"
    }
}

@Composable
private fun LoadMoreButton() {
    Button(
        onClick = { /* TODO: Implement load more */ },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text("Load More")
    }
}
