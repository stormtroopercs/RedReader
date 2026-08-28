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

package com.stormtroopercs.materialreader.repository

import kotlinx.coroutines.flow.Flow
import com.stormtroopercs.materialreader.database.dao.SubredditDao
import com.stormtroopercs.materialreader.database.entities.SubredditEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Subreddit data access.
 * Wraps SubredditDao and provides a clean API for subreddit operations.
 * Supports both local caching and network synchronization.
 */
@Singleton
class SubredditRepository @Inject constructor(
    private val subredditDao: SubredditDao
) {

    /**
     * Flow of all subreddits in the database, ordered by last update.
     */
    val allSubreddits: Flow<List<SubredditEntity>> = subredditDao.getAllSubreddits()

    /**
     * Get a single subreddit by name.
     */
    suspend fun getSubredditByName(name: String): SubredditEntity? {
        return subredditDao.getSubredditByName(name)
    }

    /**
     * Search for subreddits matching a query.
     */
    fun searchSubreddits(query: String): Flow<List<SubredditEntity>> {
        return subredditDao.searchSubreddits(query)
    }

    /**
     * Insert a single subreddit into the database.
     */
    suspend fun insertSubreddit(subreddit: SubredditEntity) {
        subredditDao.insertSubreddit(subreddit)
    }

    /**
     * Insert multiple subreddits into the database.
     */
    suspend fun insertSubreddits(subreddits: List<SubredditEntity>) {
        subredditDao.insertSubreddits(subreddits)
    }

    /**
     * Update an existing subreddit.
     */
    suspend fun updateSubreddit(subreddit: SubredditEntity) {
        subredditDao.updateSubreddit(subreddit)
    }

    /**
     * Delete a single subreddit.
     */
    suspend fun deleteSubreddit(subreddit: SubredditEntity) {
        subredditDao.deleteSubreddit(subreddit)
    }

    /**
     * Delete all subreddits from the database.
     */
    suspend fun deleteAllSubreddits() {
        subredditDao.deleteAllSubreddits()
    }

    /**
     * Get the total number of subreddits in the database.
     */
    val subredditCount: Flow<Int> = subredditDao.getSubredditCount()
}
