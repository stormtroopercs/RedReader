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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stormtroopercs.materialreader.common.LinkHandler
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.compose.net.NetRequestStatus
import com.stormtroopercs.materialreader.compose.net.fetchImage
import com.stormtroopercs.materialreader.compose.ui.RRErrorView
import com.stormtroopercs.materialreader.fragments.ReportDialog
import com.stormtroopercs.materialreader.reddit.api.SubredditSubscriptionState
import com.stormtroopercs.materialreader.settings.types.PostViewMode

/**
 * The community detail's four tabs (FINAL-DESIGN 6.3): Active (the post
 * feed) / About / Favorite (join/leave) / Mods.
 */
enum class CommunityTab {
	ACTIVE,
	ABOUT,
	FAVORITE,
	MODS,
	;

	val label: String
		get() = when (this) {
			CommunityTab.ACTIVE -> "Active"
			CommunityTab.ABOUT -> "About"
			CommunityTab.FAVORITE -> "Favorite"
			CommunityTab.MODS -> "Mods"
		}
}

/**
 * The community detail screen (FINAL-DESIGN Phase 6.3): a scrolling
 * community header (icon left, name large bold, meta line below) + the
 * reference's four tab chips (**Active** / **About** / **Favorite** /
 * **Mods**) + the standard [PostCard] feed under Active. There is **no
 * Subscribe button in the header** — join/leave lives in the Favorite tab
 * (the locked decision). The Active tab reuses the shared
 * [PostListViewModel] so the feed's sort / view mode / swipe actions /
 * post actions all behave exactly as in the list feed.
 *
 * [tabTitle] is the community title (`r/<name>`); the screen feeds the
 * shared ViewModel the `r/<name>` listing path.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDetailScreen(
	tab: CommunityTab,
	tabTitle: String,
	onTabSelected: (CommunityTab) -> Unit,
	onBack: () -> Unit,
	onNavigateToCommentList: (String) -> Unit,
	onNavigateToUserProfile: (String) -> Unit,
	onNavigateToPostSubmit: () -> Unit,
	onNavigateToSubredditSearch: () -> Unit,
	onNavigateToProfile: () -> Unit = {},
	onNavigateToRandomPost: (String) -> Unit = {},
	onNavigateToSaved: () -> Unit = {},
	onOpenListing: (String) -> Unit = {},
	onNavigateToSettings: () -> Unit = {},
	onOpenLicense: () -> Unit = {},
	/** Open a post's media in the full-screen viewer (media tap). */
	onOpenMedia: (PostItem) -> Unit = {},
) {
	val name = tabTitle.removePrefix("r/")
	val communityVm: CommunityViewModel = hiltViewModel()
	val postVm: PostListViewModel = hiltViewModel()
	val about by communityVm.about.collectAsStateWithLifecycle()
	val aboutTab by communityVm.aboutTab.collectAsStateWithLifecycle()
	val mods by communityVm.mods.collectAsStateWithLifecycle()
	val favorite by communityVm.favorite.collectAsStateWithLifecycle()
	val posts by postVm.posts.collectAsStateWithLifecycle()
	val uiState by postVm.state.collectAsStateWithLifecycle()
	val sortOption by postVm.sortOption.collectAsStateWithLifecycle()
	val actionResult by communityVm.actionResult.collectAsStateWithLifecycle()
	val snackbarHostState = remember { SnackbarHostState() }
	val context = LocalContext.current

	LaunchedEffect(name) {
		communityVm.load(name)
		// Feed the shared post list with the community's bare name (the
		// PostList route's contract) so the Active tab renders the standard
		// feed (sort / view mode / swipe all work exactly as in the list
		// feed). `name` is already the bare community name (the `r/` title
		// prefix was stripped above); `fetchPosts` normalises defensively.
		if (name.isNotBlank()) {
			postVm.fetchPosts(name)
		}
	}
	LaunchedEffect(actionResult) {
		actionResult?.let {
			snackbarHostState.showSnackbar(it)
			communityVm.clearActionResult()
		}
	}

	var sortDialogOpen by remember { mutableStateOf(false) }
	var moreActionsOpen by remember { mutableStateOf(false) }
	var aboutOpen by remember { mutableStateOf(false) }
	var changeViewOpen by remember { mutableStateOf(false) }

	// The collapsing large→small community header (FINAL-DESIGN 6.4 / DESIGN
	// §5): the banner + overview header at the top of the list collapses to
	// the app-bar pill once the feed has scrolled past it. The header is
	// list item 0 and the tab chips item 1, so the collapsed state kicks in
	// when the feed (item 2) is first visible.
	val listState = rememberLazyListState()
	val headerCollapsed by remember {
		derivedStateOf { listState.firstVisibleItemIndex >= 2 }
	}

	// The Active tab's card mode (persisted per feed, Phase 4.7).
	val viewMode = remember(name) {
		FeedPreferences.viewModeFor(feedIdFor("r/$name", null))
	}

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
			else -> postVm.performAction(activity, post, action)
		}
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = {
					if (headerCollapsed) {
						// The collapsed pill (name + subscriber count) once
						// the large header has scrolled off (6.4 / DESIGN §5).
						Row(verticalAlignment = Alignment.CenterVertically) {
							Text(
								text = about?.name?.let { "r/$it" } ?: "Community",
								fontWeight = FontWeight.SemiBold,
								maxLines = 1,
								overflow = TextOverflow.Ellipsis,
							)
							about?.subscribers?.let {
								Text(
									text = " • ${CommunityViewModel.formatCount(it)}",
									style = MaterialTheme.typography.bodySmall,
									color = MaterialTheme.colorScheme.onSurfaceVariant,
									maxLines = 1,
								)
							}
						}
					} else {
						Text(
							text = about?.name?.let { "r/$it" } ?: "Community",
							maxLines = 1,
							overflow = TextOverflow.Ellipsis,
						)
					}
				},
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
					}
				},
				actions = {
					// The reference feed's top bar carries exactly Search / Sort /
					// More actions — Change view and Refresh live in the More
					// actions grid (FINAL-DESIGN Phase 5), and the bottom-right
					// FAB is the prominent "more" entry point (posts_fab).
					IconButton(onClick = onNavigateToSubredditSearch) {
						Icon(Icons.Filled.Search, contentDescription = "Search")
					}
					IconButton(onClick = { sortDialogOpen = true }) {
						Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
					}
					IconButton(onClick = { moreActionsOpen = true }) {
						Icon(Icons.Filled.MoreVert, contentDescription = "More actions")
					}
				},
				)
		},
		snackbarHost = { SnackbarHost(snackbarHostState) },
		floatingActionButton = {
			FeedMoreFab(onClick = { moreActionsOpen = true })
		},
	) { paddingValues ->
		LazyColumn(
			state = listState,
			modifier = Modifier
				.fillMaxSize()
				.padding(paddingValues),
		) {
			// The scrolling community header (icon left, name large bold,
			// meta line below) — FINAL-DESIGN 6.3 / DESIGN §5.
			item {
				CommunityHeader(
					name = about?.name,
					subscribers = about?.subscribers,
					iconUrl = about?.iconUrl,
				)
			}
			// The four tab chips (Active / About / Favorite / Mods).
			item {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 8.dp, vertical = 6.dp),
					horizontalArrangement = Arrangement.spacedBy(8.dp),
				) {
					CommunityTab.entries.forEach { t ->
						FilterChip(
							selected = t == tab,
							onClick = { onTabSelected(t) },
							label = { Text(t.label) },
						)
					}
				}
			}
			// The active tab's content.
			item {
				when (tab) {
					CommunityTab.ACTIVE -> {
						CommunityActiveFeed(
							uiState = uiState,
							posts = posts,
							viewMode = viewMode,
							onOpenThread = onNavigateToCommentList,
							onOpenMedia = onOpenMedia,
							onAuthorClick = onNavigateToUserProfile,
							onPostAction = ::onPostAction,
						)
					}
					CommunityTab.ABOUT -> {
						CommunityAboutTab(content = aboutTab)
					}
					CommunityTab.FAVORITE -> {
						CommunityFavoriteTab(
							content = favorite,
							onToggle = {
								(context as? AppCompatActivity)?.let { activity ->
									communityVm.toggleFavorite(activity)
								}
							},
						)
					}
					CommunityTab.MODS -> {
						CommunityModsTab(
							content = mods,
							onModClick = onNavigateToUserProfile,
						)
					}
				}
			}
		}
	}

	// The 9-option sort dialog (Active = the listing default).
	if (sortDialogOpen) {
		SortOptionsDialog(
			currentId = sortOption.id,
			onDismiss = { sortDialogOpen = false },
			onSelected = { option ->
				postVm.setSortOption(option)
				sortDialogOpen = false
			},
		)
	}
	// Change-View bottom sheet: the card modes + Slides.
	if (changeViewOpen) {
		ChangeViewSheet(
			current = viewMode,
			onDismiss = { changeViewOpen = false },
			onSelect = { mode ->
				FeedPreferences.setViewModeFor(feedIdFor("r/$name", null), mode)
				changeViewOpen = false
			},
		)
	}
	// The "More actions" grid (FINAL-DESIGN Phase 5).
	if (moreActionsOpen) {
		MoreActionsSheet(
			posts = posts,
			onDismiss = { moreActionsOpen = false },
			onNavigateToSearch = {
				moreActionsOpen = false
				onNavigateToSubredditSearch()
			},
			onNavigateToProfile = {
				moreActionsOpen = false
				onNavigateToProfile()
			},
			onHideReadToggled = { postVm.refresh() },
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
				postVm.refresh()
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

/**
 * The scrolling community header (FINAL-DESIGN 6.3 / DESIGN §5): the
 * community icon (left), the name (large bold) and the meta line
 * (`r/<name> • N subs`) below. No Subscribe CTA (locked decision).
 */
@Composable
private fun CommunityHeader(
	name: String?,
	subscribers: Int?,
	iconUrl: String?,
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp, vertical = 12.dp),
	) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			if (iconUrl != null && iconUrl.isNotBlank()) {
				Box(
					modifier = Modifier
						.size(56.dp)
						.clip(CircleShape),
					contentAlignment = Alignment.Center,
				) {
					val data by fetchImage(UriString(iconUrl), scaleToMaxAxis = 160)
					when (val it = data) {
						is NetRequestStatus.Success -> Image(
							bitmap = it.result.data,
							contentDescription = null,
							contentScale = ContentScale.Crop,
							modifier = Modifier.fillMaxSize().clip(CircleShape),
						)
						else -> {
							// A present-but-failing icon URL must not leave a
							// blank circle — show the letter placeholder.
							CommunityHeaderLetterFallback(name)
						}
					}
				}
				Spacer(Modifier.width(16.dp))
			} else {
				// No icon: a colored circle with the community's first letter.
				Box(
					modifier = Modifier
						.size(56.dp)
						.clip(CircleShape)
						.background(MaterialTheme.colorScheme.primaryContainer),
					contentAlignment = Alignment.Center,
				) {
					Text(
						text = name?.firstOrNull()?.uppercase() ?: "?",
						color = MaterialTheme.colorScheme.onPrimaryContainer,
						fontWeight = FontWeight.Bold,
						style = MaterialTheme.typography.headlineMedium,
					)
				}
				Spacer(Modifier.width(16.dp))
			}
			Column {
				Text(
					text = name ?: "Community",
					style = MaterialTheme.typography.headlineSmall,
					fontWeight = FontWeight.Bold,
				)
				subscribers?.let {
					Text(
						text = "r/$name • ${CommunityViewModel.formatCount(it)} subs",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}
			}
		}
	}
}

