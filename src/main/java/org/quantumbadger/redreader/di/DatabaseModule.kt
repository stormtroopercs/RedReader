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
import org.quantumbadger.redreader.database.dao.SubredditDao
import javax.inject.Singleton

/**
 * Hilt bindings for the Room layer. As of the C6 dead-code triage (48th
 * increment) the only live surface is the `subreddits` table (the subreddit
 * search's local cache) — the `postDao`/`commentDao`/`userSessionDao`
 * providers went with their DAOs.
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
    fun provideSubredditDao(database: RedReaderDatabase): SubredditDao {
        return database.subredditDao()
    }
}
