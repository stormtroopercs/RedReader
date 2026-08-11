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

package org.quantumbadger.redreader.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for caching Reddit comments locally.
 * Supports threaded comment display and offline reading.
 */
@Entity(
    tableName = "comments",
    indices = [Index(value = ["id"], unique = true)]
)
data class CommentEntity(
    @PrimaryKey
    val id: String,
    val postId: String,
    val parentId: String?,
    val author: String?,
    val body: String?,
    val score: Int,
    val createdUtc: Long,
    val permalink: String,
    val isSubmitter: Boolean,
    val isStickied: Boolean,
    val isLocked: Boolean,
    val depth: Int = 0,
    val cachedAt: Long = System.currentTimeMillis()
)
