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
package org.quantumbadger.redreader.views

import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout
import org.quantumbadger.redreader.common.General.dpToPixels
import kotlin.math.abs
import org.quantumbadger.redreader.common.General

abstract class SwipableItemView(context: Context) : FrameLayout(context) {
    private var mSwipeStart: MotionEvent?=null
    private var mSwipeStartPointerId = -1

    private var mSwipingEnabled = true

    private var mSwipeInProgress = false

    private var mCurrentSwipeDelta = 0f
    private var mOverallSwipeDelta = 0f

    private val mSwipeHistory = SwipeHistory(30)
    private var mVelocity = 0f

    private var mCurrentSwipeAnimation: SwipeAnimation?=null

    protected abstract fun onSwipeFingerDown(
        x: Int,
        y: Int,
        xOffsetPixels: Float,
        wasOldSwipeInterrupted: Boolean
    )

    protected abstract fun onSwipeDeltaChanged(dx: Float)

    protected abstract fun allowSwipingLeft(): Boolean

    protected abstract fun allowSwipingRight(): Boolean

    fun setSwipingEnabled(swipingEnabled: Boolean) {
        mSwipingEnabled = swipingEnabled
    }

    protected fun resetSwipeState() {
        mSwipeHistory.clear()
        mSwipeStart = null
        mSwipeStartPointerId = -1
        mSwipeInProgress = false
        mCurrentSwipeDelta = 0f
        mOverallSwipeDelta = 0f
        cancelSwipeAnimation()

        updateOffset()
    }

    private fun updateOffset() {
        val overallPos = mOverallSwipeDelta + mCurrentSwipeDelta

        if ((overallPos > 0 && !allowSwipingRight()) || (overallPos < 0
                    && !allowSwipingLeft())
        ) {
            mOverallSwipeDelta = -mCurrentSwipeDelta
        }

        onSwipeDeltaChanged(mOverallSwipeDelta + mCurrentSwipeDelta)
    }

    private fun onFingerDown(x: Int, y: Int) {
        val wasOldSwipeInterrupted = (mCurrentSwipeAnimation != null) || (mOverallSwipeDelta
                != 0f)

        cancelSwipeAnimation()
        mSwipeHistory.clear()
        mVelocity = 0f
        mOverallSwipeDelta += mCurrentSwipeDelta
        mCurrentSwipeDelta = 0f
        onSwipeFingerDown(x, y, mOverallSwipeDelta, wasOldSwipeInterrupted)
    }

    private fun onFingerSwipeMove() {
        mSwipeHistory.add(mCurrentSwipeDelta, System.currentTimeMillis())
        updateOffset()
    }

    private fun onSwipeEnd() {
        if (mSwipeHistory.size() >= 2) {
            mVelocity = (mSwipeHistory.mostRecent
                    - mSwipeHistory.getAtTimeAgoMs(100)) * 10
        } else {
            mVelocity = 0f
        }

        mOverallSwipeDelta += mCurrentSwipeDelta
        mCurrentSwipeDelta = 0f

        animateSwipeToRestPosition()
    }

    private fun onSwipeCancelled() {
        mVelocity = 0f

        mOverallSwipeDelta += mCurrentSwipeDelta
        mCurrentSwipeDelta = 0f

        animateSwipeToRestPosition()
    }

    private fun animateSwipeToRestPosition() {
        val params = LiveDHM.Params() // TODO account for screen dpi!
        params.startPosition = mOverallSwipeDelta
        params.startVelocity = mVelocity
        startSwipeAnimation(SwipeAnimation(params))
    }

    private fun startSwipeAnimation(animation: SwipeAnimation?) {
        if (mCurrentSwipeAnimation != null) {
            mCurrentSwipeAnimation!!.stop()
        }

        mCurrentSwipeAnimation = animation
        mCurrentSwipeAnimation!!.start()
    }

    private fun cancelSwipeAnimation() {
        if (mCurrentSwipeAnimation != null) {
            mCurrentSwipeAnimation!!.stop()
            mCurrentSwipeAnimation = null
        }
    }

    private inner class SwipeAnimation(params: LiveDHM.Params) : RRDHMAnimation(params) {
        override fun onUpdatedPosition(position: Float) {
            mOverallSwipeDelta = position
            updateOffset()
        }

        override fun onEndPosition(endPosition: Float) {
            mOverallSwipeDelta = endPosition
            updateOffset()
            mCurrentSwipeAnimation = null
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (mSwipeInProgress) {
            return true
        }

        if (swipeStartLogic(ev)) {
            return true
        }

        return super.onInterceptTouchEvent(ev)
    }

    private fun swipeStartLogic(ev: MotionEvent): Boolean {
        if (mSwipeInProgress) {
            throw RuntimeException()
        }

        if (!mSwipingEnabled) {
            return false
        }

        val action = ev.getAction() and MotionEvent.ACTION_MASK
        val pointerId = ev.getPointerId(ev.getActionIndex())

        if (action == MotionEvent.ACTION_DOWN
            || action == MotionEvent.ACTION_POINTER_DOWN
        ) {
            if (mSwipeStart != null) {
                // We can receive duplicate DOWN events because we're visited in both
                // the onInterceptTouchEvent AND onTouchEvent methods
                return false
            }

            mSwipeStart = MotionEvent.obtain(ev)
            mSwipeStartPointerId = pointerId
            onFingerDown(ev.getX().toInt(), ev.getY().toInt())
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (mSwipeStart == null) {
                return false
            }

            if (pointerId != mSwipeStartPointerId) {
                return false
            }

            val xDelta = ev.getX() - mSwipeStart!!.getX()
            val yDelta = ev.getY() - mSwipeStart!!.getY()

            val minXDelta = dpToPixels(getContext(), 20f)
            val maxYDelta = dpToPixels(getContext(), 10f)

            if (abs(xDelta) >= minXDelta && abs(yDelta) <= maxYDelta) {
                mSwipeInProgress = true
                mCurrentSwipeDelta = 0f
                requestDisallowInterceptTouchEvent(true)
                cancelLongPress()
                return true
            }
        } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_OUTSIDE) {
            if (pointerId != mSwipeStartPointerId) {
                return false
            }

            mSwipeStart = null

            onSwipeCancelled()
        }

        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (!mSwipeInProgress) {
            if (swipeStartLogic(ev)) {
                return true
            }

            return super.onTouchEvent(ev)
        }

        if (mSwipeStart == null) {
            throw RuntimeException()
        }

        val action = ev.getAction() and MotionEvent.ACTION_MASK
        val pointerId = ev.getPointerId(ev.getActionIndex())

        if (pointerId != mSwipeStartPointerId) {
            return false
        }

        if (action == MotionEvent.ACTION_MOVE) {
            mCurrentSwipeDelta = ev.getX() - mSwipeStart!!.getX()
            onFingerSwipeMove()
        } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_OUTSIDE) {
            mSwipeStart = null
            mSwipeInProgress = false
            requestDisallowInterceptTouchEvent(false)

            onSwipeEnd()
        }

        return true
    }
}