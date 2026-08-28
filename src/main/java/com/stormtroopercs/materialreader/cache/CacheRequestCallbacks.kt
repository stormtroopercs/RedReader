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
package com.stormtroopercs.materialreader.cache

import com.stormtroopercs.materialreader.cache.CacheManager.ReadableCacheFile
import com.stormtroopercs.materialreader.common.GenericFactory
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.datastream.SeekableInputStream
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import java.io.IOException
import java.util.UUID

interface CacheRequestCallbacks {
    fun onDownloadNecessary() {}

    fun onDownloadStarted() {}

    fun onDataStreamAvailable(
        streamFactory: GenericFactory<SeekableInputStream, IOException>,
        timestamp: TimestampUTC,
        session: UUID,
        fromCache: Boolean,
        mimetype: String?
    ) {
    }

    fun onDataStreamComplete(
        streamFactory: GenericFactory<SeekableInputStream, IOException>,
        timestamp: TimestampUTC,
        session: UUID,
        fromCache: Boolean,
        mimetype: String?
    ) {
    }

    fun onProgress(
        authorizationInProgress: Boolean,
        bytesRead: Long,
        totalBytes: Long
    ) {
    }

    fun onFailure(error: RRError)

    fun onCacheFileWritten(
        cacheFile: ReadableCacheFile,
        timestamp: TimestampUTC?,
        session: UUID,
        fromCache: Boolean,
        mimetype: String?
    ) {
    }
}
