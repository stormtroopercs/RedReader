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
package org.quantumbadger.redreader.reddit

import android.content.Context
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.FragmentComponent
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import javax.inject.Singleton
import javax.inject.Named
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.common.General.sha1
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.TimestampBound
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.io.RawObjectDB
import org.quantumbadger.redreader.io.RequestResponseHandler
import org.quantumbadger.redreader.io.ThreadedRawObjectDB
import org.quantumbadger.redreader.io.UpdatedVersionListener
import org.quantumbadger.redreader.io.WeakCache
import org.quantumbadger.redreader.reddit.api.RedditAPIIndividualSubredditDataRequester
import org.quantumbadger.redreader.reddit.things.RedditSubreddit
import org.quantumbadger.redreader.reddit.things.SubredditCanonicalId
import javax.inject.Provider
import org.quantumbadger.redreader.common.General

/**
 * Per-user subreddit manager. Original Java used a static per-user singleton
 * (`getInstance(context, user)`) — restored here so legacy call sites keep
 * working.
 */
class RedditSubredditManager private constructor(
    private val context: Context,
    private val user: RedditAccount
) {
    private val subredditCache: WeakCache<SubredditCanonicalId, RedditSubreddit, RRError>

    init {
        // Subreddit cache

        val subredditDb = RawObjectDB<SubredditCanonicalId, RedditSubreddit>(
            context,
            getDbFilename("subreddits", user),
            RedditSubreddit::class.java
        )

        val subredditDbWrapper =             ThreadedRawObjectDB<SubredditCanonicalId, RedditSubreddit, RRError>(
                subredditDb,
                RedditAPIIndividualSubredditDataRequester(context, user)
            )

        subredditCache =             WeakCache<SubredditCanonicalId, RedditSubreddit, RRError>(subredditDbWrapper)
    }
    fun offerRawSubredditData(
        toWrite: MutableCollection<RedditSubreddit>,
        timestamp: TimestampUTC
    ) {
        subredditCache.performWrite(toWrite)
    }

    // TODO need way to cancel web update and start again?
    // TODO anonymous user
    // TODO Ability to temporarily flag subreddits as subscribed/unsubscribed
    // TODO Ability to temporarily add/remove subreddits from multireddits
    // TODO store favourites in preference
    enum class SubredditListType {
        SUBSCRIBED,
        MODERATED,
        MULTIREDDITS,
        MOST_POPULAR,
        DEFAULTS
    }

    fun getSubreddit(
        subredditCanonicalId: SubredditCanonicalId,
        timestampBound: TimestampBound,
        handler: RequestResponseHandler<RedditSubreddit, RRError>,
        updatedVersionListener: UpdatedVersionListener<SubredditCanonicalId, RedditSubreddit>?
    ) {
        subredditCache.performRequest(
            subredditCanonicalId,
            timestampBound,
            handler,
            updatedVersionListener
        )
    }

    fun getSubreddits(
        ids: MutableCollection<SubredditCanonicalId>,
        timestampBound: TimestampBound,
        handler: RequestResponseHandler<HashMap<SubredditCanonicalId, RedditSubreddit>, RRError>
    ) {
        subredditCache.performRequest(ids, timestampBound, handler)
    }

    companion object {
        @Volatile
        private var singleton: RedditSubredditManager? = null

        @Volatile
        private var singletonUser: RedditAccount? = null

        /**
         * Per-user singleton, matching the original Java static accessor.
         */
        @JvmStatic
        @Synchronized
        fun getInstance(context: Context, user: RedditAccount): RedditSubredditManager {
            val current = singleton
            val currentUser = singletonUser
            if (current == null || current.user !== user || currentUser != user) {
                singleton = RedditSubredditManager(context, user)
                singletonUser = user
            }

            return singleton!!
        }

        private fun getDbFilename(type: String?, user: RedditAccount): String {
            return sha1(user.username.toByteArray()) + "_" + type + "_subreddits.db"
        }
    }
}
