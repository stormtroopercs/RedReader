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
 * Room entity for caching user session information locally.
 * Enables quick account switching and offline account identification.
 */
@Entity(
    tableName = "user_sessions",
    indices = [Index(value = ["accountId"], unique = true)]
)
data class UserSessionEntity(
    @PrimaryKey
    val accountId: String,
    val username: String?,
    val karma: Int?,
    val iconUrl: String?,
    val isGold: Boolean,
    val isMod: Boolean,
    val lastAccessed: Long = System.currentTimeMillis()
)
