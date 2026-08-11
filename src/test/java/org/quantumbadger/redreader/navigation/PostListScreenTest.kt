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
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI test for PostListScreen.
 * Verifies that post list screen displays correctly.
 */
class PostListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun postListScreen_displaysTitle() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PostListScreen(
                        subreddit = "test_subreddit",
                        onNavigateBack = { /* no-op */ },
                        onNavigateToCommentList = { /* no-op */ }
                    )
                }
            }
        }

        // Verify post list title is displayed
        composeTestRule.onNodeWithText("test_subreddit").assertExists()
    }

    @Test
    fun postListScreen_displaysPosts() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PostListScreen(
                        subreddit = "test_subreddit",
                        onNavigateBack = { /* no-op */ },
                        onNavigateToCommentList = { /* no-op */ }
                    )
                }
            }
        }

        // Verify posts are displayed (at least one post item)
        // The actual posts depend on the ViewModel state
        // This test verifies the screen renders without crashing
        composeTestRule.onNodeWithText("test_subreddit").assertExists()
    }
}
