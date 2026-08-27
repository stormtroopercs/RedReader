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
package org.quantumbadger.redreader.reddit

import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.common.Constants.Reddit
import org.quantumbadger.redreader.reddit.things.SubredditCanonicalId
import java.util.Collections

// Keeps an in-memory list of all known subreddits per account
object RedditSubredditHistory {
    @Suppress("PropertyName")
    private val SUBREDDITS = HashMap<RedditAccount, HashSet<SubredditCanonicalId>>()

    private fun getForAccount(account: RedditAccount): HashSet<SubredditCanonicalId> {
        var result = SUBREDDITS.get(account)

        if (result == null) {
            result = HashSet<SubredditCanonicalId>(Reddit.DEFAULT_SUBREDDITS)
            SUBREDDITS.put(account, result)
        }

        return result
    }

    @Synchronized
    fun addSubreddit(
        account: RedditAccount,
        id: SubredditCanonicalId
    ) {
        getForAccount(account).add(id)
    }

    @Synchronized
    fun addSubreddits(
        account: RedditAccount,
        ids: MutableCollection<SubredditCanonicalId>
    ) {
        getForAccount(account).addAll(ids)
    }

    @Synchronized
    fun getSubredditsSorted(
        account: RedditAccount
    ): ArrayList<SubredditCanonicalId> {
        val result = ArrayList<SubredditCanonicalId>(
            getForAccount(
                account
            )
        )
        Collections.sort<SubredditCanonicalId>(result)
        return result
    }
}
