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

package com.stormtroopercs.materialreader.compose.ui

import android.graphics.Movie
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.stormtroopercs.materialreader.R
import com.stormtroopercs.materialreader.activities.BaseActivity
import com.stormtroopercs.materialreader.common.LinkHandler
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.compose.ctx.Dest
import com.stormtroopercs.materialreader.compose.ctx.LocalLauncher
import com.stormtroopercs.materialreader.compose.net.NetRequestStatus
import com.stormtroopercs.materialreader.compose.net.fetchAlbum
import com.stormtroopercs.materialreader.compose.net.fetchGif
import com.stormtroopercs.materialreader.compose.net.fetchImage
import com.stormtroopercs.materialreader.compose.net.fetchImageInfo
import com.stormtroopercs.materialreader.compose.net.fetchVideoStream
import com.stormtroopercs.materialreader.compose.theme.LocalComposeTheme
import com.stormtroopercs.materialreader.fragments.ImageInfoDialog
import com.stormtroopercs.materialreader.image.AlbumInfo
import com.stormtroopercs.materialreader.image.ImageInfo
import com.stormtroopercs.materialreader.views.GIFView
import com.stormtroopercs.materialreader.views.video.ExoPlayerSeekableInputStreamDataSource
import com.stormtroopercs.materialreader.views.video.ExoPlayerSeekableInputStreamDataSourceFactory
import com.stormtroopercs.materialreader.views.video.ExoPlayerWrapperView
import kotlin.math.max

