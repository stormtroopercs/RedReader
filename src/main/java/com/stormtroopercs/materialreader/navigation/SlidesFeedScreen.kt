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

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stormtroopercs.materialreader.compose.net.NetRequestStatus
import com.stormtroopercs.materialreader.compose.net.fetchImage
import com.stormtroopercs.materialreader.compose.ui.ActionBarButton
import com.stormtroopercs.materialreader.compose.ui.ActionBarRow
import com.stormtroopercs.materialreader.compose.ui.RRErrorView
import com.stormtroopercs.materialreader.common.LinkHandler
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.fragments.ReportDialog
import com.stormtroopercs.materialreader.reddit.PostSort

/**
 * The reference's signature **swipe feed** ("Slides", FINAL-DESIGN Phase 3):
 * a `VerticalPager` of one post per screen — full-bleed media with a bottom
 * gradient scrim, an overlay of avatar + meta + title + scrollable selftext,
 * and the six equal-weight action buttons (score in the upvote button, comment
 * count in the comment button).
 *
 * The top toolbar is **hidden at rest** (status bar only, per DESIGN.md §5):
 * pulling down on the first slide reveals it — it carries the back arrow, the
 * community pill (avatar + name + chevron + subscriber count; tap opens the
 * community picker) and the right-side Search / Sort / More actions. Swiping
 * to another slide (or up, off the first) hides it again.
 *
 * Media renderers: image now; NSFW/spoiler = tap-to-reveal blur; video = a
 * placeholder (inline Media3 is a follow-up).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealSlidesFeedScreen(
	subreddit: String,
	searchQuery: String? = null,
	onNavigateBack: () -> Unit,
	onNavigateToCommentList: (String) -> Unit,
	onNavigateToUserProfile: (String) -> Unit,
	onNavigateToPostSubmit: () -> Unit,
	onNavigateToSubredditSearch: () -> Unit,
	/** Open the default account's own profile (More actions → Profile). */
	onNavigateToProfile: () -> Unit = {},
	/** Jump straight to a (random) post's thread (More actions → Random). */
	onNavigateToRandomPost: (String) -> Unit = {},
	/** Open the account's saved list (More actions → Saved). */
	onNavigateToSaved: () -> Unit = {},
	/** Open an arbitrary listing path (More actions → custom slot). */
	onOpenListing: (String) -> Unit = {},
	/** Open the feed's community detail (the community pill tap, Phase 6.3). */
	onOpenCommunity: (String) -> Unit = {},
	/** Open Settings (More actions → Settings). */
	onNavigateToSettings: () -> Unit = {},
	/** Re-enter this feed as the list view (Change View → a list mode). */
	onNavigateToList: () -> Unit = {},
	/** Open the license view (More actions → About → License). */
	onOpenLicense: () -> Unit = {},
	/** Open a post's media in the full-screen viewer (media tap). */
	onOpenMedia: (PostItem) -> Unit = {},
) {
	val viewModel: PostListViewModel = hiltViewModel()
	val uiState by viewModel.state.collectAsStateWithLifecycle()
	val listTitle by viewModel.title.collectAsStateWithLifecycle()
	val community by viewModel.community.collectAsStateWithLifecycle()
	val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
	val context = LocalContext.current
	val snackbarHostState = remember { SnackbarHostState() }

	var sortDialogOpen by remember { mutableStateOf(false) }
	var moreActionsOpen by remember { mutableStateOf(false) }
	var changeViewOpen by remember { mutableStateOf(false) }
	var aboutOpen by remember { mutableStateOf(false) }
	val toolbarVisible = remember { mutableStateOf(false) }

	LaunchedEffect(subreddit, searchQuery) {
		viewModel.fetchPosts(subreddit, searchQuery)
	}

	val actionResult by viewModel.actionResult.collectAsStateWithLifecycle()
	LaunchedEffect(actionResult) {
		actionResult?.let { msg ->
			snackbarHostState.showSnackbar(msg)
			viewModel.clearActionResult()
		}
	}

	fun onPostAction(post: PostItem, action: PostAction) {
		val activity = context as? AppCompatActivity ?: return
		when (action) {
			PostAction.REPORT -> ReportDialog.show(
				activity,
				com.stormtroopercs.materialreader.reddit.kthings.RedditIdAndType(post.id),
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

	Box(modifier = Modifier.fillMaxSize()) {
		when (val state = uiState) {
			is PostListUiState.Loading -> {
				if (state.isInitialLoad) {
					Box(
						modifier = Modifier.fillMaxSize().padding(32.dp),
						contentAlignment = Alignment.Center,
					) {
						CircularProgressIndicator()
					}
				}
			}

			is PostListUiState.Error -> {
				Box(
					modifier = Modifier.fillMaxSize().padding(16.dp),
					contentAlignment = Alignment.Center,
				) {
					RRErrorView(error = state.error)
				}
			}

			is PostListUiState.Success -> {
				val posts = state.posts
				if (posts.isEmpty()) {
					Box(
						modifier = Modifier.fillMaxSize().padding(32.dp),
						contentAlignment = Alignment.Center,
					) {
						Text("No posts found")
					}
				} else {
					val pagerState = rememberPagerState(pageCount = { posts.size })

					// The collapsing toolbar: hidden at rest; revealed by a
					// downward drag while on the first slide (the reference's
					// "scroll-down reveals it"). Swiping to another slide hides
					// it again.
					val toolbarVisible = remember { mutableStateOf(false) }
					LaunchedEffect(pagerState) {
						snapshotFlow { pagerState.currentPage }
							.collect { page ->
								if (page != 0) toolbarVisible.value = false
							}
					}

					VerticalPager(
						state = pagerState,
						modifier = Modifier
							.fillMaxSize()
							.pointerInput(Unit) {
								// Observe vertical drag in the initial pass (the
								// pager still owns the gesture): any pull-down from
								// the first slide reveals the toolbar.
								detectVerticalDragGestures(
									onVerticalDrag = { _, dragAmount ->
										if (dragAmount > 0f &&
											pagerState.currentPage == 0 &&
											!toolbarVisible.value
										) {
											toolbarVisible.value = true
										}
									},
									onDragEnd = {},
									onDragCancel = {},
								)
							},
					) { page ->
						SlidePost(
							post = posts[page],
							modifier = Modifier.fillMaxSize(),
							onPostClick = { onNavigateToCommentList(posts[page].id) },
							onAuthorClick = onNavigateToUserProfile,
							onPostAction = ::onPostAction,
							onMediaClick = { onOpenMedia(posts[page]) },
						)
					}

					// The revealed toolbar (community pill + actions), sliding
					// down over the top of the slide.
					AnimatedVisibility(
						visible = toolbarVisible.value,
						enter = slideInVertically(initialOffsetY = { -it }),
						exit = slideOutVertically(targetOffsetY = { -it }),
						modifier = Modifier.fillMaxWidth(),
					) {
						SlidesToolbar(
								title = listTitle.ifEmpty { "r/$subreddit" },
								community = community,
								onSortMenuToggle = { sortDialogOpen = true },
								onMoreActionsToggle = { moreActionsOpen = true },
								onBack = onNavigateBack,
								onSearch = onNavigateToSubredditSearch,
								onCommunityTap = { onOpenCommunity(subreddit) },
								onRefresh = { viewModel.refresh() },
								onSubmit = onNavigateToPostSubmit,
								onDismiss = { toolbarVisible.value = false },
						)
					}
				}
			}
		}

		// The reference's 9-option sort dialog (Active = the listing default),
		// opened from the collapsing toolbar's Sort icon.
		if (sortDialogOpen) {
			SortOptionsDialog(
				currentId = sortOption.id,
				onDismiss = { sortDialogOpen = false },
				onSelected = { option ->
					viewModel.setSortOption(option)
					sortDialogOpen = false
				},
			)
		}

		// The "More actions" grid (FINAL-DESIGN Phase 5).
		if (moreActionsOpen) {
			MoreActionsSheet(
				posts = (uiState as? PostListUiState.Success)?.posts ?: emptyList(),
				onDismiss = { moreActionsOpen = false },
				onNavigateToSearch = {
					moreActionsOpen = false
					onNavigateToSubredditSearch()
				},
				onNavigateToProfile = {
					moreActionsOpen = false
					onNavigateToProfile()
				},
				onHideReadToggled = {
					// The pref just flipped; refetch so the slides reflect it.
					viewModel.refresh()
				},
				onOpenAbout = {
					moreActionsOpen = false
					aboutOpen = true
				},
				onNavigateToSubmit = {
					moreActionsOpen = false
					onNavigateToPostSubmit()
				},
				onNavigateToRandomPost = { postId ->
					moreActionsOpen = false
					onNavigateToRandomPost(postId)
				},
				onNavigateToSettings = {
					moreActionsOpen = false
					onNavigateToSettings()
				},
				onOpenChangeView = {
					moreActionsOpen = false
					changeViewOpen = true
				},
				onNavigateToSaved = {
					moreActionsOpen = false
					onNavigateToSaved()
				},
				onRefresh = {
					moreActionsOpen = false
					viewModel.refresh()
				},
				onOpenListing = { path ->
					moreActionsOpen = false
					onOpenListing(path)
				},
				onOpenLicense = { aboutOpen = false; onOpenLicense() },
			)
		}

		// Change View (from the grid): the card modes; picking a list mode
		// re-enters this feed as the list view.
		if (changeViewOpen) {
			ChangeViewSheet(
				current = com.stormtroopercs.materialreader.settings.types.PostViewMode.SLIDES,
				onDismiss = { changeViewOpen = false },
				onSelect = { mode ->
					FeedPreferences.setViewModeFor(
						feedIdFor(subreddit, searchQuery),
						mode,
					)
					changeViewOpen = false
					if (mode != com.stormtroopercs.materialreader.settings.types.PostViewMode.SLIDES) {
						// Leave the swipe feed for the list view.
						onNavigateToList()
					}
				},
			)
		}

		// The About dialog (More actions → About).
		if (aboutOpen) {
			AboutDialog(
				onDismiss = { aboutOpen = false },
				onOpenLicense = onOpenLicense,
			)
		}

		SnackbarHost(
			snackbarHostState,
			modifier = Modifier.align(Alignment.BottomCenter),
		)
		}
}

/**
 * The revealed top toolbar: back arrow + the **community pill** (avatar +
 * name + chevron + subscriber count; tap opens the community picker) +
 * right-side Search / Sort / More actions. Semi-transparent over the media.
 */
@Composable
private fun SlidesToolbar(
	title: String,
	community: CommunityInfo?,
	onSortMenuToggle: () -> Unit,
	onMoreActionsToggle: () -> Unit,
	onBack: () -> Unit,
	onSearch: () -> Unit,
	onCommunityTap: () -> Unit,
	onRefresh: () -> Unit,
	onSubmit: () -> Unit,
	onDismiss: () -> Unit,
) {
	Surface(
		color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
		contentColor = MaterialTheme.colorScheme.onSurface,
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(horizontal = 4.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			IconButton(onClick = onBack) {
				Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
			}

			// The community pill: avatar + name + chevron + subscriber count.
			CommunityPill(
				name = community?.name ?: title.removePrefix("r/"),
				subscribers = community?.subscribers,
				modifier = Modifier.weight(1f),
				onClick = onCommunityTap,
			)

			IconButton(onClick = onSearch) {
				Icon(Icons.Filled.Search, contentDescription = "Search")
			}
			// The reference's Sort opens the 9-option dialog (Phase 4.5), not a
			// menu.
			IconButton(onClick = onSortMenuToggle) {
				Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
			}
			IconButton(onClick = onRefresh) {
				Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
			}
			IconButton(onClick = onSubmit) {
				Icon(Icons.Filled.AddComment, contentDescription = "Submit")
			}
			// The reference's power-user surface (FINAL-DESIGN Phase 5).
			IconButton(onClick = onMoreActionsToggle) {
				Icon(Icons.Filled.MoreVert, contentDescription = "More actions")
			}
		}
	}
}

/**
 * The reference's community pill (DESIGN.md §5): a tappable capsule with a
 * circular avatar, the community name, a down chevron and the subscriber
 * count. Tap = community picker.
 */
@Composable
private fun CommunityPill(
	name: String,
	subscribers: Int?,
	modifier: Modifier = Modifier,
	onClick: () -> Unit,
) {
	Surface(
		shape = CircleShape,
		color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
		modifier = modifier
			.padding(horizontal = 8.dp, vertical = 4.dp)
			.clickable(onClick = onClick),
	) {
		Row(
			modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Box(
				modifier = Modifier
					.size(24.dp)
					.clip(CircleShape)
					.background(MaterialTheme.colorScheme.primaryContainer),
				contentAlignment = Alignment.Center,
			) {
				Text(
					text = name.firstOrNull()?.uppercase() ?: "?",
					color = MaterialTheme.colorScheme.onPrimaryContainer,
					fontWeight = FontWeight.Bold,
				)
			}
			Spacer(Modifier.width(6.dp))
			Text(
				text = name,
				fontWeight = FontWeight.SemiBold,
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
			)
			Spacer(Modifier.width(4.dp))
			Icon(
				imageVector = Icons.Filled.KeyboardArrowDown,
				contentDescription = null,
			)
			subscribers?.let {
				Spacer(Modifier.width(6.dp))
				Text(
					text = formatCompact(it) + " subscribers",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					maxLines = 1,
				)
			}
		}
	}
}

/**
 * One slide: full-bleed media + bottom scrim + overlay (avatar, meta, title,
 * scrollable selftext) + the six-button action bar.
 */
@Composable
private fun SlidePost(
	post: PostItem,
	modifier: Modifier = Modifier,
	onPostClick: () -> Unit,
	onAuthorClick: (String) -> Unit,
	onPostAction: (PostItem, PostAction) -> Unit,
	onMediaClick: () -> Unit = {},
) {
	var moreExpanded by remember { mutableStateOf(false) }

	Box(modifier = modifier.fillMaxSize()) {
		// Full-bleed media.
		SlideMedia(
			post = post,
			modifier = Modifier.fillMaxSize(),
			onReveal = { /* NSFW reveal toggled in SlideMedia */ },
			onMediaClick = onMediaClick,
		)

		// Bottom gradient scrim for legibility.
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.BottomCenter)
				.height(320.dp)
				.background(
					Brush.verticalGradient(
						colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
					)
				)
		)

		// Overlay: meta + title + selftext, above the action bar.
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.BottomCenter)
				.padding(horizontal = 12.dp, vertical = 10.dp),
		) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Box(
					modifier = Modifier
						.size(28.dp)
						.clip(CircleShape)
						.background(MaterialTheme.colorScheme.primaryContainer),
					contentAlignment = Alignment.Center,
				) {
					Text(
						text = (post.author?.firstOrNull()?.uppercase() ?: "?"),
						color = MaterialTheme.colorScheme.onPrimaryContainer,
						fontWeight = FontWeight.Bold,
					)
				}
				Spacer(Modifier.width(8.dp))
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = "r/${post.subreddit} • ${post.author ?: "Unknown"}",
						color = Color.White,
						fontWeight = FontWeight.Medium,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
					)
				}
			}
			Spacer(Modifier.height(6.dp))
			Text(
				text = post.title ?: "Untitled",
				color = Color.White,
				fontWeight = FontWeight.Bold,
				style = MaterialTheme.typography.titleLarge,
				maxLines = 3,
				overflow = TextOverflow.Ellipsis,
			)
			post.selftext?.takeIf { it.isNotBlank() }?.let { selftext ->
				Spacer(Modifier.height(6.dp))
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.heightIn(max = 120.dp)
						.verticalScroll(rememberScrollState()),
				) {
					Text(
						text = selftext,
						color = Color.White.copy(alpha = 0.9f),
						style = MaterialTheme.typography.bodyMedium,
					)
				}
			}
		}

		// Six equal-weight action buttons, pinned above the system nav.
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.BottomCenter)
				.background(Color.Black.copy(alpha = 0.5f)),
		) {
			val buttons = listOf(
				ActionBarButton(
					icon = Icons.Filled.ArrowUpward,
					contentDescription = "Upvote",
					countLabel = formatCompact(post.score),
					onClick = { onPostAction(post, PostAction.UPVOTE) },
				),
				ActionBarButton(
					icon = Icons.Filled.ArrowDownward,
					contentDescription = "Downvote",
					onClick = { onPostAction(post, PostAction.DOWNVOTE) },
				),
				ActionBarButton(
					icon = Icons.Filled.Bookmark,
					contentDescription = if (post.saved) "Unsave" else "Save",
					onClick = {
						onPostAction(post, if (post.saved) PostAction.UNSAVE else PostAction.SAVE)
					},
				),
				ActionBarButton(
					icon = Icons.Filled.AddComment,
					contentDescription = "Comments",
					countLabel = formatCompact(post.numComments),
					onClick = onPostClick,
				),
				ActionBarButton(
					icon = Icons.Filled.ArrowDownward,
					contentDescription = "Download",
					enabled = false,
				),
				ActionBarButton(
					icon = Icons.Filled.MoreVert,
					contentDescription = "More",
					onClick = { moreExpanded = true },
				),
			)
			ActionBarRow(buttons = buttons)

			// The "More" kebab's overflow menu (share / report / hide).
			Box(modifier = Modifier.align(Alignment.BottomEnd)) {
				Box(modifier = Modifier.size(0.dp)) {
					DropdownMenu(
						expanded = moreExpanded,
						onDismissRequest = { moreExpanded = false },
						modifier = Modifier.align(Alignment.BottomEnd),
					) {
						DropdownMenuItem(
							text = { Text("Share") },
							onClick = {
								moreExpanded = false
								onPostAction(post, PostAction.SHARE)
							},
						)
						DropdownMenuItem(
							text = { Text("Report") },
							onClick = {
								moreExpanded = false
								onPostAction(post, PostAction.REPORT)
							},
						)
						DropdownMenuItem(
							text = { Text(if (post.hidden) "Unhide" else "Hide") },
							onClick = {
								moreExpanded = false
								onPostAction(post, if (post.hidden) PostAction.UNHIDE else PostAction.HIDE)
							},
						)
					}
				}
			}
		}
	}
}

