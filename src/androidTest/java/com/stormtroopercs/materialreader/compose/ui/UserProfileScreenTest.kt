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

package com.stormtroopercs.materialreader.compose.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI test for UserProfileScreen.
 * Verifies that user profile screen displays correctly.
 *
 * Hosted in [HiltTestHostActivity] (a @AndroidEntryPoint) because
 * [UserProfileScreen] resolves its ViewModel with hiltViewModel(), which
 * requires a Hilt component-holder host — the plain ComponentActivity that
 * createComposeRule() launches is not one.
 */
@RunWith(AndroidJUnit4::class)
class UserProfileScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule(HiltTestHostActivity::class.java)

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
    fun userProfileScreen_displaysContentArea() {
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

        // The content area is either still loading or has reached a terminal
        // state. On the test host (anonymous account, no client ID) the profile
        // fetch fails quickly, so the Loading indicator is a transient frame we
        // can't reliably catch at idle — accept the loading indicator OR the
        // error/Retry view. Either proves the screen rendered its body instead
        // of crashing.
        val loadingVisible = composeTestRule
            .onAllNodesWithText("Loading user profile...")
            .fetchSemanticsNodes().isNotEmpty()
        val errorVisible = composeTestRule
            .onAllNodesWithText("Retry")
            .fetchSemanticsNodes().isNotEmpty()

        assertTrue(
            "Expected the profile screen to show a loading or terminal (error) state",
            loadingVisible || errorVisible
        )
    }
}
