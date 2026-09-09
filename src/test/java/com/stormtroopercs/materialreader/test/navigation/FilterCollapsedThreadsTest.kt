package com.stormtroopercs.materialreader.test.navigation

import com.stormtroopercs.materialreader.navigation.CommentItem
import com.stormtroopercs.materialreader.navigation.filterCollapsedThreads
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [filterCollapsedThreads], the flat-list filter that hides the
 * subtree of every user-collapsed comment in the comment list (the comment
 * screen renders a flat, depth-first list indented by `replyDepth`, so
 * collapsing a thread is a pure function of that order + the collapsed ids).
 * A collapsed comment's own row always stays visible — it is the summary
 * row the user taps to re-expand — only its descendants are hidden.
 */
class FilterCollapsedThreadsTest {

	// A depth-first thread:
	//   a (0)
	//     b (1)
	//       c (2)
	//     d (1)
	//   e (0)
	private val thread = listOf(
		item("a", depth = 0),
		item("b", depth = 1),
		item("c", depth = 2),
		item("d", depth = 1),
		item("e", depth = 0),
	)

	private fun item(id: String, depth: Int) = CommentItem(
		id = id,
		author = "u/$id",
		body = "body $id",
		score = 1,
		replyCount = 0,
		createdUtcTimestamp = 0,
		authorFlairText = null,
		isTopLevel = depth == 0,
		collapsed = false,
		collapsedReason = null,
		replyDepth = depth,
	)

	private fun ids(list: List<CommentItem>): List<String> = list.map { it.id }

	@Test
	fun noCollapsedShowsEverything() {
		assertEquals(listOf("a", "b", "c", "d", "e"), ids(filterCollapsedThreads(thread, emptySet())))
	}

	@Test
	fun collapsingTopLevelKeepsRowHidesThread() {
		// a's own row stays (collapsed summary); b, c are hidden.
		assertEquals(listOf("a", "e"), ids(filterCollapsedThreads(thread, setOf("a"))))
	}

	@Test
	fun collapsingMiddleKeepsRowHidesOnlyItsSubtree() {
		assertEquals(listOf("a", "b", "d", "e"), ids(filterCollapsedThreads(thread, setOf("b"))))
	}

	@Test
	fun collapsingLeafChangesNothing() {
		// c has no descendants — collapsing it only flips its own chevron.
		assertEquals(
			listOf("a", "b", "c", "d", "e"),
			ids(filterCollapsedThreads(thread, setOf("c"))),
		)
	}

	@Test
	fun collapsingLastTopLevelKeepsAllRows() {
		assertEquals(
			listOf("a", "b", "c", "d", "e"),
			ids(filterCollapsedThreads(thread, setOf("e"))),
		)
	}

	@Test
	fun collapsingTwoTopLevelsKeepsOnlyTheirRows() {
		assertEquals(listOf("a", "e"), ids(filterCollapsedThreads(thread, setOf("a", "e"))))
	}

	@Test
	fun collapsedIdNotInListIsIgnored() {
		assertEquals(
			listOf("a", "b", "c", "d", "e"),
			ids(filterCollapsedThreads(thread, setOf("ghost"))),
		)
	}

	@Test
	fun siblingAfterCollapsedThreadStillShows() {
		// g (0) -> h (1); collapse h: h stays, i is g's sibling and shows.
		val list = listOf(item("g", 0), item("h", 1), item("i", 0))
		assertEquals(listOf("g", "h", "i"), ids(filterCollapsedThreads(list, setOf("h"))))
	}

	@Test
	fun deepChainCollapsesAtAnyLevel() {
		// a(0) b(1) c(2) d(3) e(2) f(1): e is a sibling of c (both under b).
		val list = listOf(
			item("a", 0),
			item("b", 1),
			item("c", 2),
			item("d", 3),
			item("e", 2),
			item("f", 1),
		)
		// Collapsing b keeps b, hides c, d, e; f (b's sibling) shows.
		assertEquals(listOf("a", "b", "f"), ids(filterCollapsedThreads(list, setOf("b"))))
		// Collapsing c keeps c, hides only d; e (c's sibling) shows.
		assertEquals(listOf("a", "b", "c", "e", "f"), ids(filterCollapsedThreads(list, setOf("c"))))
	}
}
