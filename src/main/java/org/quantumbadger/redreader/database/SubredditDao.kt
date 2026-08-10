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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for subreddit caching.
 * Provides CRUD operations for SubredditEntity.
 */
@Dao
interface SubredditDao {

    @Query("SELECT * FROM subreddits WHERE name = :name")
    fun getSubredditByName(name: String): SubredditEntity?

    @Query("SELECT * FROM subreddits")
    fun getAllSubreddits(): Flow<List<SubredditEntity>>

    @Query("SELECT * FROM subreddits")
    fun getAllSubredditsSync(): List<SubredditEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubreddit(subreddit: SubredditEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubreddits(subreddits: List<SubredditEntity>)

    @Update
    suspend fun updateSubreddit(subreddit: SubredditEntity)

    @Query("DELETE FROM subreddits WHERE name = :name")
    suspend fun deleteSubredditByName(name: String)

    @Query("DELETE FROM subreddits")
    suspend fun deleteAllSubreddits()

    @Query("SELECT EXISTS(SELECT 1 FROM subreddits WHERE name = :name)")
    suspend fun exists(name: String): Boolean

    @Query("UPDATE subreddits SET lastFetchTime = :fetchTime, hasFetched = 1 WHERE name = :name")
    suspend fun markAsFetched(name: String, fetchTime: Long)

    @Query("UPDATE subreddits SET fetchFailureCount = fetchFailureCount + 1 WHERE name = :name")
    suspend fun incrementFailureCount(name: String)
}
