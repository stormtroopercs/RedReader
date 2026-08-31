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

import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.stormtroopercs.materialreader.R
import com.stormtroopercs.materialreader.account.RedditAccountChangeListener
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.common.AssetHelper
import com.stormtroopercs.materialreader.common.LinkHandler
import com.stormtroopercs.materialreader.common.RunnableOnce
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.reddit.api.RedditOAuth
import com.stormtroopercs.materialreader.settings.types.PostViewMode

/**
 * The shared screen-transition timing (FINAL-DESIGN 8.5): a 300ms slide on the
 * reference's **fast-out-extra-slow-in** curve (`cubicBezier(0.1, 0, 0.2, 1)`) —
 * quick launch, long, gentle settle. Applied to every push/pop transition in the
 * [NavDisplay] below.
 */
val navSlideSpec: FiniteAnimationSpec<IntOffset> =
	tween(
		durationMillis = 300,
		easing = CubicBezierEasing(0.1f, 0f, 0.2f, 1f),
	)

/**
 * App-wide navigation using Navigation 3.
 *
 * The caller (the Activity) owns the [NavigationState] so it can drive back
 * navigation from the system back button; this graph only consumes it.
 *
 * Uses the standard Nav 3 pattern:
 *   - NavigationState (holds back stacks per top-level route)
 *   - Navigator (navigate/goBack actions)
 *   - entryProvider (resolves routes to composables)
 *   - NavDisplay with entryDecorators (saveable state + ViewModel scoping)
 */
