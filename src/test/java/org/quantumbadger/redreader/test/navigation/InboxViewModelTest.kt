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

package org.quantumbadger.redreader.test.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.navigation.InboxViewModel
import org.quantumbadger.redreader.navigation.toInboxItem
import org.quantumbadger.redreader.reddit.kthings.RedditComment
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import org.quantumbadger.redreader.reddit.kthings.RedditMessage
import org.quantumbadger.redreader.reddit.kthings.RedditThing
import org.quantumbadger.redreader.reddit.kthings.RedditTimestampUTC
import org.quantumbadger.redreader.reddit.kthings.UrlEncodedString

/**
 * Tests for [toInboxItem] — the pure inbox-listing decoder used by
 * [InboxViewModel.loadInbox]. It maps a raw [RedditThing.Message] (private
 * message), [RedditThing.Comment] (post- or comment-reply), or any other
 * thing to the UI-facing [InboxViewModel.InboxItem].
 */
class InboxViewModelTest {

    private val created = RedditTimestampUTC(TimestampUTC.fromUtcSecs(1_700_000_000L))

    // --- Private message --------------------------------------------------

    @Test
    fun message_mapsToInboxItemWithAllFields() {
        val msg = RedditMessage(
            id = "msg1",
            name = RedditIdAndType("t4_msg1"),
            author = UrlEncodedString("alice"),
            dest = UrlEncodedString("bob"),
            body = UrlEncodedString("Hello Bob"),
            subject = UrlEncodedString("A subject"),
            subreddit_name_prefixed = UrlEncodedString("r/test"),
            created_utc = created
        )
        val item = toInboxItem(RedditThing.Message(msg))
        assertTrue(item is InboxViewModel.InboxItem)
        assertEquals("t4_msg1", item!!.id)
        assertEquals("A subject", item.subject)
        assertEquals("Hello Bob", item.body)
        assertEquals("alice", item.sender)
        assertEquals("bob", item.recipient)
        assertEquals("r/test", item.subreddit)
        assertEquals(1_700_000_000L, item.timestamp)
        assertEquals(false, item.isRead)
        assertEquals(InboxViewModel.MessageType.MESSAGE, item.messageType)
    }

    @Test
    fun message_withOptionalFieldsNull_mapsWithNulls() {
        val msg = RedditMessage(
            id = "msg2",
            name = RedditIdAndType("t4_msg2"),
            created_utc = created
        )
        val item = toInboxItem(RedditThing.Message(msg))
        assertTrue(item is InboxViewModel.InboxItem)
        assertNull(item!!.subject)
        assertNull(item.body)
        assertNull(item.sender)
        assertNull(item.recipient)
        assertNull(item.subreddit)
        assertEquals(InboxViewModel.MessageType.MESSAGE, item.messageType)
    }

    // --- Comment reply ----------------------------------------------------

    @Test
    fun commentWithoutLinkId_mapsToCommentReply() {
        val c = RedditComment(
            id = "c1",
            name = RedditIdAndType("t1_c1"),
            author = UrlEncodedString("carol"),
            subreddit = UrlEncodedString("Kotlin"),
            body = UrlEncodedString("Nice post!"),
            created_utc = created
        )
        val item = toInboxItem(RedditThing.Comment(c))
        assertTrue(item is InboxViewModel.InboxItem)
        assertEquals("c1", item!!.id)
        assertEquals("Comment reply", item.subject)
        assertEquals("Nice post!", item.body)
        assertEquals("Kotlin", item.sender)
        assertNull(item.recipient)
        assertEquals("Kotlin", item.subreddit)
        assertEquals(1_700_000_000L, item.timestamp)
        assertEquals(InboxViewModel.MessageType.COMMENT_REPLY, item.messageType)
    }

    @Test
    fun commentWithLinkId_mapsToPostReply() {
        val c = RedditComment(
            id = "c2",
            name = RedditIdAndType("t1_c2"),
            subreddit = UrlEncodedString("androiddev"),
            body = UrlEncodedString("Thanks!"),
            link_id = "t3_abc123",
            created_utc = created
        )
        val item = toInboxItem(RedditThing.Comment(c))
        assertTrue(item is InboxViewModel.InboxItem)
        assertEquals("c2", item!!.id)
        assertEquals("Post reply", item.subject)
        assertEquals(InboxViewModel.MessageType.POST_REPLY, item.messageType)
    }

    // --- Unknown thing ----------------------------------------------------

    @Test
    fun nonMessageOrComment_returnsNull() {
        assertNull(toInboxItem(RedditThing.User))
    }
}
