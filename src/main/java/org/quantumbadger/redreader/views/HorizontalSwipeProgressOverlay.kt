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
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import com.github.lzyzsd.circleprogress.DonutProgress
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.common.General.dpToPixels
import kotlin.math.abs
import org.quantumbadger.redreader.common.General

class HorizontalSwipeProgressOverlay(context: Context) : RelativeLayout(context) {
    private val mIcon: ImageView
    private val mProgress: DonutProgress
    private var mCurrentIconResource: Int

    init {
        val background = View(context)
        val backgroundDimensionsPx = dpToPixels(context, 200f)
        background.setBackgroundColor(Color.argb(127, 0, 0, 0))
        addView(background)
        background.getLayoutParams().width = backgroundDimensionsPx
        background.getLayoutParams().height = backgroundDimensionsPx
        (background.getLayoutParams() as LayoutParams).addRule(CENTER_IN_PARENT)

        mIcon = ImageView(context)
        mIcon.setImageResource(R.drawable.ic_action_forward_dark)
        mCurrentIconResource = R.drawable.ic_action_forward_dark
        addView(mIcon)
        (mIcon.getLayoutParams() as LayoutParams).addRule(CENTER_IN_PARENT)

        mProgress = DonutProgress(context)

        addView(mProgress)
        (mProgress.getLayoutParams() as LayoutParams).addRule(CENTER_IN_PARENT)
        val progressDimensionsPx = dpToPixels(context, 150f)
        mProgress.getLayoutParams().width = progressDimensionsPx
        mProgress.getLayoutParams().height = progressDimensionsPx

        mProgress.setAspectIndicatorDisplay(false)
        mProgress.setFinishedStrokeColor(Color.RED)
        mProgress.setUnfinishedStrokeColor(Color.argb(127, 0, 0, 0))
        val progressStrokeWidthPx = dpToPixels(context, 15f)
        mProgress.setUnfinishedStrokeWidth(progressStrokeWidthPx.toFloat())
        mProgress.setFinishedStrokeWidth(progressStrokeWidthPx.toFloat())
        mProgress.startingDegree = -90
        mProgress.initPainters()

        setVisibility(GONE)
    }

    private fun setIconResource(resource: Int) {
        if (resource != mCurrentIconResource) {
            mCurrentIconResource = resource
            mIcon.setImageResource(resource)
        }
    }

    fun onSwipeUpdate(px: Float, maxPx: Float) {
        mProgress.progress = -(px / maxPx)

        if (abs(px) > 20) {
            setVisibility(VISIBLE)
        }

        if (px < 0) {
            setIconResource(R.drawable.ic_action_forward_dark)
        } else {
            setIconResource(R.drawable.ic_action_back_dark)
        }
    }

    fun onSwipeEnd() {
        setVisibility(GONE)
    }
}
