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

import com.stormtroopercs.materialreader.common.General.readWholeStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import kotlin.math.min
import com.stormtroopercs.materialreader.common.General

class SeekableFileInputStream(file: File) : SeekableInputStream() {
    private val mFile: RandomAccessFile
    private var mPosition: Long = 0

    init {
        mFile = RandomAccessFile(file, "r")
    }

    override val position: Long get() = mPosition

    @Throws(IOException::class)
    override fun seek(position: Long) {
        mFile.seek(position)
        mPosition = position
    }

    @Throws(IOException::class)
    override fun readRemainingAsBytes(callback: ByteArrayCallback) {
        val result = readWholeStream(this)
        callback.onByteArray(result, 0, result.size)
    }

    @Throws(IOException::class)
    override fun read(): Int {
        val result = mFile.read()

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
    override fun read(buf: ByteArray?, off: Int, len: Int): Int {
        if (len == 0) {
            throw IOException("Attempted to read zero bytes")
        }

        val result = mFile.read(buf, off, len)

        if (result > 0) {
            mPosition += result.toLong()
        }

        return result
    }

    @Throws(IOException::class)
    override fun skip(n: Long): Long {
        val bytesToSkip = min(n, available().toLong())
        seek((mPosition + bytesToSkip).toInt().toLong())
        return bytesToSkip
    }

    @Throws(IOException::class)
    override fun available(): Int {
        return (mFile.length() - mPosition).toInt()
    }

    @Throws(IOException::class)
    override fun close() {
        mFile.close()
    }
}
