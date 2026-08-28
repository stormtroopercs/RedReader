/*******************************************************************************
 * This file is part of MaterialReader.
 *
 * MaterialReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MaterialReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MaterialReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.stormtroopercs.materialreader.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for caching subreddit information locally.
 * Enables faster subreddit resolution and offline subscription support.
 */
@Entity(
    tableName = "subreddits",
    indices = [Index(value = ["name"], unique = true)]
)
data class SubredditEntity(
    @PrimaryKey
    val name: String,
    val displayName: String?,
    val subscribers: Int?,
    val description: String?,
    val iconUrl: String?,
    val headerUrl: String?,
    val createdUtc: Long?,
    val lastUpdated: Long = System.currentTimeMillis()
)
