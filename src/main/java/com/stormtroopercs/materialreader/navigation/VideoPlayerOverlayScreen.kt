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

// media3's player APIs are @UnstableApi — this whole screen opts in
// (same as the image viewer's video path in ImageScreen.kt).
@file:OptIn(UnstableApi::class)

package com.stormtroopercs.materialreader.navigation

import android.app.Activity
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import com.stormtroopercs.materialreader.common.GenericFactory
import com.stormtroopercs.materialreader.common.PrefsUtility
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.common.datastream.SeekableInputStream
import com.stormtroopercs.materialreader.compose.net.NetRequestStatus
import com.stormtroopercs.materialreader.compose.net.fetchImage
import com.stormtroopercs.materialreader.compose.net.fetchVideoStream
import com.stormtroopercs.materialreader.views.video.VideoOverlayPlayerView
import java.io.IOException

/**
 * Full-screen video player (route [VideoPlayer]), opened when a feed video
 * post's media is tapped. Plays the post's cache-backed mp4 stream — the
 * same `fetchVideoStream` + [com.stormtroopercs.materialreader.views.video.
 * ExoPlayerSeekableInputStreamDataSource] pipeline the image viewer's video
 * path uses — autoplays, and carries the three centered circular, grey
 * translucent controls: mute / unmute, play / pause, fullscreen.
 *
 * While the stream is still resolving, the post's static preview still
 * ([VideoPlayer.previewUrl], Reddit's `reddit_video_preview`) is shown under
 * the spinner — the same still the feed shows while scrolling. Back (or the
 * top-right close) pops the route.
 */
@Composable
fun VideoPlayerOverlayScreen(
	url: String,
	previewUrl: String?,
	onNavigateBack: () -> Unit,
) {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(Color.Black),
	) {
		val data by fetchVideoStream(UriString(url))
		when (val it = data) {
			is NetRequestStatus.Connecting, is NetRequestStatus.Downloading -> {
				// The static preview (when the post has one) under the spinner.
				previewUrl?.takeIf { it.isNotBlank() }?.let { p ->
					val img by fetchImage(UriString(p), scaleToMaxAxis = 1280)
					when (val it = img) {
						is NetRequestStatus.Success -> Image(
							bitmap = it.result.data,
							contentDescription = null,
							contentScale = ContentScale.Crop,
							modifier = Modifier.fillMaxSize(),
						)
						else -> Unit
					}
				}
				Box(
					modifier = Modifier.fillMaxSize(),
					contentAlignment = Alignment.Center,
				) {
					CircularProgressIndicator()
				}
			}

			is NetRequestStatus.Failed -> {
				Column(
					modifier = Modifier
						.fillMaxSize()
						.padding(32.dp),
					horizontalAlignment = Alignment.CenterHorizontally,
					verticalArrangement = Arrangement.Center,
				) {
					Text(
						text = "Video failed to load",
						color = Color.White,
						style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
					)
					Spacer(Modifier.height(16.dp))
					TextButton(onClick = onNavigateBack) {
						Text("Close")
					}
				}
			}

			is NetRequestStatus.Success -> {
				it.result.metadata?.streamFactory?.let { factory ->
					VideoOverlayPlayer(
						streamFactory = factory,
						onNavigateBack = onNavigateBack,
						modifier = Modifier.fillMaxSize(),
					)
				} ?: Box(
					modifier = Modifier.fillMaxSize(),
					contentAlignment = Alignment.Center,
				) {
					CircularProgressIndicator()
				}
			}
		}

		// Close (top-right), above everything.
		IconButton(
			onClick = onNavigateBack,
			modifier = Modifier
				.align(Alignment.TopEnd)
				.padding(12.dp),
		) {
			Icon(
				imageVector = Icons.Filled.Close,
				contentDescription = "Close",
				tint = Color.White,
			)
		}
	}
}

/**
 * The playing video — a [VideoOverlayPlayerView], the media3
 * `PlayerView`-backed wrapper from the same family as the legacy
 * `ExoPlayerWrapperView` (views.video) — with the three centered circular,
 * grey translucent control buttons overlaid on it.
 */
@Composable
private fun VideoOverlayPlayer(
	streamFactory: GenericFactory<SeekableInputStream, IOException>,
	onNavigateBack: () -> Unit,
	modifier: Modifier = Modifier,
) {
	val context = LocalContext.current
	var isPlaying by remember { mutableStateOf(false) }
	var muted by remember { mutableStateOf(PrefsUtility.pref_behaviour_video_mute_default()) }
	// Plain holder (not observable state): the AndroidView factory runs
	// during layout, and assigning a State there would be a composition-time
	// write. The buttons read it on click, never during composition.
	val wrapperRef = remember { arrayOfNulls<VideoOverlayPlayerView>(1) }

	Box(modifier = modifier) {
		AndroidView(
			factory = { ctx ->
				val view = VideoOverlayPlayerView(ctx, streamFactory)
				view.playerStateListener = { playing -> isPlaying = playing }
				view.setMuted(muted)
				wrapperRef[0] = view
				view
			},
			onRelease = {
				wrapperRef[0] = null
				// Leaving the overlay (back button, navigation) — drop any
				// fullscreened state so the feed underneath isn't stuck
				// landscape.
				val activity = context as? Activity
				(it as VideoOverlayPlayerView).restoreSystemChrome(activity)
				it.release()
			},
			modifier = Modifier.fillMaxSize(),
		)

		// The three controls: centered on screen, left to right —
		// mute / unmute, play / pause, fullscreen.
		Row(
			modifier = Modifier.align(Alignment.Center),
			horizontalArrangement = Arrangement.spacedBy(28.dp),
		) {
			OverlayControlButton(
				icon = if (muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
				contentDescription = if (muted) "Unmute" else "Mute",
				onClick = {
					muted = !muted
					wrapperRef[0]?.setMuted(muted)
				},
			)
			OverlayControlButton(
				icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
				contentDescription = if (isPlaying) "Pause" else "Play",
				onClick = {
					val view = wrapperRef[0] ?: return@OverlayControlButton
					if (isPlaying) view.pause() else view.play()
				},
			)
			OverlayControlButton(
				icon = Icons.Filled.Fullscreen,
				contentDescription = "Fullscreen",
				onClick = {
					val activity = context as? Activity ?: return@OverlayControlButton
					wrapperRef[0]?.toggleFullscreen(activity)
				},
			)
		}
	}
}

/**
 * One of the overlay's circular, slightly grey translucent control buttons:
 * a 56dp circle, `grey @ ~35%` fill, white 28dp glyph.
 */
@Composable
private fun OverlayControlButton(
	icon: ImageVector,
	contentDescription: String,
	onClick: () -> Unit,
) {
	Surface(
		shape = CircleShape,
		color = Color(0x59808080),
		onClick = onClick,
	) {
		Box(
			modifier = Modifier
				.size(56.dp)
				.padding(14.dp),
			contentAlignment = Alignment.Center,
		) {
			Icon(
				imageVector = icon,
				contentDescription = contentDescription,
				tint = Color.White,
				modifier = Modifier.fillMaxSize(),
			)
		}
	}
}
