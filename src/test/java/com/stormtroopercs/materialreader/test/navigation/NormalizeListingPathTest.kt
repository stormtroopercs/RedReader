package com.stormtroopercs.materialreader.test.navigation

import com.stormtroopercs.materialreader.navigation.normalizeListingPath
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for `String.normalizeListingPath()`, the feed-path normalization that
 * keeps community listings from double-prefixing (issue #21: a `r/Palworld`
 * listing path produced `https://www.reddit.com/r/r/Palworld/` 404s). The
 * `PostList` route's `subreddit` field is contractually a bare community
 * name, but several entry points pass `r/`-prefixed values (the custom
 * slot dialog suggests `r/...` paths, external deep links carry `r/`, and
 * the community screen previously built `r/$name` explicitly).
 */
class NormalizeListingPathTest {

	// --- community names: every form must collapse to the bare name ------

	@Test
	fun bareNamePassesThrough() {
		assertEquals("palworld", "Palworld".normalizeListingPath())
	}

	@Test
	fun rPrefixedNameIsStripped() {
		assertEquals("palworld", "r/Palworld".normalizeListingPath())
	}

	@Test
	fun slashRPrefixedNameIsStripped() {
		assertEquals("palworld", "/r/Palworld".normalizeListingPath())
	}

	@Test
	fun doubledRPrefixIsStripped() {
		// The exact shape that reached the wire in issue #21.
		assertEquals("palworld", "r/r/Palworld".normalizeListingPath())
	}

	@Test
	fun surroundingWhitespaceIsTrimmed() {
		assertEquals("palworld", "  r/Palworld  ".normalizeListingPath())
	}

	// --- other path shapes must pass through untouched -------------------

	@Test
	fun userListingPathIsUnchanged() {
		// Usernames are case-sensitive — the community lowercasing must not
		// leak into `u/…` paths.
		assertEquals("u/SpecialUser/submitted", "u/SpecialUser/submitted".normalizeListingPath())
		assertEquals("u/SpecialUser/comments", "u/SpecialUser/comments".normalizeListingPath())
		assertEquals("me/submitted", "me/submitted".normalizeListingPath())
	}

	@Test
	fun multiredditPathIsUnchanged() {
		assertEquals("m/mylist", "m/mylist".normalizeListingPath())
		assertEquals("u/SpecialUser/m/mylist", "u/SpecialUser/m/mylist".normalizeListingPath())
	}

	@Test
	fun searchPathIsUnchanged() {
		assertEquals("s/palworld", "s/palworld".normalizeListingPath())
	}

	@Test
	fun defaultFeedIdsAreUnchanged() {
		assertEquals("frontpage", "frontpage".normalizeListingPath())
		assertEquals("popular", "popular".normalizeListingPath())
		assertEquals("all", "all".normalizeListingPath())
		assertEquals("", "".normalizeListingPath())
		assertEquals("", "   ".normalizeListingPath())
	}
}
