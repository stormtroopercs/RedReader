/*******************************************************************************
 * This file is part of MaterialReader.
 *
 * MaterialReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MaterialReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MaterialReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.stormtroopercs.materialreader.common.datastream

import java.io.IOException
import kotlin.math.min

class MemoryDataStream {
    private val mLock = Any()

    private var mData: ByteArray
    private var mSize: Int

    private var mFailed: IOException?=null
    private var mComplete = false

    @JvmOverloads
    constructor(initialCapacity: Int = 64 * 1024) {
        if (initialCapacity < 1) {
            throw RuntimeException("Initial capacity must be at least 1")
        }

        mData = ByteArray(initialCapacity)
        mSize = 0
    }

    constructor(data: ByteArray) {
        mData = data
        mSize = data.size
        mComplete = true
    }

    private fun ensureCapacity(desiredCapacity: Int) {
        if (desiredCapacity <= mData.size) {
            return
        }

        if (desiredCapacity > (mData.size * 2)) {
            realloc(desiredCapacity + (desiredCapacity / 2))
        } else {
            realloc(mData.size * 2)
        }
    }

    private fun realloc(newCapacity: Int) {
        if (newCapacity < mSize) {
            throw RuntimeException("Cannot shrink array")
        }

        mData = mData.copyOf(newCapacity)
    }

    fun size(): Int {
        synchronized(mLock) {
            return mSize
        }
    }

    fun writeBytes(data: ByteArray, offset: Int, length: Int) {
        synchronized(mLock) {
            ensureCapacity(mSize + length)
            System.arraycopy(data, offset, mData, mSize, length)
            mSize += length
            (mLock as Object).notifyAll()
        }
    }

    fun setComplete() {
        synchronized(mLock) {
            mComplete = true
            (mLock as Object).notifyAll()
        }
    }

    fun setFailed(e: IOException) {
        synchronized(mLock) {
            mFailed = e
            (mLock as Object).notifyAll()
        }
    }

    private fun notReadyForRead(startingPosition: Int): Boolean {
        return !mComplete && mFailed == null && mSize <= startingPosition
    }

    @Throws(IOException::class)
    fun blockingReadOneByte(position: Int): Int {
        synchronized(mLock) {
            while (notReadyForRead(position)) {
                try {
                    (mLock as Object).wait()
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }
            }
            if (mFailed != null) {
                throw mFailed!!
            }

            if (mSize > position) {
                return mData[position].toInt()
            }

            if (mComplete) {
                return -1
            }
            throw IOException("Internal error: ready conditions not true")
        }
    }

    @Throws(IOException::class)
    fun blockingRead(
        startingPosition: Int,
        output: ByteArray,
        offset: Int,
        maxLength: Int
    ): Int {
        if (maxLength == 0) {
            throw RuntimeException("Attempted to read zero bytes")
        }

        synchronized(mLock) {
            while (notReadyForRead(startingPosition)) {
                try {
                    (mLock as Object).wait()
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }
            }
            if (mFailed != null) {
                throw mFailed!!
            }

            if (mSize > startingPosition) {
                val bytesToRead = min(maxLength, mSize - startingPosition)
                System.arraycopy(mData, startingPosition, output, offset, bytesToRead)
                return bytesToRead
            }

            if (mComplete) {
                return -1
            }
            throw IOException("Internal error: ready conditions not true")
        }
    }

    val inputStream: MemoryDataStreamInputStream
        get() = MemoryDataStreamInputStream(this)

    @Throws(IOException::class)
    fun getUnderlyingByteArrayWhenComplete(
        callback: ByteArrayCallback
    ) {
        synchronized(mLock) {
            while (!mComplete && mFailed == null) {
                try {
                    (mLock as Object).wait()
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }
            }
            if (mFailed != null) {
                throw mFailed!!
            }
        }

        callback.onByteArray(mData, 0, mSize)
    }
}
