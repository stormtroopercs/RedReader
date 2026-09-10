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
@file:Suppress("DEPRECATION") // the GIF pipeline keeps android.graphics.Movie (minSdk 23)

package com.stormtroopercs.materialreader.navigation

import android.graphics.Movie
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.stormtroopercs.materialreader.common.LinkHandler
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.compose.net.NetRequestStatus
import com.stormtroopercs.materialreader.compose.net.fetchGif
import com.stormtroopercs.materialreader.compose.net.fetchImage
import com.stormtroopercs.materialreader.compose.net.fetchImageInfo
import com.stormtroopercs.materialreader.compose.net.fetchVideoStream
import com.stormtroopercs.materialreader.image.ImageInfo
import com.stormtroopercs.materialreader.views.GIFView
import com.stormtroopercs.materialreader.views.video.ExoPlayerSeekableInputStreamDataSource
import com.stormtroopercs.materialreader.views.video.ExoPlayerSeekableInputStreamDataSourceFactory
import com.stormtroopercs.materialreader.views.video.ExoPlayerWrapperView

/**
 * A piece of a rendered comment body: a run of plain [Text], an embedded
 * [Media] block (a GIF / video / still that plays inline), or a [Link]
 * (markdown `[text](url)` or a bare non-media URL) that renders as
 * prominent, tappable link text.
 */
sealed interface CommentBodySegment {
	data class Text(val value: String) : CommentBodySegment

	data class Media(val url: String) : CommentBodySegment

	data class Link(val text: String, val url: String) : CommentBodySegment
}

// A markdown image embed: `![alt](target)`. Reddit renders GIF drops as
// `![gif](giphy|<id>)` / `![gif](https://…)` / `![gif](imgur|<id>)`.
private val MARKDOWN_IMAGE = Regex("!\\[([^\\]]*)\\]\\(([^)\\s]+)\\)")

// A markdown link: `[text](url)`. The scanner skips occurrences a `!`
// precedes (those are image embeds, matched by [MARKDOWN_IMAGE]).
private val MARKDOWN_LINK = Regex("\\[([^\\]]*)\\]\\(([^)\\s]+)\\)")

// A bare URL sitting in the body text (no markdown wrapper).
private val BARE_URL = Regex("https?://[^\\s\\)\\]}>\"']+")

/**
 * Turn a markdown-image target into a playable URL.
 *
 * Reddit comment GIF embeds use two forms: a real URL, or a `host|id`
 * shorthand (e.g. `giphy|<id>`). The shorthand is mapped to the host's page
 * (or, for Giphy, its direct `.gif` file) so [fetchImageInfo] / the direct
 * pipeline can play it. Returns `null` when the target is not a host we know
 * how to play, in which case the caller keeps the raw markdown as text.
 */
private fun normalizeEmbedTarget(target: String): String? {
	val t = target.trim()
	if (t.isBlank()) return null
	if (t.startsWith("http://") || t.startsWith("https://")) return t
	val bar = t.indexOf('|')
	if (bar <= 0) return null
	val host = t.substring(0, bar).lowercase()
	val id = t.substring(bar + 1).trim()
	if (id.isBlank()) return null
	return when (host) {
		// Giphy drops as a direct GIF file so the inline player is a control-free,
		// auto-looping GIF rather than an mp4 with a playback-control bar.
		"giphy" -> "https://media.giphy.com/media/$id/giphy.gif"
		"imgur" -> "https://imgur.com/$id"
		"gfycat" -> "https://gfycat.com/$id"
		"redgifs" -> "https://www.redgifs.com/watch/$id"
		"streamable" -> "https://streamable.com/$id"
		"redd.it", "reddit", "v.redd.it" -> "https://v.redd.it/$id"
		else -> null
	}
}

/**
 * True when [uri] is (or resolves to) a playable media URL: a direct file
 * (`.jpg` / `.gif` / `.mp4` / ...) or a host whose documented URL scheme maps
 * to a direct file. The extension-only [LinkHandler] `isDirect*` checks miss
 * query-stringed image hosts such as `preview.redd.it/<id>.jpg?width=…`
 * (Reddit's own CDN) because they look past the `?` for the file extension;
 * [LinkHandler.resolveImagePatternUrl] strips the query before matching, so
 * OR-ing it in lets the parser promote those to inline media. This mirrors the
 * full-screen image viewer's own resolution so the parser and the inline
 * player agree on what counts as media.
 */
