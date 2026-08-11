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

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * App-wide navigation graph.
 * Replaces Fragment-based navigation with Compose Navigation.
 * Includes all screens: Main, PostList, CommentList, Settings, UserProfile, Inbox, PostSubmit.
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Main.path
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Main screen
        composable(Screen.Main.path) {
            MainScreen(
                onNavigateToPostList = { subreddit ->
                    navController.navigate(Screen.PostList.createRoute(subreddit))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.path)
                }
            )
        }

        // Post list screen
        composable(
            route = Screen.PostList.path,
            arguments = listOf(
                navArgument("subreddit") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val subreddit = backStackEntry.arguments?.getString("subreddit") ?: ""
            PostListScreen(
                subreddit = subreddit,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCommentList = { postId ->
                    navController.navigate(Screen.CommentList.createRoute(postId))
                },
                onNavigateToUserProfile = { username ->
                    navController.navigate(Screen.UserProfile.createRoute(username))
                },
                onNavigateToPostSubmit = {
                    navController.navigate(Screen.PostSubmit.createRoute(subreddit))
                }
            )
        }

        // Comment list screen
        composable(
            route = Screen.CommentList.path,
            arguments = listOf(
                navArgument("postId") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            CommentListScreen(
                postId = postId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings screen
        composable(Screen.Settings.path) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // User profile screen
        composable(
            route = Screen.UserProfile.path,
            arguments = listOf(
                navArgument("username") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            org.quantumbadger.redreader.compose.ui.UserProfileScreen(
                username = username,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPosts = { /* TODO: Navigate to user posts */ },
                onNavigateToComments = { /* TODO: Navigate to user comments */ },
                onSendMessage = { /* TODO: Navigate to message screen */ }
            )
        }

        // Inbox screen
        composable(Screen.Inbox.path) {
            org.quantumbadger.redreader.compose.ui.InboxScreen(
                onNavigateBack = { navController.popBackStack() },
                onMarkAllRead = { /* TODO: Mark all as read */ },
                onSendMessage = { /* TODO: Navigate to send message */ }
            )
        }

        // Post submit screen
        composable(
            route = Screen.PostSubmit.path,
            arguments = listOf(
                navArgument("subreddit") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val subreddit = backStackEntry.arguments?.getString("subreddit") ?: ""
            org.quantumbadger.redreader.compose.ui.PostSubmitScreen(
                subreddit = subreddit,
                onNavigateBack = { navController.popBackStack() },
                onSubmit = { /* TODO: Submit post */ },
                onNavigateToSubredditPicker = { /* TODO: Navigate to subreddit picker */ }
            )
        }
    }
}

/**
 * Screen route definitions.
 */
sealed class Screen(val path: String) {
    object Main : Screen("main")
    object PostList : Screen("post_list/{subreddit}") {
        fun createRoute(subreddit: String) = "post_list/$subreddit"
    }
    object CommentList : Screen("comment_list/{postId}") {
        fun createRoute(postId: String) = "comment_list/$postId"
    }
    object Settings : Screen("settings")
    object UserProfile : Screen("user_profile/{username}") {
        fun createRoute(username: String) = "user_profile/$username"
    }
    object Inbox : Screen("inbox")
    object PostSubmit : Screen("post_submit/{subreddit}") {
        fun createRoute(subreddit: String) = "post_submit/$subreddit"
    }
}
