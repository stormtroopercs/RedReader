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
import org.quantumbadger.redreader.database.dao.PostDao
import org.quantumbadger.redreader.database.entities.PostEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for Post data access.
 * Wraps PostDao and provides a clean API for post operations.
 * Supports both local caching and network synchronization.
 */
@Singleton
class PostRepository @Inject constructor(
    private val postDao: PostDao
) {

    /**
     * Flow of all posts in the database, ordered by creation time.
     */
    val allPosts: Flow<List<PostEntity>> = postDao.getAllPosts()

    /**
     * Get all posts for a specific subreddit.
     */
    fun getPostsBySubreddit(subreddit: String): Flow<List<PostEntity>> {
        return postDao.getPostsBySubreddit(subreddit)
    }

    /**
     * Get all posts by a specific author.
     */
    fun getPostsByAuthor(author: String): Flow<List<PostEntity>> {
        return postDao.getPostsByAuthor(author)
    }

    /**
     * Get a single post by ID.
     */
    suspend fun getPostById(id: String): PostEntity? {
        return postDao.getPostById(id)
    }

    /**
     * Insert a single post into the database.
     */
    suspend fun insertPost(post: PostEntity) {
        postDao.insertPost(post)
    }

    /**
     * Insert multiple posts into the database.
     */
    suspend fun insertPosts(posts: List<PostEntity>) {
        postDao.insertPosts(posts)
    }

    /**
     * Update an existing post.
     */
    suspend fun updatePost(post: PostEntity) {
        postDao.updatePost(post)
    }

    /**
     * Delete a single post.
     */
    suspend fun deletePost(post: PostEntity) {
        postDao.deletePost(post)
    }

    /**
     * Delete all posts for a specific subreddit.
     */
    suspend fun deletePostsBySubreddit(subreddit: String) {
        postDao.deletePostsBySubreddit(subreddit)
    }

    /**
     * Delete all posts from the database.
     */
    suspend fun deleteAllPosts() {
        postDao.deleteAllPosts()
    }

    /**
     * Get the total number of posts in the database.
     */
    val postCount: Flow<Int> = postDao.getPostCount()
}
