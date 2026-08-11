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
 * Room entity for caching Reddit posts locally.
 * Enables offline reading and faster subsequent loads.
 */
@Entity(
    tableName = "posts",
    indices = [Index(value = ["id"], unique = true)]
)
data class PostEntity(
    @PrimaryKey
    val id: String,
    val title: String?,
    val author: String?,
    val subreddit: String,
    val score: Int,
    val numComments: Int,
    val url: String?,
    val permalink: String,
    val isSelf: Boolean,
    val isOver18: Boolean,
    val isSpoiler: Boolean,
    val isStickied: Boolean,
    val isLocked: Boolean,
    val linkFlairText: String?,
    val authorFlairText: String?,
    val thumbnail: String?,
    val selftext: String?,
    val createdUtc: Long,
    val cachedAt: Long = System.currentTimeMillis()
)
