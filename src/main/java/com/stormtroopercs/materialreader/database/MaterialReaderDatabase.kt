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

package com.stormtroopercs.materialreader.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.stormtroopercs.materialreader.database.dao.SubredditDao
import com.stormtroopercs.materialreader.database.entities.SubredditEntity

/**
 * Room database providing local caching for MaterialReader.
 *
 * As of the C6 dead-code triage (48th increment) the only live surface is
 * the `subreddits` table — the live subreddit search seeds it as a local
 * cache (`SubredditRepository` is the write side). The `posts`/`comments`/
 * `user_sessions` entities + DAOs and their repositories were dead scaffolding
 * (added in the original Room "Phase 1.2/3" work, never wired to any live
 * screen) and were deleted.
 */
@Database(
    entities = [SubredditEntity::class],
    version = 1,
    exportSchema = true
)
abstract class MaterialReaderDatabase : RoomDatabase() {

    abstract fun subredditDao(): SubredditDao

    companion object {
        @Volatile
        private var INSTANCE: MaterialReaderDatabase? = null

        fun getDatabase(context: Context): MaterialReaderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MaterialReaderDatabase::class.java,
                    "redreader_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
