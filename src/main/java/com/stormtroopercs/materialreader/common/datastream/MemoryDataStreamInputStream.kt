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
import kotlin.math.max
import kotlin.math.min

class MemoryDataStreamInputStream(private val mStream: MemoryDataStream) : SeekableInputStream() {
    private var mPosition = 0

    @Throws(IOException::class)
    override fun read(): Int {
        val result = mStream.blockingReadOneByte(mPosition)

        if (result >= 0) {
            mPosition++
        }

        return result
    }

    @Throws(IOException::class)
    override fun read(buf: ByteArray): Int {
        return read(buf, 0, buf.size)
    }

    @Throws(IOException::class)
    override fun read(buf: ByteArray, off: Int, len: Int): Int {
        val bytesRead = mStream.blockingRead(mPosition, buf, off, len)

        if (bytesRead > 0) {
            mPosition += bytesRead
        }

        return bytesRead
    }

    override val position: Long get() = mPosition.toLong()

    @Throws(IOException::class)
    override fun seek(position: Long) {
        if (position < 0) {
            throw IOException("Attempted to seek before zero")
        }

        mPosition = position.toInt()
    }

    override fun skip(offset: Long): Long {
        val bytesToSkip = min(offset, max(0, mStream.size() - mPosition).toLong()).toInt()
        mPosition += bytesToSkip
        return bytesToSkip.toLong()
    }

    override fun available(): Int {
        return mStream.size()
    }

    override fun close() {
        // Nothing to do here
    }

    @Throws(IOException::class)
    override fun readRemainingAsBytes(callback: ByteArrayCallback) {
        mStream.getUnderlyingByteArrayWhenComplete(ByteArrayCallback { buf: ByteArray?, offset: Int, length: Int ->
            callback.onByteArray(
                buf!!,
                offset + mPosition,
                length - mPosition
            )
        })
    }
}
