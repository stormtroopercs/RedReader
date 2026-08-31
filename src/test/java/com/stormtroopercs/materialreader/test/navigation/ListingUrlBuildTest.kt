package com.stormtroopercs.materialreader.test.navigation

import android.app.Application
import com.stormtroopercs.materialreader.common.General
import com.stormtroopercs.materialreader.common.PrefsUtility
import com.stormtroopercs.materialreader.navigation.buildListingUri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Regression tests for [buildListingUri] — the listing-URL builder that
 * replaced the hand-rolled `when` whose `else -> "https://www.reddit.com/r/$listPath/"`
 * fallthrough produced `r/r/<name>` 404s (issue #21 + the 2026-08-30
 * bug-report-compilation user reports: listings fail to load / wrong URL).
 *
 * Every community name form — bare, `r/`-prefixed, doubled `r/r/` — must
 * collapse to the single canonical `r/<name>/.json`. Unknown paths must
 * return null (the "Invalid listing" error), never a wrong URL.
 *
 * Note: the Reddit API host is `oauth.reddit.com` (Constants.Reddit.domain),
 * not `www.reddit.com`, so assertions use the real API authority.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ListingUrlBuildTest {

	@Before
	fun initPrefs() {
		// PrefsUtility reads shared preferences (e.g. pref_behaviour_nsfw inside
		// SearchPostListURL.generateJsonUri); prime its static state the same
		// way EdgeToEdgeInsetsTest does so it works under Robolectric.
		val app = RuntimeEnvironment.getApplication() as Application
		try {
			val resField = PrefsUtility::class.java.getDeclaredField("mRes")
			resField.isAccessible = true
			resField.set(null, app.resources)

			val prefsField = PrefsUtility::class.java.getDeclaredField("sharedPrefs")
			prefsField.isAccessible = true
			prefsField.set(null, General.getSharedPrefs(app))
		} catch (e: ReflectiveOperationException) {
			throw RuntimeException(e)
		}
	}

	private fun uri(listPath: String, searchQuery: String? = null): String? = buildListingUri(listPath, searchQuery, null)?.toString()

	// --- community listings: every form -> r/<name>/.json -----------------

	@Test
	fun bareCommunityName() {
		assertEquals("https://oauth.reddit.com/r/palworld/.json", uri("Palworld"))
	}

	@Test
	fun rPrefixedCommunityName() {
		// The exact shape the bug reports describe.
		assertEquals("https://oauth.reddit.com/r/palworld/.json", uri("r/Palworld"))
	}

	@Test
	fun slashRPrefixedCommunityName() {
		assertEquals("https://oauth.reddit.com/r/palworld/.json", uri("/r/Palworld"))
	}

	@Test
	fun doubledRPrefixCommunityName() {
		// The exact 404 from issue #21.
		assertEquals("https://oauth.reddit.com/r/palworld/.json", uri("r/r/Palworld"))
	}

	@Test
	fun surroundingWhitespaceCommunityName() {
		assertEquals("https://oauth.reddit.com/r/palworld/.json", uri("  r/Palworld  "))
	}

	// --- default feeds -----------------------------------------------------

	@Test
	fun frontpage() {
		assertEquals("https://oauth.reddit.com/.json", uri("frontpage"))
	}

	@Test
	fun popular() {
		assertEquals("https://oauth.reddit.com/r/popular/.json", uri("popular"))
	}

	@Test
	fun all() {
		assertEquals("https://oauth.reddit.com/r/all/.json", uri("all"))
	}

	@Test
	fun blankIsFrontpage() {
		assertEquals("https://oauth.reddit.com/.json", uri(""))
	}

	// --- user post listings ----------------------------------------------

	@Test
	fun userSubmitted() {
		assertEquals(
			"https://oauth.reddit.com/user/SpecialUser/submitted/.json",
			uri("u/SpecialUser/submitted"),
		)
	}

	@Test
	fun userSaved() {
		assertEquals(
			"https://oauth.reddit.com/user/SpecialUser/saved/.json",
			uri("u/SpecialUser/saved"),
		)
	}

	// --- multireddits -----------------------------------------------------

	@Test
	fun ownMultireddit() {
		assertEquals("https://oauth.reddit.com/me/m/mylist/.json", uri("m/mylist"))
	}

	@Test
	fun userMultireddit() {
		assertEquals(
			"https://oauth.reddit.com/user/SpecialUser/m/mylist/.json",
			uri("u/SpecialUser/m/mylist"),
		)
	}

	// --- search listings (location must not double-prefix) ---------------

	@Test
	fun globalSearch() {
		assertEquals(
			"https://oauth.reddit.com/search/.json?q=kotlin",
			uri("", "kotlin"),
		)
	}

	@Test
	fun communitySearchBareName() {
		assertEquals(
			"https://oauth.reddit.com/r/palworld/search/.json?restrict_sr=on&q=kotlin",
			uri("palworld", "kotlin"),
		)
	}

	@Test
	fun communitySearchRPrefixedName() {
		// The bug: a `r/`-prefixed location used to build r/r/<name>/search.
		assertEquals(
			"https://oauth.reddit.com/r/palworld/search/.json?restrict_sr=on&q=kotlin",
			uri("r/Palworld", "kotlin"),
		)
	}

	@Test
	fun multiredditSearch() {
		assertEquals(
			"https://oauth.reddit.com/me/m/mylist/search/.json?restrict_sr=on&q=kotlin",
			uri("m/mylist", "kotlin"),
		)
	}

	// --- unknown paths must error, never 404 ------------------------------

	@Test
	fun unknownPathReturnsNull() {
		assertNull(uri("not-a-real-feed-shape"))
	}
}
