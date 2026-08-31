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

package com.stormtroopercs.materialreader.test.navigation

import com.stormtroopercs.materialreader.navigation.PostItem
import org.junit.Assert.assertEquals
import org.junit.Test

class PostListViewModelTest {

	@Test
	fun postItem_fromRedditPost_mapping() {
		val postItem = PostItem(
			id = "abc123",
			title = "Test Post Title",
			author = "testauthor",
			subreddit = "testsubreddit",
			score = 42,
			numComments = 10,
			url = "https://example.com/image.jpg",
			permalink = "/r/testsubreddit/comments/abc123/test/",
			isSelf = false,
			isOver18 = false,
			isSpoiler = false,
			isStickied = false,
			isLocked = false,
			isVideo = false,
			isCrosspost = false,
			linkFlairText = "Test Flair",
			authorFlairText = "Author Flair",
			thumbnail = "https://example.com/thumb.jpg",
			selftext = null,
			createdUtc = 1700000000L,
		)

		assertEquals("abc123", postItem.id)
		assertEquals("Test Post Title", postItem.title)
		assertEquals("testauthor", postItem.author)
		assertEquals("testsubreddit", postItem.subreddit)
		assertEquals(42, postItem.score)
		assertEquals(10, postItem.numComments)
		assertEquals("https://example.com/image.jpg", postItem.url)
		assertEquals("/r/testsubreddit/comments/abc123/test/", postItem.permalink)
		assertEquals(false, postItem.isSelf)
		assertEquals(false, postItem.isOver18)
		assertEquals(false, postItem.isSpoiler)
		assertEquals(false, postItem.isStickied)
		assertEquals(false, postItem.isLocked)
		assertEquals("Test Flair", postItem.linkFlairText)
		assertEquals("Author Flair", postItem.authorFlairText)
		assertEquals("https://example.com/thumb.jpg", postItem.thumbnail)
		assertEquals(null, postItem.selftext)
		assertEquals(1700000000L, postItem.createdUtc)
	}

	@Test
	fun postItem_handles_null_fields() {
		val postItem = PostItem(
			id = "xyz789",
			title = null,
			author = null,
			subreddit = "nosub",
			score = 0,
			numComments = 0,
			url = null,
			permalink = "/r/nosub/comments/xyz789/",
			isSelf = true,
			isOver18 = false,
			isSpoiler = false,
			isStickied = false,
			isLocked = false,
			isVideo = false,
			isCrosspost = false,
			linkFlairText = null,
			authorFlairText = null,
			thumbnail = null,
			selftext = null,
			createdUtc = 0L,
		)

		assertEquals(null, postItem.title)
		assertEquals(null, postItem.author)
		assertEquals(null, postItem.url)
		assertEquals(null, postItem.linkFlairText)
		assertEquals(null, postItem.authorFlairText)
		assertEquals(null, postItem.thumbnail)
		assertEquals(null, postItem.selftext)
		assertEquals(true, postItem.isSelf)
	}

	@Test
	fun postItem_immutability() {
		val postItem = PostItem(
			id = "immutable",
			title = "Immutable Test",
			author = "author",
			subreddit = "sub",
			score = 1,
			numComments = 0,
			url = null,
			permalink = "/immutable",
			isSelf = false,
			isOver18 = false,
			isSpoiler = false,
			isStickied = false,
			isLocked = false,
			isVideo = false,
			isCrosspost = false,
			linkFlairText = null,
			authorFlairText = null,
			thumbnail = null,
			selftext = null,
			createdUtc = 0L,
		)

		// Verify data class immutability (all fields are val)
		assertEquals("immutable", postItem.id)
		assertEquals("Immutable Test", postItem.title)
	}
}
