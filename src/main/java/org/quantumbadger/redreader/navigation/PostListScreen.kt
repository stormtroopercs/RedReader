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

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.compose.net.NetRequestStatus
import org.quantumbadger.redreader.compose.net.fetchImage
import org.quantumbadger.redreader.compose.theme.ComposeThemePostCard
import org.quantumbadger.redreader.compose.theme.LocalComposeTheme
import org.quantumbadger.redreader.compose.ui.RRErrorView
import org.quantumbadger.redreader.common.LinkHandler
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.fragments.ReportDialog
import org.quantumbadger.redreader.reddit.PostSort

/**
 * Post list screen composable.
 * Displays posts from a subreddit with real data from PostListViewModel.
 * Uses Material 3 components and the existing Compose theme system.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealPostListScreen(
    subreddit: String,
    searchQuery: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToCommentList: (String) -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToPostSubmit: () -> Unit
) {
    val viewModel: PostListViewModel = hiltViewModel()
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val currentSort by viewModel.sortBy.collectAsStateWithLifecycle()
    val listTitle by viewModel.title.collectAsStateWithLifecycle()

    LaunchedEffect(subreddit, searchQuery) {
        viewModel.fetchPosts(subreddit, searchQuery)
    }
    val theme = LocalComposeTheme.current.postCard

    var sortByMenuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Surface the result of the last post action (vote / save / hide) as a
    // Snackbar, then clear it so a repeat action re-triggers it.
    val actionResult by viewModel.actionResult.collectAsStateWithLifecycle()
    LaunchedEffect(actionResult) {
        actionResult?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearActionResult()
        }
    }

    // A post action: votes / save / hide / unhide go to the ViewModel (which
    // hits the Reddit API); report opens the report dialog; share hands the
    // permalink to the OS share sheet.
    fun onPostAction(post: PostItem, action: PostAction) {
        val activity = context as? AppCompatActivity ?: return
        when (action) {
            PostAction.REPORT -> ReportDialog.show(
                activity,
                org.quantumbadger.redreader.reddit.kthings.RedditIdAndType(post.id),
                post.subreddit,
                isComment = false
            )
            PostAction.SHARE -> LinkHandler.shareText(
                activity,
                post.title,
                "https://www.reddit.com${post.permalink}"
            )
            else -> viewModel.performAction(activity, post, action)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                scrollBehavior = null,
                title = {
                    Text(
                        text = listTitle.ifEmpty { "r/$subreddit" },
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
                    IconButton(onClick = viewModel::refresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                    IconButton(onClick = { sortByMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.Sort,
                            contentDescription = "Sort"
                        )
                    }
                    DropdownMenu(
                        expanded = sortByMenuExpanded,
                        onDismissRequest = { sortByMenuExpanded = false }
                    ) {
                        val sortOptions = listOf(
                            PostSort.HOT to "Hot",
                            PostSort.NEW to "New",
                            PostSort.RISING to "Rising",
                            PostSort.TOP_ALL to "Top (All time)",
                            PostSort.BEST to "Best"
                        )
                        sortOptions.forEach { (sort, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        fontWeight =
                                            if (sort == currentSort)
                                                FontWeight.Bold
                                            else
                                                FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.setSortBy(sort)
                                    sortByMenuExpanded = false
                                }
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToPostSubmit) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Submit"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is PostListUiState.Loading -> {
                    if (state.isInitialLoad) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is PostListUiState.Success -> {
                    PostListContent(
                        posts = state.posts,
                        theme = theme,
                        onPostClick = { post ->
                            onNavigateToCommentList(post.id)
                        },
                        onAuthorClick = { author ->
                            onNavigateToUserProfile(author)
                        },
                        onPostAction = ::onPostAction
                    )
                }

                is PostListUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
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
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Actual list of posts.
 */
@Composable
private fun PostListContent(
    posts: List<PostItem>,
    theme: ComposeThemePostCard,
    onPostClick: (PostItem) -> Unit,
    onAuthorClick: (String) -> Unit,
    onPostAction: (PostItem, PostAction) -> Unit
) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (posts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No posts found",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            items(posts, key = { it.id }) { post ->
                PostItemCard(
                    modifier = Modifier.animateItem(),
                    post = post,
                    theme = theme,
                    onClick = { onPostClick(post) },
                    onAuthorClick = onAuthorClick,
                    onPostAction = onPostAction
                )
            }
        }
    }
}

