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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for user session caching.
 * Provides CRUD operations for UserSessionEntity.
 */
@Dao
interface UserSessionDao {

    @Query("SELECT * FROM user_sessions WHERE name = :name")
    fun getUserSessionByName(name: String): UserSessionEntity?

    @Query("SELECT * FROM user_sessions")
    fun getAllUserSessions(): Flow<List<UserSessionEntity>>

    @Query("SELECT * FROM user_sessions")
    fun getAllUserSessionsSync(): List<UserSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSession(userSession: UserSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSessions(userSessions: List<UserSessionEntity>)

    @Update
    suspend fun updateUserSession(userSession: UserSessionEntity)

    @Query("DELETE FROM user_sessions WHERE name = :name")
    suspend fun deleteUserSessionByName(name: String)

    @Query("DELETE FROM user_sessions")
    suspend fun deleteAllUserSessions()

    @Query("SELECT EXISTS(SELECT 1 FROM user_sessions WHERE name = :name)")
    suspend fun exists(name: String): Boolean
}
