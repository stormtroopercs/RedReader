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
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.common.PrefsUtility

/**
 * Draws the left margin for comments based on the RedditPreparedComment#indentation number
 */
internal class IndentView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val mPaint = Paint()
    private var mIndent = 0

    private val mPixelsPerIndent: Int
    private val mHalfALine: Int

    private val mPrefDrawLines: Boolean

    private var mLineBuffer: FloatArray

    init {
        mPixelsPerIndent = dpToPixels(context, 10.0f)
        val mPixelsPerLine = dpToPixels(context, 2f)
        mHalfALine = mPixelsPerLine / 2

        val rrIndentBackgroundCol: Int
        val rrIndentLineCol: Int

        run {
            val attr = context.obtainStyledAttributes(
                intArrayOf(
                    R.attr.rrIndentBackgroundCol,
                    R.attr.rrIndentLineCol
                )
            )
            rrIndentBackgroundCol = attr.getColor(0, General.COLOR_INVALID)
            rrIndentLineCol = attr.getColor(1, General.COLOR_INVALID)
            attr.recycle()
        }

        this.setBackgroundColor(rrIndentBackgroundCol)
        mPaint.setColor(rrIndentLineCol)
        mPaint.setStrokeWidth(mPixelsPerLine.toFloat())

        mPrefDrawLines = PrefsUtility.pref_appearance_indentlines()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val height = getMeasuredHeight()

        if (mPrefDrawLines) {
            // i keeps track of indentation, and
            // l is to populate the float[] with line co-ordinates
            var l = 0
            var i = 0
            while (i < mIndent) {
                val x = ((mPixelsPerIndent * ++i) - mHalfALine).toFloat()
                mLineBuffer[l++] = x // start-x
                mLineBuffer[l++] = 0f // start-y
                mLineBuffer[l++] = x // stop-x
                mLineBuffer[l++] = height.toFloat() // stop-y
            }
            canvas.drawLines(mLineBuffer, mPaint)
        } else {
            val rightLine = (getWidth() - mHalfALine).toFloat()
            canvas.drawLine(rightLine, 0f, rightLine, getHeight().toFloat(), mPaint)
        }
    }

    /**
     * Sets the indentation for the View
     *
     * @param indent comment indentation number
     */
    fun setIndentation(indent: Int) {
        getLayoutParams().width = (mPixelsPerIndent * indent)
        mIndent = indent

        if (mPrefDrawLines) {
            mLineBuffer = FloatArray(mIndent * 4)
        }

        invalidate()
        requestLayout()
    }
}
