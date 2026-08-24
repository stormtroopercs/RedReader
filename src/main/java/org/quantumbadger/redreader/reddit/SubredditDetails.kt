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

import org.quantumbadger.redreader.common.HasUniqueId
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.reddit.things.InvalidSubredditNameException
import org.quantumbadger.redreader.reddit.things.RedditSubreddit
import org.quantumbadger.redreader.reddit.things.SubredditCanonicalId

class SubredditDetails : HasUniqueId {
    val id: SubredditCanonicalId
    val name: String
    val url: UriString
    val publicDescriptionHtmlEscaped: String?
    val subscribers: Int?

    constructor(subreddit: RedditSubreddit) {
        id = subreddit.canonicalId
        name = subreddit.display_name!!
        url = subreddit.getUrl()
        publicDescriptionHtmlEscaped = subreddit.public_description_html
        subscribers = subreddit.subscribers
    }

    constructor(subreddit: SubredditCanonicalId) {
        id = subreddit
        name = subreddit.displayNameLowercase
        url = UriString(subreddit.toString())
        publicDescriptionHtmlEscaped = null
        subscribers = null
    }

    override val uniqueId: String get() = id.toString()

    companion object {
        fun newWithRuntimeException(
            subreddit: RedditSubreddit
        ): SubredditDetails {
            try {
                return SubredditDetails(subreddit)
            } catch (e: InvalidSubredditNameException) {
                throw RuntimeException(e)
            }
        }
    }
}