/** The Active tab: the standard PostCard feed (the shared post list). */
@Composable
private fun CommunityActiveFeed(
	uiState: PostListUiState,
	posts: List<PostItem>,
	viewMode: PostViewMode,
	onOpenThread: (String) -> Unit,
	onOpenMedia: (PostItem) -> Unit,
	onAuthorClick: (String) -> Unit,
	onPostAction: (PostItem, PostAction) -> Unit,
) {
	when (val state = uiState) {
		is PostListUiState.Loading -> {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(160.dp),
				contentAlignment = Alignment.Center,
			) {
				if (state.isInitialLoad) CircularProgressIndicator()
			}
		}
		is PostListUiState.Success -> {
			if (state.posts.isEmpty()) {
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.height(120.dp),
					contentAlignment = Alignment.Center,
				) {
					Text(
						text = "No posts found",
						style = MaterialTheme.typography.bodyLarge,
						textAlign = TextAlign.Center,
					)
				}
			} else {
				posts.forEach { post ->
					PostCard(
						post = post,
						mode = viewMode,
						onOpenThread = { onOpenThread(post.id) },
						onMediaClick = { onOpenMedia(post) },
						onAuthorClick = onAuthorClick,
						onPostAction = { p, a -> onPostAction(p, a) },
						swipeEnabled = true,
						onSwipeUpvote = { onPostAction(post, PostAction.UPVOTE) },
						onSwipeDownvote = { onPostAction(post, PostAction.DOWNVOTE) },
						onSwipeHide = { onPostAction(post, if (post.hidden) PostAction.UNHIDE else PostAction.HIDE) },
					)
				}
			}
		}
		is PostListUiState.Error -> {
			Box(
				modifier = Modifier
					.fillMaxWidth()
					.height(120.dp),
				contentAlignment = Alignment.Center,
			) {
				RRErrorView(error = state.error)
			}
		}
	}
}

