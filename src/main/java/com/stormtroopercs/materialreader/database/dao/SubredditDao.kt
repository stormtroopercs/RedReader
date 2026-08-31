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

package com.stormtroopercs.materialreader.database.dao

import androidx.room.*
import com.stormtroopercs.materialreader.database.entities.SubredditEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for SubredditEntity operations.
 * Provides CRUD operations and Flow-based observation for reactive UI updates.
 */
@Dao
interface SubredditDao {

	@Query("SELECT * FROM subreddits ORDER BY lastUpdated DESC")
	fun getAllSubreddits(): Flow<List<SubredditEntity>>

	@Query("SELECT * FROM subreddits WHERE name = :name LIMIT 1")
	suspend fun getSubredditByName(name: String): SubredditEntity?

	@Query("SELECT * FROM subreddits WHERE name LIKE :query || '%' ORDER BY subscribers DESC LIMIT 20")
	fun searchSubreddits(query: String): Flow<List<SubredditEntity>>

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertSubreddit(subreddit: SubredditEntity)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertSubreddits(subreddits: List<SubredditEntity>)

	@Update
	suspend fun updateSubreddit(subreddit: SubredditEntity)

	@Delete
	suspend fun deleteSubreddit(subreddit: SubredditEntity)

	@Query("DELETE FROM subreddits")
	suspend fun deleteAllSubreddits()

	@Query("SELECT COUNT(*) FROM subreddits")
	fun getSubredditCount(): Flow<Int>
}
