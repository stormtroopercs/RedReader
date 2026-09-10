package com.stormtroopercs.materialreader.test.navigation

import com.stormtroopercs.materialreader.navigation.CommentBodySegment
import com.stormtroopercs.materialreader.navigation.hasCommentBodyLink
import com.stormtroopercs.materialreader.navigation.hasCommentBodyMedia
import com.stormtroopercs.materialreader.navigation.parseCommentBodySegments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [parseCommentBodySegments], the pure parser that splits a raw
 * comment body into runs of text and the media blocks embedded in it (Reddit
 * GIF drops render as `![gif](host|id)` / `![gif](url)` / bare media URLs).
 */
class CommentBodySegmentsTest {

	private fun text(value: String) = CommentBodySegment.Text(value)
	private fun media(url: String) = CommentBodySegment.Media(url)
	private fun link(text: String, url: String) = CommentBodySegment.Link(text, url)

	@Test
	fun plainBodyIsOneTextSegment() {
		assertEquals(listOf(text("just a normal comment")), parseCommentBodySegments("just a normal comment"))
	}

	@Test
	fun giphyPipeEmbedBecomesDirectGifMedia() {
		// `![gif](giphy|<id>)` → the direct giphy .gif file.
		val segs = parseCommentBodySegments("![gif](giphy|d81n9rX8stxBRbqrbG)")
		assertEquals(listOf(media("https://media.giphy.com/media/d81n9rX8stxBRbqrbG/giphy.gif")), segs)
	}

	@Test
	fun imgurPipeEmbedBecomesPageMedia() {
		assertEquals(
			listOf(media("https://imgur.com/abc123")),
			parseCommentBodySegments("![gif](imgur|abc123)"),
		)
	}

	@Test
	fun gfycatPipeEmbedBecomesPageMedia() {
		assertEquals(
			listOf(media("https://gfycat.com/SomeCat")),
			parseCommentBodySegments("![gif](gfycat|SomeCat)"),
		)
	}

	@Test
	fun unknownPipeHostStaysText() {
		// A `host|id` for a host we can't play stays visible as raw text.
		assertEquals(listOf(text("![gif](unknownhost|xyz)")), parseCommentBodySegments("![gif](unknownhost|xyz)"))
	}

	@Test
	fun markdownUrlEmbedBecomesMedia() {
		val url = "https://i.imgur.com/xyz.gif"
		assertEquals(listOf(media(url)), parseCommentBodySegments("![gif]($url)"))
	}

	@Test
	fun textAroundEmbedIsPreserved() {
		val segs = parseCommentBodySegments("check this out\n\n![gif](giphy|abc123)\n\nthat was a gif")
		assertEquals(
			listOf(
				text("check this out\n\n"),
				media("https://media.giphy.com/media/abc123/giphy.gif"),
				text("\n\nthat was a gif"),
			),
			segs,
		)
	}

	@Test
	fun bareMediaUrlBecomesMedia() {
		val url = "https://media.giphy.com/media/xyz/giphy.gif"
		assertEquals(
			listOf(
				text("look: "),
				media(url),
				text(" cool right?"),
			),
			parseCommentBodySegments("look: $url cool right?"),
		)
	}

	@Test
	fun bareArticleUrlBecomesLink() {
		// A non-media URL (an article) renders as a tappable link.
		val url = "https://www.bbc.com/news/article-123"
		assertEquals(
			listOf(
				text("read "),
				link(url, url),
				text(" now"),
			),
			parseCommentBodySegments("read $url now"),
		)
	}

	@Test
	fun emptyBodyIsSingleEmptyText() {
		assertEquals(listOf(text("")), parseCommentBodySegments(""))
	}

	@Test
	fun multipleEmbedsInterleave() {
		val segs = parseCommentBodySegments("![gif](giphy|a)\ntext\n![gif](giphy|b)")
		assertEquals(
			listOf(
				media("https://media.giphy.com/media/a/giphy.gif"),
				text("\ntext\n"),
				media("https://media.giphy.com/media/b/giphy.gif"),
			),
			segs,
		)
	}

