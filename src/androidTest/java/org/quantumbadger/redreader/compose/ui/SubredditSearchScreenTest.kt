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
 * along with RedReader.  If not, see <http://www.gnu.org/licenses>.\
 ******************************************************************************/

package org.quantumbadger.redreader.compose.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI test for SubredditSearchScreen.
 *
 * Hosted in [HiltTestHostActivity] (a @AndroidEntryPoint) because
 * [SubredditSearchScreen] resolves its ViewModel with hiltViewModel(), which
 * requires a Hilt component-holder host — the plain ComponentActivity that
 * createComposeRule() launches is not one.
 */
@RunWith(AndroidJUnit4::class)
class SubredditSearchScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule(HiltTestHostActivity::class.java)

    @Test
    fun subredditSearchScreen_displaysTitle() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SubredditSearchScreen(
                        onNavigateBack = { /* no-op */ },
                        onSubredditSelected = { /* no-op */ }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Search Subreddits").assertExists()
    }

    @Test
    fun subredditSearchScreen_displaysIdleHint() {
        composeTestRule.setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SubredditSearchScreen(
                        onNavigateBack = { /* no-op */ },
                        onSubredditSelected = { /* no-op */ }
                    )
                }
            }
        }

        // No query typed yet → the idle hint.
        composeTestRule.onNodeWithText("Type to search subreddits").assertExists()
    }
}
