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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.quantumbadger.redreader.compose.theme.LocalComposeTheme
import org.quantumbadger.redreader.compose.theme.StyledText
import org.quantumbadger.redreader.compose.ui.RRErrorView
import org.quantumbadger.redreader.common.time.TimestampUTC
import java.util.concurrent.TimeUnit

/**
 * Comment list content with real data from CommentListViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealCommentListScreen(
    postId: String,
    onNavigateBack: () -> Unit
) {
    val viewModel: CommentListViewModel = hiltViewModel()
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val listTitle by viewModel.title.collectAsStateWithLifecycle()

    LaunchedEffect(postId) {
        viewModel.fetchComments(postId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                scrollBehavior = null,
                title = {
                    Text(
                        text = listTitle.ifEmpty { "Comments" },
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is CommentListUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is CommentListUiState.Success -> {
                CommentListContent(
                    postTitle = state.postTitle,
                    postAuthor = state.postAuthor,
                    comments = state.comments,
                    moreCommentsAvailable = state.moreCommentsAvailable,
                    modifier = Modifier.padding(paddingValues)
                )
            }

            is CommentListUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        RRErrorView(error = state.error)
                        FilledTonalButton(
                            onClick = { viewModel.refresh() },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Actual comment list UI.
 */
@Composable
private fun CommentListContent(
    postTitle: String?,
    postAuthor: String?,
    comments: List<CommentItem>,
    moreCommentsAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    val theme = LocalComposeTheme.current.postCard
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        // Optional: show post header
        postTitle?.takeIf { it.isNotBlank() }?.let { title ->
            item {
                PostHeaderCard(
                    title = title,
                    author = postAuthor,
                    theme = theme
                )
            }
        }

        if (comments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No comments yet",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            items(comments, key = { it.id }) { comment ->
                CommentCard(
                    comment = comment,
                    theme = theme
                )
            }
        }
    }
}

/**
 * Post header card showing the original post title/author above the comment list.
 */
@Composable
private fun PostHeaderCard(
    title: String,
    author: String?,
    theme: org.quantumbadger.redreader.compose.theme.ComposeThemePostCard
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.backgroundColor)
            .clickable(onClick = {})
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis
        )

        author?.takeIf { it.isNotBlank() }?.let { auth ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Posted by u/$auth",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Divider
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp),
        onDraw = {
            drawLine(
                color = theme.iconColor.copy(alpha = 0.1f),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx()
            )
        }
    )
}

/**
 * Single comment card with voting, author, body, and replies.
 */
@Composable
private fun CommentCard(
    comment: CommentItem,
    theme: org.quantumbadger.redreader.compose.theme.ComposeThemePostCard
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.backgroundColor)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        // Left side: vote buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = "Upvote",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = formatScore(comment.score),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )

            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = "Downvote",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Main content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp)
        ) {
            // Author and metadata
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (comment.author != null) "u/${comment.author}" else "[deleted]",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                // Flair
                comment.authorFlairText?.takeIf { it.isNotBlank() }?.let { flair ->
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = flair,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Timestamp
                Spacer(Modifier.width(6.dp))
                Text(
                    text = formatTimeAgo(comment.createdUtcTimestamp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Body
            Spacer(Modifier.height(6.dp))
            Text(
                text = comment.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = Int.MAX_VALUE,
                overflow = TextOverflow.Visible
            )

            // Actions row
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {},
                    content = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.Message,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (comment.replyCount > 0) "${comment.replyCount} replies" else "Reply",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )

                Spacer(Modifier.width(16.dp))

                TextButton(
                    onClick = {},
                    content = {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Report", style = MaterialTheme.typography.labelSmall)
                    }
                )

                Spacer(Modifier.width(16.dp))

                TextButton(
                    onClick = {},
                    content = {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("More", style = MaterialTheme.typography.labelSmall)
                    }
                )
            }
        }
    }

    // Divider
    androidx.compose.foundation.Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 60.dp),
        onDraw = {
            drawLine(
                color = theme.iconColor.copy(alpha = 0.1f),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx()
            )
        }
    )
}

private fun formatScore(score: Int): String {
    return when {
        score >= 1_000_000 -> String.format("%.1fM", score / 1_000_000.0)
        score >= 1_000 -> String.format("%.1fK", score / 1_000.0)
        else -> score.toString()
    }
}

private fun formatTimeAgo(timestampSeconds: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - timestampSeconds

    return when {
        diff < 60 -> "$diff seconds ago"
        diff < 3600 -> "${diff / 60} minutes ago"
        diff < 86400 -> "${diff / 3600} hours ago"
        diff < 604800 -> "${diff / 86400} days ago"
        else -> {
            val date = java.util.Date(timestampSeconds * 1000)
            java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                .format(date)
        }
    }
}
