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
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.quantumbadger.redreader.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.quantumbadger.redreader.database.dao.CommentDao
import org.quantumbadger.redreader.database.dao.PostDao
import org.quantumbadger.redreader.database.dao.SubredditDao
import org.quantumbadger.redreader.database.dao.UserSessionDao
import org.quantumbadger.redreader.database.entities.CommentEntity
import org.quantumbadger.redreader.database.entities.PostEntity
import org.quantumbadger.redreader.database.entities.SubredditEntity
import org.quantumbadger.redreader.database.entities.UserSessionEntity

/**
 * Room database providing local caching for RedReader.
 * Manages all entities and provides DAOs for data access.
 * Supports offline reading and faster subsequent loads.
 */
@Database(
    entities = [
        PostEntity::class,
        CommentEntity::class,
        SubredditEntity::class,
        UserSessionEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class RedReaderDatabase : RoomDatabase() {

    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun subredditDao(): SubredditDao
    abstract fun userSessionDao(): UserSessionDao

    companion object {
        @Volatile
        private var INSTANCE: RedReaderDatabase? = null

        fun getDatabase(context: Context): RedReaderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RedReaderDatabase::class.java,
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
