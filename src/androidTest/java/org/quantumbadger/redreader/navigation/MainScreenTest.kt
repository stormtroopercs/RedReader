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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test for MainScreen.
 * Verifies that the main screen displays correctly and navigation works.
 */
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainScreen_displaysSubredditOptions() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        onNavigateToPostList = { /* no-op */ },
                        onNavigateToSettings = { /* no-op */ }
                    )
                }
            }
        }

        // Verify subreddit options are displayed
        composeTestRule.onNodeWithText("frontpage").assertExists()
        composeTestRule.onNodeWithText("popular").assertExists()
        composeTestRule.onNodeWithText("all").assertExists()
    }

    @Test
    fun mainScreen_frontpage_clickTriggersNavigation() {
        var navigatedTo = false
        var subreddit = ""

        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        onNavigateToPostList = { sub ->
                            navigatedTo = true
                            subreddit = sub
                        },
                        onNavigateToSettings = { /* no-op */ }
                    )
                }
            }
        }

        // Click on frontpage
        composeTestRule.onNodeWithText("frontpage").performClick()

        // Verify navigation was triggered
        assert(navigatedTo) { "Navigation should have been triggered" }
        assert(subreddit == "frontpage") { "Subreddit should be 'frontpage'" }
    }

    @Test
    fun mainScreen_settings_clickTriggersNavigation() {
        var settingsNavigated = false

        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        onNavigateToPostList = { /* no-op */ },
                        onNavigateToSettings = { settingsNavigated = true }
                    )
                }
            }
        }

        // Click on settings button (it's an Icon, addressed by content description)
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        // Verify settings navigation was triggered
        assert(settingsNavigated) { "Settings navigation should have been triggered" }
    }
}
