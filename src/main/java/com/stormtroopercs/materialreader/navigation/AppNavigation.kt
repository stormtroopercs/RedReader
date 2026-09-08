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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
	// Part 1: OAuth failures surface in a dialog instead of a silent goBack() —
	// the old path popped the login screen and dropped the user on the app home
	// with no trace of why login failed (only a logcat line).
	val oAuthError = remember { mutableStateOf<String?>(null) }

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
	// Opens a video post in the full-screen video overlay (the feed's video
	// media tap → its static preview still, tapped to the player with the
	// centered mute / play / pause / fullscreen controls). For a video post
	// `url` is the mp4 stream (Reddit `media.reddit_video.fallback_url`),
	// `videoPreviewUrl` the `reddit_video_preview` still.
	val openVideo: (PostItem) -> Unit = { post ->
		val url = post.url?.takeIf { it.isNotBlank() && !it.startsWith("reddit.com") }
		if (url != null) {
			navigator.navigate(
				VideoPlayer(
					url = url,
					previewUrl = post.videoPreviewUrl?.takeIf { it.isNotBlank() },
				),
			)
		}
	}
	// Opens a **link post's** article in the in-app WebView route (a tap on
	// its resolved og:image preview): the post's `url` is the linked page
	// (the news article), not an image.
	val openLink: (PostItem) -> Unit = { post ->
		val url = post.url?.takeIf { it.isNotBlank() && !it.contains("reddit.com") }
		if (url != null) {
			navigator.navigate(WebViewRoute(url = url, title = post.title))
		}
	}
	DisposableEffect(accountManager) {
		val listener = RedditAccountChangeListener {
			accountName.value = accountManager.defaultAccount.username
		}
		accountManager.addUpdateListener(listener)
		onDispose { accountManager.removeUpdateListener(listener) }
	}

	// The feed surface (FINAL-DESIGN Phase 4.7), shared by the Posts-tab home
	// feed ([Main]) and any pushed [PostList] child. The feed's persisted view
	// mode decides the surface: Slides → the signature swipe feed; every other
	// mode → the list view in that card mode. It is read reactively so a
	// Change-View selection swaps the surface in place (recomposition) instead
	// of re-navigating — a re-navigation pushed a second, equal NavKey entry
	// and blanked the screen.
	@Composable
	fun feedScreen(
		subreddit: String,
		searchQuery: String?,
		isTabRoot: Boolean,
		titleOverride: String?,
	) {
		val viewMode = FeedPreferences.effectiveViewMode(
			FeedPreferences.effectiveKey(subreddit, searchQuery),
		)
		if (viewMode == PostViewMode.SLIDES) {
			RealSlidesFeedScreen(
				subreddit = subreddit,
				searchQuery = searchQuery,
				onNavigateBack = { navigator.goBack() },
				onNavigateToCommentList = { postId ->
					navigator.navigate(CommentList(postId))
				},
				onNavigateToUserProfile = { username ->
					navigator.navigate(UserProfile(username))
				},
				onNavigateToPostSubmit = {
					navigator.navigate(PostSubmit(subreddit))
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
				// The community pill opens the community detail (Phase 6.3);
				// non-community feeds fall back to the community search.
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
				onOpenLicense = openLicense,
				onOpenMedia = openMedia,
				onOpenVideo = openVideo,
				onOpenLink = openLink,
				isTabRoot = isTabRoot,
				titleOverride = titleOverride,
			)
		} else {
			RealPostListScreen(
				subreddit = subreddit,
				searchQuery = searchQuery,
				onNavigateBack = { navigator.goBack() },
				onNavigateToCommentList = { postId ->
					navigator.navigate(CommentList(postId))
				},
				onNavigateToUserProfile = { username ->
					navigator.navigate(UserProfile(username))
				},
				onNavigateToPostSubmit = {
					navigator.navigate(PostSubmit(subreddit))
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
				onOpenVideo = openVideo,
				onOpenLink = openLink,
				onOpenSubreddits = {
					navigator.navigate(Subreddits)
				},
				isTabRoot = isTabRoot,
				titleOverride = titleOverride,
			)
		}
	}

	AppShell(
		navigationState = navigationState,
		accountName = accountName.value,
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
				// Top-level: Posts tab = the user's home feed (FINAL-DESIGN
				// Phase 2.1): the frontpage listing (Reddit `/`, the user's
				// personalized feed when authenticated). Rendered as the tab
				// root: no back arrow, the tab title, and the drawer hamburger
				// when a drawer is enabled. The legacy menu screen (search field
				// + frontpage/popular/all rows) is retired — search lives in the
				// feed's top bar, and the subreddit directory lives in the drawer.
				entry<Main> {
					feedScreen(
						subreddit = "frontpage",
						searchQuery = null,
						isTabRoot = true,
						titleOverride = "Posts",
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
				// surface (FINAL-DESIGN Phase 4.7) — see [feedScreen].
				entry<PostList> { key ->
					feedScreen(
						subreddit = key.subreddit,
						searchQuery = key.searchQuery,
						isTabRoot = false,
						titleOverride = null,
					)
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

				// Child: the signed-in user's subscribed subreddits (the Posts
				// tab's Subreddits chip; the same list the drawer's Subscriptions
				// section shows — one MainScreenViewModel per entry).
				entry<Subreddits> {
					SubredditsScreen(
						onNavigateBack = { navigator.goBack() },
						onOpenSubreddit = { subreddit ->
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
						onOpenVideo = openVideo,
						onOpenLink = openLink,
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

				// Child: Full-screen video player (a feed video post's media tap).
				// The screen resolves the post's mp4 stream (same path ImageScreen's
				// video tab uses) and plays it with the centered translucent
				// mute / play-pause / fullscreen controls.
				entry<VideoPlayer> { key ->
					VideoPlayerOverlayScreen(
						url = key.url,
						previewUrl = key.previewUrl,
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
							// Part 1: show the error on screen (dialog on the home
							// screen) instead of a silent goBack() — the user must
							// know why login failed, not just be dropped on the
							// front page.
							Log.e("AppNavigation", "OAuth failed: $error")
							oAuthError.value = error
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

		// Part 1: the OAuth error dialog. Shown above whatever route is
		// current — the login screen has already been popped, and the user must
		// not be dropped on the home screen without knowing why login failed.
		oAuthError.value?.let { error ->
			AlertDialog(
				onDismissRequest = { oAuthError.value = null },
				title = { Text("Login failed") },
				text = { Text(error) },
				confirmButton = {
					TextButton(onClick = { oAuthError.value = null }) {
						Text("OK")
					}
				},
			)
		}
	}
}

/**
 * The entry-point for a listing that may not be a community: community
 * names route through the community detail (Phase 6.3) — the other feeds
 * (frontpage, user, multireddit, search) open the standard list feed.
 */
private fun isCommunityFeedPath(subreddit: String): Boolean = FeedPreferences.isCommunityFeedPath(subreddit)
