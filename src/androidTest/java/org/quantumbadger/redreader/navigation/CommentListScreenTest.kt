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

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.quantumbadger.redreader.compose.ctx.RRComposeContextTest
import org.quantumbadger.redreader.compose.ui.HiltTestHostActivity

/**
 * Compose UI test for CommentListScreen.
 * Verifies that comment list screen displays correctly.
 *
 * Hosted in [HiltTestHostActivity] (a @AndroidEntryPoint) because
 * [RealCommentListScreen] resolves its ViewModel with hiltViewModel(), which
 * requires a Hilt component-holder host — the plain ComponentActivity that
 * createComposeRule() launches is not one. The screen is wrapped in
 * RRComposeContextTest, which provides the LocalComposeTheme /
 * LocalComposePrefs / LocalLauncher that its error and loading views consume.
 */
@RunWith(AndroidJUnit4::class)
class CommentListScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule(HiltTestHostActivity::class.java)

    private fun setContent(onNavigateBack: () -> Unit) {
        composeTestRule.setContent {
            RRComposeContextTest {
                RealCommentListScreen(
                    postId = "test_post_id",
                    onNavigateBack = onNavigateBack
                )
            }
        }
    }

    @Test
    fun commentListScreen_displaysTitle() {
        setContent(onNavigateBack = { /* no-op */ })

        // Verify comment list title is displayed
        composeTestRule.onNodeWithText("Comments").assertExists()
    }

    @Test
    fun commentListScreen_backButtonWorks() {
        var backClicked = false

        setContent(onNavigateBack = { backClicked = true })

        // Click back button
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        // Verify back navigation was triggered
        assert(backClicked) { "Back navigation should have been triggered" }
    }
}
