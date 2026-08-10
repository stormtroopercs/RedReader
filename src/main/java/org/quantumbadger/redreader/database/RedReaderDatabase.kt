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
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date

@Database(
    entities = [PostEntity::class, CommentEntity::class, SubredditEntity::class, UserSessionEntity::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3)
    ]
)
@TypeConverters(Converters::class)
abstract class RedReaderDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun subredditDao(): SubredditDao
    abstract fun userSessionDao(): UserSessionDao

    companion object {
        private const val DATABASE_NAME = "redreader.db"

        @Volatile
        private var INSTANCE: RedReaderDatabase? = null

        fun getDatabase(
            @ApplicationContext context: Context
        ): RedReaderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RedReaderDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Migration from version 1 to 2.
 */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: androidx.room.SupportSQLiteDatabase) {
        // No schema changes needed for this migration
    }
}