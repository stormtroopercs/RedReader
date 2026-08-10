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

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface PostDao {

    @Query("SELECT * FROM posts WHERE id = :id")
    fun getPostById(id: String): PostEntity?

    @Query("SELECT * FROM posts")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts")
    fun getAllPostsList(): List<PostEntity>

    @Query("SELECT * FROM posts ORDER BY created_utc DESC LIMIT :limit OFFSET :offset")
    fun getPaginatedPosts(limit: Int, offset: Int): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE subreddit = :subreddit ORDER BY created_utc DESC")
    fun getPostsBySubreddit(subreddit: String): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(posts: List<PostEntity>)

    @Update
    fun update(post: PostEntity)

    @Delete
    fun delete(post: PostEntity)

    @Query("DELETE FROM posts")
    fun deleteAll()

    @Query("SELECT COUNT(*) FROM posts")
    fun getPostCount(): Int

    @Query("DELETE FROM posts WHERE created_utc < :timestamp")
    fun deleteOldPosts(timestamp: Long): Int
}
