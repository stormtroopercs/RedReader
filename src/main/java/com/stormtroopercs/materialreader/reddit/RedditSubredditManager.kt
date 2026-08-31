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
package com.stormtroopercs.materialreader.reddit

import android.content.Context
import com.stormtroopercs.materialreader.account.RedditAccount
import com.stormtroopercs.materialreader.common.General.sha1
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.TimestampBound
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.io.RawObjectDB
import com.stormtroopercs.materialreader.io.RequestResponseHandler
import com.stormtroopercs.materialreader.io.ThreadedRawObjectDB
import com.stormtroopercs.materialreader.io.UpdatedVersionListener
import com.stormtroopercs.materialreader.io.WeakCache
import com.stormtroopercs.materialreader.reddit.api.RedditAPIIndividualSubredditDataRequester
import com.stormtroopercs.materialreader.reddit.things.RedditSubreddit
import com.stormtroopercs.materialreader.reddit.things.SubredditCanonicalId

/**
 * Per-user subreddit manager. Original Java used a static per-user singleton
 * (`getInstance(context, user)`) — restored here so legacy call sites keep
 * working.
 */
class RedditSubredditManager private constructor(
	private val context: Context,
	private val user: RedditAccount,
) {
	private val subredditCache: WeakCache<SubredditCanonicalId, RedditSubreddit, RRError>

	init {
		// Subreddit cache

		val subredditDb = RawObjectDB<SubredditCanonicalId, RedditSubreddit>(
			context,
			getDbFilename("subreddits", user),
			RedditSubreddit::class.java,
		)

		val subredditDbWrapper = ThreadedRawObjectDB<SubredditCanonicalId, RedditSubreddit, RRError>(
			subredditDb,
			RedditAPIIndividualSubredditDataRequester(context, user),
		)

		subredditCache = WeakCache<SubredditCanonicalId, RedditSubreddit, RRError>(subredditDbWrapper)
	}
	fun offerRawSubredditData(
		toWrite: MutableCollection<RedditSubreddit>,
		timestamp: TimestampUTC,
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
		DEFAULTS,
	}

	fun getSubreddit(
		subredditCanonicalId: SubredditCanonicalId,
		timestampBound: TimestampBound,
		handler: RequestResponseHandler<RedditSubreddit, RRError>,
		updatedVersionListener: UpdatedVersionListener<SubredditCanonicalId, RedditSubreddit>?,
	) {
		subredditCache.performRequest(
			subredditCanonicalId,
			timestampBound,
			handler,
			updatedVersionListener,
		)
	}

	fun getSubreddits(
		ids: MutableCollection<SubredditCanonicalId>,
		timestampBound: TimestampBound,
		handler: RequestResponseHandler<HashMap<SubredditCanonicalId, RedditSubreddit>, RRError>,
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

		private fun getDbFilename(type: String?, user: RedditAccount): String = sha1(user.username.toByteArray()) + "_" + type + "_subreddits.db"
	}
}