private fun isPlayableMediaUrl(uri: UriString): Boolean = LinkHandler.isDirectStillImage(uri) ||
	LinkHandler.isDirectGifFile(uri) ||
	LinkHandler.isDirectVideoFile(uri) ||
	LinkHandler.resolveImagePatternUrl(uri) != null

/**
 * Split a raw comment body into [CommentBodySegment]s: runs of text, the
 * media blocks embedded in it, and links. Recognized:
 *  - markdown image embeds `![alt](target)` whose target is a URL or a
 *    `host|id` shorthand (see [normalizeEmbedTarget]) → [CommentBodySegment.Media];
 *  - markdown links `[text](url)` whose target is a playable media URL →
 *    [CommentBodySegment.Media], otherwise → [CommentBodySegment.Link];
 *  - bare URLs in the text: direct image / GIF / video files per
 *    [LinkHandler] → [CommentBodySegment.Media], everything else →
 *    [CommentBodySegment.Link].
 *
 * Anything else stays text. A body with no media or links yields a single
 * [CommentBodySegment.Text] segment equal to the original body.
 */
fun parseCommentBodySegments(body: String): List<CommentBodySegment> {
	val segments = ArrayList<CommentBodySegment>()
	val text = StringBuilder()
	fun flush() {
		if (text.isNotEmpty()) {
			segments.add(CommentBodySegment.Text(text.toString()))
			text.clear()
		}
	}

	var i = 0
	while (i < body.length) {
		val md = MARKDOWN_IMAGE.find(body, i)
		val ml = MARKDOWN_LINK.find(body, i)
		val bu = BARE_URL.find(body, i)
		// The earliest token (image embed, markdown link, or bare URL) at or
		// after i. Image embeds beat a link starting at the same index (an
		// image's `!` immediately precedes the link's `[`).
		val match = when {
			md != null &&
				(ml == null || md.range.first <= ml.range.first) &&
				(bu == null || md.range.first <= bu.range.first) -> md
			ml != null && (bu == null || ml.range.first <= bu.range.first) -> ml
			else -> bu
		}
		if (match == null) {
			// No more tokens — the rest of the body is plain text.
			text.append(body.substring(i))
			break
		}
		// Append any text sitting between i and the token.
		if (match.range.first > i) {
			text.append(body.substring(i, match.range.first))
		}
		i = match.range.last + 1
		if (match === md) {
			val target = normalizeEmbedTarget(match.groupValues[2])
			if (target != null) {
				flush()
				segments.add(CommentBodySegment.Media(target))
			} else {
				// An embed for a host we can't play — keep it visible as text.
				text.append(match.value)
			}
		} else if (match === ml) {
			val linkText = match.groupValues[1]
			val target = match.groupValues[2]
			if (target.startsWith("http://") || target.startsWith("https://")) {
				val uri = UriString(target)
				if (isPlayableMediaUrl(uri)) {
					// A markdown link pointing at a direct media file plays inline.
					flush()
					segments.add(CommentBodySegment.Media(target))
				} else {
					// A regular link — render it as prominent tappable text.
					flush()
					segments.add(CommentBodySegment.Link(linkText, target))
				}
			} else {
				val playable = normalizeEmbedTarget(target)
				if (playable != null) {
					// A `host|id` shorthand we know how to play.
					flush()
					segments.add(CommentBodySegment.Media(playable))
				} else {
					// A shorthand for a host we can't play — keep visible as text.
					text.append(match.value)
				}
			}
		} else {
			val url = match.value
			val uri = UriString(url)
			if (isPlayableMediaUrl(uri)) {
				flush()
				segments.add(CommentBodySegment.Media(url))
			} else {
				// A non-media URL (an article link, etc.) — render it as
				// prominent tappable text.
				flush()
				segments.add(CommentBodySegment.Link(url, url))
			}
		}
	}
	flush()
	if (segments.isEmpty()) segments.add(CommentBodySegment.Text(body))
	return segments
}

/**
 * True when the comment body embeds at least one piece of media (a GIF,
 * video, or image token). Drives the "Images" comment-nav menu option.
 */
fun hasCommentBodyMedia(body: String): Boolean = parseCommentBodySegments(body).any { it is CommentBodySegment.Media }

/**
 * True when the comment body contains at least one non-media link (a
 * markdown `[text](url)` or a bare URL that the parser keeps as tappable
 * link text). Drives the "Links" comment-nav menu option. A URL that the
 * parser promotes to a playable media block does not count.
 */
