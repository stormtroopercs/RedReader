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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI test for InboxScreen.
 * Verifies that inbox screen displays correctly.
 *
 * Hosted in [HiltTestHostActivity] (a @AndroidEntryPoint) because [InboxScreen]
 * resolves its ViewModel with hiltViewModel(), which requires a Hilt
 * component-holder host — the plain ComponentActivity that createComposeRule()
 * launches is not one.
 */
@RunWith(AndroidJUnit4::class)
class InboxScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule(HiltTestHostActivity::class.java)

    @Test
    fun inboxScreen_displaysTitle() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    InboxScreen(
                        onNavigateBack = { /* no-op */ },
                        onMarkAllRead = { /* no-op */ },
                        onSendMessage = { /* no-op */ }
                    )
                }
            }
        }

        // Verify inbox title is displayed
        composeTestRule.onNodeWithText("Inbox").assertExists()
    }

    @Test
    fun inboxScreen_displaysContentArea() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    InboxScreen(
                        onNavigateBack = { /* no-op */ },
                        onSendMessage = { /* no-op */ }
                    )
                }
            }
        }

        // Anonymous on the test host (no signed-in account) resolves the
        // inbox to a terminal state — either the empty list ("No messages
        // yet") or the "sign in" error. The transient loading frame is
        // skipped by waiting for idle, so assert on one of the two stable
        // terminal renderings rather than a specific one.
        val hasMessages = composeTestRule
            .onAllNodesWithText("No messages yet")
            .fetchSemanticsNodes().isNotEmpty()
        val hasSignInError = composeTestRule
            .onAllNodesWithText("Sign in to view your inbox")
            .fetchSemanticsNodes().isNotEmpty()
        org.junit.Assert.assertTrue(
            "expected the empty inbox state or the sign-in error",
            hasMessages || hasSignInError
        )
    }
}
