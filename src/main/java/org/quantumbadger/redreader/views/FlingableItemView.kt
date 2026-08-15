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
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.common.General.setLayoutMatchWidthWrapHeight
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

abstract class FlingableItemView(context: Context) : SwipableItemView(context) {
    private val mFlingHintOuter: FrameLayout

    private val mFlingHintLeft: TextView
    private val mFlingHintRight: TextView

    private var mSwipeReady = false
    private var mRightFlingHintShown = false
    private var mLeftFlingHintShown = false

    private var mFlingHintAnimation: FlingHintAnimation?=null
    private var mFlingHintYPos = 0f

    private val mOffsetBeginAllowed: Int
    private val mOffsetActionPerformed: Int

    private val rrIconFfLeft: Drawable?
    private val rrIconFfRight: Drawable?
    private val rrIconTick: Drawable?

    init {
        mOffsetBeginAllowed = dpToPixels(context, 50f)
        mOffsetActionPerformed = dpToPixels(context, 150f)

        val rrListBackgroundCol: Int

        run {
            val attr = context.obtainStyledAttributes(
                intArrayOf(
                    R.attr.rrIconFfLeft,
                    R.attr.rrIconFfRight,
                    R.attr.rrIconTick,
                    R.attr.rrListBackgroundCol
                )
            )
            rrIconFfLeft = attr.getDrawable(0)
            rrIconFfRight = attr.getDrawable(1)
            rrIconTick = attr.getDrawable(2)
            rrListBackgroundCol = attr.getColor(3, General.COLOR_INVALID)
            attr.recycle()
        }

        mFlingHintOuter = LayoutInflater.from(context)
            .inflate(R.layout.fling_hint, null, false) as FrameLayout

        addView(mFlingHintOuter)
        setLayoutMatchWidthWrapHeight(mFlingHintOuter)

        mFlingHintLeft = mFlingHintOuter.findViewById<TextView>(R.id.reddit_post_fling_text_left)
        mFlingHintRight = mFlingHintOuter.findViewById<TextView>(R.id.reddit_post_fling_text_right)

        mFlingHintLeft.setCompoundDrawablesWithIntrinsicBounds(
            null,
            rrIconFfLeft,
            null,
            null
        )
        mFlingHintRight.setCompoundDrawablesWithIntrinsicBounds(
            null,
            rrIconFfRight,
            null,
            null
        )

        setBackgroundColor(rrListBackgroundCol)
    }

    fun setFlingingEnabled(flingingEnabled: Boolean) {
        mFlingHintOuter.setVisibility(if (flingingEnabled) VISIBLE else GONE)
        setSwipingEnabled(flingingEnabled)
    }

    protected abstract fun onSetItemFlingPosition(position: Float)

    protected abstract val flingLeftText: String

    protected abstract val flingRightText: String

    protected abstract fun allowFlingingLeft(): Boolean

    protected abstract fun allowFlingingRight(): Boolean

    protected abstract fun onFlungLeft()

    protected abstract fun onFlungRight()

    override fun allowSwipingLeft(): Boolean {
        return allowFlingingLeft()
    }

    override fun allowSwipingRight(): Boolean {
        return allowFlingingRight()
    }

    private fun updateFlingHintPosition() {
        mFlingHintOuter.setTranslationY(mFlingHintYPos)
    }

    private inner class FlingHintAnimation(params: LiveDHM.Params?) : RRDHMAnimation(params) {
        override fun onUpdatedPosition(position: Float) {
            mFlingHintYPos = position
            updateFlingHintPosition()
        }

        override fun onEndPosition(endPosition: Float) {
            mFlingHintYPos = endPosition
            updateFlingHintPosition()
            mFlingHintAnimation = null
        }
    }

    override fun onSwipeFingerDown(
        x: Int,
        y: Int,
        xOffsetPixels: Float,
        wasOldSwipeInterrupted: Boolean
    ) {
        if (mOffsetBeginAllowed > abs(xOffsetPixels)) {
            mFlingHintLeft.setText(this.flingLeftText)
            mFlingHintRight.setText(this.flingRightText)

            mFlingHintLeft.setCompoundDrawablesWithIntrinsicBounds(
                null,
                rrIconFfLeft,
                null,
                null
            )
            mFlingHintRight.setCompoundDrawablesWithIntrinsicBounds(
                null,
                rrIconFfRight,
                null,
                null
            )

            mSwipeReady = true
        }

        val height = mFlingHintOuter.getMeasuredHeight()
        val parentHeight = getMeasuredHeight()

        val oldAnimation = mFlingHintAnimation

        if (mFlingHintAnimation != null) {
            mFlingHintAnimation!!.stop()
            mFlingHintAnimation = null
        }

        if (parentHeight > height * 3) {
            var yPos = min(max(y - height / 2, 0), parentHeight - height)

            if (wasOldSwipeInterrupted) {
                if (abs(yPos - mFlingHintYPos) < height) {
                    yPos = mFlingHintYPos.toInt()
                }

                val params = LiveDHM.Params()
                params.startPosition = mFlingHintYPos
                params.endPosition = yPos.toFloat()

                if (oldAnimation != null) {
                    params.startVelocity = oldAnimation.currentVelocity
                }

                mFlingHintAnimation = FlingHintAnimation(params)
                mFlingHintAnimation!!.start()
            } else {
                mFlingHintYPos = yPos.toFloat()
                updateFlingHintPosition()
            }
        } else {
            mFlingHintYPos = ((parentHeight - height) / 2).toFloat()
            updateFlingHintPosition()
        }
    }

    public override fun onSwipeDeltaChanged(xOffsetPixels: Float) {
        onSetItemFlingPosition(xOffsetPixels)

        val absOffset = abs(xOffsetPixels)

        if (mSwipeReady && absOffset > mOffsetActionPerformed) {
            if (xOffsetPixels > 0) {
                onFlungRight()
                mFlingHintRight.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    rrIconTick,
                    null,
                    null
                )
            } else {
                onFlungLeft()
                mFlingHintLeft.setCompoundDrawablesWithIntrinsicBounds(
                    null,
                    rrIconTick,
                    null,
                    null
                )
            }

            mSwipeReady = false
        } else if (absOffset > 5) {
            if (xOffsetPixels > 0) {
                // Right swipe

                if (!mRightFlingHintShown) {
                    mRightFlingHintShown = true
                    mFlingHintRight.setVisibility(VISIBLE)
                }

                if (mLeftFlingHintShown) {
                    mLeftFlingHintShown = false
                    mFlingHintLeft.setVisibility(INVISIBLE)
                }
            } else {
                // Left swipe

                if (!mLeftFlingHintShown) {
                    mLeftFlingHintShown = true
                    mFlingHintLeft.setVisibility(VISIBLE)
                }

                if (mRightFlingHintShown) {
                    mRightFlingHintShown = false
                    mFlingHintRight.setVisibility(INVISIBLE)
                }
            }
        } else {
            if (mRightFlingHintShown) {
                mRightFlingHintShown = false
                mFlingHintRight.setVisibility(INVISIBLE)
            }

            if (mLeftFlingHintShown) {
                mLeftFlingHintShown = false
                mFlingHintLeft.setVisibility(INVISIBLE)
            }
        }
    }
}
