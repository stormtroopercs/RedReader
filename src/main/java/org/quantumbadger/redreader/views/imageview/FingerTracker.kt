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

import android.view.MotionEvent
import org.quantumbadger.redreader.BuildConfig
import org.quantumbadger.redreader.common.MutableFloatPoint2D

class FingerTracker(private val mListener: FingerListener) {
    interface FingerListener {
        fun onFingerDown(finger: Finger)

        fun onFingersMoved()

        fun onFingerUp(finger: Finger)
    }

    private val mFingers: Array<Finger> = Array(10) { Finger() }

    fun onTouchEvent(event: MotionEvent) {
        val action = event.getActionMasked()
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                // ACTION_DOWN starts the gesture, and all fingers must be up at this point
                if (action == MotionEvent.ACTION_DOWN) {
                    assertThatAllFingersAreInactive("before ACTION_DOWN")
                }

                for (f in mFingers) {
                    if (!f.mActive) {
                        f.onDown(event)
                        mListener.onFingerDown(f)
                        break
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                for (finger in mFingers) {
                    if (finger.mActive) {
                        finger.onMove(event)
                    }
                }

                mListener.onFingersMoved()
            }

            MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> {
                val id = event.getPointerId(event.getActionIndex())

                for (f in mFingers) {
                    if (f.mActive && f.mAndroidId == id) {
                        f.onUp(event)
                        mListener.onFingerUp(f)
                        break
                    }
                }
                // ACTION_UP ends the gesture, and all fingers must be up at this point
                if (action == MotionEvent.ACTION_UP) {
                    assertThatAllFingersAreInactive("after ACTION_UP")
                }
            }

            MotionEvent.ACTION_CANCEL ->                // ACTION_CANCEL ends the gesture, process all fingers
                for (f in mFingers) {
                    if (f.mActive) {
                        f.onUp(event)
                        mListener.onFingerUp(f)
                    }
                }

        }
    }

    private fun assertThatAllFingersAreInactive(`when`: String?) {
        if (BuildConfig.DEBUG) {
            for (f in mFingers) {
                check(!f.mActive) { "Finger for pointer id " + f.mAndroidId + " is active " + `when` }
            }
        }
    }

    class Finger {
        var mActive: Boolean = false

        var mAndroidId: Int = 0

        val mStartPos: MutableFloatPoint2D = MutableFloatPoint2D()
        val mCurrentPos: MutableFloatPoint2D = MutableFloatPoint2D()
        val mLastPos: MutableFloatPoint2D = MutableFloatPoint2D()
        val mPosDifference: MutableFloatPoint2D = MutableFloatPoint2D()
        val mTotalPosDifference: MutableFloatPoint2D = MutableFloatPoint2D()

        var mDownStartTime: Long = 0
        var mDownDuration: Long = 0

        fun onDown(event: MotionEvent) {
            val index = event.getActionIndex()
            mActive = true
            mAndroidId = event.getPointerId(index)
            mCurrentPos.set(event, index)
            mLastPos.set(mCurrentPos)
            mStartPos.set(mCurrentPos)
            mPosDifference.reset()
            mTotalPosDifference.reset()
            mDownStartTime = event.getDownTime()
            mDownDuration = 0
        }

        fun onMove(event: MotionEvent) {
            val index = event.findPointerIndex(mAndroidId)
            if (index >= 0) {
                mLastPos.set(mCurrentPos)
                mCurrentPos.set(event, index)
                mCurrentPos.sub(mLastPos, mPosDifference)
                mCurrentPos.sub(mStartPos, mTotalPosDifference)
                mDownDuration = event.getEventTime() - mDownStartTime
            }
        }

        fun onUp(event: MotionEvent) {
            mLastPos.set(mCurrentPos)
            mCurrentPos.set(event, event.getActionIndex())
            mCurrentPos.sub(mLastPos, mPosDifference)
            mCurrentPos.sub(mStartPos, mTotalPosDifference)
            mDownDuration = event.getEventTime() - mDownStartTime

            mActive = false
        }
    }
}
