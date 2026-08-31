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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stormtroopercs.materialreader.R
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.compose.net.NetRequestStatus
import com.stormtroopercs.materialreader.compose.net.fetchImage
import com.stormtroopercs.materialreader.compose.theme.LocalComposeTheme

/**
 * A post's media preview shared by the list feed's [PostCard] and the
 * legacy list screen: a still image (via the existing `fetchImage`
 * pipeline) with a video badge. Fixed-size (e.g. a card's 96dp thumb) or
 * full-width depending on [size].
 */
@Composable
fun PostThumbnailPreview(
	uri: String,
	isVideo: Boolean = false,
	modifier: Modifier = Modifier,
	size: Dp? = null,
	shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
	contentScale: ContentScale = ContentScale.Crop,
) {
	val theme = LocalComposeTheme.current.postCard
	val backgroundModifier = if (size != null) {
		modifier.size(size).clip(shape)
	} else {
		modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(shape)
	}

	Box(
		modifier = backgroundModifier.background(theme.previewImageBackgroundColor),
		contentAlignment = Alignment.Center,
	) {
		val data by fetchImage(UriString(uri), scaleToMaxAxis = 640)

		when (val it = data) {
			is NetRequestStatus.Connecting -> {
				CircularProgressIndicator()
			}

			is NetRequestStatus.Downloading -> {
				CircularProgressIndicator(progress = { it.fractionComplete })
			}

			is NetRequestStatus.Failed -> {
				// Empty box for failed images.
			}

			is NetRequestStatus.Success -> {
				Image(
					bitmap = it.result.data,
					contentDescription = null,
					contentScale = contentScale,
					modifier = Modifier.fillMaxSize(),
				)

				if (isVideo) {
					Box(
						modifier = Modifier
							.fillMaxSize()
							.background(Color.Black.copy(alpha = 0.3f)),
						contentAlignment = Alignment.Center,
					) {
						Icon(
							painter = painterResource(R.drawable.icon_play),
							contentDescription = "Play video",
							tint = Color.White,
							modifier = Modifier.size(48.dp),
						)
					}
				}
			}
		}
	}
}