/** The About tab: the community's about-facts (FINAL-DESIGN 6.3). */
@Composable
private fun CommunityAboutTab(content: CommunityViewModel.TabContent) {
	when (content) {
		CommunityViewModel.TabContent.Loading -> CenteredState { CircularProgressIndicator() }
		is CommunityViewModel.TabContent.Success -> {
			Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
				content.facts.forEach { fact ->
					Column(modifier = Modifier.padding(vertical = 10.dp)) {
						Text(
							text = fact.label,
							style = MaterialTheme.typography.labelMedium,
							color = MaterialTheme.colorScheme.primary,
							fontWeight = FontWeight.SemiBold,
						)
						Spacer(Modifier.height(4.dp))
						Text(
							text = fact.value,
							style = MaterialTheme.typography.bodyLarge,
						)
					}
				}
			}
		}
		is CommunityViewModel.TabContent.Error -> {
			CenteredState {
				Text(text = content.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
			}
		}
	}
}

/** The Favorite tab: the join/leave action (FINAL-DESIGN 6.3, no header CTA). */
@Composable
private fun CommunityFavoriteTab(
	content: CommunityViewModel.FavoriteContent,
	onToggle: () -> Unit,
) {
	when (content) {
		CommunityViewModel.FavoriteContent.Loading -> CenteredState { CircularProgressIndicator() }
		CommunityViewModel.FavoriteContent.NotSignedIn -> {
			CenteredState {
				Column(horizontalAlignment = Alignment.CenterHorizontally) {
					Text(
						text = "Sign in to join or leave communities",
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						textAlign = TextAlign.Center,
					)
				}
			}
		}
		is CommunityViewModel.FavoriteContent.Subscribed -> {
			val subscribed = content.state == SubredditSubscriptionState.SUBSCRIBED
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.padding(24.dp),
				horizontalAlignment = Alignment.CenterHorizontally,
			) {
				Icon(
					imageVector = if (subscribed) Icons.Filled.Star else Icons.Filled.StarBorder,
					contentDescription = null,
					modifier = Modifier.size(48.dp),
					tint = if (subscribed) {
						MaterialTheme.colorScheme.primary
					} else {
						MaterialTheme.colorScheme.onSurfaceVariant
					},
				)
				Spacer(Modifier.height(12.dp))
				Text(
					text = if (subscribed) "You are following this community" else "Not following this community",
					style = MaterialTheme.typography.bodyLarge,
					textAlign = TextAlign.Center,
				)
				Spacer(Modifier.height(20.dp))
				if (content.state == SubredditSubscriptionState.SUBSCRIBING ||
					content.state == SubredditSubscriptionState.UNSUBSCRIBING
				) {
					CircularProgressIndicator()
				} else {
					Button(onClick = onToggle) {
						Text(if (subscribed) "Leave" else "Join")
					}
				}
			}
		}
	}
}

