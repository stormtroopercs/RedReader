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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey
import androidx.compose.runtime.saveable.Saver
import androidx.navigation3.runtime.serialization.NavKeySerializer
import kotlinx.serialization.json.Json

private val navKeySaver = Saver<NavKey, String>(
    save = { key -> Json.encodeToString(NavKeySerializer(), key) },
    restore = { json -> Json.decodeFromString(NavKeySerializer(), json) }
)

/**
 * Create a navigation state that persists config changes and process death.
 *
 * @param startRoute - the start route. The user will exit the app through this route.
 * @param topLevelRoutes - all top-level routes (displayed in navigation bar/rail/drawer).
 */
@Composable
fun rememberNavigationState(
    startRoute: NavKey,
    topLevelRoutes: Set<NavKey>
): NavigationState {
    val topLevelRoute = rememberSaveable(stateSaver = navKeySaver) {
        mutableStateOf(startRoute)
    }

    // Maintain a back stack for each top-level route
    val backStacks = topLevelRoutes.associateWith { key ->
        remember { mutableStateListOf(key) }
    }

    return remember(startRoute, topLevelRoutes) {
        NavigationState(
            startRoute = startRoute,
            topLevelRoute = topLevelRoute,
            backStacks = backStacks
        )
    }
}

/**
 * State holder for navigation state.
 *
 * @param startRoute - the start route. The user will exit the app through this route.
 * @param topLevelRoute - the current top level route
 * @param backStacks - the back stacks for each top level route
 */
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, SnapshotStateList<NavKey>>
) {
    var topLevelRoute: NavKey by topLevelRoute
        private set

    /**
     * Switch the active top-level route (tab switch).
     */
    fun switchTopLevel(route: NavKey) {
        topLevelRoute = route
    }

    /**
     * Land directly on [child] under the [root] top-level route: switch to
     * [root] (resetting its back stack to the base) and push [child]. Used for
     * cold-start deep links (e.g. tapping a notification) where the app should
     * open on a child screen of a top-level route rather than at the root.
     */
    fun navigateTo(root: NavKey, child: NavKey) {
        topLevelRoute = root
        val stack = backStacks[root] ?: return
        if (stack.size != 1 || stack[0] != root) {
            stack.clear()
            stack.add(root)
        }
        stack.add(child)
    }

    /**
     * The back stack for the currently active top-level route. This is the
     * list passed to NavDisplay: it is a live [SnapshotStateList], so
     * pushing/popping child routes (or switching top-level routes) recomposes
     * the display.
     */
    val activeBackStack: List<NavKey>
        get() = backStacks[topLevelRoute] ?: emptyList()

    /**
     * Whether a back action is available from the current position: a child
     * entry to pop on the active top-level stack, or a non-start top-level
     * route to return from.
     */
    fun canGoBack(): Boolean {
        val stack = backStacks[topLevelRoute] ?: return false
        return stack.size > 1 || topLevelRoute != startRoute
    }
}