fun hasCommentBodyLink(body: String): Boolean = parseCommentBodySegments(body).any { it is CommentBodySegment.Link }

/**
 * Renders a comment body: embedded GIF / video / stills play inline
 * (FINAL-DESIGN 7.1 comment body), and every link (markdown `[text](url)`
 * or a bare URL) is drawn as prominent, underlined, tappable link text so
 * the user knows it opens.
 *
 * [onOpenMedia] is invoked when the user taps a media block to open it in
 * the full-screen viewer. Link taps are always routed through the app's
 * standard [LinkHandler.onLinkClicked] dispatcher (in-app viewer for
 * media-bearing URLs, the in-app/external browser for pages).
 */
@Composable
fun CommentBody(
	body: String,
	onOpenMedia: (String) -> Unit = {},
) {
	val context = LocalContext.current
	val onOpenLink: (String) -> Unit = { url ->
		(context as? AppCompatActivity)?.let { host -> LinkHandler.onLinkClicked(host, UriString(url)) }
	}
	val segments = remember(body) { parseCommentBodySegments(body) }
	if (segments.size == 1 && segments[0] is CommentBodySegment.Text) {
		// Fast path: no media, no links — render exactly as the plain body text.
		Text(
			text = body,
			style = MaterialTheme.typography.bodyMedium,
			maxLines = Int.MAX_VALUE,
			overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
		)
		return
	}
	// Render maximal runs of Text/Link segments as one text span (so a
	// sentence containing several links is a single line of text); media
	// blocks break the runs.
	var run = listOf<CommentBodySegment>()

	@Composable
	fun emitRun(parts: List<CommentBodySegment>) {
		if (parts.isEmpty()) return
		Text(
			text = buildBodyRun(parts, onOpenLink),
			style = MaterialTheme.typography.bodyMedium,
			maxLines = Int.MAX_VALUE,
			overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
		)
	}
	for (segment in segments) {
		when (segment) {
			is CommentBodySegment.Media -> {
				emitRun(run)
				run = emptyList()
				Spacer(Modifier.height(6.dp))
				CommentMediaBlock(url = segment.url, onOpenMedia = onOpenMedia)
				Spacer(Modifier.height(6.dp))
			}

			is CommentBodySegment.Text, is CommentBodySegment.Link -> run += segment
		}
	}
	emitRun(run)
}

/**
 * Builds one text run (plain text interleaved with links) as an
 * [AnnotatedString] whose links are prominent: accent-coloured, underlined,
 * and tappable (a [LinkAnnotation.Clickable] carrying the target URL).
 */
@Composable
private fun buildBodyRun(
	segments: List<CommentBodySegment>,
	onOpenLink: (String) -> Unit,
): AnnotatedString {
	val builder = AnnotatedString.Builder()
	for (segment in segments) {
		when (segment) {
			is CommentBodySegment.Text -> builder.append(segment.value)

			is CommentBodySegment.Link -> {
				val start = builder.length
				builder.append(segment.text)
				val end = builder.length
				builder.addStyle(
					SpanStyle(
						color = MaterialTheme.colorScheme.primary,
						textDecoration = TextDecoration.Underline,
					),
					start,
					end,
				)
				builder.addLink(
					LinkAnnotation.Clickable(
						tag = segment.url,
						linkInteractionListener = { onOpenLink(segment.url) },
					),
					start,
					end,
				)
			}

			is CommentBodySegment.Media -> Unit // media never lands in a text run
		}
	}
	return builder.toAnnotatedString()
}

/**
 * Plays one embedded media block inline: a direct image / GIF / video file
 * loads straight through the [fetchImage] / [fetchGif] / [fetchVideoStream]
 * pipeline; a page-URL host (imgur / gfycat / redgifs / streamable /
 * v.redd.it) is resolved via [fetchImageInfo] (the same `LinkHandler` host
 * resolution the full-screen viewer uses) and then played. The block is
 * tappable to open in the full-screen viewer.
 */
