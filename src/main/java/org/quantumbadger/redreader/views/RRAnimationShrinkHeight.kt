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

import android.view.View
import android.view.ViewGroup
import kotlin.math.sin

class RRAnimationShrinkHeight(private val mTarget: View) : RRAnimation() {
    private val mLayoutParams: ViewGroup.LayoutParams
    private val mStartHeight: Int

    init {
        mLayoutParams = mTarget.getLayoutParams()
        mStartHeight = mTarget.getMeasuredHeight()
    }

    override fun handleFrame(nanosSinceAnimationStart: Long): Boolean {
        mLayoutParams.height = (mStartHeight * interpolateSine(
            1.0 - nanosSinceAnimationStart.toDouble() / DURATION_NANOS.toDouble()
        )).toInt()

        mTarget.setLayoutParams(mLayoutParams)

        val finished = nanosSinceAnimationStart > DURATION_NANOS

        if (finished) {
            mTarget.setVisibility(View.GONE)
        }

        return !finished
    }

    companion object {
        @Suppress("PropertyName")
        private val DURATION_NANOS = 500L * 1000 * 1000

        private fun interpolateSine(fraction: Double): Double {
            return 0.5 + sin((fraction - 0.5) * Math.PI) / 2
        }
    }
}
