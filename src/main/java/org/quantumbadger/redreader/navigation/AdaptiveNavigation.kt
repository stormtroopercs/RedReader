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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationDrawerSheet
import androidx.compose.material3.NavigationDrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import org.quantumbadger.redreader.compose.activity.ComposeBaseActivity
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneLayoutInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.ui.platform.LocalConfiguration
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.time.TimestampUTC
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Adaptive navigation host that selects the appropriate layout
 * based on the current window size class (phone, tablet, desktop).
 *
 * Phone (<600dp): Single-pane with drawer navigation.
 * Tablet (600-840dp): Two-pane with sidebar.
 * Desktop (>840dp): Full two-pane with top-level navigation.
 */
@Composable
fun AdaptiveAppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val windowWidthDp = LocalConfiguration.current.screenWidthDp
    val adaptiveType = when {
        windowWidthDp < 600 -> NavigationSuiteType.NavigationBar
        windowWidthDp < 840 -> NavigationSuiteType.NavigationDrawer
        else -> NavigationSuiteType.NavigationRail
    }

    NavigationSuiteScaffold(
        navigationSuite = {
            when (adaptiveType) {
                NavigationSuiteType.NavigationBar -> {
                    // Phone: Bottom navigation bar
                    PhoneNavigationBar(
                        navController = navController,
                        currentDestination = currentDestination
                    )
                }
                NavigationSuiteType.NavigationDrawer -> {
                    // Tablet: Side drawer
                    TabletNavigationDrawer(
                        navController = navController,
                        currentDestination = currentDestination,
                        drawerState = drawerState,
                        scope = scope
                    )
                }
                NavigationSuiteType.NavigationRail -> {
                    // Desktop: Navigation rail
                    DesktopNavigationRail(
                        navController = navController,
                        currentDestination = currentDestination
                    )
                }
                else -> {
                    // Fallback: Navigation rail
                    DesktopNavigationRail(
                        navController = navController,
                        currentDestination = currentDestination
                    )
                }
            }
        }
    ) {
        AdaptiveNavHost(
            navController = navController,
            currentDestination = currentDestination
        )
    }
}