/** The Mods tab: the community's moderators (FINAL-DESIGN 6.3). */
@Composable
private fun CommunityModsTab(
	content: CommunityViewModel.ModsContent,
	onModClick: (String) -> Unit,
) {
	when (content) {
		CommunityViewModel.ModsContent.Loading -> CenteredState { CircularProgressIndicator() }
		is CommunityViewModel.ModsContent.Success -> {
			if (content.moderators.isEmpty()) {
				CenteredState {
					Text(
						text = "No moderators listed",
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
				}
			} else {
				content.moderators.forEach { mod ->
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.clickable { onModClick(mod.name) }
							.padding(horizontal = 16.dp, vertical = 10.dp),
						verticalAlignment = Alignment.CenterVertically,
					) {
						Box(
							modifier = Modifier
								.size(40.dp)
								.clip(CircleShape)
								.background(MaterialTheme.colorScheme.surfaceVariant),
							contentAlignment = Alignment.Center,
						) {
							if (mod.iconUrl != null && mod.iconUrl.isNotBlank()) {
									val data by fetchImage(UriString(mod.iconUrl), scaleToMaxAxis = 96)
									when (val it = data) {
										is NetRequestStatus.Success -> Image(
											bitmap = it.result.data,
											contentDescription = null,
											contentScale = ContentScale.Crop,
											modifier = Modifier.fillMaxSize().clip(CircleShape),
										)
										else -> Icon(
											imageVector = Icons.Filled.Person,
											contentDescription = null,
											modifier = Modifier.size(20.dp),
										)
									}
								} else {
								Icon(
									imageVector = Icons.Filled.Person,
									contentDescription = null,
									modifier = Modifier.size(20.dp),
								)
							}
						}
						Spacer(Modifier.width(12.dp))
						Text(
							text = mod.name,
							style = MaterialTheme.typography.bodyLarge,
						)
					}
				}
			}
		}
		is CommunityViewModel.ModsContent.Error -> {
			CenteredState {
				Text(text = content.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
			}
		}
	}
}

/** A vertically+horizontally centered content block (loading / empty states). */
@Composable
private fun CenteredState(content: @Composable () -> Unit) {
	Box(
		modifier = Modifier
			.fillMaxWidth()
			.height(160.dp),
		contentAlignment = Alignment.Center,
	) {
		content()
	}
}

/**
 * The community header's letter placeholder (shown when the icon URL is
 * present but the fetch hasn't produced a bitmap yet / failed): a filled
 * circle with the community's first letter, same styling as the no-icon
 * branch so a failing icon never reads as a blank hole.
 */
@Composable
private fun CommunityHeaderLetterFallback(name: String?) {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.primaryContainer),
		contentAlignment = Alignment.Center,
	) {
		Text(
			text = name?.firstOrNull()?.uppercase() ?: "?",
			color = MaterialTheme.colorScheme.onPrimaryContainer,
			fontWeight = FontWeight.Bold,
			style = MaterialTheme.typography.headlineMedium,
		)
	}
}
