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
import android.widget.RelativeLayout
import com.github.lzyzsd.circleprogress.DonutProgress
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.common.General.dpToPixels

class ButtonLoadingSpinnerView(context: Context) : RelativeLayout(context) {
    val mProgressView: DonutProgress

    init {
        val typedArray = context.obtainStyledAttributes(
            intArrayOf(
                R.attr.rrLoadingRingForegroundCol,
                R.attr.rrLoadingRingBackgroundCol
            )
        )

        val foreground = typedArray.getColor(0, Color.MAGENTA)
        val background = typedArray.getColor(1, Color.GREEN)

        typedArray.recycle()

        mProgressView = DonutProgress(context)
        mProgressView.setAspectIndicatorDisplay(false)
        mProgressView.setIndeterminate(true)
        mProgressView.setFinishedStrokeColor(foreground)
        mProgressView.setUnfinishedStrokeColor(background)
        val progressStrokeWidthPx = dpToPixels(context, 4f)
        mProgressView.setUnfinishedStrokeWidth(progressStrokeWidthPx.toFloat())
        mProgressView.setFinishedStrokeWidth(progressStrokeWidthPx.toFloat())
        mProgressView.startingDegree = -90
        mProgressView.initPainters()

        addView(mProgressView)
        val progressDimensionsPx = dpToPixels(context, 24f)
        mProgressView.getLayoutParams().width = progressDimensionsPx
        mProgressView.getLayoutParams().height = progressDimensionsPx
        (mProgressView.getLayoutParams() as LayoutParams).addRule(CENTER_IN_PARENT)
    }
}
