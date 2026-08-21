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

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

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
                MainScreen(
                    onNavigateToPostList = { subreddit ->
                        navigator.navigate(PostList(subreddit))
                    },
                    onNavigateToSettings = {
                        navigator.navigate(Settings)
                    }
                )
            }

            // Top-level: Settings screen
            entry<Settings> {
                SettingsScreen(
                    onNavigateBack = { navigator.goBack() }
                )
            }

            // Child: Post list
            entry<PostList> { key ->
                RealPostListScreen(
                    subreddit = key.subreddit,
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
                    onNavigateBack = { navigator.goBack() }
                )
            }

            // Child: User profile
            entry<UserProfile> { key ->
                org.quantumbadger.redreader.compose.ui.UserProfileScreen(
                    username = key.username,
                    onNavigateBack = { navigator.goBack() },
                    onNavigateToPosts = {
                        navigator.navigate(PostList("u/${key.username}/submitted"))
                    },
                    onNavigateToComments = {
                        navigator.navigate(PostList("u/${key.username}/comments"))
                    },
                    onSendMessage = {
                        navigator.navigate(CommentReply(postId = "", commentId = null))
                    }
                )
            }

            // Child: Inbox
            entry<Inbox> {
                org.quantumbadger.redreader.compose.ui.InboxScreen(
                    onNavigateBack = { navigator.goBack() },
                    onMarkAllRead = {
                        // TODO: wire up inbox mark-all-read
                    },
                    onSendMessage = {
                        navigator.navigate(CommentReply(postId = "", commentId = null))
                    }
                )
            }

            // Child: Post submit
            entry<PostSubmit> { key ->
                org.quantumbadger.redreader.compose.ui.PostSubmitScreen(
                    subreddit = key.subreddit,
                    onNavigateBack = { navigator.goBack() },
                    onSubmit = {
                        // TODO: wire up post submission
                    },
                    onNavigateToSubredditPicker = {
                        navigator.navigate(SubredditSearch)
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
                    postId = key.postId,
                    commentId = key.commentId,
                    onNavigateBack = { navigator.goBack() },
                    onSubmit = { body ->
                        // TODO: wire up comment reply submission
                    }
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
                org.quantumbadger.redreader.compose.ui.HtmlViewScreen(
                    html = key.html,
                    title = key.title,
                    onNavigateBack = { navigator.goBack() }
                )
            }

            // Child: OAuth Login
            entry<OAuthLogin> {
                org.quantumbadger.redreader.compose.ui.OAuthLoginScreen(
                    onOAuthComplete = { callbackUrl ->
                        // TODO: handle OAuth callback
                        navigator.goBack()
                    },
                    onOAuthError = { error ->
                        // TODO: show error dialog
                        navigator.goBack()
                    }
                )
            }
        }
    )
}
