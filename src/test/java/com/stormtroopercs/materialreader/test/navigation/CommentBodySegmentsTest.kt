package com.stormtroopercs.materialreader.test.navigation

import com.stormtroopercs.materialreader.navigation.CommentBodySegment
import com.stormtroopercs.materialreader.navigation.parseCommentBodySegments
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for [parseCommentBodySegments], the pure parser that splits a raw
 * comment body into runs of text and the media blocks embedded in it (Reddit
 * GIF drops render as `![gif](host|id)` / `![gif](url)` / bare media URLs).
 */
class CommentBodySegmentsTest {

	private fun text(value: String) = CommentBodySegment.Text(value)
	private fun media(url: String) = CommentBodySegment.Media(url)

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
	fun bareArticleUrlStaysText() {
		// A non-media URL (an article) is not media — it stays as text.
		val url = "https://www.bbc.com/news/article-123"
		assertEquals(listOf(text("read $url now")), parseCommentBodySegments("read $url now"))
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
}
