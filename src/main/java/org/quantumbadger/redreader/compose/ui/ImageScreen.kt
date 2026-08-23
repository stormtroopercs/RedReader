/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.quantumbadger.redreader.compose.ui

import android.graphics.Movie
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.compose.net.NetRequestStatus
import org.quantumbadger.redreader.compose.net.fetchAlbum
import org.quantumbadger.redreader.compose.net.fetchGif
import org.quantumbadger.redreader.compose.net.fetchImage
import org.quantumbadger.redreader.compose.net.fetchVideoStream
import org.quantumbadger.redreader.compose.theme.LocalComposeTheme
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.image.AlbumInfo
import org.quantumbadger.redreader.image.ImageInfo
import org.quantumbadger.redreader.views.GIFView
import org.quantumbadger.redreader.views.video.ExoPlayerSeekableInputStreamDataSource
import org.quantumbadger.redreader.views.video.ExoPlayerSeekableInputStreamDataSourceFactory
import org.quantumbadger.redreader.views.video.ExoPlayerWrapperView
import kotlin.math.max

/**
 * In-app Compose full-screen image viewer.
 *
 * Three modes:
 *  - Single still: [albumUrl] null, [isGif] false, [isVideo] false — loads
 *    via [fetchImage] (the same cache-backed loader [NetImage] uses, scaled
 *    to a 2048 px longest axis) and renders with [ZoomableImage].
 *  - Single GIF: [albumUrl] null, [isGif] true — loads as an Android [Movie]
 *    via [fetchGif] (the same `readRemainingAsBytes` +
 *    `GIFView.prepareMovie` path the legacy `ImageViewActivity` uses) and
 *    renders with the legacy [GIFView] via [ZoomableGif].
 *  - Single video: [albumUrl] null, [isVideo] true — streams via
 *    [fetchVideoStream] and plays in the legacy [ExoPlayerWrapperView]
 *    ([VideoImage]).
 *  - Album: [albumUrl] non-null — resolves via [fetchAlbum] and shows a
 *    horizontal [HorizontalPager] over the album's images (swipe between
 *    them), each page a still, GIF, or video per its [ImageInfo.mediaType];
 *    the tapped image opens at [albumIndex].
 *
 * All still/GIF pages share the same two-finger pinch-zoom, pan-while-zoomed,
 * and double-tap-to-zoom gestures, and the same slim back bar. The legacy
 * `ImageViewActivity` remains the destination for API-resolved GIF hosts
 * (gfycat / redgifs / giphy) and albums containing non-direct (page-URL)
 * images that need host resolution.
 */
@Composable
fun ImageScreen(
    url: UriString,
    isGif: Boolean = false,
    isVideo: Boolean = false,
    albumUrl: UriString? = null,
    albumIndex: Int = 0,
    onBackPressed: () -> Unit,
    maxCanvasDimension: Int = 2048
) {
    val theme = LocalComposeTheme.current

    val barTitle = if (albumUrl != null) {
        stringResource(R.string.image_gallery)
    } else {
        url.value.substringAfterLast('/').takeIf { it.isNotBlank() } ?: "Image"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.postCard.listBackgroundColor)
    ) {
        // Slim top bar with a back affordance (mirrors AlbumScreen's bar).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RRIconButton(
                onClick = { onBackPressed() },
                icon = R.drawable.ic_action_back_dark,
                contentDescription = R.string.action_back,
                tint = theme.album.toolbarIconColor
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = barTitle,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (albumUrl != null) {
            AlbumPager(
                albumUrl = albumUrl,
                startIndex = albumIndex,
                theme = theme
            )
        } else {
            MediaImage(
                url = url,
                isGif = isGif,
                isVideo = isVideo,
                maxCanvasDimension = maxCanvasDimension,
                theme = theme
            )
        }
    }
}

/**
 * Loads and renders a single media item: still via [fetchImage], GIF via
 * [fetchGif], or video via [fetchVideoStream] — with spinner / error states.
 * Stills and GIFs get the shared pinch-zoom gestures; video plays in the
 * legacy [ExoPlayerWrapperView] (self-contained player with controls).
 */
