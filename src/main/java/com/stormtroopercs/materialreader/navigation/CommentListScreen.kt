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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
import android.content.ClipData
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.compose.theme.LocalComposeTheme
import com.stormtroopercs.materialreader.compose.ui.RRErrorView
import com.stormtroopercs.materialreader.common.PrefsUtility
import com.stormtroopercs.materialreader.fragments.ReportDialog
import com.stormtroopercs.materialreader.reddit.kthings.RedditIdAndType

/**
 * Comment list content with real data from [CommentListViewModel].
 *
 * Phase 7 (FINAL-DESIGN 7.x): each comment is a full-width, cardless
 * [CommentRow] (7.1) — a 16dp circular avatar, an author line
 * (`u/name • N points • age`, + `(edited)` when edited), a wrapped body, and
 * nesting by leading indent only (~16dp per level, no guide lines). Collapsed
 * threads render as a tappable `View more (N)` row that expands on tap (7.2).
 * A bottom comment-nav `FilterChip` row (7.4) and a Compose FAB that opens the
 * reply editor (7.5) round the screen out. Vote / save / report dispatch to
 * the ViewModel (Reddit `api/vote` / `api/save`); Copy link copies the comment
 * permalink; Reply navigates to the reply editor with the comment's full
 * `t1_…` id as the parent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealCommentListScreen(
    postId: String,
    onNavigateBack: () -> Unit,
    onReply: (CommentItem) -> Unit,
    onReplyToPost: () -> Unit
) {
    val viewModel: CommentListViewModel = hiltViewModel()
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val listTitle by viewModel.title.collectAsStateWithLifecycle()
    val expanding by viewModel.expanding.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboard.current
    val clipboardScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(postId) {
        viewModel.fetchComments(postId)
    }

    // Surface the result of the last comment action (vote/save) as a Snackbar,
    // then clear it so a repeat action re-triggers it.
    val actionResult by viewModel.actionResult.collectAsStateWithLifecycle()
    LaunchedEffect(actionResult) {
        actionResult?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionResult()
        }
    }

    // A comment action: upvote / downvote / save go to the ViewModel (which
    // hits the Reddit API); copy link copies the comment URL to the clipboard;
    // report opens the report dialog.
    fun onCommentAction(comment: CommentItem, action: CommentAction) {
        val activity = context as? AppCompatActivity
        when (action) {
            CommentAction.REPORT -> activity?.let {
                ReportDialog.show(it, RedditIdAndType(comment.fullName), comment.subreddit ?: "", isComment = true)
            }
            CommentAction.COPY_LINK -> comment.permalink?.let { perm ->
                clipboardScope.launch {
                    clipboardManager.setClipEntry(
                        ClipEntry(ClipData.newPlainText("Link", "https://www.reddit.com$perm"))
                    )
                }
            }
            else -> activity?.let { viewModel.performAction(it, comment, action) }
        }
    }

    // The signed-in username — drives the "Me" nav chip (7.4).
    val me = remember {
        RedditAccountManager.getInstance(context).defaultAccount?.username
    }

    // Comment-nav chip selection (7.4). "Me" filters to the account's own
    // comments; "New comments" sorts most-recent-first. The media chips
    // (Images / Links / Videos) are selectable toggles — their filtering needs
    // per-comment media metadata not yet parsed, so they select but do not
    // (yet) change the list.
    var selectedChip by remember { mutableStateOf("All") }
    val chips = listOf("Images", "Links", "Me", "New comments", "OP", "Threads", "Videos")

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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
        },
        floatingActionButton = {
            // Compose FAB (7.5): reply to the post.
            ExtendedFloatingActionButton(
                onClick = onReplyToPost,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Reply") }
            )
        },
        bottomBar = {
            // Comment-nav chip row (7.4).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chips.forEach { chip ->
                    FilterChip(
                        selected = selectedChip == chip,
                        onClick = { selectedChip = if (selectedChip == chip) "All" else chip },
                        label = { Text(chip, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (val state = uiState) {
            is CommentListUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    if (expanding) {
                        Text(
                            text = "Loading comments…",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }

            is CommentListUiState.Success -> {
                // Apply the nav-chip filters (7.4).
                var shown = state.comments
                if (selectedChip == "Me") {
                    me?.let { u -> shown = shown.filter { it.author == u } }
                }
                if (selectedChip == "New comments") {
                    shown = shown.sortedByDescending { it.createdUtcTimestamp }
                }

                CommentListContent(
                    postTitle = state.postTitle,
                    postAuthor = state.postAuthor,
                    comments = shown,
                    expanding = expanding,
                    onCommentAction = ::onCommentAction,
                    onExpandMore = { viewModel.expandMore(it) },
                    onReply = onReply,
                    onReplyToPost = onReplyToPost,
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
    expanding: Boolean,
    onCommentAction: (CommentItem, CommentAction) -> Unit,
    onExpandMore: (CommentItem) -> Unit,
    onReply: (CommentItem) -> Unit,
    onReplyToPost: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = LocalComposeTheme.current.postCard
    val listState = rememberLazyListState()
    val showAvatars = remember { PrefsUtility.appearance_user_show_avatars() }

    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        postTitle?.takeIf { it.isNotBlank() }?.let { title ->
            item {
                PostHeaderCard(
                    title = title,
                    author = postAuthor,
                    onReplyToPost = onReplyToPost
                )
            }
        }

        if (comments.isEmpty() && !expanding) {
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
                if (comment.collapsedReason == "MORE") {
                    ViewMoreRow(
                        count = comment.replyCount,
                        depth = comment.replyDepth,
                        expanding = expanding,
                        onClick = { onExpandMore(comment) }
                    )
                } else {
                    CommentRow(
                        comment = comment,
                        showAvatars = showAvatars,
                        onCommentAction = onCommentAction,
                        onReply = onReply
                    )
                }
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
    onReplyToPost: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onReplyToPost)
            .padding(horizontal = 16.dp, vertical = 14.dp)
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
}

/**
 * A single comment (FINAL-DESIGN 7.1): full-width, cardless. A 16dp circular
 * avatar (optional), an author line (`u/name • N points • age`, + `(edited)`),
 * a wrapped body, and a compact action row. Nesting is a leading indent of
 * ~16dp per level — no vertical guide lines.
 */
