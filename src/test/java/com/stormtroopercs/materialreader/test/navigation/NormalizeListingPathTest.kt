package com.stormtroopercs.materialreader.test.navigation

import com.stormtroopercs.materialreader.navigation.normalizeListingPath
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [normalizeListingPath], the feed-path normalization that keeps
 * community listings from double-prefixing (issue #21: a `r/Palworld`
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
        assertEquals("palworld", normalizeListingPath("Palworld"))
    }

    @Test
    fun rPrefixedNameIsStripped() {
        assertEquals("palworld", normalizeListingPath("r/Palworld"))
    }

    @Test
    fun slashRPrefixedNameIsStripped() {
        assertEquals("palworld", normalizeListingPath("/r/Palworld"))
    }

    @Test
    fun doubledRPrefixIsStripped() {
        // The exact shape that reached the wire in issue #21.
        assertEquals("palworld", normalizeListingPath("r/r/Palworld"))
    }

    @Test
    fun surroundingWhitespaceIsTrimmed() {
        assertEquals("palworld", normalizeListingPath("  r/Palworld  "))
    }

    // --- other path shapes must pass through untouched --------------------

    @Test
    fun userListingPathIsUnchanged() {
        // Usernames are case-sensitive — the community lowercasing must not
        // leak into `u/…` paths.
        assertEquals("u/SpecialUser/submitted", normalizeListingPath("u/SpecialUser/submitted"))
        assertEquals("u/SpecialUser/comments", normalizeListingPath("u/SpecialUser/comments"))
        assertEquals("me/submitted", normalizeListingPath("me/submitted"))
    }

    @Test
    fun multiredditPathIsUnchanged() {
        assertEquals("m/mylist", normalizeListingPath("m/mylist"))
        assertEquals("u/SpecialUser/m/mylist", normalizeListingPath("u/SpecialUser/m/mylist"))
    }

    @Test
    fun searchPathIsUnchanged() {
        assertEquals("s/palworld", normalizeListingPath("s/palworld"))
    }

    @Test
    fun defaultFeedIdsAreUnchanged() {
        assertEquals("frontpage", normalizeListingPath("frontpage"))
        assertEquals("popular", normalizeListingPath("popular"))
        assertEquals("all", normalizeListingPath("all"))
        assertEquals("", normalizeListingPath(""))
        assertEquals("", normalizeListingPath("   "))
    }
}
