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

package org.quantumbadger.redreader.test.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.quantumbadger.redreader.jsonwrap.JsonValue
import org.quantumbadger.redreader.navigation.SubredditSearchViewModel
import org.quantumbadger.redreader.navigation.toSubredditItem

/**
 * Tests for the subreddit-search decoding path ([toSubredditItem] — the
 * jsonwrap `RedditThing`/`RedditSubreddit` unwrap of one search-listing
 * child) and the subscriber-count display formatting.
 */
class SubredditSearchViewModelTest {

    /** A representative subset of a modern /subreddits/search.json child.
     *  Note: `name` is the *thing id* (`t5_…`); the real name is in
     *  `display_name`, and the community icon in `icon_img`. */
    private val searchChildJson = """
        {
          "kind": "t5",
          "data": {
            "name": "t5_2uah7",
            "id": "2uah7",
            "display_name": "AskAnAmerican",
            "display_name_prefixed": "r/AskAnAmerican",
            "url": "/r/AskAnAmerican/",
            "title": "Ask Americans about their country!",
            "subscribers": 1169373,
            "description": "Welcome to /r/AskAnAmerican!",
            "icon_img": "https://a.thumbs.redditmedia.com/xyz123.png",
            "header_img": "https://styles.redditmedia.com/banner.png",
            "accounts_active": 999,
            "over18": false
          }
        }
    """.trimIndent()

    @Test
    fun `search child decodes to item using display name`() {
        val child = JsonValue.parse(searchChildJson.toByteArray().inputStream())

        val item = toSubredditItem(child)

        assertNotNull(item)
        // The modern `name` field holds the thing id — the display name is
        // `display_name`.
        assertEquals("AskAnAmerican", item!!.name)
        assertEquals(1169373, item.subscribers)
        assertEquals("Welcome to /r/AskAnAmerican!", item.description)
        assertEquals("https://a.thumbs.redditmedia.com/xyz123.png", item.iconUrl)
    }

    @Test
    fun `missing display name falls back to url path`() {
        val json = """
            {
              "kind": "t5",
              "data": {
                "name": "t5_abcde",
                "url": "/r/fallbacksr/",
                "subscribers": 5
              }
            }
        """.trimIndent()

        val item = toSubredditItem(JsonValue.parse(json.toByteArray().inputStream()))

        assertNotNull(item)
        assertEquals("fallbacksr", item!!.name)
        assertEquals(5, item.subscribers)
    }

    @Test
    fun `missing fields decode to nulls`() {
        val json = """
            {
              "kind": "t5",
              "data": {
                "display_name": "bare",
                "subscribers": 0
              }
            }
        """.trimIndent()

        val item = toSubredditItem(JsonValue.parse(json.toByteArray().inputStream()))

        assertNotNull(item)
        assertEquals("bare", item!!.name)
        assertEquals(0, item.subscribers)
        assertNull(item.description)
        assertNull(item.iconUrl)
    }

    @Test
    fun `non-subreddit child yields null`() {
        // A t1 comment child is not a subreddit.
        val json = """
            {
              "kind": "t1",
              "data": {
                "id": "abc",
                "body": "not a subreddit"
              }
            }
        """.trimIndent()

        val item = toSubredditItem(JsonValue.parse(json.toByteArray().inputStream()))

        assertNull(item)
    }

    @Test
    fun `subscriber label formats magnitudes`() {
        fun label(count: Int?) = SubredditSearchViewModel.SubredditItem(
            name = "x",
            subscribers = count,
            description = null,
            iconUrl = null
        ).subscribersLabel()

        assertEquals("1.2M", label(1_234_567))
        assertEquals("45.7K", label(45_678))
        assertEquals("42", label(42))
        assertNull(label(null))
    }
}
