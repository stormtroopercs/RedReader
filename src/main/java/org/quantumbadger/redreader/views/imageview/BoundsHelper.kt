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

import kotlin.math.min

class BoundsHelper(
    private val mResolutionX: Int, private val mResolutionY: Int,
    private val mImageResolutionX: Int, private val mImageResolutionY: Int,
    private val mCoordinateHelper: CoordinateHelper
) {
    private val mMinScale: Float

    init {
        mMinScale = min(
            mResolutionX.toFloat() / mImageResolutionX.toFloat(),
            mResolutionY.toFloat() / mImageResolutionY.toFloat()
        )
    }

    fun applyMinScale() {
        mCoordinateHelper.setScale(mMinScale)
    }

    val isMinScale: Boolean
        get() = mCoordinateHelper.scale - 0.000001f <= mMinScale

    fun applyBounds() {
        if (mCoordinateHelper.scale < mMinScale) {
            applyMinScale()
        }

        val scale = mCoordinateHelper.scale
        val posOffset = mCoordinateHelper.getPositionOffset()

        val scaledImageWidth = mImageResolutionX.toFloat() * scale
        val scaledImageHeight = mImageResolutionY.toFloat() * scale

        if (scaledImageWidth <= mResolutionX) {
            posOffset.x = (mResolutionX - scaledImageWidth) / 2
        } else if (posOffset.x > 0) {
            posOffset.x = 0f
        } else if (posOffset.x < mResolutionX - scaledImageWidth) {
            posOffset.x = mResolutionX - scaledImageWidth
        }

        if (scaledImageHeight <= mResolutionY) {
            posOffset.y = (mResolutionY - scaledImageHeight) / 2
        } else if (posOffset.y > 0) {
            posOffset.y = 0f
        } else if (posOffset.y < mResolutionY - scaledImageHeight) {
            posOffset.y = mResolutionY - scaledImageHeight
        }
    }

    fun getMinScale(): Float {
        return mMinScale
    }
}