@Composable
fun AppNavGraph(navigationState: NavigationState) {
	val navigator = Navigator(navigationState)

	val context = LocalContext.current
	// The license title, resolved configuration-aware (stringResource) rather
	// than via LocalContext.current (which Lint flags as configuration-unaware).
	val licenseTitle = stringResource(R.string.title_license)
	val accountManager = remember { RedditAccountManager.getInstance(context) }
	val accountName = remember {
		mutableStateOf(accountManager.defaultAccount.username)
	}

	// Opens the license in the HtmlView route (the shared "License" handler
	// the More-actions grid's About dialog uses).
	val openLicense: () -> Unit = {
		AssetHelper.loadAssetAsString(context, "license.html")?.let { html ->
			navigator.navigate(HtmlView(html, licenseTitle))
		}
	}

	// Opens a post's media in the full-screen viewer. Albums (imgur /a or
	// gallery, reddit /gallery) open the swiping Album route; everything else
	// (direct still / GIF / video file, or a page-URL host that needs live
	// resolution) opens the standalone Image route — ImageScreen self-resolves
	// page URLs via fetchImageInfo and renders still / GIF / video. Posts
	// with no resolvable media (self posts) are a no-op.
	val openMedia: (PostItem) -> Unit = { post ->
		val url = post.url?.takeIf { it.isNotBlank() && !it.startsWith("reddit.com") }
		if (url != null) {
			if (LinkHandler.imgurAlbumPattern.matcher(url).find() ||
				LinkHandler.redditGalleryPattern.matcher(url).find()
			) {
				navigator.navigate(Album(url))
			} else {
				navigator.navigate(
					Image(
						url = url,
						isGif = LinkHandler.isDirectGifFile(UriString(url)),
						isVideo = post.isVideo || LinkHandler.isDirectVideoFile(UriString(url)),
					),
				)
			}
		}
	}
	DisposableEffect(accountManager) {
		val listener = RedditAccountChangeListener {
			accountName.value = accountManager.defaultAccount.username
		}
		accountManager.addUpdateListener(listener)
		onDispose { accountManager.removeUpdateListener(listener) }
	}

	AppShell(
		navigationState = navigationState,
	) {
		NavDisplay(
			backStack = navigationState.activeBackStack,
			onBack = { navigator.goBack() },
			entryDecorators = listOf(
				rememberSaveableStateHolderNavEntryDecorator(),
				rememberViewModelStoreNavEntryDecorator(),
			),
			transitionSpec = {
				// Push: the new screen slides in from the right over the previous
				// one, with the reference's fast-out-extra-slow-in easing (8.5).
				slideInHorizontally(initialOffsetX = { it }, animationSpec = navSlideSpec) togetherWith
					slideOutHorizontally(targetOffsetX = { -it }, animationSpec = navSlideSpec)
			},
			popTransitionSpec = {
				// Pop (system back, up arrow, in-app "back" nav): the leaving
				// screen slides out to the left and the underlying one slides back
				// in — same easing.
				slideInHorizontally(initialOffsetX = { -it }, animationSpec = navSlideSpec) togetherWith
					slideOutHorizontally(targetOffsetX = { it }, animationSpec = navSlideSpec)
			},
			entryProvider = entryProvider {
				// Top-level: Main screen
				entry<Main> {
					MainScreen(
						accountName = accountName.value,
						onNavigateToPostList = { subreddit ->
							navigator.navigate(PostList(subreddit))
						},
						onNavigateToSettings = {
							navigator.navigate(Settings)
						},
						onNavigateToLogin = {
							navigator.navigate(OAuthLogin)
						},
						onNavigateToInbox = {
							navigator.navigate(Inbox)
						},
						onNavigateToProfile = {
							// The account row opens the signed-in user's own
							// profile (karma, avatar, sign out).
							navigator.navigate(UserProfile(it))
						},
						onNavigateToSubredditSearch = {
							navigator.navigate(SubredditSearch)
						},
					)
				}

				// Top-level: Explore tab (the reference's 2nd bottom-nav destination).
				entry<Explore> {
					ExploreScreen(
						onNavigateToSubreddit = { subreddit ->
							navigator.navigate(PostList(subreddit))
						},
						onNavigateToCommunity = { name ->
							navigator.navigate(Community(name))
						},
						onNavigateToSearch = {
							navigator.navigate(SubredditSearch)
						},
					)
				}

				// Top-level: Settings screen
				entry<Settings> {
					SettingsScreen(
						onNavigateBack = { navigator.goBack() },
						onNavigateToChangelog = { navigator.navigate(Changelog) },
						onNavigateToBugReport = { navigator.navigate(BugReport) },
						onNavigateToLicense = runLabel@{
							// Read the license asset and open the Compose HtmlView
							// route (replaces the legacy HtmlViewActivity.showAsset
							// launch, retired in the 41st increment).
							val html = AssetHelper.loadAssetAsString(context, "license.html")
								?: return@runLabel
							navigator.navigate(HtmlView(html, licenseTitle))
						},
					)
				}

				// Child: Post list. The feed's persisted view mode decides the
				// surface (FINAL-DESIGN Phase 4.7): Slides → the signature swipe
				// feed (the default for community feeds, and any feed the user
				// has explicitly set to slides); every other mode → the list
				// view in that card mode.
				entry<PostList> { key ->
					val feedId = feedIdFor(key.subreddit, key.searchQuery)
					// No explicit selection: community feeds open in the
					// signature swipe feed (the Phase 3 default); everything
					// else opens in the list view.
					val viewMode = if (FeedPreferences.hasViewModeFor(feedId)) {
						FeedPreferences.viewModeFor(feedId)
					} else {
						if (key.searchQuery == null && isCommunityFeedPath(key.subreddit)) {
							PostViewMode.SLIDES
						} else {
							PostViewMode.CARDS
						}
					}
					if (viewMode == PostViewMode.SLIDES) {
						RealSlidesFeedScreen(
							subreddit = key.subreddit,
							searchQuery = key.searchQuery,
							onNavigateBack = { navigator.goBack() },
							onNavigateToCommentList = { postId ->
								navigator.navigate(CommentList(postId))
							},
							onNavigateToUserProfile = { username ->
								navigator.navigate(UserProfile(username))
							},
							onNavigateToPostSubmit = {
								navigator.navigate(PostSubmit(key.subreddit))
							},
							onNavigateToSubredditSearch = {
								navigator.navigate(SubredditSearch)
							},
							onNavigateToProfile = {
								val username = RedditAccountManager.getInstance(context).defaultAccount.username
								navigator.navigate(UserProfile(username))
							},
							onNavigateToRandomPost = { postId ->
								navigator.navigate(CommentList(postId))
							},
							onNavigateToSaved = {
								val username = RedditAccountManager.getInstance(context).defaultAccount.username
								navigator.navigate(PostList("u/$username/saved"))
							},
							onOpenListing = { path ->
								navigator.navigate(PostList(path))
							},
							// The community pill opens the community detail
							// (Phase 6.3); non-community feeds fall back to the
							// community search.
							onOpenCommunity = { communityPath ->
								if (isCommunityFeedPath(communityPath)) {
									navigator.navigate(Community(communityPath.removePrefix("r/")))
								} else {
									navigator.navigate(SubredditSearch)
								}
							},
							onNavigateToSettings = {
								navigator.navigate(Settings)
							},
							onNavigateToList = {
								// Re-enter this feed; the new entry's composition
								// reads the persisted (now list) view mode.
								navigator.navigate(PostList(key.subreddit, key.searchQuery))
							},
							onOpenLicense = openLicense,
							onOpenMedia = openMedia,
						)
					} else {
						RealPostListScreen(
							subreddit = key.subreddit,
							searchQuery = key.searchQuery,
							onNavigateBack = { navigator.goBack() },
							onNavigateToCommentList = { postId ->
								navigator.navigate(CommentList(postId))
							},
							onNavigateToUserProfile = { username ->
								navigator.navigate(UserProfile(username))
							},
							onNavigateToPostSubmit = {
								navigator.navigate(PostSubmit(key.subreddit))
							},
							onNavigateToSubredditSearch = {
								navigator.navigate(SubredditSearch)
							},
							onNavigateToSlides = {
								// Persist the selection, then re-enter this feed:
								// the new entry's composition reads the persisted
								// mode and renders the swipe feed.
								FeedPreferences.setViewModeFor(feedId, PostViewMode.SLIDES)
								navigator.navigate(PostList(key.subreddit, key.searchQuery))
							},
							onNavigateToProfile = {
								val username = RedditAccountManager.getInstance(context).defaultAccount.username
								navigator.navigate(UserProfile(username))
							},
							onNavigateToRandomPost = { postId ->
								navigator.navigate(CommentList(postId))
							},
							onNavigateToSaved = {
								val username = RedditAccountManager.getInstance(context).defaultAccount.username
								navigator.navigate(PostList("u/$username/saved"))
							},
							onOpenListing = { path ->
								navigator.navigate(PostList(path))
							},
							onNavigateToSettings = {
								navigator.navigate(Settings)
							},
							onOpenLicense = openLicense,
							onOpenMedia = openMedia,
						)
					}
				}

				// Child: Comment list
				entry<CommentList> { key ->
					RealCommentListScreen(
						postId = key.postId,
						onNavigateBack = { navigator.goBack() },
						onReply = { comment ->
							// The comment's full `t1_…` id is the reply's parent
							// thing id.
							navigator.navigate(CommentReply(comment.fullName))
						},
						onReplyToPost = {
							// The post's full `t3_…` id is the reply's parent
							// thing id.
							navigator.navigate(CommentReply(key.postId))
						},
					)
				}

				// Child: User profile
				entry<UserProfile> { key ->
					// Same entry-scoped instance the screen resolves itself.
					val userProfileViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel<UserProfileViewModel>()
					com.stormtroopercs.materialreader.compose.ui.UserProfileScreen(
						username = key.username,
						onNavigateBack = { navigator.goBack() },
						onNavigateToPosts = {
							navigator.navigate(PostList("u/${key.username}/submitted"))
						},
						onNavigateToComments = {
							navigator.navigate(CommentList("u/${key.username}/comments"))
						},
						onSendMessage = {
							navigator.navigate(PMSend(recipient = key.username))
						},
						onSignOut = {
							// Remove the account, then land back on the main
							// screen root (its account row flips to "Sign in to
							// Reddit" through the account change listener).
							userProfileViewModel.signOut()
							navigationState.navigateTo(Main)
						},
						onReLogin = {
							// A denied block permission needs a fresh token: push
							// the in-app OAuth route (the 50th increment retired
							// the legacy OAuthLoginActivity that did this).
							navigator.navigate(OAuthLogin)
						},
					)
				}

				// Child: Inbox
				entry<Inbox> {
					com.stormtroopercs.materialreader.compose.ui.InboxScreen(
						onNavigateBack = { navigator.goBack() },
						onSendMessage = {
							// "New message" → the PM composer (the legacy inbox
							// "new message" button opened the PM composer, not a
							// comment reply).
							navigator.navigate(PMSend())
						},
						onReplyToMessage = { message ->
							// Replying to an inbox message opens the PM composer
							// prefilled with the message's author + "Re: <subject>"
							// (the legacy inbox's reply action).
							navigator.navigate(
								PMSend(
									recipient = message.sender,
									subject = "Re: ${message.subject ?: ""}",
								),
							)
						},
					)
				}

				// Child: Post submit
				entry<PostSubmit> { key ->
					com.stormtroopercs.materialreader.compose.ui.PostSubmitScreen(
						subreddit = key.subreddit,
						shareUrl = key.shareUrl,
						onNavigateBack = { navigator.goBack() },
						onSubmitted = {
							navigator.goBack()
						},
					)
				}

				// Child: Subreddit search
				entry<SubredditSearch> {
					com.stormtroopercs.materialreader.compose.ui.SubredditSearchScreen(
						onNavigateBack = { navigator.goBack() },
						onSubredditSelected = { subreddit ->
							navigator.navigate(PostList(subreddit))
						},
					)
				}

				// Child: Community detail (FINAL-DESIGN Phase 6.3). The screen
				// resolves its own Hilt ViewModels (scoped to this entry); the
				// tab is route-local state so switching tabs does not re-navigate.
				entry<Community> { key ->
					var tab by remember { mutableStateOf(CommunityTab.ACTIVE) }
					CommunityDetailScreen(
						tab = tab,
						tabTitle = "r/${key.subreddit}",
						onTabSelected = { tab = it },
						onBack = { navigator.goBack() },
						onNavigateToCommentList = { postId ->
							navigator.navigate(CommentList(postId))
						},
						onNavigateToUserProfile = { username ->
							navigator.navigate(UserProfile(username))
						},
						onNavigateToPostSubmit = {
							navigator.navigate(PostSubmit(key.subreddit))
						},
						onNavigateToSubredditSearch = {
							navigator.navigate(SubredditSearch)
						},
						onNavigateToProfile = {
							val username = RedditAccountManager.getInstance(context).defaultAccount.username
							navigator.navigate(UserProfile(username))
						},
						onNavigateToRandomPost = { postId ->
							navigator.navigate(CommentList(postId))
						},
						onNavigateToSaved = {
							val username = RedditAccountManager.getInstance(context).defaultAccount.username
							navigator.navigate(PostList("u/$username/saved"))
						},
						onOpenListing = { path ->
							navigator.navigate(PostList(path))
						},
						onNavigateToSettings = {
							navigator.navigate(Settings)
						},
						onOpenLicense = openLicense,
						onOpenMedia = openMedia,
					)
				}

				// Child: Comment reply
				entry<CommentReply> { key ->
					com.stormtroopercs.materialreader.compose.ui.CommentReplyScreen(
						parentThingId = key.parentThingId,
						onDone = { navigator.goBack() },
						onNavigateBack = { navigator.goBack() },
					)
				}

				// Child: Comment / post edit
				entry<CommentEdit> { key ->
					com.stormtroopercs.materialreader.compose.ui.CommentEditScreen(
						idAndType = com.stormtroopercs.materialreader.reddit.kthings.RedditIdAndType(
							key.idAndType,
						),
						initialText = key.initialText,
						isSelfPost = key.isSelfPost,
						onDone = { navigator.goBack() },
						onNavigateBack = { navigator.goBack() },
					)
				}

				// Child: PM composer
				entry<PMSend> { key ->
					com.stormtroopercs.materialreader.compose.ui.PMSendScreen(
						initialRecipient = key.recipient,
						initialSubject = key.subject,
						initialText = key.text,
						onDone = { navigator.goBack() },
						onNavigateBack = { navigator.goBack() },
					)
				}

				// Child: Reddit Terms
				entry<RedditTerms> {
					com.stormtroopercs.materialreader.compose.ui.RedditTermsScreen(
						onDone = { navigator.goBack() },
					)
				}

				// Child: Changelog
				entry<Changelog> {
					com.stormtroopercs.materialreader.compose.ui.ChangelogScreen(
						onNavigateBack = { navigator.goBack() },
					)
				}

				// Child: Bug Report
				entry<BugReport> {
					com.stormtroopercs.materialreader.compose.ui.BugReportScreen(
						onNavigateBack = { navigator.goBack() },
					)
				}

				// Child: WebView (URL)
				entry<WebViewRoute> { key ->
					com.stormtroopercs.materialreader.compose.ui.WebViewScreen(
						url = key.url,
						title = key.title,
						onNavigateBack = { navigator.goBack() },
					)
				}

				// Child: HTML View
				entry<HtmlView> { key ->
					// Register the live WebView with HtmlViewBackHandler so the
					// activity's system-back override can walk the document's own
					// history before popping this screen (legacy HtmlViewActivity
					// behaviour, preserved after its retirement).
					androidx.compose.runtime.DisposableEffect(Unit) {
						onDispose {
							HtmlViewBackHandler.clear()
						}
					}
					com.stormtroopercs.materialreader.compose.ui.HtmlViewScreen(
						html = key.html,
						title = key.title,
						onNavigateBack = { navigator.goBack() },
						onWebViewCreated = { HtmlViewBackHandler.register(it) },
					)
				}

				// Child: Album (imgur / reddit gallery)
				entry<Album> { key ->
					com.stormtroopercs.materialreader.compose.ui.AlbumScreen(
						albumUrl = UriString(key.url),
						onBackPressed = { navigator.goBack() },
					)
				}

				// Child: full-screen still-image viewer (direct image URLs)
				entry<Image> { key ->
					com.stormtroopercs.materialreader.compose.ui.ImageScreen(
						url = UriString(key.url),
						isGif = key.isGif,
						isVideo = key.isVideo,
						albumUrl = key.albumUrl?.let { UriString(it) },
						albumIndex = key.albumIndex,
						onBackPressed = { navigator.goBack() },
					)
				}

				// Child: OAuth Login
				entry<OAuthLogin> {
					val context = LocalContext.current
					com.stormtroopercs.materialreader.compose.ui.OAuthLoginScreen(
						onOAuthComplete = { callbackUrl ->
							// Exchange the code for tokens and store the account
							// (mirrors the legacy AccountListDialog.onActivityResult
							// path). completeLogin shows its own progress and
							// success/failure dialogs.
							val activity = context as? AppCompatActivity
							if (activity == null) {
								Log.e("AppNavigation", "No host activity for OAuth callback")
								navigator.goBack()
								return@OAuthLoginScreen
							}
							RedditOAuth.completeLogin(
								activity,
								Uri.parse(callbackUrl),
								RunnableOnce(Runnable { navigator.goBack() }),
							)
						},
						onOAuthError = { error ->
							Log.e("AppNavigation", "OAuth failed: $error")
							navigator.goBack()
						},
					)
				}

				// Child: Account management (replaces the legacy AccountListDialog)
				entry<Accounts> {
					com.stormtroopercs.materialreader.compose.ui.AccountListScreen(
						onNavigateBack = { navigator.goBack() },
						onNavigateToLogin = { navigator.navigate(OAuthLogin) },
					)
				}
			},
		)
	}
}

/**
 * True when [subreddit] is a community feed (r/<name>) — the feeds that
 * open in the signature swipe feed by default.
 */
private fun isCommunityFeedPath(subreddit: String): Boolean = subreddit.isNotBlank() &&
	!subreddit.startsWith("u/") &&
	!subreddit.startsWith("m/") &&
	!subreddit.startsWith("s/") &&
	subreddit != "frontpage" &&
	subreddit != "popular" &&
	subreddit != "all"
