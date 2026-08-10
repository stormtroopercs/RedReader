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

package org.quantumbadger.redreader.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.quantumbadger.redreader.database.CommentDao
import org.quantumbadger.redreader.database.PostDao
import org.quantumbadger.redreader.database.RedReaderDatabase
import javax.inject.Singleton

/**
 * Hilt module that provides repository and database dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideRedReaderDatabase(
        @ApplicationContext context: Context
    ): RedReaderDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            RedReaderDatabase::class.java,
            "redreader.db"
        ).build()
    }

    @Provides
    @Singleton
    fun providePostDao(database: RedReaderDatabase): PostDao {
        return database.postDao()
    }

    @Provides
    @Singleton
    fun provideCommentDao(database: RedReaderDatabase): CommentDao {
        return database.commentDao()
    }
}
