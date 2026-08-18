/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package org.quantumbadger.redreader.views.video

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import org.quantumbadger.redreader.common.MutableFloatPoint2D
import org.quantumbadger.redreader.common.collections.Stack
import org.quantumbadger.redreader.views.imageview.BasicGestureHandler
import org.quantumbadger.redreader.views.imageview.FingerTracker
import org.quantumbadger.redreader.views.imageview.FingerTracker.Finger
import org.quantumbadger.redreader.views.imageview.FingerTracker.FingerListener

/**
 * Touch handler for the video player. Behaves like
 * [BasicGestureHandler] for taps and single-finger horizontal swipes,
 * but additionally supports pinch-to-zoom and (while zoomed in)
 * single-finger panning, applied to an [ExoPlayerWrapperView].
 */
class VideoGestureHandler
    (
    private val mListener: BasicGestureHandler.Listener,
    private val mPlayerView: ExoPlayerWrapperView
) : OnTouchListener, FingerListener {
    private enum class TouchState {
        ONE_FINGER_DOWN,
        ONE_FINGER_DRAG,
        TWO_FINGER_PINCH
    }

    private val mFingerTracker = FingerTracker(this)

    private val mScreenDensity: Float

    private var mCurrentTouchState: TouchState?=null

    private var mDragFinger: Finger?=null
    private var mPinchFinger1: Finger?=null
    private var mPinchFinger2: Finger?=null
    private val mSpareFingers = Stack<Finger?>(8)

    private val mTmpPoint1 = MutableFloatPoint2D()
    private val mTmpPoint2 = MutableFloatPoint2D()

    init {
        mScreenDensity = mPlayerView.getResources().getDisplayMetrics().density
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        mFingerTracker.onTouchEvent(event)
        return true
    }

    override fun onFingerDown(finger: Finger) {
        if (mCurrentTouchState == null) {
            mCurrentTouchState = TouchState.ONE_FINGER_DOWN
            mDragFinger = finger
        } else {
            when (mCurrentTouchState) {
                TouchState.ONE_FINGER_DRAG -> {
                    mListener.onHorizontalSwipeEnd()

                    mCurrentTouchState = TouchState.TWO_FINGER_PINCH
                    mPinchFinger1 = mDragFinger
                    mPinchFinger2 = finger
                    mDragFinger = null
                }

                TouchState.ONE_FINGER_DOWN -> {
                    mCurrentTouchState = TouchState.TWO_FINGER_PINCH
                    mPinchFinger1 = mDragFinger
                    mPinchFinger2 = finger
                    mDragFinger = null
                }

                else -> mSpareFingers.push(finger)
            }
        }
    }

    override fun onFingersMoved() {
        if (mCurrentTouchState == null) {
            return
        }

        when (mCurrentTouchState) {
            TouchState.ONE_FINGER_DOWN -> {
                run {
                    if (mDragFinger!!.mTotalPosDifference.distanceSquared()
                        >= 100f * mScreenDensity * mScreenDensity
                    ) {
                        mCurrentTouchState = TouchState.ONE_FINGER_DRAG
                    }
                }
                if (mPlayerView.isZoomedIn) {
                    mPlayerView.panBy(
                        mDragFinger!!.mPosDifference.x,
                        mDragFinger!!.mPosDifference.y
                    )
                } else {
                    mListener.onHorizontalSwipe(mDragFinger!!.mTotalPosDifference.x)
                }
            }

            TouchState.ONE_FINGER_DRAG -> if (mPlayerView.isZoomedIn) {
                mPlayerView.panBy(
                    mDragFinger!!.mPosDifference.x,
                    mDragFinger!!.mPosDifference.y
                )
            } else {
                mListener.onHorizontalSwipe(mDragFinger!!.mTotalPosDifference.x)
            }

            TouchState.TWO_FINGER_PINCH -> {
                val oldDistance =                     mPinchFinger1!!.mLastPos.euclideanDistanceTo(mPinchFinger2!!.mLastPos)
                val newDistance =                     mPinchFinger1!!.mCurrentPos.euclideanDistanceTo(
                        mPinchFinger2!!.mCurrentPos
                    )

                val oldCentre = mTmpPoint1
                mPinchFinger1!!.mLastPos.add(mPinchFinger2!!.mLastPos, oldCentre)
                oldCentre.scale(0.5)

                val newCentre = mTmpPoint2
                mPinchFinger1!!.mCurrentPos.add(mPinchFinger2!!.mCurrentPos, newCentre)
                newCentre.scale(0.5)

                if (oldDistance > 0) {
                    mPlayerView.scaleBy(
                        (newDistance / oldDistance).toFloat(),
                        newCentre.x,
                        newCentre.y
                    )
                }

                mPlayerView.panBy(
                    newCentre.x - oldCentre.x,
                    newCentre.y - oldCentre.y
                )
            }
        }
    }

    override fun onFingerUp(finger: Finger) {
        if (mSpareFingers.remove(finger)) {
            return
        }

        if (mCurrentTouchState == null) {
            return
        }

        when (mCurrentTouchState) {
            TouchState.ONE_FINGER_DOWN -> {
                mListener.onHorizontalSwipeEnd()

                if (finger.mDownDuration < TAP_MAX_DURATION_MS) {
                    mListener.onSingleTap()
                }

                mCurrentTouchState = null
                mDragFinger = null
            }

            TouchState.ONE_FINGER_DRAG -> {
                mListener.onHorizontalSwipeEnd()

                if (mSpareFingers.isEmpty) {
                    mCurrentTouchState = null
                    mDragFinger = null
                } else {
                    mDragFinger = mSpareFingers.pop()
                }
            }

            TouchState.TWO_FINGER_PINCH -> if (mSpareFingers.isEmpty) {
                mCurrentTouchState = TouchState.ONE_FINGER_DRAG
                mDragFinger =                     if (mPinchFinger1 === finger) mPinchFinger2 else mPinchFinger1
                mPinchFinger1 = null
                mPinchFinger2 = null
            } else {
                if (mPinchFinger1 === finger) {
                    mPinchFinger1 = mSpareFingers.pop()
                } else {
                    mPinchFinger2 = mSpareFingers.pop()
                }
            }
        }
    }

    companion object {
        private const val TAP_MAX_DURATION_MS: Long = 300
    }
}