/**
 * In-app Compose full-screen image viewer.
 *
 * Standalone media (37th): the viewer self-resolves. Direct still-image /
 * `.gif` / video file URLs load straight via [fetchImage] / [fetchGif] /
 * [fetchVideoStream]; page-URL hosts (imgur / gfycat / redgifs / streamable /
 * v.redd.it / deviantart / imgflip / makeameme / giphy) resolve via
 * [fetchImageInfo] (the same `LinkHandler.getImageInfo` host resolution the
 * legacy `ImageViewActivity` used) and then render exactly the same way —
 * still via [ZoomableImage], GIF via [ZoomableGif], video via [VideoImage].
 * Resolved media also exposes the media toolbar: save, share media / share
 * link, and an image-info dialog when the host returned title / caption /
 * dimensions. A host that only offers an embedded web player (e.g. RedGifs)
 * or a failed resolution shows an error with a "view in browser" button.
 *
 * Album: [albumUrl] non-null — resolves via [fetchAlbum] and shows a
 * horizontal [HorizontalPager] over the album's images (swipe between
 * them), each page a still, GIF, or video per its [ImageInfo.mediaType];
 * the tapped image opens at [albumIndex]. Album entries whose URLs are
 * page/API URLs needing host resolution (38th) are resolved per page via
 * [fetchImageInfo] (the same [ResolvedAlbumImage] mechanism the standalone
 * path uses).
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
    val launch = LocalLauncher.current

    val standalone = albumUrl == null

    // Standalone links that are not direct media files need host resolution
    // (imgur / gfycat / redgifs / streamable / v.redd.it / deviantart / ...).
    // Resolve them here — the same host resolution the legacy ImageViewActivity
    // performed — and render the result with the same still / GIF / video
    // components (37th).
    val needsResolution = standalone &&
        !LinkHandler.isDirectStillImage(url) &&
        !LinkHandler.isDirectGifFile(url) &&
        !LinkHandler.isDirectVideoFile(url)
    // Composed conditionally so the resolver's LaunchedEffect is created /
    // disposed exactly when a standalone link needs host resolution.
    val resolved: NetRequestStatus<ImageInfo>? =
        if (needsResolution) fetchImageInfo(url).value else null

    val info: ImageInfo? = when (val r = resolved) {
        null -> null
        is NetRequestStatus.Success -> r.result
        else -> null
    }

    val mediaUrl = info?.original?.url ?: url
    val effectiveIsGif = info?.let {
        it.mediaType == ImageInfo.MediaType.GIF || it.isAnimated == true
    } ?: isGif
    val effectiveIsVideo = info?.let {
        it.mediaType == ImageInfo.MediaType.VIDEO
    } ?: isVideo

    val barTitle = when {
        !standalone -> stringResource(R.string.image_gallery)
        info?.title?.isNotBlank() == true -> info.title.orEmpty()
        else -> mediaUrl.value.substringAfterLast('/').takeIf { it.isNotBlank() } ?: "Image"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.postCard.listBackgroundColor)
    ) {
        // Slim top bar: back, title, and (standalone) the media-action toolbar
        // (save / share / image info) — the Compose equivalent of the legacy
        // ImageViewActivity floating toolbar.
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
            if (standalone && (info != null || !needsResolution)) {
                MediaToolbar(
                    mediaUrl = mediaUrl,
                    shareLinkUrl = url,
                    info = info,
                    theme = theme,
                    launch = launch
                )
            }
        }

        if (!standalone) {
            AlbumPager(
                albumUrl = albumUrl,
                startIndex = albumIndex,
                theme = theme
            )
        } else if (needsResolution) {
            when (val r = resolved) {
                null -> MediaSpinner(theme)
                is NetRequestStatus.Connecting, is NetRequestStatus.Downloading -> MediaSpinner(theme)
                is NetRequestStatus.Failed -> {
                    ResolutionFailure(
                        error = r.error,
                        browserUrl = url.value,
                        launch = launch
                    )
                }
                is NetRequestStatus.Success -> {
                    val result = r.result
                    if (result.mediaType == null && result.urlEmbeddedPlayer == null) {
                        // A host that resolved to no playable media (e.g. a
                        // RedGifs page with only an embedded web player).
                        ResolutionFailure(
                            error = null,
                            browserUrl = result.urlEmbeddedPlayer?.value
                                ?: result.original.url.value,
                            launch = launch
                        )
                    } else {
                        MediaImage(
                            url = result.original.url,
                            isGif = result.mediaType == ImageInfo.MediaType.GIF
                                || result.isAnimated == true,
                            isVideo = result.mediaType == ImageInfo.MediaType.VIDEO,
                            maxCanvasDimension = maxCanvasDimension,
                            theme = theme
                        )
                    }
                }
            }
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
 * Media-action toolbar for standalone media: save the media, a share menu
 * (share the media file / share the source link), and — when the host
 * resolution returned title / caption / dimensions — an image-info dialog.
 * Mirrors the legacy `ImageViewActivity` floating toolbar via the same
 * `FileUtils` / `LinkHandler` helpers the legacy menu used.
 */
@Composable
private fun MediaToolbar(
    mediaUrl: UriString,
    shareLinkUrl: UriString,
    info: ImageInfo?,
    theme: com.stormtroopercs.materialreader.compose.theme.ComposeTheme,
    launch: (Dest) -> Unit
) {
    val context = LocalContext.current

    RRIconButton(
        onClick = { launch(Dest.SaveMedia(mediaUrl)) },
        icon = R.drawable.download,
        contentDescription = R.string.action_save_image,
        tint = theme.album.toolbarIconColor
    )

    RRDropdownMenuIconButton(
        icon = R.drawable.ic_action_share_dark,
        contentDescription = R.string.action_share
    ) {
        Item(
            icon = R.drawable.ic_action_image_dark,
            text = R.string.action_share_image,
            onClick = { launch(Dest.ShareMedia(mediaUrl)) }
        )
        Item(
            icon = R.drawable.ic_action_link_dark,
            text = R.string.action_share_link,
            onClick = { launch(Dest.ShareLink(shareLinkUrl)) }
        )
    }

    if (info != null) {
        RRIconButton(
            onClick = {
                (context as? BaseActivity)?.supportFragmentManager?.let { fm ->
                    ImageInfoDialog.newInstance(info).show(fm, null)
                }
            },
            icon = R.drawable.ic_action_info_dark,
            contentDescription = R.string.props_image_title,
            tint = theme.album.toolbarIconColor
        )
    }
}

