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
 * along with RedReader.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */
package org.quantumbadger.redreader.account

import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.reddit.api.RedditOAuth.AccessToken
import org.quantumbadger.redreader.reddit.api.RedditOAuth.RefreshToken

class RedditAccount(
    username: String,
    refreshToken: RefreshToken?,
    priority: Long,
    clientId: String?
) {
    @JvmField
    val username: String
    @JvmField
    val canonicalUsername: String
    @JvmField
    val refreshToken: RefreshToken?

    @get:Synchronized
    var mostRecentAccessToken: AccessToken? = null
        private set

    @JvmField
    val priority: Long
    @JvmField
    val clientId: String?

    init {
        if (username == null) {
            throw RuntimeException("Null user in RedditAccount")
        }

        this.username = username.trim { it <= ' ' }
        this.canonicalUsername = StringUtils.asciiLowercase(this.username)
        this.refreshToken = refreshToken
        this.priority = priority
        this.clientId = clientId
    }

    val isAnonymous: Boolean
        get() = username.isEmpty()

    val isNotAnonymous: Boolean
        get() = !this.isAnonymous

    @Synchronized
    fun setAccessToken(token: AccessToken?) {
        this.mostRecentAccessToken = token
    }

    override fun equals(o: Any?): Boolean {
        if (o !is RedditAccount) {
            return false
        }

        val other = o

        return canonicalUsername.equals(other.canonicalUsername, ignoreCase = true)
                && clientId == other.clientId
                && refreshToken == other.refreshToken
    }

    override fun hashCode(): Int {
        return this.canonicalUsername.hashCode()
    }
}
