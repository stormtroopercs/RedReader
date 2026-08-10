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
                }
            )
        }

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

        composable(Screen.Settings.path) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
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
}