@Composable
private fun MediaImage(
    url: UriString,
    isGif: Boolean,
    isVideo: Boolean,
    maxCanvasDimension: Int,
    theme: org.quantumbadger.redreader.compose.theme.ComposeTheme
) {
    val data by fetchImage(url, scaleToMaxAxis = maxCanvasDimension)
    val gifData by fetchGif(url)
    val videoData by fetchVideoStream(url)

    when {
        isVideo -> {
            when (val it = videoData) {
                NetRequestStatus.Connecting, is NetRequestStatus.Downloading -> {
                    MediaSpinner(theme)
                }
                is NetRequestStatus.Failed -> {
                    RRErrorView(error = it.error)
                }
                is NetRequestStatus.Success -> {
                    it.result.metadata?.let { metadata ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            VideoImage(streamFactory = metadata.streamFactory, theme = theme)
                        }
                    } ?: MediaSpinner(theme)
                }
            }
        }

        isGif -> {
            when (val it = gifData) {
                NetRequestStatus.Connecting, is NetRequestStatus.Downloading -> {
                    MediaSpinner(theme)
                }
                is NetRequestStatus.Failed -> {
                    RRErrorView(error = it.error)
                }
                is NetRequestStatus.Success -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ZoomableGif(movie = it.result.data)
                    }
                }
            }
        }

        else -> {
            when (val it = data) {
                NetRequestStatus.Connecting, is NetRequestStatus.Downloading -> {
                    MediaSpinner(theme)
                }
                is NetRequestStatus.Failed -> {
                    RRErrorView(error = it.error)
                }
                is NetRequestStatus.Success -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ZoomableImage(bitmap = it.result.data)
                    }
                }
            }
        }
    }
}

/**
 * Plays a video from a cache-backed [org.quantumbadger.redreader.common.
 * GenericFactory] stream in the legacy [ExoPlayerWrapperView] (a
 * self-contained media3 `PlayerView` wrapper: builds its own `ExoPlayer`,
 * autoplays, honours the video zoom / playback-controls prefs, and exposes
 * its own control bar). The [media source] is built exactly as the legacy
 * `ImageViewActivity.playWithExoplayer` does, so playback routes through the
 * same cache pipeline.
 */
@OptIn(UnstableApi::class)
@Composable
private fun VideoImage(
    streamFactory: org.quantumbadger.redreader.common.GenericFactory<org.quantumbadger.redreader.common.datastream.SeekableInputStream, java.io.IOException>,
    theme: org.quantumbadger.redreader.compose.theme.ComposeTheme,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            val mediaSource: MediaSource = ProgressiveMediaSource
                .Factory(ExoPlayerSeekableInputStreamDataSourceFactory(true, streamFactory))
                .createMediaSource(MediaItem.fromUri(ExoPlayerSeekableInputStreamDataSource.URI))
            ExoPlayerWrapperView(context, mediaSource, ExoPlayerWrapperView.Listener {}, 0)
        },
        onRelease = { it.release() },
        modifier = modifier.fillMaxSize()
    )
}

@Composable
private fun MediaSpinner(theme: org.quantumbadger.redreader.compose.theme.ComposeTheme) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = theme.album.toolbarIconColor)
    }
}

/**
 * Album mode: resolves [albumUrl] via [fetchAlbum] and shows a horizontal
 * pager over its images (swipe left/right), opening at [startIndex]. A
 * "N of M" indicator sits at the bottom. Each page renders through
 * [MediaImage] (still or GIF per its media type).
 */
