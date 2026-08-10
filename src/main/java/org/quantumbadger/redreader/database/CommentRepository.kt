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

import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Repository for comment caching and retrieval.
 * Wraps Room database operations for comments.
 */
@ViewModelScoped
class CommentRepository @Inject constructor(
    private val commentDao: CommentDao
) {

    val allComments: Flow<List<CommentEntity>> = commentDao.getAllComments()

    fun getCommentById(id: String): CommentEntity? = commentDao.getCommentById(id)

    suspend fun insertComment(comment: CommentEntity) = commentDao.insertComment(comment)

    suspend fun insertComments(comments: List<CommentEntity>) = commentDao.insertComments(comments)

    suspend fun updateComment(comment: CommentEntity) = commentDao.updateComment(comment)

    suspend fun deleteComment(comment: CommentEntity) = commentDao.deleteComment(comment)

    suspend fun deleteAllComments() = commentDao.deleteAllComments()
}