/**
 * Shown when a standalone host link cannot be displayed in-app: a failed
 * [fetchImageInfo] resolution ([error] non-null) or a host that resolved to
 * no playable media, only an embedded web page. Both offer "view in browser"
 * via [Dest.WebBrowser].
 */
@Composable
private fun ResolutionFailure(
    error: com.stormtroopercs.materialreader.common.RRError?,
    browserUrl: String,
    launch: (Dest) -> Unit
) {
    val theme = LocalComposeTheme.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (error != null) {
            RRErrorView(error = error)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { launch(Dest.WebBrowser(browserUrl)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(stringResource(R.string.action_external))
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
    theme: com.stormtroopercs.materialreader.compose.theme.ComposeTheme
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
 * Plays a video from a cache-backed [com.stormtroopercs.materialreader.common.
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
    streamFactory: com.stormtroopercs.materialreader.common.GenericFactory<com.stormtroopercs.materialreader.common.datastream.SeekableInputStream, java.io.IOException>,
    theme: com.stormtroopercs.materialreader.compose.theme.ComposeTheme,
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
private fun MediaSpinner(theme: com.stormtroopercs.materialreader.compose.theme.ComposeTheme) {
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
    theme: com.stormtroopercs.materialreader.compose.theme.ComposeTheme
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
                        ResolvedAlbumImage(
                            image = it.result.images[page],
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
 * One page of an album. If the entry's `original` URL is a direct media file
 * (still / `.gif` / video), it renders straight through [MediaImage]; if it
 * is a page/API URL that needs host resolution (an imgur / gfycat /
 * redgifs / streamable / v.redd.it / deviantart image inside an album —
 * the last `ImageViewActivity` gap, 38th), it resolves via [fetchImageInfo]
 * and renders the result the same way. A host that resolves to no playable
 * media shows a small inline error for that page only.
 */
@Composable
private fun ResolvedAlbumImage(
    image: ImageInfo,
    maxCanvasDimension: Int,
    theme: com.stormtroopercs.materialreader.compose.theme.ComposeTheme
) {
    val isDirect = LinkHandler.isDirectStillImage(image.original.url) ||
        LinkHandler.isDirectGifFile(image.original.url) ||
        LinkHandler.isDirectVideoFile(image.original.url)

    if (isDirect) {
        MediaImage(
            url = image.original.url,
            isGif = image.mediaType == ImageInfo.MediaType.GIF
                || image.isAnimated == true,
            isVideo = image.mediaType == ImageInfo.MediaType.VIDEO,
            maxCanvasDimension = maxCanvasDimension,
            theme = theme
        )
    } else {
        val resolved = fetchImageInfo(image.original.url).value
        when (val r = resolved) {
            is NetRequestStatus.Connecting, is NetRequestStatus.Downloading -> {
                MediaSpinner(theme)
            }

            is NetRequestStatus.Failed -> {
                RRErrorView(error = r.error)
            }

            is NetRequestStatus.Success -> {
                val result = r.result
                if (result.mediaType == null && result.urlEmbeddedPlayer == null) {
                    // A host that resolved to no playable media (e.g. an
                    // embedded web player) — show a note for this page.
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.error_inline_preview_failed_message),
                            color = theme.album.toolbarIconColor
                        )
                    }
                } else {
                    MediaImage(
                        url = result.original.url,
                        isGif = result.mediaType == ImageInfo.MediaType.GIF
                            || result.isAnimated == true,
                        isVideo = result.mediaType == ImageInfo.MediaType.VIDEO,
                        maxCanvasDimension = maxCanvasDimension,
                        theme = theme
                    )
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
