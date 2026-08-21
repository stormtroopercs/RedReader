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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test for SettingsScreen.
 * Verifies that settings screen displays correctly and interactions work.
 */
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_displaysTitle() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        onNavigateBack = { /* no-op */ }
                    )
                }
            }
        }

        // Verify settings title is displayed
        composeTestRule.onNodeWithText("Settings").assertExists()
    }

    @Test
    fun settingsScreen_displaysCategories() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        onNavigateBack = { /* no-op */ }
                    )
                }
            }
        }

        // Verify settings categories are displayed
        composeTestRule.onNodeWithText("Appearance").assertExists()
        composeTestRule.onNodeWithText("Behaviour").assertExists()
        composeTestRule.onNodeWithText("Network").assertExists()
        composeTestRule.onNodeWithText("Cache").assertExists()
        composeTestRule.onNodeWithText("About").assertExists()
    }

    @Test
    fun settingsScreen_backButtonWorks() {
        var backClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        onNavigateBack = { backClicked = true }
                    )
                }
            }
        }

        // Click back button
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Verify back navigation was triggered
        assert(backClicked) { "Back navigation should have been triggered" }
    }
}