@Composable
private fun AlbumPager(
    albumUrl: UriString,
    startIndex: Int,
    theme: org.quantumbadger.redreader.compose.theme.ComposeTheme
) {
    val album by fetchAlbum(albumUrl)

    when (val it = album) {
        NetRequestStatus.Connecting, is NetRequestStatus.Downloading -> {
            MediaSpinner(theme)
        }

        is NetRequestStatus.Failed -> {
            RRErrorView(error = it.error)
        }

        is NetRequestStatus.Success -> {
            if (it.result.images.isEmpty()) {
                MediaSpinner(theme)
            } else {
                val pagerState = rememberPagerState(
                    initialPage = startIndex.coerceIn(0, it.result.images.size - 1)
                ) { it.result.images.size }

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val image = it.result.images[page]
                        MediaImage(
                            url = image.original.url,
                            isGif = image.mediaType == ImageInfo.MediaType.GIF
                                || image.isAnimated == true,
                            isVideo = image.mediaType == ImageInfo.MediaType.VIDEO,
                            maxCanvasDimension = 2048,
                            theme = theme
                        )
                    }

                    // "N of M" position indicator.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${it.result.images.size}",
                            color = theme.album.toolbarIconColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Two-finger pinch-zoomable, pannable, double-tap-to-zoom still image.
 *
 * [scale] is clamped to [1f, 4f]; panning is clamped so the visible portion of
 * the image can never be dragged fully off the container.
 */
@Composable
private fun ZoomableImage(
    bitmap: ImageBitmap,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val imageSize = remember(bitmap) { IntSize(bitmap.width, bitmap.height) }
    val imageAspect = remember(imageSize) {
        if (imageSize.height > 0) imageSize.width.toFloat() / imageSize.height else 1f
    }

    // Size of the image as fitted (ContentScale.Fit) into the container.
    val fittedSize = remember(containerSize, imageAspect) {
        if (containerSize.width == 0 || containerSize.height == 0) Size.Zero
        else if (containerSize.width.toFloat() / containerSize.height < imageAspect) {
            Size(containerSize.width.toFloat(), containerSize.width.toFloat() / imageAspect)
        } else {
            Size(containerSize.height * imageAspect, containerSize.height.toFloat())
        }
    }

    fun maxOffsetX() = max(0f, (scale - 1f) * fittedSize.width / 2f)
    fun maxOffsetY() = max(0f, (scale - 1f) * fittedSize.height / 2f)

    fun clampOffset(o: Offset) = Offset(
        x = o.x.coerceIn(-maxOffsetX(), maxOffsetX()),
        y = o.y.coerceIn(-maxOffsetY(), maxOffsetY())
    )

    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { containerSize = it.size }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, gesturePan, gestureZoom, _ ->
                    val newScale = (scale * gestureZoom).coerceIn(1f, 4f)
                    val newOffset = if (newScale <= 1f) {
                        Offset.Zero
                    } else {
                        clampOffset(offset + gesturePan)
                    }
                    scale = newScale
                    offset = newOffset
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.01f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
    )
}

/**
 * Zoomable animated-GIF view. Wraps the legacy [GIFView] (which animates the
 * [movie] and self-centres it) in an `AndroidView`, applying the same
 * pinch-zoom / pan / double-tap gestures as [ZoomableImage]. The [GIFView]
 * fills its bounds, so the fitted-size clamp is the container size itself.
 */
@Composable
private fun ZoomableGif(
    movie: Movie,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    fun maxOffsetX() = max(0f, (scale - 1f) * containerSize.width / 2f)
    fun maxOffsetY() = max(0f, (scale - 1f) * containerSize.height / 2f)

    fun clampOffset(o: Offset) = Offset(
        x = o.x.coerceIn(-maxOffsetX(), maxOffsetX()),
        y = o.y.coerceIn(-maxOffsetY(), maxOffsetY())
    )

    AndroidView(
        factory = { context -> GIFView(context, movie) },
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { containerSize = it.size }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, gesturePan, gestureZoom, _ ->
                    val newScale = (scale * gestureZoom).coerceIn(1f, 4f)
                    val newOffset = if (newScale <= 1f) {
                        Offset.Zero
                    } else {
                        clampOffset(offset + gesturePan)
                    }
                    scale = newScale
                    offset = newOffset
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.01f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
    )
}
