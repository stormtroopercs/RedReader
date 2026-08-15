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

import org.quantumbadger.redreader.common.MutableFloatPoint2D
import kotlin.math.pow

class ImageViewScaleAnimation(
    private val mTargetScale: Float,
    private val mCoordinateHelper: CoordinateHelper,
    stepCount: Int,
    screenCoord: MutableFloatPoint2D
) {
    private val mStepSize: Float
    private val mScreenCoord = MutableFloatPoint2D()

    init {
        mStepSize = (mTargetScale / mCoordinateHelper.scale).toDouble()
            .pow((1.0 / stepCount.toDouble())).toFloat()
        mScreenCoord.set(screenCoord)
    }

    fun onStep(): Boolean {
        mCoordinateHelper.scaleAboutScreenPoint(mScreenCoord, mStepSize)

        if (mStepSize > 1) {
            if (mTargetScale <= mCoordinateHelper.scale) {
                mCoordinateHelper.setScaleAboutScreenPoint(mScreenCoord, mTargetScale)
                return false
            }
        } else {
            if (mTargetScale >= mCoordinateHelper.scale) {
                mCoordinateHelper.setScaleAboutScreenPoint(mScreenCoord, mTargetScale)
                return false
            }
        }

        return true
    }
}
