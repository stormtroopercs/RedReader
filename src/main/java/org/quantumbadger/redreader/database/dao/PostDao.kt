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

package org.quantumbadger.redreader.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.quantumbadger.redreader.database.entities.PostEntity

/**
 * Data Access Object for PostEntity operations.
 * Provides CRUD operations and Flow-based observation for reactive UI updates.
 */
@Dao
interface PostDao {

    @Query("SELECT * FROM posts ORDER BY createdUtc DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :id LIMIT 1")
    suspend fun getPostById(id: String): PostEntity?

    @Query("SELECT * FROM posts WHERE subreddit = :subreddit ORDER BY createdUtc DESC")
    fun getPostsBySubreddit(subreddit: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE author = :author ORDER BY createdUtc DESC")
    fun getPostsByAuthor(author: String): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Delete
    suspend fun deletePost(post: PostEntity)

    @Query("DELETE FROM posts WHERE subreddit = :subreddit")
    suspend fun deletePostsBySubreddit(subreddit: String)

    @Query("DELETE FROM posts")
    suspend fun deleteAllPosts()

    @Query("SELECT COUNT(*) FROM posts")
    fun getPostCount(): Flow<Int>
}
