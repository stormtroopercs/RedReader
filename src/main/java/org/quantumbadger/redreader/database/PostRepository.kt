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
 * Repository for post caching and retrieval.
 * Wraps Room database operations for posts.
 */
@ViewModelScoped
class PostRepository @Inject constructor(
    private val postDao: PostDao
) {

    val allPosts: Flow<List<PostEntity>> = postDao.getAllPosts()

    fun getPostById(id: String): PostEntity? = postDao.getPostById(id)

    suspend fun insertPost(post: PostEntity) = postDao.insertPost(post)

    suspend fun insertPosts(posts: List<PostEntity>) = postDao.insertPosts(posts)

    suspend fun updatePost(post: PostEntity) = postDao.updatePost(post)

    suspend fun deletePost(post: PostEntity) = postDao.deletePost(post)

    suspend fun deleteAllPosts() = postDao.deleteAllPosts()
}