/**
 * A single post card with title, author, score, thumbnail, and metadata.
 */
@Composable
private fun PostItemCard(
    modifier: Modifier = Modifier,
    post: PostItem,
    theme: ComposeThemePostCard,
    onClick: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onPostAction: (PostItem, PostAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(theme.backgroundColor)
            .clickable(onClick = onClick)
    ) {
        // Thumbnail/media preview
        post.thumbnail?.let { thumbnail ->
            ThumbnailPreview(
                uri = thumbnail,
                theme = theme,
                isVideo = post.isVideo,
                isGallery = false
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left side: vote buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.width(44.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = { onPostAction(post, PostAction.UPVOTE) }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Upvote",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = formatScore(post.score),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = { onPostAction(post, PostAction.DOWNVOTE) }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Downvote",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Main content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                // Title
                Text(
                    text = post.title ?: "Untitled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                // Meta: author, comment count, time, flair
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    post.author?.takeIf { it.isNotBlank() }?.let { author ->
                        Text(
                            text = author,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { onAuthorClick(author) }
                        )
                    }

                    Text(
                        text = "  ·  ${post.numComments} comments",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "  ·  ${formatTimeAgo(post.createdUtc)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Flair
                    post.linkFlairText?.takeIf { it.isNotBlank() }?.let { flair ->
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.shapes.small
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = flair,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Selftext preview
                post.selftext?.takeIf { it.isNotBlank() }?.let { selftext ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = selftext.take(200) + if (selftext.length > 200) "..." else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // More menu
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More options"
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = {
                            expanded = false
                            onPostAction(post, PostAction.SHARE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (post.saved) "Unsave" else "Save") },
                        onClick = {
                            expanded = false
                            onPostAction(
                                post,
                                if (post.saved) PostAction.UNSAVE else PostAction.SAVE
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Report") },
                        onClick = {
                            expanded = false
                            onPostAction(post, PostAction.REPORT)
                        }
                    )
                }
            }
        }

        // Bottom divider
        if (post.linkFlairText != null || post.isCrosspost) {
            PostDivider(theme = theme)
        }
    }
}

/**
 * Media/thumbnail preview. When [size] is null, renders a full-width 16:9
 * preview; otherwise a fixed-size (e.g. avatar) preview clipped to [shape].
 */
@Composable
private fun ThumbnailPreview(
    uri: String,
    theme: ComposeThemePostCard,
    modifier: Modifier = Modifier,
    isVideo: Boolean = false,
    isGallery: Boolean = false,
    size: androidx.compose.ui.unit.Dp? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp),
    contentScale: ContentScale = ContentScale.Crop
) {
    val backgroundModifier = if (size != null) {
        modifier.size(size).clip(shape)
    } else {
        modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(shape)
    }

    Box(
        modifier = backgroundModifier
            .background(theme.previewImageBackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        val data by fetchImage(
            UriString(uri),
            scaleToMaxAxis = 640
        )

        when (val it = data) {
            is NetRequestStatus.Connecting -> {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            }

            is NetRequestStatus.Downloading -> {
                CircularProgressIndicator(
                    modifier = Modifier.padding(24.dp),
                    progress = { it.fractionComplete }
                )
            }

            is NetRequestStatus.Failed -> {
                // Empty box for failed images
            }

            is NetRequestStatus.Success -> {
                Image(
                    bitmap = it.result.data,
                    contentDescription = null,
                    contentScale = contentScale,
                    modifier = Modifier
                        .fillMaxSize()
                )

                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.icon_play),
                            contentDescription = "Play video",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
        // isGallery reserved for a future gallery badge
        if (isGallery) { /* no-op */ }
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

@Composable
private fun PostDivider(theme: ComposeThemePostCard) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp),
        onDraw = {
            drawLine(
                color = theme.iconColor.copy(alpha = 0.1f),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 1.dp.toPx()
            )
        }
    )
}
