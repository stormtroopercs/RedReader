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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.compose.net.NetRequestStatus
import org.quantumbadger.redreader.compose.net.fetchImage
import org.quantumbadger.redreader.compose.theme.LocalComposeTheme
import org.quantumbadger.redreader.common.UriString
import kotlin.math.max

/**
 * In-app Compose full-screen still-image viewer.
 *
 * Loads the image at up to [maxCanvasDimension] px on the longest axis via
 * [fetchImage] (the same cache-backed loader [NetImage] uses) and renders it
 * centred on a full-screen surface with two-finger pinch-to-zoom, panning
 * while zoomed, and double-tap to toggle zoom. The legacy `ImageViewActivity`
 * remains the destination for animated GIFs, video, and album (multi-image)
 * URLs; this route is for unambiguously-still direct image URLs.
 */
@Composable
fun ImageScreen(
    url: UriString,
    onBackPressed: () -> Unit,
    maxCanvasDimension: Int = 2048
) {
    val theme = LocalComposeTheme.current

    val data by fetchImage(url, scaleToMaxAxis = maxCanvasDimension)

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
                text = url.value.substringAfterLast('/').takeIf { it.isNotBlank() }
                    ?: "Image",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        when (val it = data) {
            NetRequestStatus.Connecting, is NetRequestStatus.Downloading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = theme.album.toolbarIconColor)
                }
            }

            is NetRequestStatus.Failed -> {
                RRErrorView(error = it.error)
            }

            is NetRequestStatus.Success -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    ZoomableImage(bitmap = it.result.data)
                }
            }
        }
    }
}

/**
 * Two-finger pinch-zoomable, pannable, double-tap-to-zoom image.
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
