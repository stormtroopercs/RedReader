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

import android.graphics.Bitmap
import android.graphics.Matrix
import org.quantumbadger.redreader.common.General.divideCeil
import kotlin.math.min

class ImageTileSourceWholeBitmap(private val mBitmap: Bitmap) : ImageTileSource {
    private val mWidth: Int
    private val mHeight: Int

    init {
        mWidth = mBitmap.getWidth()
        mHeight = mBitmap.getHeight()
    }

    override fun getWidth(): Int {
        return mWidth
    }

    override fun getHeight(): Int {
        return mHeight
    }

    override fun getTileSize(): Int {
        return TILE_SIZE
    }

    override fun getHTileCount(): Int {
        return divideCeil(getWidth(), TILE_SIZE)
    }

    override fun getVTileCount(): Int {
        return divideCeil(getHeight(), TILE_SIZE)
    }

    override fun getTile(sampleSize: Int, tileX: Int, tileY: Int): Bitmap? {
        if (sampleSize == 1 && TILE_SIZE >= mWidth && TILE_SIZE >= mHeight) {
            return mBitmap
        }

        val tileStartX: Int = tileX * TILE_SIZE
        val tileStartY: Int = tileY * TILE_SIZE
        val tileEndX = min(mWidth, (tileX + 1) * TILE_SIZE)
        val tileEndY = min(mHeight, (tileY + 1) * TILE_SIZE)

        val inputTileWidthPx = tileEndX - tileStartX
        val inputTileHeightPx = tileEndY - tileStartY

        if (sampleSize == 1) {
            return Bitmap.createBitmap(
                mBitmap,
                tileStartX,
                tileStartY,
                inputTileWidthPx,
                inputTileHeightPx
            )
        }

        val scaleMatrix = Matrix()
        scaleMatrix.setScale(1.0f / sampleSize, 1.0f / sampleSize)

        return Bitmap.createBitmap(
            mBitmap,
            tileStartX,
            tileStartY,
            inputTileWidthPx,
            inputTileHeightPx,
            scaleMatrix,
            true
        )
    }

    override fun dispose() {
        // Nothing to do here
    }

    companion object {
        private const val TILE_SIZE = 512
    }
}
