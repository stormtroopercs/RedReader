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
 * along with RedReader.  If not, see <http://www.gnu.org/licenses>.
 ******************************************************************************/

package org.quantumbadger.redreader.test.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.quantumbadger.redreader.jsonwrap.JsonValue
import org.quantumbadger.redreader.navigation.MainScreenViewModel
import org.quantumbadger.redreader.navigation.toSubscribedItem

/**
 * Unit tests for [toSubscribedItem] — the subscribed-listing decoder used
 * by [MainScreenViewModel]. The modern listing child shape mirrors the
 * search shape: `name` is the thing id, the real name is in
 * `display_name`.
 */
class MainScreenViewModelTest {

    /** A representative subset of a /subreddits/mine/subscriber.json child. */
    private val childJson = """
        {
          "kind": "t5",
          "data": {
            "name": "t5_2qh0i",
            "display_name": "Kotlin",
            "subscribers": 107832,
            "icon_img": "https://styles.redditmedia.com/t5_2qh0i/community-icon.png",
            "url": "/r/Kotlin/"
          }
        }
    """.trimIndent()

    @Test
    fun `toSubscribedItem maps display_name subscribers and icon_img`() {
        val child = JsonValue.parse(childJson.toByteArray().inputStream())
        val item = toSubscribedItem(child)

        assertNotNull(item)
        assertEquals("Kotlin", item!!.name)
        assertEquals(107832, item.subscribers)
        assertEquals("https://styles.redditmedia.com/t5_2qh0i/community-icon.png", item.iconUrl)
    }

    @Test
    fun `toSubscribedItem formats subscriber label with K suffix`() {
        val child = JsonValue.parse(childJson.toByteArray().inputStream())
        val item = toSubscribedItem(child)!!
        assertEquals("107.8K", item.subscribersLabel())
    }

    @Test
    fun `toSubscribedItem returns null when display_name is missing`() {
        val json = """
            {
              "kind": "t5",
              "data": {
                "name": "t5_12345",
                "subscribers": 42
              }
            }
        """.trimIndent()
        val child = JsonValue.parse(json.toByteArray().inputStream())
        // No display_name and no /r/<name>/ url → no usable name
        assertNull(toSubscribedItem(child))
    }

    @Test
    fun `subscribersLabel handles millions and small counts`() {
        assertEquals("1.2M", MainScreenViewModel.SubscribedSubreddit("x", 1_234_567, null).subscribersLabel())
        assertEquals("45", MainScreenViewModel.SubscribedSubreddit("x", 45, null).subscribersLabel())
        assertNull(MainScreenViewModel.SubscribedSubreddit("x", null, null).subscribersLabel())
    }
}
