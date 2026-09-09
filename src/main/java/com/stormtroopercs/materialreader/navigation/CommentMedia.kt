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
 * A piece of a rendered comment body: either a run of plain [Text] or an
 * embedded [Media] block (a GIF / video / still) that should play inline.
 */
sealed interface CommentBodySegment {
	data class Text(val value: String) : CommentBodySegment

	data class Media(val url: String) : CommentBodySegment
}

// A markdown image embed: `![alt](target)`. Reddit renders GIF drops as
// `![gif](giphy|<id>)` / `![gif](https://…)` / `![gif](imgur|<id>)`.
private val MARKDOWN_IMAGE = Regex("!\\[([^\\]]*)\\]\\(([^)\\s]+)\\)")

// A bare media URL sitting in the body text (no markdown wrapper).
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
 * Split a raw comment body into [CommentBodySegment]s: runs of text and the
 * media blocks embedded in it. Recognized media:
 *  - markdown image embeds `![alt](target)` whose target is a URL or a
 *    `host|id` shorthand (see [normalizeEmbedTarget]);
 *  - bare URLs in the text that [LinkHandler] recognises as a direct image /
 *    GIF / video file.
 *
 * Anything else stays text. A body with no media yields a single [Text]
 * segment equal to the original body.
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
		val bu = BARE_URL.find(body, i)
		// The earliest token (markdown embed or bare URL) at or after i.
		val match = when {
			md != null && (bu == null || md.range.first <= bu.range.first) -> md
			bu != null -> bu
			else -> null
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
		} else {
			val url = match.value
			val uri = UriString(url)
			if (LinkHandler.isDirectStillImage(uri) ||
				LinkHandler.isDirectGifFile(uri) ||
				LinkHandler.isDirectVideoFile(uri)
			) {
				flush()
				segments.add(CommentBodySegment.Media(url))
			} else {
				// A non-media URL (an article link, etc.) — keep as text.
				text.append(url)
			}
		}
	}
	flush()
	if (segments.isEmpty()) segments.add(CommentBodySegment.Text(body))
	return segments
}

/**
 * Renders a comment body, playing any embedded GIF / video / still inline
 * (FINAL-DESIGN 7.1 comment body) instead of leaving the raw markdown visible.
 *
 * [onOpenMedia] is invoked when the user taps a media block to open it in the
 * full-screen viewer.
 */
@Composable
fun CommentBody(
	body: String,
	onOpenMedia: (String) -> Unit = {},
) {
	val segments = remember(body) { parseCommentBodySegments(body) }
	if (segments.size == 1 && segments[0] is CommentBodySegment.Text) {
		// Fast path: no media — render exactly as the plain body text.
		Text(
			text = body,
			style = MaterialTheme.typography.bodyMedium,
			maxLines = Int.MAX_VALUE,
			overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
		)
		return
	}
	segments.forEach { segment ->
		when (segment) {
			is CommentBodySegment.Text -> Text(
				text = segment.value,
				style = MaterialTheme.typography.bodyMedium,
				maxLines = Int.MAX_VALUE,
				overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
			)

			is CommentBodySegment.Media -> {
				Spacer(Modifier.height(6.dp))
				CommentMediaBlock(url = segment.url, onOpenMedia = onOpenMedia)
				Spacer(Modifier.height(6.dp))
			}
		}
	}
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
