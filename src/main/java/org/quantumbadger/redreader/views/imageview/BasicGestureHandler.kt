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
package org.quantumbadger.redreader.views.imageview

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import org.quantumbadger.redreader.views.imageview.FingerTracker.Finger
import org.quantumbadger.redreader.views.imageview.FingerTracker.FingerListener

class BasicGestureHandler
    (private val mListener: Listener) : OnTouchListener, FingerListener {
    interface Listener {
        fun onSingleTap()

        fun onHorizontalSwipe(pixels: Float)

        fun onHorizontalSwipeEnd()
    }

    private val mFingerTracker = FingerTracker(this)

    private var mFirstFinger: Finger?=null
    private var mCurrentFingerCount = 0

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        mFingerTracker.onTouchEvent(event)
        return true
    }

    override fun onFingerDown(finger: Finger) {
        mCurrentFingerCount++

        if (mCurrentFingerCount > 1) {
            mFirstFinger = null
        } else {
            mFirstFinger = finger
        }
    }

    override fun onFingersMoved() {
        if (mFirstFinger != null) {
            mListener.onHorizontalSwipe(mFirstFinger!!.mTotalPosDifference.x)
        }
    }

    override fun onFingerUp(finger: Finger) {
        mCurrentFingerCount--

        if (mFirstFinger != null) {
            mListener.onHorizontalSwipeEnd()

            // TODO
            if (mFirstFinger!!.mDownDuration < 300 && mFirstFinger!!.mPosDifference.x < 20 && mFirstFinger!!.mPosDifference.y < 20) {
                mListener.onSingleTap()
            }

            mFirstFinger = null
        }
    }
}
