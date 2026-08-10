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
package org.quantumbadger.redreader.image

import android.graphics.Bitmap
import kotlin.math.max

object ThumbnailScaler {
    private const val maxHeightWidthRatio = 3.0f

    private fun scaleAndCrop(
        src: Bitmap,
        w: Int,
        h: Int,
        newWidth: Int
    ): Bitmap {
        val scaleFactor = newWidth.toFloat() / w.toFloat()
        val scaled = Bitmap.createScaledBitmap(
            src,
            Math.round(scaleFactor * src.getWidth()),
            Math.round(scaleFactor * src.getHeight()),
            true
        )

        val result = Bitmap.createBitmap(
            scaled,
            0,
            0,
            newWidth,
            Math.round(h.toFloat() * scaleFactor)
        )

        if (result != scaled) {
            scaled.recycle()
        }

        return result
    }

    fun scale(image: Bitmap, width: Int): Bitmap {
        val heightWidthRatio = image.getHeight().toFloat() / image.getWidth().toFloat()

        if (heightWidthRatio >= 1.0f && heightWidthRatio <= maxHeightWidthRatio) {
            // Use as-is.

            return Bitmap.createScaledBitmap(
                image,
                width,
                Math.round(heightWidthRatio * width),
                true
            )
        } else if (heightWidthRatio < 1.0f) {
            // Wide image. Crop horizontally.

            return scaleAndCrop(image, image.getHeight(), image.getHeight(), width)
        } else {
            // Tall image.

            return scaleAndCrop(
                image,
                image.getWidth(),
                Math.round(image.getWidth() * maxHeightWidthRatio),
                width
            )
        }
    }

    fun scaleNoCrop(image: Bitmap, desiredSquareSizePx: Int): Bitmap {
        val currentSquareSizePx = max(image.getWidth(), image.getHeight())

        val scale = desiredSquareSizePx.toFloat() / currentSquareSizePx.toFloat()

        return Bitmap.createScaledBitmap(
            image,
            Math.round(scale * image.getWidth()),
            Math.round(scale * image.getHeight()),
            true
        )
    }
}
