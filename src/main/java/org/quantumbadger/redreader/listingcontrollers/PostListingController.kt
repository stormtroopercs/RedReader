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
package org.quantumbadger.redreader.listingcontrollers

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.fragments.PostListingFragment
import org.quantumbadger.redreader.reddit.PostSort
import org.quantumbadger.redreader.reddit.things.InvalidSubredditNameException
import org.quantumbadger.redreader.reddit.things.SubredditCanonicalId
import org.quantumbadger.redreader.reddit.url.PostListingURL
import org.quantumbadger.redreader.reddit.url.RedditURLParser
import org.quantumbadger.redreader.reddit.url.SubredditPostListURL
import org.quantumbadger.redreader.reddit.url.UserPostListingURL
import java.util.UUID
import android.net.Uri

// TODO add notification/header for abnormal sort order
class PostListingController(url: PostListingURL, context: Context?) {
    var session: UUID?=null
    private var url: PostListingURL

    init {
        var url = url
        if (url.pathType() == RedditURLParser.SUBREDDIT_POST_LISTING_URL) {
            if (url.asSubredditPostListURL().order == null) {
                var order = PrefsUtility.pref_behaviour_postsort()

                if (order == PostSort.BEST
                    && (url.asSubredditPostListURL().type
                            != SubredditPostListURL.Type.FRONTPAGE)
                ) {
                    order = PostSort.HOT
                }

                url = url.asSubredditPostListURL().sort(order)
            }
        } else if (url.pathType() == RedditURLParser.USER_POST_LISTING_URL) {
            if (url.asUserPostListURL().order == null) {
                url = url.asUserPostListURL().sort(PrefsUtility.pref_behaviour_user_postsort())
            }
        } else if (url.pathType() == RedditURLParser.MULTIREDDIT_POST_LISTING_URL) {
            if (url.asMultiredditPostListURL().order == null) {
                url = url.asMultiredditPostListURL()
                    .sort(PrefsUtility.pref_behaviour_multi_postsort())
            }
        }

        this.url = url
    }

    val isSortable: Boolean
        get() {
            if (url.pathType() == RedditURLParser.USER_POST_LISTING_URL) {
                return (url.asUserPostListURL().type == UserPostListingURL.Type.SUBMITTED)
            }
            return (url.pathType() == RedditURLParser.SUBREDDIT_POST_LISTING_URL)
                    || (url.pathType() == RedditURLParser.MULTIREDDIT_POST_LISTING_URL)
                    || (url.pathType() == RedditURLParser.SEARCH_POST_LISTING_URL)
        }

    val isFrontPage: Boolean
        get() = url.pathType() == RedditURLParser.SUBREDDIT_POST_LISTING_URL
                && (url.asSubredditPostListURL().type
                == SubredditPostListURL.Type.FRONTPAGE)

    var sort: PostSort?
        get() {
            if (url.pathType() == RedditURLParser.SUBREDDIT_POST_LISTING_URL) {
                return url.asSubredditPostListURL().order
            }

            if (url.pathType() == RedditURLParser.MULTIREDDIT_POST_LISTING_URL) {
                return url.asMultiredditPostListURL().order
            }

            if (url.pathType() == RedditURLParser.SEARCH_POST_LISTING_URL) {
                return url.asSearchPostListURL().order
            }

            if (url.pathType() == RedditURLParser.USER_POST_LISTING_URL) {
                return url.asUserPostListURL().order
            }

            return null
        }
        set(order) {
            if (url.pathType() == RedditURLParser.SUBREDDIT_POST_LISTING_URL) {
                url = url.asSubredditPostListURL().sort(order)
            } else if (url.pathType() == RedditURLParser.MULTIREDDIT_POST_LISTING_URL) {
                url = url.asMultiredditPostListURL().sort(order)
            } else if (url.pathType() == RedditURLParser.SEARCH_POST_LISTING_URL) {
                url = url.asSearchPostListURL().sort(order)
            } else if (url.pathType() == RedditURLParser.USER_POST_LISTING_URL) {
                url = url.asUserPostListURL().sort(order)
            } else {
                throw RuntimeException("Cannot set sort for this URL")
            }
        }

    val uri: Uri
        get() = url.generateJsonUri()!!

    fun get(
        parent: AppCompatActivity,
        force: Boolean,
        savedInstanceState: Bundle?
    ): PostListingFragment {
        if (force) {
            session = null
        }
        return PostListingFragment(
            parent,
            savedInstanceState,
            this.uri,
            session,
            force
        )
    }

    val isSubreddit: Boolean
        get() = url.pathType() == RedditURLParser.SUBREDDIT_POST_LISTING_URL
                && (url.asSubredditPostListURL().type
                == SubredditPostListURL.Type.SUBREDDIT)

    val isSubredditCombination: Boolean
        get() = url.pathType() == RedditURLParser.SUBREDDIT_POST_LISTING_URL
                && (url.asSubredditPostListURL().type
                == SubredditPostListURL.Type.SUBREDDIT_COMBINATION)

    val isMultireddit: Boolean
        get() = url.pathType() == RedditURLParser.MULTIREDDIT_POST_LISTING_URL

    val isSearchResults: Boolean
        get() = url.pathType() == RedditURLParser.SEARCH_POST_LISTING_URL

    val isSubredditSearchResults: Boolean
        get() = this.isSearchResults && url.asSearchPostListURL().subreddit != null

    val isUserPostListing: Boolean
        get() = url.pathType() == RedditURLParser.USER_POST_LISTING_URL

    fun subredditCanonicalName(): SubredditCanonicalId? {
        if (url.pathType() == RedditURLParser.SUBREDDIT_POST_LISTING_URL
            && ((url.asSubredditPostListURL().type
                    == SubredditPostListURL.Type.SUBREDDIT)
                    || (url.asSubredditPostListURL().type
                    == SubredditPostListURL.Type.SUBREDDIT_COMBINATION))
        ) {
            try {
                return SubredditCanonicalId(url.asSubredditPostListURL().subreddit!!)
            } catch (e: InvalidSubredditNameException) {
                throw RuntimeException(e)
            }
        } else if (url.pathType() == RedditURLParser.SEARCH_POST_LISTING_URL
            && url.asSearchPostListURL().subreddit != null
        ) {
            try {
                return SubredditCanonicalId(url.asSearchPostListURL().subreddit!!)
            } catch (e: InvalidSubredditNameException) {
                throw RuntimeException(e)
            }
        }

        return null
    }

    fun multiredditName(): String? {
        if (url.pathType() == RedditURLParser.MULTIREDDIT_POST_LISTING_URL) {
            return url.asMultiredditPostListURL().name
        }

        return null
    }

    fun multiredditUsername(): String? {
        if (url.pathType() == RedditURLParser.MULTIREDDIT_POST_LISTING_URL) {
            return url.asMultiredditPostListURL().username
        }

        return null
    }
}
