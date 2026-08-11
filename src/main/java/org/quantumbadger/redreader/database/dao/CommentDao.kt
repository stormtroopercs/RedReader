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
import org.quantumbadger.redreader.database.entities.CommentEntity

/**
 * Data Access Object for CommentEntity operations.
 * Provides CRUD operations and Flow-based observation for reactive UI updates.
 */
@Dao
interface CommentDao {

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY depth ASC, createdUtc ASC")
    fun getCommentsByPost(postId: String): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE id = :id LIMIT 1")
    suspend fun getCommentById(id: String): CommentEntity?

    @Query("SELECT * FROM comments WHERE parentId = :parentId ORDER BY createdUtc ASC")
    fun getCommentsByParent(parentId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CommentEntity>)

    @Update
    suspend fun updateComment(comment: CommentEntity)

    @Delete
    suspend fun deleteComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE postId = :postId")
    suspend fun deleteCommentsByPost(postId: String)

    @Query("DELETE FROM comments")
    suspend fun deleteAllComments()

    @Query("SELECT COUNT(*) FROM comments")
    fun getCommentCount(): Flow<Int>
}
