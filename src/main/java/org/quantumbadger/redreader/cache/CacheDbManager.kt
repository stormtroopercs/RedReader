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

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.time.TimeDuration
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.common.time.TimestampUTC.Companion.fromUtcMs
import org.quantumbadger.redreader.common.time.TimestampUTC.Companion.now
import java.io.IOException
import java.util.Locale
import java.util.UUID

internal class CacheDbManager(context: Context?) :
    SQLiteOpenHelper(context, CACHE_DB_FILENAME, null, CACHE_DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        val queryString = String.format(
            "CREATE TABLE %s (" +
                    "%s INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "%s TEXT NOT NULL," +
                    "%s TEXT NOT NULL," +
                    "%s TEXT NOT NULL," +
                    "%s INTEGER," +
                    "%s INTEGER," +
                    "%s INTEGER," +
                    "%s TEXT," +
                    "%s INTEGER," +
                    "%s INTEGER," +
                    "%s INTEGER," +
                    "UNIQUE (%s, %s, %s) ON CONFLICT REPLACE)",
            TABLE,
            FIELD_ID,
            FIELD_URL,
            FIELD_USER,
            FIELD_SESSION,
            FIELD_TIMESTAMP,
            FIELD_STATUS,
            FIELD_TYPE,
            FIELD_MIMETYPE,
            FIELD_COMPRESSION_TYPE,
            FIELD_LENGTH_COMPRESSED,
            FIELD_LENGTH_UNCOMPRESSED,
            FIELD_USER, FIELD_URL, FIELD_SESSION
        )

        db.execSQL(queryString)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        if (oldVersion < 2) {
            db.execSQL(
                String.format(
                    Locale.US,
                    "ALTER TABLE %s ADD COLUMN %s INTEGER NOT NULL DEFAULT %d",
                    TABLE,
                    FIELD_COMPRESSION_TYPE,
                    CacheCompressionType.NONE.databaseId
                )
            )

            db.execSQL(
                String.format(
                    Locale.US,
                    "ALTER TABLE %s ADD COLUMN %s INTEGER NOT NULL DEFAULT %d",
                    TABLE,
                    FIELD_LENGTH_UNCOMPRESSED,
                    0
                )
            )

            db.execSQL(
                String.format(
                    Locale.US,
                    "ALTER TABLE %s ADD COLUMN %s INTEGER NOT NULL DEFAULT %d",
                    TABLE,
                    FIELD_LENGTH_COMPRESSED,
                    0
                )
            )
        }
    }

    @Synchronized
    fun selectById(id: Long): Optional<CacheEntry> {
        val db = getReadableDatabase()

        db.query(
            TABLE,
            CacheEntry.Companion.DB_FIELDS,
            String.format(Locale.US, "%s=?", FIELD_ID),
            arrayOf<String>(id.toString()),
            null,
            null,
            FIELD_TIMESTAMP + " DESC"
        ).use { cursor ->
            val entries = readEntriesFromCursor(cursor)
            if (entries.isEmpty()) {
                return Optional.Companion.empty<CacheEntry>()
            } else {
                return Optional.Companion.of<CacheEntry>(entries.get(0))
            }
        }
    }

    @Synchronized
    fun select(
        url: UriString,
        user: String?,
        session: UUID?
    ): MutableList<CacheEntry> {
        val db = getReadableDatabase()

        val queryString: String
        val queryParams: Array<String?>

        if (session == null) {
            queryString = String.format(
                Locale.US,
                "%s=%d AND %s=? AND %s=?",
                FIELD_STATUS,
                STATUS_DONE,
                FIELD_URL,
                FIELD_USER
            )
            queryParams = arrayOf<String?>(url.value, user)
        } else {
            queryString = String.format(
                Locale.US,
                "%s=%d AND %s=? AND %s=? AND %s=?",
                FIELD_STATUS,
                STATUS_DONE,
                FIELD_URL,
                FIELD_USER,
                FIELD_SESSION
            )
            queryParams = arrayOf<String?>(url.value, user, session.toString())
        }

        db.query(
            TABLE,
            CacheEntry.Companion.DB_FIELDS,
            queryString,
            queryParams,
            null,
            null,
            FIELD_TIMESTAMP + " DESC"
        ).use { cursor ->
            return readEntriesFromCursor(cursor)
        }
    }

    private fun readEntriesFromCursor(cursor: Cursor): MutableList<CacheEntry> {
        val result = ArrayList<CacheEntry>()

        while (cursor.moveToNext()) {
            result.add(CacheEntry(cursor))
        }

        return result
    }

    @Synchronized
    @Throws(IOException::class)
    fun newEntry(
        url: UriString,
        user: RedditAccount,
        fileType: Int,
        session: UUID,
        mimetype: String?,
        compressionType: CacheCompressionType,
        lengthCompressed: Long,
        lengthUncompressed: Long
    ): Long {
        if (session == null) {
            throw RuntimeException("No session to write")
        }

        val db = this.getWritableDatabase()

        val row = ContentValues()

        row.put(FIELD_URL, url.value)
        row.put(FIELD_USER, user.username)
        row.put(FIELD_SESSION, session.toString())
        row.put(FIELD_TYPE, fileType)
        row.put(FIELD_STATUS, STATUS_MOVING)
        row.put(FIELD_TIMESTAMP, now().toUtcMs())
        row.put(FIELD_MIMETYPE, mimetype)
        row.put(FIELD_COMPRESSION_TYPE, compressionType.databaseId)
        row.put(FIELD_LENGTH_COMPRESSED, lengthCompressed)
        row.put(FIELD_LENGTH_UNCOMPRESSED, lengthUncompressed)

        val result = db.insert(TABLE, null, row)

        if (result < 0) {
            throw IOException("DB insert failed")
        }

        return result
    }

    @Synchronized
    fun setEntryDone(id: Long) {
        val db = this.getWritableDatabase()

        val row = ContentValues()
        row.put(FIELD_STATUS, STATUS_DONE)

        db.update(TABLE, row, FIELD_ID + "=?", arrayOf<String>(id.toString()))
    }

    @Synchronized
    fun delete(id: Long): Int {
        val db = this.getWritableDatabase()
        return db.delete(TABLE, FIELD_ID + "=?", arrayOf<String>(id.toString()))
    }

    @Synchronized
    fun getFilesToPrune(
        currentFiles: HashSet<Long>,
        maxAge: java.util.HashMap<Int, TimeDuration>,
        defaultMaxAge: TimeDuration
    ): ArrayList<Long> {
        val db = this.getWritableDatabase()

        val currentTime = now()

        val cursor = db.query(
            TABLE,
            arrayOf<String>(FIELD_ID, FIELD_TIMESTAMP, FIELD_TYPE),
            null,
            null,
            null,
            null,
            null,
            null
        )

        val currentEntries = HashSet<Long>()
        val entriesToDelete = ArrayList<Long>()
        val filesToDelete = ArrayList<Long>(32)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val timestamp = fromUtcMs(cursor.getLong(1))
            val type = cursor.getInt(2)

            val pruneIfBeforeMs: TimestampUTC

            if (maxAge.containsKey(type)) {
                pruneIfBeforeMs = currentTime.subtract(maxAge.getValue(type))
            } else {
                Log.e("RR DEBUG cache", "Using default age! Filetype " + type)
                pruneIfBeforeMs = currentTime.subtract(defaultMaxAge)
            }

            if (!currentFiles.contains(id)) {
                entriesToDelete.add(id)
            } else if (timestamp.isLessThan(pruneIfBeforeMs)) {
                entriesToDelete.add(id)
                filesToDelete.add(id)
            } else {
                currentEntries.add(id)
            }
        }

        for (id in currentFiles) {
            if (!currentEntries.contains(id)) {
                filesToDelete.add(id)
            }
        }

        if (!entriesToDelete.isEmpty()) {
            val query = StringBuilder(
                String.format(
                    Locale.US,
                    "DELETE FROM %s WHERE %s IN (",
                    TABLE,
                    FIELD_ID
                )
            )

            query.append(entriesToDelete.removeAt(entriesToDelete.size - 1))

            for (id in entriesToDelete) {
                query.append(",").append(id)
                if (query.length > 512 * 1024) {
                    break
                }
            }

            query.append(')')

            db.execSQL(query.toString())
        }

        cursor.close()

        return filesToDelete
    }

    @Synchronized
    fun emptyTheWholeCache() {
        val db = this.getWritableDatabase()
        db.execSQL(String.format(Locale.US, "DELETE FROM %s", TABLE))
    }

    @get:Synchronized
    val filesToSize: HashMap<Long, Int>
        get() {
            val db = this.getWritableDatabase()

            val cursor = db.query(
                TABLE,
                arrayOf<String>(
                    FIELD_ID,
                    FIELD_TYPE
                ),
                null,
                null,
                null,
                null,
                null,
                null
            )

            val filesToCheck =                 java.util.HashMap<Long, Int>(32)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val type = cursor.getInt(1)

                filesToCheck.put(id, type)
            }

            cursor.close()

            return filesToCheck
        }

    companion object {
        private const val CACHE_DB_FILENAME = "cache.db"
        private const val TABLE = "web"

        const val FIELD_URL: String = "url"
        const val FIELD_ID: String = "id"
        const val FIELD_TIMESTAMP: String = "timestamp"
        const val FIELD_SESSION: String = "session"
        const val FIELD_USER: String = "user"
        const val FIELD_STATUS: String = "status"
        const val FIELD_TYPE: String = "type"
        const val FIELD_MIMETYPE: String = "mimetype"
        const val FIELD_COMPRESSION_TYPE: String = "compressionType"
        const val FIELD_LENGTH_UNCOMPRESSED: String = "lengthUncompressed"
        const val FIELD_LENGTH_COMPRESSED: String = "lengthCompressed"

        private const val STATUS_MOVING = 1
        private const val STATUS_DONE = 2

        private const val CACHE_DB_VERSION = 2
    }
}