@Composable
private fun CommentRow(
    comment: CommentItem,
    showAvatars: Boolean,
    onCommentAction: (CommentItem, CommentAction) -> Unit,
    onReply: (CommentItem) -> Unit
) {
    var moreMenuExpanded by remember { mutableStateOf(false) }
    // Cap the indent so very deep threads stay readable.
    val indentLevels = minOf(comment.replyDepth, 8)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indentLevels * 16).dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
    ) {
        // 16dp circular avatar (7.1) — an initial avatar coloured by the
        // username hash; shown only when the setting is on.
        if (showAvatars) {
            CommentAvatar(name = comment.author, show = comment.author != null)
            Spacer(Modifier.width(10.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            // Author line.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (comment.author != null) "u/${comment.author}" else "[deleted]",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                comment.authorFlairText?.takeIf { it.isNotBlank() }?.let { flair ->
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = flair,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${formatScore(comment.score)} points",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = formatTimeAgo(comment.createdUtcTimestamp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                if (comment.edited) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "(edited)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Body.
            Spacer(Modifier.height(6.dp))
            Text(
                text = comment.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = Int.MAX_VALUE,
                overflow = TextOverflow.Visible
            )

            // Compact action row.
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ActionIcon(
                    icon = Icons.Default.ArrowUpward,
                    label = "Upvote",
                    onClick = { onCommentAction(comment, CommentAction.UPVOTE) }
                )
                Text(
                    text = formatScore(comment.score),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                ActionIcon(
                    icon = Icons.Default.ArrowDownward,
                    label = "Downvote",
                    onClick = { onCommentAction(comment, CommentAction.DOWNVOTE) }
                )
                if (comment.replyCount > 0) {
                    Spacer(Modifier.width(12.dp))
                    ActionIcon(
                        icon = Icons.AutoMirrored.Default.Message,
                        label = "${comment.replyCount} replies",
                        onClick = { onReply(comment) }
                    )
                } else {
                    Spacer(Modifier.width(12.dp))
                    ActionIcon(
                        icon = Icons.AutoMirrored.Default.Message,
                        label = "Reply",
                        onClick = { onReply(comment) }
                    )
                }
                Spacer(Modifier.width(12.dp))
                ActionIcon(
                    icon = Icons.Default.Flag,
                    label = "Report",
                    onClick = { onCommentAction(comment, CommentAction.REPORT) }
                )
                Box {
                    ActionIcon(
                        icon = Icons.Default.MoreVert,
                        label = "More",
                        onClick = { moreMenuExpanded = true }
                    )
                    DropdownMenu(
                        expanded = moreMenuExpanded,
                        onDismissRequest = { moreMenuExpanded = false }
                    ) {
                        val saveLabel = if (comment.saved) "Unsave" else "Save"
                        DropdownMenuItem(
                            text = { Text(saveLabel) },
                            onClick = {
                                moreMenuExpanded = false
                                onCommentAction(
                                    comment,
                                    if (comment.saved) CommentAction.UNSAVE else CommentAction.SAVE
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy link") },
                            onClick = {
                                moreMenuExpanded = false
                                onCommentAction(comment, CommentAction.COPY_LINK)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * A 16dp circular comment avatar (7.1): a single coloured initial derived
 * from the username. [show] is false for deleted/anonymous comments (no
 * avatar slot is drawn by the caller in that case).
 */
@Composable
private fun CommentAvatar(name: String?, show: Boolean) {
    if (!show || name == null) return
    val hue = (name.hashCode() and 0x7fffffff) % 360
    val color = Color(0.45f, 0.45f, 0.55f).let {
        // A muted, distinguishable fill keyed on the username.
        Color(
            red = 0.35f + 0.25f * kotlin.math.cos(Math.toRadians(hue.toDouble())).toFloat(),
            green = 0.35f + 0.25f * kotlin.math.cos(Math.toRadians((hue + 120).toDouble())).toFloat(),
            blue = 0.45f + 0.25f * kotlin.math.cos(Math.toRadians((hue + 240).toDouble())).toFloat()
        )
    }
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.first().uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

/**
 * A compact icon+label comment action (part of the 7.1 action row).
 */
@Composable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 4.dp, vertical = 0.dp
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/**
 * A collapsed-thread continuation row (7.2): `View more (N)`, full-width and
 * tappable, in place of the hidden children. Expands on tap.
 */
@Composable
private fun ViewMoreRow(
    count: Int,
    depth: Int,
    expanding: Boolean,
    onClick: () -> Unit
) {
    val indentLevels = minOf(depth, 8)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (indentLevels * 16).dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (expanding) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Loading…",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "View more ($count)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
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
