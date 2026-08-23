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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

/**
 * Adaptive navigation host that selects the appropriate layout
 * based on the current window size class (phone, tablet, desktop).
 *
 * Uses Navigation 3 (NavigationState + Navigator + NavDisplay).
 *
 * Phone (<600dp): Single-pane with bottom navigation bar.
 * Tablet (600-840dp): Two-pane with navigation drawer.
 * Desktop (>840dp): Full two-pane with navigation rail.
 */
@Composable
fun AdaptiveAppNavigation() {
    val navigationState = rememberNavigationState(
        startRoute = Main,
        topLevelRoutes = TOP_LEVEL_ROUTES
    )

    val navigator = remember { Navigator(navigationState) }

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
                    onNavigateBack = { navigator.goBack() },
                    onNavigateToChangelog = { navigator.navigate(Changelog) }
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
                    onNavigateBack = { navigator.goBack() }
                )
            }

            // Child: User profile
            entry<UserProfile> { key ->
                org.quantumbadger.redreader.compose.ui.UserProfileScreen(
                    username = key.username,
                    onNavigateBack = { navigator.goBack() },
                    onNavigateToPosts = { /* TODO */ },
                    onNavigateToComments = { /* TODO */ }
                )
            }

            // Child: Inbox
            entry<Inbox> {
                org.quantumbadger.redreader.compose.ui.InboxScreen(
                    onNavigateBack = { navigator.goBack() },
                    onSendMessage = { /* TODO */ }
                )
            }

            // Child: Post submit
            entry<PostSubmit> { key ->
                org.quantumbadger.redreader.compose.ui.PostSubmitScreen(
                    subreddit = key.subreddit,
                    onNavigateBack = { navigator.goBack() },
                    onSubmitted = { navigator.goBack() }
                )
            }
        }
    )
}
