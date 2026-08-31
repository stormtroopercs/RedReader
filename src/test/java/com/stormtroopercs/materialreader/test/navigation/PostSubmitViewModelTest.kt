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

import com.stormtroopercs.materialreader.navigation.normalizeSubreddit
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [normalizeSubreddit] — the pure normalisation of user-typed
 * subreddit input used by [com.stormtroopercs.materialreader.navigation.PostSubmitViewModel].
 * Trims whitespace, lowercases, and strips any leading `/`, leading `r/`, and
 * trailing `/` so a variety of inputs resolve to the bare subreddit name.
 */
class PostSubmitViewModelTest {

	@Test
	fun plainName_unchanged() {
		assertEquals("kotlin", normalizeSubreddit("kotlin"))
	}

	@Test
	fun uppercasedName_isLowercased() {
		assertEquals("kotlin", normalizeSubreddit("Kotlin"))
	}

	@Test
	fun leadingSlashStripped() {
		assertEquals("kotlin", normalizeSubreddit("/kotlin"))
	}

	@Test
	fun leadingRSlashStripped() {
		assertEquals("kotlin", normalizeSubreddit("r/kotlin"))
	}

	@Test
	fun trailingSlashStripped() {
		assertEquals("kotlin", normalizeSubreddit("kotlin/"))
	}

	@Test
	fun fullUrlPrefixStripped() {
		assertEquals("kotlin", normalizeSubreddit("r/Kotlin/"))
	}

	@Test
	fun leadingSlashAndRSlashStripped() {
		assertEquals("kotlin", normalizeSubreddit("/r/kotlin"))
	}

	@Test
	fun surroundingWhitespaceTrimmed() {
		assertEquals("kotlin", normalizeSubreddit("  kotlin  "))
	}

	@Test
	fun allDecorationsStripped() {
		assertEquals("kotlin", normalizeSubreddit("  R/Kotlin/  "))
	}

	@Test
	fun multipleLeadingSlashesStripped() {
		assertEquals("kotlin", normalizeSubreddit("///kotlin"))
	}

	@Test
	fun multipleTrailingSlashesStripped() {
		assertEquals("kotlin", normalizeSubreddit("kotlin///"))
	}

	@Test
	fun multiWordSubredditPreserved() {
		assertEquals("askscience", normalizeSubreddit("r/AskScience"))
	}

	@Test
	fun emptyInput_staysEmpty() {
		assertEquals("", normalizeSubreddit(""))
	}

	@Test
	fun whitespaceOnlyInput_becomesEmpty() {
		assertEquals("", normalizeSubreddit("   "))
	}
}
