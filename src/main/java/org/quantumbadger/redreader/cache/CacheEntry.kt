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
package org.quantumbadger.redreader.cache

import android.database.Cursor
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.common.time.TimestampUTC.Companion.fromUtcMs
import java.util.UUID

class CacheEntry internal constructor(cursor: Cursor) {
    val id: Long
    val url: UriString
    val session: UUID
    val timestamp: TimestampUTC
    val mimetype: String?
    val cacheCompressionType: CacheCompressionType
    val lengthCompressed: Long
    val lengthUncompressed: Long

    init {
        id = cursor.getLong(0)
        url = UriString(cursor.getString(1))
        session = UUID.fromString(cursor.getString(2))
        timestamp = fromUtcMs(cursor.getLong(3))
        mimetype = cursor.getString(4)
        cacheCompressionType = CacheCompressionType.Companion.fromDatabaseId(
            cursor.getInt(5)
        )
        lengthCompressed = cursor.getLong(6)
        lengthUncompressed = cursor.getLong(7)
    }

    companion object {
        @Suppress("PropertyName")
        val DB_FIELDS: Array<String?> = arrayOf<String?>(
            CacheDbManager.Companion.FIELD_ID,
            CacheDbManager.Companion.FIELD_URL,
            CacheDbManager.Companion.FIELD_SESSION,
            CacheDbManager.Companion.FIELD_TIMESTAMP,
            CacheDbManager.Companion.FIELD_MIMETYPE,
            CacheDbManager.Companion.FIELD_COMPRESSION_TYPE,
            CacheDbManager.Companion.FIELD_LENGTH_COMPRESSED,
            CacheDbManager.Companion.FIELD_LENGTH_UNCOMPRESSED
        )
    }
}
