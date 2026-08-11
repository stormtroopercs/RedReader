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

package org.quantumbadger.redreader.repository

import kotlinx.coroutines.flow.Flow
import org.quantumbadger.redreader.database.dao.CommentDao
import org.quantumbadger.redreader.database.entities.CommentEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Comment data access.
 * Wraps CommentDao and provides a clean API for comment operations.
 * Supports both local caching and network synchronization.
 */
@Singleton
class CommentRepository @Inject constructor(
    private val commentDao: CommentDao
) {

    /**
     * Flow of all comments in the database, ordered by post and creation time.
     */
    val allComments: Flow<List<CommentEntity>> = commentDao.getCommentsByPost("")

    /**
     * Get all comments for a specific post.
     */
    fun getCommentsForPost(postId: String): Flow<List<CommentEntity>> {
        return commentDao.getCommentsByPost(postId)
    }

    /**
     * Get a single comment by ID.
     */
    suspend fun getCommentById(id: String): CommentEntity? {
        return commentDao.getCommentById(id)
    }

    /**
     * Get child comments for a specific parent comment.
     */
    fun getChildComments(parentId: String): Flow<List<CommentEntity>> {
        return commentDao.getCommentsByParent(parentId)
    }

    /**
     * Insert a single comment into the database.
     */
    suspend fun insertComment(comment: CommentEntity) {
        commentDao.insertComment(comment)
    }

    /**
     * Insert multiple comments into the database.
     */
    suspend fun insertComments(comments: List<CommentEntity>) {
        commentDao.insertComments(comments)
    }

    /**
     * Update an existing comment.
     */
    suspend fun updateComment(comment: CommentEntity) {
        commentDao.updateComment(comment)
    }

    /**
     * Delete a single comment.
     */
    suspend fun deleteComment(comment: CommentEntity) {
        commentDao.deleteComment(comment)
    }

    /**
     * Delete all comments for a specific post.
     */
    suspend fun deleteCommentsByPost(postId: String) {
        commentDao.deleteCommentsByPost(postId)
    }

    /**
     * Delete all comments from the database.
     */
    suspend fun deleteAllComments() {
        commentDao.deleteAllComments()
    }

    /**
     * Get the total number of comments in the database.
     */
    val commentCount: Flow<Int> = commentDao.getCommentCount()
}