@Composable
private fun PhoneNavigationBar(
    navController: NavHostController,
    currentDestination: androidx.navigation.NavDestination?
) {
    // Bottom bar with minimal items
    androidx.compose.material3.BottomNavigation {
        BottomNavigationItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentDestination?.route == "main"
        ) {
            navController.navigate("main") {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        BottomNavigationItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentDestination?.route == "settings"
        ) {
            navController.navigate("settings") {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
}

@Composable
private fun TabletNavigationDrawer(
    navController: NavHostController,
    currentDestination: androidx.navigation.NavDestination?,
    drawerState: NavigationDrawerState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Column(modifier = Modifier.width(280.dp)) {
        androidx.compose.material3.Divider()

        DrawerItem(
            icon = Icons.Default.Home,
            label = "Home",
            selected = currentDestination?.route == "main"
        ) {
            scope.launch { drawerState.close() }
            navController.navigate("main") {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
            }
        }

        DrawerItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            selected = currentDestination?.route == "settings"
        ) {
            scope.launch { drawerState.close() }
            navController.navigate("settings") {
                launchSingleTop = true
            }
        }
    }
}

@Composable
private fun DesktopNavigationRail(
    navController: NavHostController,
    currentDestination: androidx.navigation.NavDestination?
) {
    Column(modifier = Modifier.width(72.dp)) {
        androidx.compose.material3.Divider()

        androidx.compose.material3.NavigationRailItem(
            icon = {
                Icon(
                    Icons.Default.Home,
                    contentDescription = "Home",
                    modifier = Modifier.padding(4.dp)
                )
            },
            label = { Text("Home") },
            selected = currentDestination?.route == "main"
        ) {
            navController.navigate("main") {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        androidx.compose.material3.NavigationRailItem(
            icon = {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.padding(4.dp)
                )
            },
            label = { Text("Settings") },
            selected = currentDestination?.route == "settings"
        ) {
            navController.navigate("settings") {
                launchSingleTop = true
            }
        }
    }
}

@Composable
private fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, contentDescription = null) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

/**
 * Main adaptive nav host that renders the correct screens
 * based on window size class.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun AdaptiveNavHost(
    navController: NavHostController,
    currentDestination: androidx.navigation.NavDestination?
) {
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val layoutInfo = windowAdaptiveInfo.calculatePaneLayoutInfo()

    val paneScope = androidx.compose.runtime.Composable {
        // Content for the current pane based on window size
    }

    // Determine layout type from window size class
    val layoutType = when (windowAdaptiveInfo.windowSizeClass) {
        is WindowAdaptiveInfo.WindowWidthSizeClass.Compact,
        is WindowAdaptiveInfo.WindowHeightSizeClass.Compact -> {
            // Phone: Single pane, all screens in stack
            SinglePaneNavHost(navController)
        }
        is WindowAdaptiveInfo.WindowWidthSizeClass.Medium,
        is WindowAdaptiveInfo.WindowHeightSizeClass.Medium -> {
            // Tablet: Two-pane with master-detail
            TwoPaneNavHost(navController)
        }
        is WindowAdaptiveInfo.WindowWidthSizeClass.Expanded,
        is WindowAdaptiveInfo.WindowHeightSizeClass.Expanded -> {
            // Desktop: Two-pane with sidebar detail
            DesktopTwoPaneNavHost(navController)
        }
        else -> {
            // Fallback: Single pane
            SinglePaneNavHost(navController)
        }
    }

    // Render the appropriate layout
    layoutType
}

@Composable
private fun SinglePaneNavHost(navController: NavHostController) {
    // Single pane: All screens in a single column with navigation
    Column(modifier = Modifier.fillMaxSize()) {
        androidx.navigation.compose.NavHost(
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
                    androidx.navigation.navArgument("subreddit") { defaultValue = "" }
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
                    androidx.navigation.navArgument("postId") { defaultValue = "" }
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
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun TwoPaneNavHost(navController: NavHostController) {
    // Tablet: Two-pane layout with sidebar
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()

    // Check if we're in two-pane capable mode
    if (windowAdaptiveInfo.windowSizeClass is WindowAdaptiveInfo.WindowWidthSizeClass.Medium
        || windowAdaptiveInfo.windowSizeClass is WindowAdaptiveInfo.WindowWidthSizeClass.Expanded
    ) {
        // Two-pane: Show list on left, detail on right
        androidx.compose.material3.adaptive.layout.TwoPane(
            firstPane = {
                // List pane (subreddit list or post list)
                Column(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.material3.Text(
                        text = "Tablet Layout",
                        modifier = Modifier.padding(16.dp)
                    )
                    androidx.compose.material3.Text(
                        text = "Subreddit list on left, post detail on right",
                        modifier = Modifier.padding(16.dp)
                    )

                    // In a real implementation, this would show the subreddit list
                    // or post list in the first pane and the post detail in the second
                }
            },
            secondPane = {
                // Detail pane
                Column(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.material3.Text(
                        text = "Post detail area",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            },
            twoPaneSizingMode = androidx.compose.material3.adaptive.layout.TwoPaneSizingMode.Maximized
        )
    } else {
        // Fallback to single pane
        SinglePaneNavHost(navController)
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun DesktopTwoPaneNavHost(navController: NavHostController) {
    // Desktop: Full two-pane with top-level navigation
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()

    if (windowAdaptiveInfo.windowSizeClass is WindowAdaptiveInfo.WindowWidthSizeClass.Expanded) {
        // Desktop layout with sidebar
        androidx.compose.material3.adaptive.layout.TwoPane(
            firstPane = {
                // Sidebar with navigation items
                Column(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.material3.Text(
                        text = "Desktop Navigation",
                        modifier = Modifier.padding(16.dp)
                    )

                    // Navigation items for desktop
                    listOf("Home", "Popular", "All", "Saved").forEach { item ->
                        androidx.compose.material3.Text(
                            text = item,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .clickable {
                                    // Handle navigation
                                }
                        )
                    }
                }
            },
            secondPane = {
                // Main content area
                Column(modifier = Modifier.fillMaxSize()) {
                    androidx.compose.material3.Text(
                        text = "Main Content Area",
                        modifier = Modifier.padding(16.dp)
                    )
                    // This would contain the main screen content
                }
            },
            twoPaneSizingMode = androidx.compose.material3.adaptive.layout.TwoPaneSizingMode.Maximized
        )
    } else {
        // Fallback to two-pane
        TwoPaneNavHost(navController)
    }
}

/**
 * Screen route definitions (kept in sync with AppNavigation.kt).
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

/**
 * Utility functions for adaptive layout detection.
 */
object AdaptiveLayoutUtils {

    /**
     * Returns true if the device is a tablet (>= 600dp width).
     */
    fun isTabletDevice(context: android.content.Context): Boolean {
        val configuration = context.resources.configuration
        return configuration.screenWidthDp >= 600
    }

    /**
     * Returns the current window size class (Compact, Medium, Expanded).
     */
    fun getWindowSizeClass(
        context: android.content.Context
    ): WindowSizeClass {
        val widthDp = LocalConfiguration.current.screenWidthDp
        return when {
            widthDp < 600 -> WindowSizeClass.Compact
            widthDp < 840 -> WindowSizeClass.Medium
            else -> WindowSizeClass.Expanded
        }
    }

    enum class WindowSizeClass {
        Compact,   // Phone (< 600dp)
        Medium,    // Tablet (600-840dp)
        Expanded   // Desktop (> 840dp)
    }
}
