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

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TimeBar
import androidx.media3.ui.TimeBar.OnScrubListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stormtroopercs.materialreader.R
import com.stormtroopercs.materialreader.R.string
import com.stormtroopercs.materialreader.common.AndroidCommon
import com.stormtroopercs.materialreader.common.General.dpToPixels
import com.stormtroopercs.materialreader.common.General.setLayoutMatchWidthWrapHeight
import com.stormtroopercs.materialreader.common.PrefsUtility
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.min

@OptIn(UnstableApi::class)
class ExoPlayerWrapperView(
	context: Context,
	mediaSource: MediaSource,
	private val mListener: Listener,
	controlsMarginRightDp: Int,
) : FrameLayout(context) {
	fun interface Listener {
		fun onError()
	}

	private val mVideoPlayer: ExoPlayer

	private val mControlView: RelativeLayout?
	private val mPlayButton: ImageButton?

	private val mTimeBarView: DefaultTimeBar?
	private val mTimeTextView: TextView?

	private val mSpeedTextView: TextView?
	private var mCurrentPlaybackSpeed = 1.0f

	private val mVideoPlayerView: PlayerView

	private var mZoomScale = 1.0f
	private var mZoomTranslationX = 0f
	private var mZoomTranslationY = 0f

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

		videoPlayerView.setPivotX(0f)
		videoPlayerView.setPivotY(0f)

		addView(videoPlayerView)

		videoPlayerView.setUseController(false)
		videoPlayerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
		videoPlayerView.setPlayer(mVideoPlayer)
		videoPlayerView.requestFocus()

		mVideoPlayer.setMediaSource(mediaSource)
		mVideoPlayer.prepare()

		mVideoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE)

		mVideoPlayer.setPlayWhenReady(true)

		if (PrefsUtility.pref_behaviour_video_zoom_default()) {
			videoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
		} else {
			videoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT)
		}

		if (PrefsUtility.pref_behaviour_video_playback_controls()) {
			mControlView = RelativeLayout(context)
			addView(mControlView)

			val controlBar = LinearLayout(context)
			mControlView.addView(controlBar)
			controlBar.setBackgroundColor(Color.argb(220, 40, 40, 40))
			controlBar.setOrientation(LinearLayout.VERTICAL)

			run {
				val controlBarLayoutParams = controlBar.getLayoutParams() as RelativeLayout.LayoutParams
				controlBarLayoutParams.width = LayoutParams.WRAP_CONTENT
				controlBarLayoutParams.height = LayoutParams.WRAP_CONTENT
				controlBarLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
				controlBarLayoutParams.rightMargin = dpToPixels(
					context,
					controlsMarginRightDp.toFloat(),
				)
			}

			val buttons = LinearLayout(context)
			controlBar.addView(buttons)
			buttons.setOrientation(LinearLayout.HORIZONTAL)
			setLayoutMatchWidthWrapHeight(buttons)

			addButton(
				createButton(
					context,
					mControlView,
					R.drawable.icon_restart,
					string.video_restart,
					OnClickListener { view: View? ->
						mVideoPlayer.seekTo(0)
						updateProgress()
					},
				),
				buttons,
			)

			addButton(
				createButton(
					context,
					mControlView,
					R.drawable.icon_rewind,
					string.video_rewind,
					OnClickListener { view: View? ->
						if (mVideoPlayer.getCurrentPosition() > 3000) {
							mVideoPlayer.seekTo(mVideoPlayer.getCurrentPosition() - 3000)
						} else {
							mVideoPlayer.seekTo(0)
						}
						updateProgress()
					},
				),
				buttons,
			)

			mPlayButton = createButton(
				context,
				mControlView,
				R.drawable.icon_pause,
				string.video_pause,
				OnClickListener { view: View? ->
					mVideoPlayer.setPlayWhenReady(!mVideoPlayer.getPlayWhenReady())
					updateProgress()
				},
			)

			if (PrefsUtility.pref_behaviour_video_frame_step()) {
				val frameDuration = (
					1000f / (
						if (mVideoPlayer.getVideoFormat() != null) {
							mVideoPlayer.getVideoFormat()!!.frameRate
						} else {
							30f
						}
						)
					).toLong()

				val stepBackButton: ImageButton = createButton(
					context,
					mControlView,
					R.drawable.icon_step_back,
					string.video_step_back,
					OnClickListener { view: View? ->
						mVideoPlayer.seekTo(mVideoPlayer.getCurrentPosition() - frameDuration)
						updateProgress()
					},
				)

				val stepForwardButton: ImageButton = createButton(
					context,
					mControlView,
					R.drawable.icon_step_forward,
					string.video_step_forward,
					OnClickListener { view: View? ->
						mVideoPlayer.seekTo(mVideoPlayer.getCurrentPosition() + frameDuration)
						updateProgress()
					},
				)

				mVideoPlayer.addListener(object : Player.Listener {
					override fun onIsPlayingChanged(isPlaying: Boolean) {
						if (isPlaying) {
							stepBackButton.setImageAlpha(0x3F)
							stepBackButton.setContentDescription(
								context.getString(string.video_step_back_disabled),
							)
							stepBackButton.setEnabled(false)

							stepForwardButton.setImageAlpha(0x3F)
							stepForwardButton.setContentDescription(
								context.getString(string.video_step_forward_disabled),
							)
							stepForwardButton.setEnabled(false)
						} else {
							stepBackButton.setImageAlpha(0xFF)
							stepBackButton.setContentDescription(
								context.getString(string.video_step_back),
							)
							stepBackButton.setEnabled(true)

							stepForwardButton.setImageAlpha(0xFF)
							stepForwardButton.setContentDescription(
								context.getString(string.video_step_forward),
							)
							stepForwardButton.setEnabled(true)
						}
					}
				})

				addButton(stepBackButton, buttons)
				addButton(mPlayButton, buttons)
				addButton(stepForwardButton, buttons)
			} else {
				addButton(mPlayButton, buttons)
			}

			addButton(
				createButton(
					context,
					mControlView,
					R.drawable.icon_fastforward,
					string.video_fast_forward,
					OnClickListener { view: View? ->
						mVideoPlayer.seekTo(mVideoPlayer.getCurrentPosition() + 3000)
						updateProgress()
					},
				),
				buttons,
			)

			run {
				val zoomButton = AtomicReference<ImageButton?>()
				zoomButton.set(
					Companion.createButton(
						context,
						mControlView!!,
						R.drawable.ic_zoom_in_dark,
						string.video_zoom_in,
						OnClickListener { v: View? ->
							resetZoom()
							if (videoPlayerView.getResizeMode()
								== AspectRatioFrameLayout.RESIZE_MODE_FIT
							) {
								videoPlayerView.setResizeMode(
									AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
								)
								zoomButton.get()!!.setImageResource(R.drawable.ic_zoom_out_dark)
								zoomButton.get()!!.setContentDescription(
									context.getString(string.video_zoom_out),
								)
							} else {
								videoPlayerView.setResizeMode(
									AspectRatioFrameLayout.RESIZE_MODE_FIT,
								)
								zoomButton.get()!!.setImageResource(R.drawable.ic_zoom_in_dark)
								zoomButton.get()!!.setContentDescription(
									context.getString(string.video_zoom_in),
								)
							}
						},
					),
				)

				if (videoPlayerView.getResizeMode() == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) {
					zoomButton.get()!!.setImageResource(R.drawable.ic_zoom_out_dark)
					zoomButton.get()!!.setContentDescription(
						context.getString(string.video_zoom_out),
					)
				}
				Companion.addButton(zoomButton.get()!!, buttons)
			}

			addButton(
				createButton(
					context,
					mControlView,
					R.drawable.ic_time_dark,
					string.video_speed_setting,
					OnClickListener { view: View? ->
						openSpeedSettingDialog(context)
					},
				),
				buttons,
			)

			mTimeBarView = DefaultTimeBar(context, null)
			controlBar.addView(mTimeBarView)

			run {
				val seekBarLayoutParams = mTimeBarView!!.getLayoutParams() as LinearLayout.LayoutParams
				val marginPx = dpToPixels(context, 8f)
				seekBarLayoutParams.setMargins(marginPx, marginPx, marginPx, marginPx)
			}

			mTimeBarView.addListener(object : OnScrubListener {
				override fun onScrubStart(timeBar: TimeBar, position: Long) {
				}

				override fun onScrubMove(timeBar: TimeBar, position: Long) {
					mVideoPlayer.seekTo(position)
				}

				override fun onScrubStop(
					timeBar: TimeBar,
					position: Long,
					canceled: Boolean,
				) {
					mVideoPlayer.seekTo(position)
				}
			})

			val updateProgressRunnable: Runnable = object : Runnable {
				override fun run() {
					updateProgress()

					if (!mReleased) {
						AndroidCommon.UI_THREAD_HANDLER.postDelayed(this, 150)
					}
				}
			}

			updateProgressRunnable.run()

			val timeAndSpeedLayout = LinearLayout(context)
			timeAndSpeedLayout.setOrientation(LinearLayout.HORIZONTAL)
			controlBar.addView(timeAndSpeedLayout)

			mTimeTextView = TextView(context)
			timeAndSpeedLayout.addView(mTimeTextView)
			mTimeTextView.setTextColor(Color.WHITE)
			mTimeTextView.setTextSize(18f)

			mSpeedTextView = TextView(context)
			timeAndSpeedLayout.addView(mSpeedTextView)
			mSpeedTextView.setTextColor(Color.WHITE)
			// Initially empty
			mSpeedTextView.setText("")

			val marginSidesPx = dpToPixels(context, 16f)
			val marginBottomPx = dpToPixels(context, 8f)

			(mTimeTextView.getLayoutParams() as MarginLayoutParams)
				.setMargins(marginSidesPx, 0, marginSidesPx, marginBottomPx)
			mTimeTextView.setImportantForAccessibility(
				IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
			)

			mControlView.setVisibility(GONE)
		} else {
			mControlView = null
			mTimeBarView = null
			mTimeTextView = null
			mSpeedTextView = null
			mPlayButton = null
		}

		videoPlayerView.setLayoutParams(
			LayoutParams(
				LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT,
			),
		)

		mVideoPlayer.addListener(object : Player.Listener {
			override fun onPlayerError(error: PlaybackException) {
				Log.e(TAG, "ExoPlayer error", error)
				mListener.onError()
			}

			override fun onPositionDiscontinuity(
				oldPosition: Player.PositionInfo,
				newPosition: Player.PositionInfo,
				reason: Int,
			) {
				updateProgress()
			}

			override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
				if (mPlayButton == null) {
					return
				}

				if (playWhenReady) {
					mPlayButton.setImageResource(R.drawable.icon_pause)
					mPlayButton.setContentDescription(context.getString(string.video_pause))
				} else {
					mPlayButton.setImageResource(R.drawable.icon_play)
					mPlayButton.setContentDescription(context.getString(string.video_play))
				}
			}
		})
	}

	fun handleTap() {
		if (mControlView == null) {
			return
		}

		if (mControlView.getVisibility() != VISIBLE) {
			mControlView.setVisibility(VISIBLE)
		} else {
			mControlView.setVisibility(GONE)
		}
	}

	val isZoomedIn: Boolean
		get() = mZoomScale > 1.001f

	fun scaleBy(factor: Float, focalX: Float, focalY: Float) {
		val newScale = max(
			1.0f,
			min(MAX_ZOOM_SCALE, mZoomScale * factor),
		)

		val appliedFactor = newScale / mZoomScale

		// Adjust the translation so that the content under the focal point
		// stays under the focal point as the scale changes
		mZoomTranslationX = focalX - (focalX - mZoomTranslationX) * appliedFactor
		mZoomTranslationY = focalY - (focalY - mZoomTranslationY) * appliedFactor

		mZoomScale = newScale

		applyZoomTransform()
	}

	fun panBy(dx: Float, dy: Float) {
		mZoomTranslationX += dx
		mZoomTranslationY += dy
		applyZoomTransform()
	}

	fun resetZoom() {
		mZoomScale = 1.0f
		mZoomTranslationX = 0f
		mZoomTranslationY = 0f
		applyZoomTransform()
	}

	private fun applyZoomTransform() {
		// Clamp the translation so that the scaled view always covers the
		// viewport (the scale pivot is the top left corner)

		val minTranslationX = getWidth() * (1.0f - mZoomScale)
		val minTranslationY = getHeight() * (1.0f - mZoomScale)

		mZoomTranslationX = max(minTranslationX, min(0f, mZoomTranslationX))
		mZoomTranslationY = max(minTranslationY, min(0f, mZoomTranslationY))

		mVideoPlayerView.setScaleX(mZoomScale)
		mVideoPlayerView.setScaleY(mZoomScale)
		mVideoPlayerView.setTranslationX(mZoomTranslationX)
		mVideoPlayerView.setTranslationY(mZoomTranslationY)
	}

	fun release() {
		if (!mReleased) {
			removeAllViews()
			mVideoPlayer.release()
			mReleased = true
		}
	}

	fun updateProgress() {
		if (mTimeBarView != null && mTimeTextView != null && !mReleased) {
			val durationMs = mVideoPlayer.getDuration()

			if (durationMs > 0) {
				val currentPositionMs = mVideoPlayer.getCurrentPosition()

				mTimeBarView.setDuration(durationMs)
				mTimeBarView.setPosition(currentPositionMs)
				mTimeBarView.setBufferedPosition(mVideoPlayer.getBufferedPosition())

				val newText: String = (
					msToMinutesAndSecondsString(currentPositionMs) +
						" / " +
						msToMinutesAndSecondsString(durationMs)
					)

				if (!newText.contentEquals(mTimeTextView.getText())) {
					mTimeTextView.setText(newText)
				}
			} else {
				mTimeBarView.setDuration(0)
				mTimeBarView.setPosition(0)
				mTimeBarView.setBufferedPosition(0)
			}
		}
	}

	var isMuted: Boolean
		get() = mVideoPlayer.getVolume() < 0.01f
		set(mute) {
			mVideoPlayer.setVolume((if (mute) 0 else 1).toFloat())
		}

	val isControlViewVisible: Int
		get() = if (mControlView != null) mControlView.getVisibility() else GONE

	private fun openSpeedSettingDialog(context: Context) {
		// Pause video playback before opening the dialog
		mVideoPlayer.setPlayWhenReady(false)

		val builder = MaterialAlertDialogBuilder(context)
		builder.setTitle(string.video_speed_instruction)

		val speedSettingsLayout = LinearLayout(context)
		speedSettingsLayout.setOrientation(LinearLayout.VERTICAL)
		speedSettingsLayout.setPadding(20, 20, 20, 20)

		val speedValue = TextView(context)
		speedValue.setText(
			String.format(
				Locale.US,
				getResources().getString(string.video_speed_term) + ": %.2fx",
				mCurrentPlaybackSpeed,
			),
		)
		speedValue.setTextSize(18f)
		speedValue.setPadding(10, 10, 10, 10)

		val speedSeekBar = SeekBar(context)
		speedSeekBar.setMax(300 - 1) // 3.00 is max for now

		val seekBarParams = LinearLayout.LayoutParams(
			LinearLayout.LayoutParams.MATCH_PARENT,
			LinearLayout.LayoutParams.WRAP_CONTENT,
		)
		seekBarParams.height = 150
		speedSeekBar.setLayoutParams(seekBarParams)

		speedSeekBar.setProgress(((mCurrentPlaybackSpeed - 0.01) * 100).toInt())

		// Listener for changes in the SeekBar's position
		speedSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
			override fun onProgressChanged(
				seekBar: SeekBar?,
				progress: Int,
				fromUser: Boolean,
			) {
				val selectedSpeed = 0.01f * (progress + 1)
				speedValue.setText(
					String.format(
						Locale.US,
						getResources().getString(string.video_speed_term) + ": %.2fx",
						selectedSpeed,
					),
				)
			}

			override fun onStartTrackingTouch(seekBar: SeekBar?) {}

			override fun onStopTrackingTouch(seekBar: SeekBar?) {}
		})

		speedSettingsLayout.addView(speedValue)
		speedSettingsLayout.addView(speedSeekBar)

		// Creating rows for the speed preset buttons
		val rows = arrayOfNulls<LinearLayout>(4)
		for (i in rows.indices) {
			rows[i] = LinearLayout(context)
			rows[i]!!.setOrientation(LinearLayout.HORIZONTAL)
			speedSettingsLayout.addView(rows[i])
		}

		// Labels and values for the preset speed buttons
		val speedLabels = arrayOf<String?>("0.5", "1.0", "1.5", "2.0")
		val speedProgressValues = intArrayOf(49, 99, 149, 199)

		// Adding preset speed buttons to the layout
		for (i in speedLabels.indices) {
			addPresetSpeedButton(
				rows[i / 4]!!,
				speedLabels[i],
				speedProgressValues[i],
				speedSeekBar,
				speedValue,
				context,
			)
		}

		builder.setView(speedSettingsLayout)

		builder.setPositiveButton(
			string.dialog_go,
			DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
				mCurrentPlaybackSpeed = 0.01f * (speedSeekBar.getProgress() + 1)
				mVideoPlayer.setPlaybackSpeed(mCurrentPlaybackSpeed)
				mSpeedTextView!!.setText(String.format(Locale.US, "(%.2fx)", mCurrentPlaybackSpeed))
				// Resume video playback when dialog is dismissed
				mVideoPlayer.setPlayWhenReady(true)
			},
		)

		builder.setNegativeButton(
			string.dialog_cancel,
			DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
				// Resume video playback when dialog is dismissed
				mVideoPlayer.setPlayWhenReady(true)
			},
		)

		builder.setOnCancelListener(
			DialogInterface.OnCancelListener { dialog: DialogInterface? ->
				// Resume video playback when dialog is canceled
				mVideoPlayer.setPlayWhenReady(true)
			},
		)

		builder.show()
	}

	// Method to add a preset speed button to the layout
	private fun addPresetSpeedButton(
		rowLayout: LinearLayout,
		label: String?,
		progress: Int,
		seekBar: SeekBar,
		speedValue: TextView,
		context: Context?,
	) {
		val speedButton = Button(context)
		speedButton.setText(label)
		// Setting button click behavior to update the SeekBar and speedValue TextView
		speedButton.setOnClickListener(
			OnClickListener { v: View? ->
				seekBar.setProgress(progress)
				val selectedSpeed = 0.01f * (progress + 1)
				speedValue.setText(
					String.format(
						Locale.US,
						getResources().getString(string.video_speed_term) + ": %.2fx",
						selectedSpeed,
					),
				)
			},
		)

		val params = LinearLayout.LayoutParams(
			0,
			LayoutParams.WRAP_CONTENT,
			1.0f,
		)
		rowLayout.addView(speedButton, params)
	}

	companion object {
		private const val TAG = "ExoPlayerWrapperView"

		private const val MAX_ZOOM_SCALE = 8.0f

		private fun createButton(
			context: Context,
			root: ViewGroup,
			@DrawableRes image: Int,
			@StringRes description: Int,
			clickListener: OnClickListener,
		): ImageButton {
			val ib = LayoutInflater.from(context).inflate(
				R.layout.flat_image_button,
				root,
				false,
			) as ImageButton

			val buttonPadding = dpToPixels(context, 14f)
			ib.setPadding(buttonPadding, buttonPadding, buttonPadding, buttonPadding)

			ib.setImageResource(image)
			ib.setContentDescription(context.getString(description))

			ib.setOnClickListener(clickListener)

			return ib
		}

		private fun addButton(button: ImageButton, layout: LinearLayout) {
			layout.addView(button)

			val layoutParams = button.getLayoutParams() as LinearLayout.LayoutParams

			layoutParams.width = LayoutParams.WRAP_CONTENT
			layoutParams.height = LayoutParams.WRAP_CONTENT
		}

		fun msToMinutesAndSecondsString(ms: Long): String {
			if (ms < 0) {
				return "<negative time>"
			}

			val secondsTotal = (ms / 1000).toInt()

			val mins = secondsTotal / 60
			val secs = secondsTotal % 60

			return String.format(Locale.US, "%d:%02d", mins, secs)
		}
	}
}