	// --- hasCommentBodyMedia ("Images" menu filter) ------------------------

	@Test
	fun mediaFlagTrueForGiphyEmbed() {
		assertTrue(hasCommentBodyMedia("![gif](giphy|d81n9rX8stxBRbqrbG)"))
	}

	@Test
	fun mediaFlagTrueForBareGifUrl() {
		assertTrue(hasCommentBodyMedia("check https://media.giphy.com/media/xyz/giphy.gif"))
	}

	@Test
	fun mediaFlagFalseForPlainText() {
		assertFalse(hasCommentBodyMedia("just a normal comment"))
	}

	@Test
	fun mediaFlagFalseForArticleLink() {
		assertFalse(hasCommentBodyMedia("read https://www.bbc.com/news/article-123 now"))
	}

	// --- hasCommentBodyLink ("Links" menu filter) --------------------------

	@Test
	fun linkFlagTrueForArticleUrl() {
		assertTrue(hasCommentBodyLink("see https://www.bbc.com/news/article-123"))
	}

	@Test
	fun linkFlagTrueForLoneArticleUrl() {
		assertTrue(hasCommentBodyLink("https://www.bbc.com/news/article-123"))
	}

	@Test
	fun linkFlagFalseForPlainText() {
		assertFalse(hasCommentBodyLink("just a normal comment"))
	}

	@Test
	fun linkFlagFalseForMediaOnlyBody() {
		assertFalse(hasCommentBodyLink("![gif](giphy|d81n9rX8stxBRbqrbG)"))
	}

	@Test
	fun linkFlagFalseForEmptyBody() {
		assertFalse(hasCommentBodyLink(""))
	}

	@Test
	fun mediaAndLinkFlagsCoexist() {
		val body = "![gif](giphy|a) and read https://www.bbc.com/news/article-123"
		assertTrue(hasCommentBodyMedia(body))
		assertTrue(hasCommentBodyLink(body))
	}

	// --- markdown links [text](url) --------------------------------------

	@Test
	fun markdownLinkBecomesLinkSegment() {
		val url = "https://www.youtube.com/watch?v=ciHZwS71yZc"
		assertEquals(
			listOf(
				text("check "),
				link("this video", url),
				text(" out"),
			),
			parseCommentBodySegments("check [this video]($url) out"),
		)
	}

	@Test
	fun markdownLinkToPlayableMediaBecomesMedia() {
		val url = "https://media.giphy.com/media/xyz/giphy.gif"
		assertEquals(listOf(media(url)), parseCommentBodySegments("[the gif]($url)"))
	}

	@Test
	fun markdownLinkToHostIdShorthandBecomesMedia() {
		assertEquals(
			listOf(media("https://imgur.com/abc123")),
			parseCommentBodySegments("[cat](imgur|abc123)"),
		)
	}

	@Test
	fun markdownLinkToUnknownHostStaysText() {
		assertEquals(
			listOf(text("[x](unknownhost|abc)")),
			parseCommentBodySegments("[x](unknownhost|abc)"),
		)
	}

	@Test
	fun imageEmbedStillBeatsLinkAtSamePosition() {
		// `![gif](giphy|abc)` also matches the markdown-link pattern; the
		// image scanner must win and produce media, not a link.
		assertEquals(
			listOf(media("https://media.giphy.com/media/abc/giphy.gif")),
			parseCommentBodySegments("![gif](giphy|abc)"),
		)
	}

	@Test
	fun markdownLinkDrivesLinksFilter() {
		val url = "https://www.youtube.com/watch?v=ciHZwS71yZc"
		assertTrue(hasCommentBodyLink("[this video]($url)"))
		assertFalse(hasCommentBodyMedia("[this video]($url)"))
	}
}
