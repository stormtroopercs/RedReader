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

class MultiScaleTileManager(
    imageTileSource: ImageTileSource?,
    thread: ImageViewTileLoaderThread?,
    x: Int,
    y: Int,
    listener: ImageViewTileLoader.Listener?
) {
    private val mTileLoaders: Array<ImageViewTileLoader?>

    private var mDesiredScaleIndex = -1

    private val mLock = Any()

    init {
        mTileLoaders =
            arrayOfNulls<ImageViewTileLoader>(sampleSizeToScaleIndex(MAX_SAMPLE_SIZE) + 1)

        for (s in mTileLoaders.indices) {
            mTileLoaders[s] = ImageViewTileLoader(
                imageTileSource,
                thread,
                x,
                y,
                scaleIndexToSampleSize(s),
                listener,
                mLock
            )
        }
    }

    val atDesiredScale: Bitmap?
        get() = mTileLoaders[mDesiredScaleIndex]!!.get()

    fun markAsWanted(desiredScaleIndex: Int) {
        if (desiredScaleIndex == mDesiredScaleIndex) {
            return
        }

        mDesiredScaleIndex = desiredScaleIndex

        synchronized(mLock) {
            mTileLoaders[desiredScaleIndex]!!.markAsWanted()
            for (s in mTileLoaders.indices) {
                if (s != desiredScaleIndex) {
                    mTileLoaders[s]!!.markAsUnwanted()
                }
            }
        }
    }

    fun markAsUnwanted() {
        if (mDesiredScaleIndex == -1) {
            return
        }

        mDesiredScaleIndex = -1

        synchronized(mLock) {
            for (s in mTileLoaders.indices) {
                mTileLoaders[s]!!.markAsUnwanted()
            }
        }
    }

    companion object {
        const val MAX_SAMPLE_SIZE: Int = 32

        fun scaleIndexToSampleSize(scaleIndex: Int): Int {
            return 1 shl scaleIndex
        }

        fun sampleSizeToScaleIndex(sampleSize: Int): Int {
            return Integer.numberOfTrailingZeros(sampleSize)
        }
    }
}
