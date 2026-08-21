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

package org.quantumbadger.redreader.activities

import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import org.quantumbadger.redreader.compose.activity.ComposeBaseActivity
import org.quantumbadger.redreader.navigation.AppNavGraph
import org.quantumbadger.redreader.navigation.Main
import org.quantumbadger.redreader.navigation.NavigationState
import org.quantumbadger.redreader.navigation.Navigator
import org.quantumbadger.redreader.navigation.TOP_LEVEL_ROUTES

/**
 * Compose-based MainActivity using Navigation 3.
 * Replaces the legacy Fragment-based MainActivity.
 *
 * The [NavigationState] is owned by the Activity (not the composition) so the
 * system back button can pop it: Navigation 3 at these androidx versions does
 * not expose a back API to the Activity, so back navigation is driven through
 * [baseActivityOnBackPressed].
 */
class MainActivityCompose : ComposeBaseActivity() {

    private val navigationState = NavigationState(
        startRoute = Main,
        topLevelRoute = mutableStateOf(Main),
        backStacks = TOP_LEVEL_ROUTES.associateWith { key ->
            mutableStateListOf<NavKey>(key)
        }
    )

    private val navigator = Navigator(navigationState)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentCompose {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AppNavGraph(navigationState)
            }
        }
    }

    /**
     * Route system back into the Navigation 3 back stack. Consumes the press
     * while there is anything to go back to; at the root it falls through to
     * the default behaviour (finish the activity). This is the back path on
     * every API level, since the legacy OnBackPressedCallback (see
     * BaseActivity) is the registered handler for the system back button.
     */
    override fun baseActivityOnBackPressed(): Boolean {
        if (navigationState.canGoBack()) {
            navigator.goBack()
            return true
        }
        return false
    }

    override fun baseActivityMustInterceptBack(): Boolean {
        return navigationState.canGoBack()
    }
}
