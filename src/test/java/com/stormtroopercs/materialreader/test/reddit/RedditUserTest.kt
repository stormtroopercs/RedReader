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
package com.stormtroopercs.materialreader.test.reddit

import com.stormtroopercs.materialreader.jsonwrap.JsonValue
import com.stormtroopercs.materialreader.reddit.things.RedditThing
import com.stormtroopercs.materialreader.reddit.things.RedditUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for RedditUser parsing (the modern Reddit API's user shape) and its
 * avatar URL resolution: the account picture arrives as `icon` (a base64 data
 * URI) with the legacy `icon_img` (a plain URL) as the fallback.
 */
class RedditUserTest {
	private fun parse(json: String): RedditUser {
		val value = JsonValue.parse(json.toByteArray().inputStream())
		return value.asObject<RedditUser>(RedditUser::class.java)!!
	}

	// A representative subset of a modern /user/<name>/about.json payload:
	// includes boxed (nullable) field types (created_utc Long?, comment_karma
	// Int?, has_mail Boolean?, icon_size Int?) — all of which must survive
	// reflective population.
	private val modernAboutJson = """
        {
          "name": "test_user",
          "id": "abc123",
          "created_utc": 1600000000,
          "comment_karma": 42,
          "link_karma": 7,
          "is_gold": false,
          "is_mod": false,
          "is_employee": false,
          "is_suspended": false,
          "has_mail": false,
          "icon": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADgQF/zUu16AAAAABJRU5ErkJggg==",
          "icon_size": [100, 100]
        }
	""".trimIndent()

	@Test
	fun `modern about json parses with boxed nullable fields`() {
		val user = parse(modernAboutJson)

		assertEquals("test_user", user.name)
		assertEquals("abc123", user.id)
		assertEquals(42, user.comment_karma)
		assertEquals(7, user.link_karma)
		assertEquals(false, user.is_gold)
		assertEquals(false, user.has_mail)
		assertNotNull(user.created_utc)
		assertEquals(1600000000L, user.created_utc)
	}

	@Test
	fun `icon url prefers the modern icon field`() {
		val user = parse(modernAboutJson)

		val iconUrl = user.iconUrl
		assertNotNull(iconUrl)
		assertEquals(
			"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADgQF/zUu16AAAAABJRU5ErkJggg==",
			iconUrl!!.value,
		)
	}

	@Test
	fun `icon url falls back to legacy icon img`() {
		val json = """
            {
              "name": "legacy_user",
              "comment_karma": 1,
              "icon_img": "https://www.redditstatic.com/avatar.png"
            }
		""".trimIndent()

		val user = parse(json)

		assertNull(user.icon)
		assertEquals("https://www.redditstatic.com/avatar.png", user.iconUrl?.value)
	}

	@Test
	fun `icon url is null when neither field is present`() {
		val json = """{ "name": "no_avatar", "comment_karma": 0 }"""

		val user = parse(json)

		assertNull(user.iconUrl)
	}

	@Test
	fun `icon wins when both fields are present`() {
		val json = """
            {
              "name": "both",
              "comment_karma": 0,
              "icon_img": "https://www.redditstatic.com/old.png",
              "icon": "data:image/png;base64,AAA="
            }
		""".trimIndent()

		val user = parse(json)

		assertEquals("data:image/png;base64,AAA=", user.iconUrl?.value)
	}

	// /user/{name}/about.json arrives as a RedditThing envelope
	// ({kind: "t2", data: {...}}), not a bare user object. The profile screen
	// must unwrap it — parsing the top level as a RedditUser silently yields
	// an empty object (every field null).
	@Test
	fun `about json envelope unwraps to user`() {
		val envelope = """
            {
              "kind": "t2",
              "data": {
                "name": "envelope_user",
                "id": "env123",
                "created_utc": 1601163853.0,
                "comment_karma": 0,
                "link_karma": 1,
                "is_gold": false,
                "is_mod": false,
                "is_employee": false,
                "has_mail": false,
                "icon_img": "https://www.redditstatic.com/avatars/defaults/v2/avatar_default_2.png"
              }
            }
		""".trimIndent()

		val thing = JsonValue.parse(envelope.toByteArray().inputStream())
			.asObject<RedditThing>(RedditThing::class.java)
		assertNotNull(thing)
		assertEquals("t2", thing!!.kind)

		val user = thing.asUser()

		assertEquals("envelope_user", user.name)
		assertEquals("env123", user.id)
		assertEquals(1, user.link_karma)
		assertEquals(0, user.comment_karma)
		assertEquals(1601163853L, user.created_utc)
		assertNotNull(user.iconUrl)
		assertTrue(user.iconUrl!!.value.startsWith("https://www.redditstatic.com/"))
	}
}
