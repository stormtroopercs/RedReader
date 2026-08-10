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

@Dao
interface CommentDao {

    @Query("SELECT * FROM comments WHERE id = :id")
    fun getCommentById(id: String): CommentEntity?

    @Query("SELECT * FROM comments")
    fun getAllComments(): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE link_id = :postId ORDER BY created_utc ASC")
    fun getCommentsByPost(postId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(comments: List<CommentEntity>)

    @Update
    fun update(comment: CommentEntity)

    @Delete
    fun delete(comment: CommentEntity)

    @Query("DELETE FROM comments")
    fun deleteAll()

    @Query("DELETE FROM comments WHERE link_id = :postId")
    fun deleteCommentsForPost(postId: String): Int
}