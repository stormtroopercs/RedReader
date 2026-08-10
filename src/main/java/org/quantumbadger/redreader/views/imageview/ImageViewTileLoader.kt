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
import android.util.Log
import androidx.annotation.UiThread
import org.quantumbadger.redreader.common.AndroidCommon

class ImageViewTileLoader(
    private val mSource: ImageTileSource,
    private val mThread: ImageViewTileLoaderThread,
    private val mX: Int,
    private val mY: Int,
    private val mSampleSize: Int,
    private val mListener: Listener,
    private val mLock: Any
) {
    @UiThread
    interface Listener {
        fun onTileLoaded(x: Int, y: Int, sampleSize: Int)

        fun onTileLoaderOutOfMemory()

        fun onTileLoaderException(t: Throwable?)
    }

    private var mWanted = false

    private var mResult: Bitmap?=null

    private val mNotifyRunnable: Runnable

    init {
        mNotifyRunnable = Runnable { mListener.onTileLoaded(mX, mY, mSampleSize) }
    }

    // Caller must synchronize on mLock
    fun markAsWanted() {
        if (mWanted) {
            return
        }

        if (mResult != null) {
            throw RuntimeException("Not wanted, but the image is loaded anyway!")
        }

        mThread.enqueue(this)
        mWanted = true
    }

    fun doPrepare() {
        synchronized(mLock) {
            if (!mWanted) {
                return
            }
            if (mResult != null) {
                return
            }
        }

        val tile: Bitmap?

        try {
            tile = mSource.getTile(mSampleSize, mX, mY)
        } catch (e: OutOfMemoryError) {
            AndroidCommon.UI_THREAD_HANDLER.post(NotifyOOMRunnable())
            return
        } catch (t: Throwable) {
            Log.e("ImageViewTileLoader", "Exception in getTile()", t)
            AndroidCommon.UI_THREAD_HANDLER.post(NotifyErrorRunnable(t))
            return
        }

        synchronized(mLock) {
            if (mWanted) {
                mResult = tile
            } else if (tile != null) {
                tile.recycle()
            }
        }

        AndroidCommon.UI_THREAD_HANDLER.post(mNotifyRunnable)
    }

    fun get(): Bitmap? {
        synchronized(mLock) {
            if (!mWanted) {
                throw RuntimeException("Attempted to get unwanted image!")
            }
            return mResult
        }
    }

    // Caller must synchronize on mLock
    fun markAsUnwanted() {
        mWanted = false

        if (mResult != null) {
            mResult!!.recycle()
            mResult = null
        }
    }

    private inner class NotifyOOMRunnable : Runnable {
        override fun run() {
            mListener.onTileLoaderOutOfMemory()
        }
    }

    private inner class NotifyErrorRunnable(private val mError: Throwable?) : Runnable {
        override fun run() {
            mListener.onTileLoaderException(mError)
        }
    }
}
