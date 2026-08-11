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

package org.quantumbadger.redreader.compose.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test for UserProfileScreen.
 * Verifies that user profile screen displays correctly.
 */
class UserProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun userProfileScreen_displaysUsername() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    UserProfileScreen(
                        username = "testuser",
                        onNavigateBack = { /* no-op */ },
                        onNavigateToPosts = { /* no-op */ },
                        onNavigateToComments = { /* no-op */ },
                        onSendMessage = { /* no-op */ }
                    )
                }
            }
        }

        // Verify username is displayed
        composeTestRule.onNodeWithText("u/testuser").assertExists()
    }

    @Test
    fun userProfileScreen_displaysLoadingState() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    UserProfileScreen(
                        username = "testuser",
                        onNavigateBack = { /* no-op */ },
                        onNavigateToPosts = { /* no-op */ },
                        onNavigateToComments = { /* no-op */ },
                        onSendMessage = { /* no-op */ }
                    )
                }
            }
        }

        // Verify loading state is displayed initially
        composeTestRule.onNodeWithText("Loading user profile...").assertExists()
    }
}
