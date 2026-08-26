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
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.\
 ******************************************************************************/

package org.quantumbadger.redreader.navigation

import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.account.RedditAccountChangeListener
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.common.AssetHelper
import org.quantumbadger.redreader.common.RunnableOnce
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.reddit.api.RedditOAuth

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

    NavDisplay(
        backStack = navigationState.activeBackStack,
        onBack = { navigator.goBack() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            // Top-level: Main screen
            entry<Main> {
                val context = LocalContext.current
                val accountManager = remember { RedditAccountManager.getInstance(context) }
                val accountName = remember {
                    mutableStateOf(accountManager.defaultAccount.username)
                }
                DisposableEffect(accountManager) {
                    val listener = RedditAccountChangeListener {
                        accountName.value = accountManager.defaultAccount.username
                    }
                    accountManager.addUpdateListener(listener)
                    onDispose { accountManager.removeUpdateListener(listener) }
                }
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
                    }
                )
            }

            // Top-level: Settings screen
            entry<Settings> {
                val context = LocalContext.current
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
                        navigator.navigate(HtmlView(html, context.getString(R.string.title_license)))
                    }
                )
            }

            // Child: Post list
            entry<PostList> { key ->
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
                    }
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
                    }
                )
            }

            // Child: User profile
            entry<UserProfile> { key ->
                // Same entry-scoped instance the screen resolves itself.
                val userProfileViewModel = androidx.hilt.navigation.compose.hiltViewModel<UserProfileViewModel>()
                org.quantumbadger.redreader.compose.ui.UserProfileScreen(
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
                    }
                )
            }

            // Child: Inbox
            entry<Inbox> {
                org.quantumbadger.redreader.compose.ui.InboxScreen(
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
                                subject = "Re: ${message.subject ?: ""}"
                            )
                        )
                    }
                )
            }

            // Child: Post submit
            entry<PostSubmit> { key ->
                org.quantumbadger.redreader.compose.ui.PostSubmitScreen(
                    subreddit = key.subreddit,
                    shareUrl = key.shareUrl,
                    onNavigateBack = { navigator.goBack() },
                    onSubmitted = {
                        navigator.goBack()
                    }
                )
            }

            // Child: Subreddit search
            entry<SubredditSearch> {
                org.quantumbadger.redreader.compose.ui.SubredditSearchScreen(
                    onNavigateBack = { navigator.goBack() },
                    onSubredditSelected = { subreddit ->
                        navigator.navigate(PostList(subreddit))
                    }
                )
            }

            // Child: Comment reply
            entry<CommentReply> { key ->
                org.quantumbadger.redreader.compose.ui.CommentReplyScreen(
                    parentThingId = key.parentThingId,
                    onDone = { navigator.goBack() },
                    onNavigateBack = { navigator.goBack() }
                )
            }

            // Child: Comment / post edit
            entry<CommentEdit> { key ->
                org.quantumbadger.redreader.compose.ui.CommentEditScreen(
                    idAndType = org.quantumbadger.redreader.reddit.kthings.RedditIdAndType(
                        key.idAndType
                    ),
                    initialText = key.initialText,
                    isSelfPost = key.isSelfPost,
                    onDone = { navigator.goBack() },
                    onNavigateBack = { navigator.goBack() }
                )
            }

            // Child: PM composer
            entry<PMSend> { key ->
                org.quantumbadger.redreader.compose.ui.PMSendScreen(
                    initialRecipient = key.recipient,
                    initialSubject = key.subject,
                    initialText = key.text,
                    onDone = { navigator.goBack() },
                    onNavigateBack = { navigator.goBack() }
                )
            }

            // Child: Reddit Terms
            entry<RedditTerms> {
                org.quantumbadger.redreader.compose.ui.RedditTermsScreen(
                    onDone = { navigator.goBack() }
                )
            }

            // Child: Changelog
            entry<Changelog> {
                org.quantumbadger.redreader.compose.ui.ChangelogScreen(
                    onNavigateBack = { navigator.goBack() }
                )
            }

            // Child: Bug Report
            entry<BugReport> {
                org.quantumbadger.redreader.compose.ui.BugReportScreen(
                    onNavigateBack = { navigator.goBack() }
                )
            }

            // Child: WebView (URL)
            entry<WebViewRoute> { key ->
                org.quantumbadger.redreader.compose.ui.WebViewScreen(
                    url = key.url,
                    title = key.title,
                    onNavigateBack = { navigator.goBack() }
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
                org.quantumbadger.redreader.compose.ui.HtmlViewScreen(
                    html = key.html,
                    title = key.title,
                    onNavigateBack = { navigator.goBack() },
                    onWebViewCreated = { HtmlViewBackHandler.register(it) }
                )
            }

            // Child: Album (imgur / reddit gallery)
            entry<Album> { key ->
                org.quantumbadger.redreader.compose.ui.AlbumScreen(
                    albumUrl = UriString(key.url),
                    onBackPressed = { navigator.goBack() }
                )
            }

            // Child: full-screen still-image viewer (direct image URLs)
            entry<Image> { key ->
                org.quantumbadger.redreader.compose.ui.ImageScreen(
                    url = UriString(key.url),
                    isGif = key.isGif,
                    isVideo = key.isVideo,
                    albumUrl = key.albumUrl?.let { UriString(it) },
                    albumIndex = key.albumIndex,
                    onBackPressed = { navigator.goBack() }
                )
            }

            // Child: OAuth Login
            entry<OAuthLogin> {
                val context = LocalContext.current
                org.quantumbadger.redreader.compose.ui.OAuthLoginScreen(
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
                            RunnableOnce(Runnable { navigator.goBack() })
                        )
                    },
                    onOAuthError = { error ->
                        Log.e("AppNavigation", "OAuth failed: $error")
                        navigator.goBack()
                    }
                )
            }
        }
    )
}
