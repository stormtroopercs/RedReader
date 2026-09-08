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

package com.stormtroopercs.materialreader.views.video

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.stormtroopercs.materialreader.R
import com.stormtroopercs.materialreader.common.GenericFactory
import com.stormtroopercs.materialreader.common.datastream.SeekableInputStream
import java.io.IOException

/**
 * A self-contained video player for the full-screen feed-video overlay
 * ([com.stormtroopercs.materialreader.navigation.VideoPlayerOverlayScreen]).
 *
 * Builds its own [ExoPlayer] over the post's cache-backed stream (the same
 * [ExoPlayerSeekableInputStreamDataSourceFactory] the legacy
 * [ExoPlayerWrapperView] and the image viewer's video path use), autoplays,
 * repeats, honours the video-zoom resize pref, and exposes the small surface
 * the overlay's three control buttons drive: [play] / [pause], [setMuted],
 * [toggleFullscreen] (immersive sticky + landscape), and [playerStateListener]
 * (play/pause state for the play/pause glyph).
 */
@OptIn(UnstableApi::class)
class VideoOverlayPlayerView(
	context: Context,
	streamFactory: GenericFactory<SeekableInputStream, IOException>,
) : FrameLayout(context) {
	/** Notified on the main thread whenever the play state changes. */
	var playerStateListener: ((Boolean) -> Unit)? = null

	private val mVideoPlayer: ExoPlayer
	private val mVideoPlayerView: PlayerView
	private var mReleased = false

	init {
		val trackSelector = DefaultTrackSelector(context)
		mVideoPlayer = ExoPlayer.Builder(context)
			.setTrackSelector(trackSelector)
			.build()

		val videoPlayerView = LayoutInflater.from(context).inflate(
			R.layout.video_player_view,
			this,
			false,
		) as PlayerView
		mVideoPlayerView = videoPlayerView
		addView(videoPlayerView)

		videoPlayerView.setUseController(false)
		videoPlayerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
		videoPlayerView.setPlayer(mVideoPlayer)
		videoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)

		videoPlayerView.setLayoutParams(
			LayoutParams(
				LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT,
			),
		)

		val mediaSource: MediaSource = ProgressiveMediaSource
			.Factory(ExoPlayerSeekableInputStreamDataSourceFactory(true, streamFactory))
			.createMediaSource(MediaItem.fromUri(ExoPlayerSeekableInputStreamDataSource.URI))
		mVideoPlayer.setMediaSource(mediaSource)
		mVideoPlayer.prepare()
		mVideoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE)
		mVideoPlayer.setPlayWhenReady(true)

		mVideoPlayer.addListener(object : Player.Listener {
			override fun onPlayerError(error: PlaybackException) {
				// The overlay shows its own failure state (the stream fetch);
				// a decode error just parks the player.
				mVideoPlayer.setPlayWhenReady(false)
			}

			override fun onIsPlayingChanged(isPlaying: Boolean) {
				playerStateListener?.invoke(isPlaying)
			}
		})
	}

	fun play() {
		mVideoPlayer.setPlayWhenReady(true)
	}

	fun pause() {
		mVideoPlayer.setPlayWhenReady(false)
	}

	fun setMuted(muted: Boolean) {
		mVideoPlayer.volume = if (muted) 0f else 1f
	}

	/**
	 * Toggles fullscreen on the hosting activity: immersive-sticky system
	 * bars + landscape while active, the previous (windowed, bars-shown)
	 * state restored otherwise.
	 */
	fun toggleFullscreen(activity: Activity) {
		if (activity.isInLandscapeFullscreen()) {
			activity.exitFullscreen()
		} else {
			activity.enterFullscreen()
		}
	}

	/**
	 * Unconditionally restore the hosting activity's windowed state (bars
	 * shown, unspecified orientation) — called when the overlay is disposed
	 * (e.g. the user backs out of it) so a fullscreened session doesn't leak
	 * into the feed.
	 */
	fun restoreSystemChrome(activity: Activity?) {
		if (activity?.isInLandscapeFullscreen() == true) {
			activity.exitFullscreen()
		}
	}

	fun release() {
		if (mReleased) return
		mReleased = true
		mVideoPlayer.release()
	}

	companion object {
		private fun Activity.isInLandscapeFullscreen(): Boolean = requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
			requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

		private fun Activity.enterFullscreen() {
			window.decorView.systemUiVisibility =
				View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
				View.SYSTEM_UI_FLAG_FULLSCREEN or
				View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
				View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
				View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
			requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
		}

		private fun Activity.exitFullscreen() {
			window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
			requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
		}
	}
}
