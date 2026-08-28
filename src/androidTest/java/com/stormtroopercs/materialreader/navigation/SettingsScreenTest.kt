/*******************************************************************************
 * This file is part of MaterialReader.
 *
 * MaterialReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MaterialReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MaterialReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.stormtroopercs.materialreader.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

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
                        onNavigateBack = { /* no-op */ },
                        onNavigateToChangelog = { /* no-op */ }
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
                        onNavigateBack = { /* no-op */ },
                        onNavigateToChangelog = { /* no-op */ }
                    )
                }
            }
        }

        // Verify the settings categories are all reachable. They are laid out
        // in a LazyColumn, so only the first two are composed in the initial
        // viewport — scroll the list and collect the category headers as they
        // come into view.
        val expected = listOf("Appearance", "Behaviour", "Network", "Cache", "About")
        val seen = mutableSetOf<String>()

        fun recordVisible() {
            for (category in expected) {
                if (composeTestRule
                        .onAllNodesWithText(category)
                        .fetchSemanticsNodes().isNotEmpty()
                ) {
                    seen += category
                }
            }
        }

        recordVisible()
        repeat(15) {
            if (seen.containsAll(expected)) return@repeat
            // Scroll the LazyColumn: a downward drag in the lower half of the
            // screen (absolute coordinates — the host activity fills the screen).
            composeTestRule.onRoot().performTouchInput {
                swipe(
                    start = Offset(540f, 1200f),
                    end = Offset(540f, 400f)
                )
            }
            recordVisible()
        }

        assertEquals(expected.toSet(), seen)
    }

    @Test
    fun settingsScreen_backButtonWorks() {
        var backClicked = false

        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        onNavigateBack = { backClicked = true },
                        onNavigateToChangelog = { /* no-op */ }
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
