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

package org.quantumbadger.redreader.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.quantumbadger.redreader.database.entities.UserSessionEntity

/**
 * Data Access Object for UserSessionEntity operations.
 * Provides CRUD operations and Flow-based observation for reactive UI updates.
 */
@Dao
interface UserSessionDao {

    @Query("SELECT * FROM user_sessions ORDER BY lastAccessed DESC")
    fun getAllSessions(): Flow<List<UserSessionEntity>>

    @Query("SELECT * FROM user_sessions WHERE accountId = :accountId LIMIT 1")
    suspend fun getSessionByAccountId(accountId: String): UserSessionEntity?

    @Query("SELECT * FROM user_sessions WHERE username = :username LIMIT 1")
    suspend fun getSessionByUsername(username: String): UserSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UserSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<UserSessionEntity>)

    @Update
    suspend fun updateSession(session: UserSessionEntity)

    @Delete
    suspend fun deleteSession(session: UserSessionEntity)

    @Query("DELETE FROM user_sessions")
    suspend fun deleteAllSessions()

    @Query("SELECT COUNT(*) FROM user_sessions")
    fun getSessionCount(): Flow<Int>
}
