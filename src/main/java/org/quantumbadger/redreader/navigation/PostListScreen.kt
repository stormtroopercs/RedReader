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

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScrollConnection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.compose.net.NetRequestStatus
import org.quantumbadger.redreader.compose.net.fetchImage
import org.quantumbadger.redreader.compose.ui.RRErrorView
import org.quantumbadger.redreader.compose.theme.LocalComposeTheme
import org.quantumbadger.redreader.compose.theme.StyledText
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.invokeIf
import org.quantumbadger.redreader.common.invokeIfNotNull
import org.quantumbadger.redreader.common.invokeIfNotNull
import org.quantumbadger.redreader.image.ImageUrlInfo
import org.quantumbadger.redreader.common.time.TimestampUTC
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FlagOutlined
import org.quantumbadger.redreader.common.Optional
import kotlin.math.roundToInt

/**
 * Post list screen composable.
 * Displays posts from a subreddit with real data from PostListViewModel.
 * Uses Material 3 components and the existing Compose theme system.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealPostListScreen(
    subreddit: String,
    onNavigateBack: () -> Unit,
    onNavigateToCommentList: (String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: PostListViewModel = hiltViewModel()
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val theme = LocalComposeTheme.current.postCard

    var sortByMenuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Refresh on sort change
    val currentSort by remember(viewModel.sortBy) {
        mutableIntStateOf(PrefsUtility.prefPostsSort(context).ordinal)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                scrollBehavior = null,
                title = {
                    Text(
                        text = "r/$subreddit",
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
                    IconButton(onClick = viewModel::refresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                    IconButton(onClick = { sortByMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort"
                        )
                    }
                    DropdownMenu(
                        expanded = sortByMenuExpanded,
                        onDismissRequest = { sortByMenuExpanded = false }
                    ) {
                        val sortOptions = listOf(
                            "Best",
                            "Hot",
                            "New",
                            "Top",
                            "Rising"
                        )
                        sortOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    val newSort = when (option) {
                                        "Best" -> PrefsUtility.SortType.BEST
                                        "Hot" -> PrefsUtility.SortType.HOT
                                        "New" -> PrefsUtility.SortType.NEW
                                        "Top" -> PrefsUtility.SortType.TOP
                                        "Rising" -> PrefsUtility.SortType.RISING
                                        else -> PrefsUtility.SortType.BEST
                                    }
                                    PrefsUtility.prefPostsSort(context, newSort)
                                    sortByMenuExpanded = false
                                }
                            )
                        }
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
                        // Centered loading indicator for initial load
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
                        onRetry = { viewModel.fetchPosts(subreddit) }
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
                                onClick = { viewModel.fetchPosts(subreddit) },
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
}

/**
 * Actual list of posts with pull-to-refresh capability.
 */
@Composable
private fun PostListContent(
    posts: List<PostItem>,
    theme: org.quantumbadger.redreader.compose.theme.ComposeThemePostCard,
    onPostClick: (PostItem) -> Unit,
    onRetry: () -> Unit
) {
    var pullRefreshRefreshing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

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
                    post = post,
                    theme = theme,
                    onClick = { onPostClick(post) }
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
    post: PostItem,
    theme: org.quantumbadger.redreader.compose.theme.ComposeThemePostCard,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.backgroundColor)
            .clickable(onClick = onClick)
    ) {
        // Thumbnail/media preview
        post.thumbnailUri?.let { thumbnailUri ->
            ThumbnailPreview(
                uri = thumbnailUri,
                isVideo = post.isVideo,
                isGallery = post.isGallery,
                width = theme.previewImageWidth,
                height = theme.previewImageHeight,
                contentScale = ContentScale.Crop
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
                modifier = Modifier.width(40.dp)
            ) {
                // Upvote arrow (placeholder)
                Icon(
                    painter = painterResource(R.drawable.chevron_up_dark),
                    contentDescription = "Upvote",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )

                Text(
                    text = formatScore(post.score),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )

                // Downvote arrow (placeholder)
                Icon(
                    painter = painterResource(R.drawable.chevron_down_dark),
                    contentDescription = "Downvote",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
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

                // Meta: author, subreddit, time, score
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Author avatar (placeholder)
                    post.authorUri?.let { authorUrl ->
                        ThumbnailPreview(
                            uri = authorUrl,
                            size = 20.dp,
                            shape = CircleShape,
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(6.dp))
                    }

                    val timeAgo = post.createdUtcFormatted
                    Text(
                        text = "p${post.numComments}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = " · ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = timeAgo,
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
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Share") },
                        onClick = { /* TODO: share post */ expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Save") },
                        onClick = { /* TODO: save post */ expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Report") },
                        onClick = { /* TODO: report post */ expanded = false }
                    )
                }
            }
        }

        // Bottom divider
        if (post.linkFlairText != null || post.isCrosspost) {
            Divider(theme = theme)
        }
    }
}

@Composable
private fun ThumbnailPreview(
    uri: String,
    isVideo: Boolean = false,
    isGallery: Boolean = false,
    width: Int,
    height: Int,
    contentScale: ContentScale
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(width.dp)
            .invokeIf(isVideo || isGallery) {
                background(MaterialTheme.colorScheme.primaryContainer)
            },
        contentAlignment = Alignment.Center
    ) {
        val imageUri = uri
        val data by fetchImage(
            org.quantumbadger.redreader.common.UriString.from(
                java.net.URI.create(imageUri)
            ),
            scaleToMaxAxis = width
        )

        when (val it = data) {
            is org.quantumbadger.redreader.compose.net.NetRequestStatus.Connecting -> {
                CircularProgressIndicator(modifier = Modifier.padding(24.dp))
            }

            is org.quantumbadger.redreader.compose.net.NetRequestStatus.Downloading -> {
                CircularProgressIndicator(
                    modifier = Modifier.padding(24.dp),
                    progress = { it.fractionComplete }
                )
            }

            is org.quantumbadger.redreader.compose.net.NetRequestStatus.Failed -> {
                // Empty box for failed images
            }

            is org.quantumbadger.redreader.compose.net.NetRequestStatus.Success -> {
                Image(
                    bitmap = it.result.data,
                    contentDescription = null,
                    contentScale = contentScale,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(width.toFloat() / height.toFloat())
                )

                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(0.3f)),
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
    }
}

private fun formatScore(score: Int): String {
    return when {
        score >= 1_000_000 -> String.format("%.1fM", score / 1_000_000.0)
        score >= 1_000 -> String.format("%.1fK", score / 1_000.0)
        else -> score.toString()
    }
}

@Composable
private fun Divider(theme: org.quantumbadger.redreader.compose.theme.ComposeThemePostCard) {
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
