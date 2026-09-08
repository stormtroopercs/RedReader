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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stormtroopercs.materialreader.common.LinkHandler
import com.stormtroopercs.materialreader.compose.ui.RRErrorView
import com.stormtroopercs.materialreader.fragments.ReportDialog

/**
 * Post list screen composable (FINAL-DESIGN Phase 4): the reference's list
 * feed — a top bar with Search / Sort / View / Submit actions, the feed
 * filter chips (Active-sort / Communities / Instances), and the post cards
 * in the user's chosen card mode (persisted per feed). The sort is the
 * reference's 9-option dialog; the card mode is the Change-View bottom
 * sheet (picking Slides re-enters this feed as the swipe feed). Swipe-to-
 * action gestures on the cards are on by default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealPostListScreen(
	subreddit: String,
	searchQuery: String? = null,
	onNavigateBack: () -> Unit,
	onNavigateToCommentList: (String) -> Unit,
	onNavigateToUserProfile: (String) -> Unit,
	onNavigateToPostSubmit: () -> Unit,
	onNavigateToSubredditSearch: () -> Unit,
	/** Open the default account's own profile (More actions → Profile). */
	onNavigateToProfile: () -> Unit = {},
	/** Open the full-screen list of the account's subscribed subreddits
	 * (the feed's Subreddits chip). */
	onOpenSubreddits: () -> Unit = {},
	/** Jump straight to a (random) post's thread (More actions → Random). */
	onNavigateToRandomPost: (String) -> Unit = {},
	/** Open the account's saved list (More actions → Saved). */
	onNavigateToSaved: () -> Unit = {},
	/** Open an arbitrary listing path (More actions → custom slot). */
	onOpenListing: (String) -> Unit = {},
	/** Open Settings (More actions → Settings). */
	onNavigateToSettings: () -> Unit = {},
	/** Open the license view (More actions → About → License). */
	onOpenLicense: () -> Unit = {},
	/** Open a post's media in the full-screen viewer (media tap). */
	onOpenMedia: (PostItem) -> Unit = {},
	/** Open a post's video in the full-screen video overlay (video-media tap). */
	onOpenVideo: (PostItem) -> Unit = {},
	/** Open a link post's article (a tap on its resolved article preview). */
	onOpenLink: (PostItem) -> Unit = {},
	/**
	 * True when this feed is the top-level Posts tab (the app root) rather than
	 * a pushed child: the top bar then carries the drawer hamburger (when the
	 * drawer is enabled) instead of a back arrow, and the tab's title.
	 */
	isTabRoot: Boolean = false,
	/** The tab-root title override (e.g. "Posts"); null = the feed's own title. */
	titleOverride: String? = null,
) {
	val viewModel: PostListViewModel = hiltViewModel()
	val uiState by viewModel.state.collectAsStateWithLifecycle()
	val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
	val listTitle by viewModel.title.collectAsStateWithLifecycle()
	// The drawer opener, when this is the Posts tab root and a drawer is
	// enabled (Drawer / Both styles). Null in the Bottom style → back arrow.
	val openDrawer = LocalOpenDrawer.current

	LaunchedEffect(subreddit, searchQuery) {
		viewModel.fetchPosts(subreddit, searchQuery)
	}

	var sortDialogOpen by remember { mutableStateOf(false) }
	var changeViewOpen by remember { mutableStateOf(false) }
	var moreActionsOpen by remember { mutableStateOf(false) }
	var aboutOpen by remember { mutableStateOf(false) }
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
				com.stormtroopercs.materialreader.reddit.kthings.RedditIdAndType(post.id),
				post.subreddit,
				isComment = false,
			)
			PostAction.SHARE -> LinkHandler.shareText(
				activity,
				post.title,
				"https://www.reddit.com${post.permalink}",
			)
			else -> viewModel.performAction(activity, post, action)
		}
	}

	// The card mode for this feed (persisted per feed, Phase 4.7). Read
	// reactively from the observable prefs: a Change-View selection
	// recomposes the entry in place (list mode swap) — the slides switch
	// happens at the entry level.
	val viewMode = FeedPreferences.viewModeFor(FeedPreferences.effectiveKey(subreddit, searchQuery))

	Scaffold(
		topBar = {
			CenterAlignedTopAppBar(
				scrollBehavior = null,
				title = {
					Text(
						text = titleOverride ?: listTitle.ifEmpty { "r/$subreddit" },
						fontWeight = FontWeight.SemiBold,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
					)
				},
				navigationIcon = {
					if (isTabRoot && openDrawer != null) {
						// The Posts tab root with a drawer: a hamburger (there's no
						// back to go to).
						IconButton(onClick = openDrawer) {
							Icon(
								imageVector = Icons.Filled.Menu,
								contentDescription = "Open menu",
							)
						}
					} else {
						IconButton(onClick = onNavigateBack) {
							Icon(
								imageVector = Icons.AutoMirrored.Filled.ArrowBack,
								contentDescription = "Back",
							)
						}
					}
				},
				actions = {
					// The reference feed's top bar carries exactly Search / Sort /
					// More actions — Change view and Refresh live in the More
					// actions grid (FINAL-DESIGN Phase 5), and submit is the
					// bottom-right FAB (Phase 5 / posts_fab).
					IconButton(onClick = onNavigateToSubredditSearch) {
						Icon(Icons.Filled.Search, contentDescription = "Search")
					}
					IconButton(onClick = { sortDialogOpen = true }) {
						Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
					}
					// The reference's power-user surface (FINAL-DESIGN
					// Phase 5): the "More actions" grid, opened from the
					// top bar's More icon.
					IconButton(onClick = { moreActionsOpen = true }) {
						Icon(Icons.Filled.MoreVert, contentDescription = "More actions")
					}
				},
			)
		},
		snackbarHost = { SnackbarHost(snackbarHostState) },
	) { paddingValues ->
		Box(
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues),
		) {
			Column(
				modifier = Modifier.fillMaxSize(),
			) {
				// The feed's filter chips: the Active sort chip (opens the
				// 9-option dialog) + Subreddits (the account's subscribed
				// subreddits — the same list as the drawer's Subscriptions
				// section).
				FeedFilterChips(
					sortLabel = sortOption.chipLabel,
					onSortTap = { sortDialogOpen = true },
					onSubredditsTap = onOpenSubreddits,
				)

				when (val state = uiState) {
					is PostListUiState.Loading -> {
						if (state.isInitialLoad) {
							Box(
								modifier = Modifier
									.fillMaxSize()
									.padding(32.dp),
								contentAlignment = Alignment.Center,
							) {
								CircularProgressIndicator()
							}
						}
					}

					is PostListUiState.Success -> {
						if (state.posts.isEmpty()) {
							Box(
								modifier = Modifier
									.fillMaxSize()
									.padding(32.dp),
								contentAlignment = Alignment.Center,
							) {
								Text(
									text = "No posts found",
									style = MaterialTheme.typography.bodyLarge,
								)
							}
						} else {
							LazyColumn(
								modifier = Modifier.fillMaxSize(),
								content = {
									items(state.posts, key = { it.id }) { post ->
										PostCard(
											post = post,
											mode = viewMode,
											modifier = Modifier.animateItem(),
											onOpenThread = { onNavigateToCommentList(post.id) },
											onMediaClick = { onOpenMedia(post) },
											onOpenVideo = { onOpenVideo(post) },
											onOpenLink = { onOpenLink(post) },
											onAuthorClick = onNavigateToUserProfile,
											onOpenSubreddit = { name -> onOpenListing("r/$name") },
											onPostAction = ::onPostAction,
											swipeEnabled = true,
											onSwipeUpvote = { onPostAction(post, PostAction.UPVOTE) },
											onSwipeDownvote = { onPostAction(post, PostAction.DOWNVOTE) },
											onSwipeHide = { onPostAction(post, if (post.hidden) PostAction.UNHIDE else PostAction.HIDE) },
										)
									}
								},
							)
						}
					}

					is PostListUiState.Error -> {
						Box(
							modifier = Modifier
								.fillMaxSize()
								.padding(16.dp),
							contentAlignment = Alignment.Center,
						) {
							Column(
								horizontalAlignment = Alignment.CenterHorizontally,
								verticalArrangement = Arrangement.spacedBy(16.dp),
							) {
								RRErrorView(error = state.error)
								FilledTonalButton(
									onClick = { viewModel.refresh() },
									modifier = Modifier.align(Alignment.CenterHorizontally),
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

			// The reference's feed "more" FAB (posts_fab): bottom-right,
			// opens the same More actions grid as the top bar's More icon.
			FeedMoreFab(
				onClick = { moreActionsOpen = true },
				modifier = Modifier
					.align(Alignment.BottomEnd)
					.padding(16.dp),
			)
		}
	}

	// The reference's 9-option sort dialog (Active = the listing default).
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

	// The Change-View bottom sheet: the card modes + Slides. Selecting a
	// mode persists it; the entry recomposes in place (the slides switch
	// is the entry's job — the surface swap needs no re-navigation).
	if (changeViewOpen) {
		ChangeViewSheet(
			current = viewMode,
			onDismiss = { changeViewOpen = false },
			onSelect = { mode ->
				FeedPreferences.setViewModeFor(
					FeedPreferences.effectiveKey(subreddit, searchQuery),
					mode,
				)
				changeViewOpen = false
			},
			onCustomize = {
				changeViewOpen = false
				onNavigateToSettings()
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
				// The pref just flipped; refetch so the list reflects it.
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
			onOpenLicense = {
				aboutOpen = false
				onOpenLicense()
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
}

// The per-feed preference key now lives in [FeedPreferences.effectiveKey]
// (shared by every feed surface so one feed has one view mode).
