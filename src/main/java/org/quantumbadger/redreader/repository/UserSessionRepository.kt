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
import org.quantumbadger.redreader.database.dao.UserSessionDao
import org.quantumbadger.redreader.database.entities.UserSessionEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for UserSession data access.
 * Wraps UserSessionDao and provides a clean API for session operations.
 * Supports offline account identification and quick account switching.
 */
@Singleton
class UserSessionRepository @Inject constructor(
    private val userSessionDao: UserSessionDao
) {

    /**
     * Flow of all user sessions in the database, ordered by last access.
     */
    val allSessions: Flow<List<UserSessionEntity>> = userSessionDao.getAllSessions()

    /**
     * Get a single user session by account ID.
     */
    suspend fun getSessionByAccountId(accountId: String): UserSessionEntity? {
        return userSessionDao.getSessionByAccountId(accountId)
    }

    /**
     * Get a single user session by username.
     */
    suspend fun getSessionByUsername(username: String): UserSessionEntity? {
        return userSessionDao.getSessionByUsername(username)
    }

    /**
     * Insert a single user session into the database.
     */
    suspend fun insertSession(session: UserSessionEntity) {
        userSessionDao.insertSession(session)
    }

    /**
     * Insert multiple user sessions into the database.
     */
    suspend fun insertSessions(sessions: List<UserSessionEntity>) {
        userSessionDao.insertSessions(sessions)
    }

    /**
     * Update an existing user session.
     */
    suspend fun updateSession(session: UserSessionEntity) {
        userSessionDao.updateSession(session)
    }

    /**
     * Delete a single user session.
     */
    suspend fun deleteSession(session: UserSessionEntity) {
        userSessionDao.deleteSession(session)
    }

    /**
     * Delete all user sessions from the database.
     */
    suspend fun deleteAllSessions() {
        userSessionDao.deleteAllSessions()
    }

    /**
     * Get the total number of user sessions in the database.
     */
    val sessionCount: Flow<Int> = userSessionDao.getSessionCount()
}
