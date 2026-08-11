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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.quantumbadger.redreader.database.RedReaderDatabase
import org.quantumbadger.redreader.database.dao.CommentDao
import org.quantumbadger.redreader.database.dao.PostDao
import org.quantumbadger.redreader.database.dao.SubredditDao
import org.quantumbadger.redreader.database.dao.UserSessionDao
import javax.inject.Singleton

/**
 * Hilt module providing Room database and DAO instances.
 * Provides singleton database and all DAOs for dependency injection.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRedReaderDatabase(@ApplicationContext context: Context): RedReaderDatabase {
        return RedReaderDatabase.getDatabase(context)
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

    @Provides
    @Singleton
    fun provideSubredditDao(database: RedReaderDatabase): SubredditDao {
        return database.subredditDao()
    }

    @Provides
    @Singleton
    fun provideUserSessionDao(database: RedReaderDatabase): UserSessionDao {
        return database.userSessionDao()
    }
}