/**
 * The slide's media: a still image (post url, else thumbnail) full-bleed; an
 * NSFW/spoiler overlay (blurred until tapped to reveal); a video placeholder.
 */
@Composable
private fun SlideMedia(
	post: PostItem,
	modifier: Modifier = Modifier,
	onReveal: () -> Unit,
	onMediaClick: () -> Unit = {},
) {
	val revealed = remember { mutableStateOf(!(post.isOver18 || post.isSpoiler)) }
	val mediaUrl = post.url?.takeIf { it.isNotBlank() && !it.startsWith("reddit.com") }
		?: post.thumbnail
		?.takeIf { it.isNotBlank() && it != "default" }

	Box(
		modifier = modifier
			.then(if (mediaUrl != null) Modifier.clickable(onClick = onMediaClick) else Modifier)
	) {
		if (post.isVideo) {
			// Video placeholder (inline Media3 is a follow-up).
			Box(
				modifier = Modifier.fillMaxSize().background(Color.Black),
				contentAlignment = Alignment.Center,
			) {
				Icon(
					imageVector = Icons.Filled.PlayCircle,
					contentDescription = "Video",
					tint = Color.White.copy(alpha = 0.7f),
					modifier = Modifier.size(96.dp),
				)
			}
		} else if (mediaUrl != null) {
			val data by fetchImage(UriString(mediaUrl), scaleToMaxAxis = 1280)
			val blurred = !revealed.value
			when (val it = data) {
				is NetRequestStatus.Connecting -> Box(
					modifier = Modifier.fillMaxSize(),
					contentAlignment = Alignment.Center,
				) { CircularProgressIndicator() }

				is NetRequestStatus.Downloading -> Box(
					modifier = Modifier.fillMaxSize(),
					contentAlignment = Alignment.Center,
				) {
					CircularProgressIndicator(progress = { it.fractionComplete })
				}

				is NetRequestStatus.Failed -> Box(modifier = Modifier.fillMaxSize()) {
					// Nothing — the scrim + overlay still render over a blank.
				}

				is NetRequestStatus.Success -> Image(
					bitmap = it.result.data,
					contentDescription = null,
					contentScale = ContentScale.Crop,
					modifier = Modifier
						.fillMaxSize()
						.then(if (blurred) Modifier.blur(24.dp) else Modifier),
				)
			}
			if (blurred) {
				Box(
					modifier = Modifier
						.fillMaxSize()
						.clickable {
							revealed.value = true
							onReveal()
						}
						.background(Color.Black.copy(alpha = 0.35f)),
					contentAlignment = Alignment.Center,
				) {
					Text(
						text = if (post.isOver18) "NSFW — tap to reveal" else "Spoiler — tap to reveal",
						color = Color.White,
						fontWeight = FontWeight.SemiBold,
						modifier = Modifier
							.background(Color.Black.copy(alpha = 0.6f), CircleShape)
							.padding(horizontal = 16.dp, vertical = 10.dp),
					)
				}
			}
		} else {
			// Self post with no media: a neutral backdrop so the overlay reads.
			Box(
				modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
			)
		}
	}
}

/** Compact count for action buttons and the pill (1.2K, 3.4M). */
private fun formatCompact(value: Int): String {
	return when {
		value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
		value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
		else -> value.toString()
	}
}
