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

package org.quantumbadger.redreader.compose.adaptive

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.quantumbadger.redreader.navigation.*

/**
 * Adaptive navigation container for RedReader.
 * - On phones: Single-pane navigation with back stack
 * - On tablets/desktop: Two-pane layout with master-detail view
 *
 * Uses WindowSizeClass to determine layout mode:
 * - Compact width (phone): Single pane, full-screen navigation
 * - Medium/Expanded width (tablet): Two-pane master-detail
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveNavContainer(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    // In a real implementation, we'd pass WindowSizeClass from the host
    // For now, we'll use a simple heuristic based on available width
    var isTwoPane by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // TODO: Integrate with Accompanist/Window Size Class library
        // For now, default to single-pane (phone) layout
        isTwoPane = false
    }

    if (isTwoPane) {
        TwoPaneLayout(navController)
    } else {
        SinglePaneLayout(navController)
    }
}

@Composable
private fun SinglePaneLayout(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.path
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

@Composable
private fun TwoPaneLayout(navController: NavHostController) {
    var selectedPostId by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Master pane - post list
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            PostListScreen(
                subreddit = "frontpage",
                onNavigateBack = { /* no back in two-pane */ },
                onNavigateToCommentList = { postId ->
                    selectedPostId = postId
                    navController.navigate(Screen.CommentList.createRoute(postId))
                }
            )
        }

        // Detail pane - comment list or empty state
        if (selectedPostId != null) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                CommentListScreen(
                    postId = selectedPostId!!,
                    onNavigateBack = { selectedPostId = null }
                )
            }
        } else {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.PostAdd,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Select a post to view comments",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen route definitions for adaptive navigation.
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
