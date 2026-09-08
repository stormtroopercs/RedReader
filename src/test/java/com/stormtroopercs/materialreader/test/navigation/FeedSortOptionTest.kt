package com.stormtroopercs.materialreader.test.navigation

import com.stormtroopercs.materialreader.navigation.FeedSortOption
import com.stormtroopercs.materialreader.reddit.PostSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [FeedSortOption] — the feed sort model behind the
 * two-level sort dialog (six options; Top / Controversial open a
 * time-window sub-list). Covers the option set + order, the window
 * resolution ([withWindow] / [windowSort]), the persisted-id
 * reconstruction ([forId] round trip for `top:day`-style ids), and the
 * feed chip's [FeedSortOption.chipLabel].
 */
class FeedSortOptionTest {

	private val options get() = FeedSortOption.options

	// --- the six options, in dialog order (top to bottom) ----------------

	@Test
	fun optionOrderIsBestHotNewRisingTopControversial() {
		assertEquals(
			listOf("best", "hot", "new", "rising", "top", "controversial"),
			options.map { it.id },
		)
	}

	@Test
	fun onlyTopAndControversialHaveTimeWindows() {
		assertEquals(
			listOf("top", "controversial"),
			options.filter { it.hasTimeWindows }.map { it.id },
		)
	}

	@Test
	fun defaultSortIsBest() {
		// The view model + prefs fallback both resolve an unknown id to the
		// first option — the listing's own default order.
		val fallback = FeedSortOption.forId("never-seen-before")
		assertEquals("best", fallback.id)
		assertNull(fallback.urlSort)
	}

	// --- the time windows, in dialog order (top to bottom) ----------------

	@Test
	fun windowOrderIsHourDayWeekMonthYearAllTime() {
		assertEquals(
			listOf("hour", "day", "week", "month", "year", "all"),
			FeedSortOption.timeWindows.map { it.id },
		)
	}

	@Test
	fun windowLabels() {
		assertEquals(
			listOf("Hour", "Day", "Week", "Month", "Year", "All Time"),
			FeedSortOption.timeWindows.map { it.label },
		)
	}

	// --- withWindow: resolving Top / Controversial to a window ------------

	@Test
	fun topResolvesToEveryWindow() {
		val top = options.first { it.id == "top" }
		val expected = mapOf(
			"hour" to PostSort.TOP_HOUR,
			"day" to PostSort.TOP_DAY,
			"week" to PostSort.TOP_WEEK,
			"month" to PostSort.TOP_MONTH,
			"year" to PostSort.TOP_YEAR,
			"all" to PostSort.TOP_ALL,
		)
		expected.forEach { (window, sort) ->
			val resolved = FeedSortOption.withWindow(top, window)
			assertEquals("top:$window", resolved.id)
			assertEquals(window, resolved.timeWindow)
			assertEquals(sort, resolved.urlSort)
		}
	}

	@Test
	fun controversialResolvesToEveryWindow() {
		val controversial = options.first { it.id == "controversial" }
		val expected = mapOf(
			"hour" to PostSort.CONTROVERSIAL_HOUR,
			"day" to PostSort.CONTROVERSIAL_DAY,
			"week" to PostSort.CONTROVERSIAL_WEEK,
			"month" to PostSort.CONTROVERSIAL_MONTH,
			"year" to PostSort.CONTROVERSIAL_YEAR,
			"all" to PostSort.CONTROVERSIAL_ALL,
		)
		expected.forEach { (window, sort) ->
			val resolved = FeedSortOption.withWindow(controversial, window)
			assertEquals("controversial:$window", resolved.id)
			assertEquals(window, resolved.timeWindow)
			assertEquals(sort, resolved.urlSort)
		}
	}

	// --- forId: persisted-id reconstruction -------------------------------

	@Test
	fun forIdReconstructsWindowedIds() {
		val topDay = FeedSortOption.forId("top:day")
		assertEquals("top:day", topDay.id)
		assertEquals("Top", topDay.label)
		assertEquals(PostSort.TOP_DAY, topDay.urlSort)

		val controversialWeek = FeedSortOption.forId("controversial:week")
		assertEquals("controversial:week", controversialWeek.id)
		assertEquals("Controversial", controversialWeek.label)
		assertEquals(PostSort.CONTROVERSIAL_WEEK, controversialWeek.urlSort)
	}

	@Test
	fun windowedIdRoundTripsThroughWithWindow() {
		val top = options.first { it.id == "top" }
		for (window in FeedSortOption.timeWindows.map { it.id }) {
			val resolved = FeedSortOption.withWindow(top, window)
			val reloaded = FeedSortOption.forId(resolved.id)
			assertEquals(resolved.id, reloaded.id)
			assertEquals(resolved.timeWindow, reloaded.timeWindow)
			assertEquals(resolved.urlSort, reloaded.urlSort)
			assertEquals(resolved.chipLabel, reloaded.chipLabel)
		}
	}

	@Test
	fun forIdFallsBackForUnknownWindow() {
		// A windowed id with a window that no longer exists resolves to Best
		// (the safe default), never a wrong sort.
		assertEquals("best", FeedSortOption.forId("top:decade").id)
	}

	@Test
	fun forIdStillResolvesPlainIds() {
		assertEquals("hot", FeedSortOption.forId("hot").id)
		assertEquals(PostSort.HOT, FeedSortOption.forId("hot").urlSort)
		assertEquals("top", FeedSortOption.forId("top").id)
		assertTrue(FeedSortOption.forId("top").hasTimeWindows)
	}

	// --- the feed chip's label --------------------------------------------

	@Test
	fun chipLabelIsPlainWithoutAWindow() {
		for (option in options) {
			assertEquals(option.label, option.chipLabel)
		}
	}

	@Test
	fun chipLabelCarriesTheWindow() {
		val topDay = FeedSortOption.withWindow(options.first { it.id == "top" }, "day")
		assertEquals("Top · Day", topDay.chipLabel)

		val controversialAll = FeedSortOption.withWindow(
			options.first { it.id == "controversial" },
			"all",
		)
		assertEquals("Controversial · All Time", controversialAll.chipLabel)
	}
}
