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
package org.quantumbadger.redreader.views.video

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import java.io.IOException
import java.util.Objects

@OptIn(UnstableApi::class)
class ExoPlayerSeekableInputStreamDataSource(
    isNetwork: Boolean,
    private val mStreamFactory: GenericFactory<SeekableInputStream, IOException>
) : BaseDataSource(isNetwork) {
    private var mCurrentStream: SeekableInputStream?=null

    @Throws(IOException::class)
    override fun open(dataSpec: DataSpec): Long {
        if (mCurrentStream != null) {
            throw IOException("Already open!")
        }

        transferInitializing(dataSpec)

        mCurrentStream = mStreamFactory.create()
        mCurrentStream!!.seek(dataSpec.position)

        transferStarted(dataSpec)

        return C.LENGTH_UNSET.toLong()
    }

    @Throws(IOException::class)
    override fun read(
        buffer: ByteArray,
        offset: Int,
        readLength: Int
    ): Int {
        if (readLength == 0) {
            return 0
        }

        val result = Objects.requireNonNull<SeekableInputStream>(mCurrentStream)
            .read(buffer, offset, readLength)

        if (result < 0) {
            return C.RESULT_END_OF_INPUT
        }

        bytesTransferred(result)
        return result
    }

    override fun getUri(): Uri? {
        return URI
    }

    @Throws(IOException::class)
    override fun close() {
        if (mCurrentStream != null) {
            mCurrentStream!!.close()
            mCurrentStream = null
            transferEnded()
        }
    }

    companion object {
        @Suppress("PropertyName")
        val URI: Uri = Uri.parse("redreader://video")
    }
}