@Composable
fun CommentMediaBlock(
	url: String,
	onOpenMedia: (String) -> Unit,
) {
	val uri = UriString(url)
	val direct = LinkHandler.isDirectStillImage(uri) ||
		LinkHandler.isDirectGifFile(uri) ||
		LinkHandler.isDirectVideoFile(uri)
	val resolved = if (!direct) fetchImageInfo(uri).value else null
	val info = (resolved as? NetRequestStatus.Success)?.result

	val mediaUrl = info?.original?.url ?: uri
	val isVideo = info?.mediaType == ImageInfo.MediaType.VIDEO ||
		(direct && LinkHandler.isDirectVideoFile(uri))
	val isGif = if (isVideo) {
		false
	} else {
		info?.let { it.mediaType == ImageInfo.MediaType.GIF || it.isAnimated == true }
			?: (direct && LinkHandler.isDirectGifFile(uri))
	}

	val box = Modifier
		.fillMaxWidth()
		.height(220.dp)
		.clip(RoundedCornerShape(8.dp))
		.background(MaterialTheme.colorScheme.surfaceVariant)
		.clickable { onOpenMedia(url) }

	when {
		resolved is NetRequestStatus.Failed -> Box(box, contentAlignment = Alignment.Center) {
			Text(
				text = "Couldn't load this media",
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}

		info != null && info.mediaType == null && info.urlEmbeddedPlayer == null -> Box(
			box,
			contentAlignment = Alignment.Center,
		) {
			Text(
				text = "This media can't be played here",
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}

		info == null && !direct -> Box(box, contentAlignment = Alignment.Center) {
			CircularProgressIndicator()
		}

		isVideo -> CommentInlineVideo(mediaUrl, box)
		isGif -> CommentInlineGif(mediaUrl, box)
		else -> CommentInlineStill(mediaUrl, box)
	}
}

/** Plays a video (e.g. a resolved giphy / gfycat / imgur mp4) with the
 *  looping, auto-playing [ExoPlayerWrapperView]. */
@OptIn(UnstableApi::class)
@Composable
private fun CommentInlineVideo(
	url: UriString,
	modifier: Modifier,
) {
	val videoData by fetchVideoStream(url)
	when (val it = videoData) {
		NetRequestStatus.Connecting, is NetRequestStatus.Downloading -> Box(
			modifier,
			contentAlignment = Alignment.Center,
		) { CircularProgressIndicator() }

		is NetRequestStatus.Failed -> Box(modifier, contentAlignment = Alignment.Center) {
			Text(
				text = "Couldn't load video",
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}

		is NetRequestStatus.Success -> {
			val factory = it.result.metadata?.streamFactory
			if (factory != null) {
				val mediaSource: MediaSource = ProgressiveMediaSource
					.Factory(ExoPlayerSeekableInputStreamDataSourceFactory(true, factory))
					.createMediaSource(MediaItem.fromUri(ExoPlayerSeekableInputStreamDataSource.URI))
				AndroidView(
					factory = { context ->
						ExoPlayerWrapperView(context, mediaSource, ExoPlayerWrapperView.Listener {}, 0)
					},
					onRelease = { it.release() },
					modifier = modifier,
				)
			} else {
				Box(modifier, contentAlignment = Alignment.Center) {
					CircularProgressIndicator()
				}
			}
		}
	}
}

/** Plays an animated GIF (a `.gif` file) as a control-free, auto-looping
 *  [GIFView]. */
@Composable
private fun CommentInlineGif(
	url: UriString,
	modifier: Modifier,
) {
	val gifData by fetchGif(url)
	when (val it = gifData) {
		NetRequestStatus.Connecting, is NetRequestStatus.Downloading -> Box(
			modifier,
			contentAlignment = Alignment.Center,
		) { CircularProgressIndicator() }

		is NetRequestStatus.Failed -> Box(modifier, contentAlignment = Alignment.Center) {
			Text(
				text = "Couldn't load GIF",
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}

		is NetRequestStatus.Success -> {
			val movie: Movie = it.result.data
			AndroidView(
				factory = { context -> GIFView(context, movie) },
				modifier = modifier,
			)
		}
	}
}

/** Shows a still image, fitted within the block. */
@Composable
private fun CommentInlineStill(
	url: UriString,
	modifier: Modifier,
) {
	val data by fetchImage(url, scaleToMaxAxis = 1024)
	when (val it = data) {
		NetRequestStatus.Connecting, is NetRequestStatus.Downloading -> Box(
			modifier,
			contentAlignment = Alignment.Center,
		) { CircularProgressIndicator() }

		is NetRequestStatus.Failed -> Box(modifier, contentAlignment = Alignment.Center) {
			Text(
				text = "Couldn't load image",
				style = MaterialTheme.typography.labelMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
			)
		}

		is NetRequestStatus.Success -> Image(
			bitmap = it.result.data,
			contentDescription = null,
			contentScale = ContentScale.Fit,
			modifier = modifier,
		)
	}
}
