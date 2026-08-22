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

package org.quantumbadger.redreader.activities

import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.AndroidEntryPoint
import org.quantumbadger.redreader.compose.activity.ComposeBaseActivity
import org.quantumbadger.redreader.navigation.Album
import org.quantumbadger.redreader.navigation.AppNavGraph
import org.quantumbadger.redreader.navigation.Changelog
import org.quantumbadger.redreader.navigation.CommentList
import org.quantumbadger.redreader.navigation.CommentReply
import org.quantumbadger.redreader.navigation.Inbox
import org.quantumbadger.redreader.navigation.Main
import org.quantumbadger.redreader.navigation.NavigationState
import org.quantumbadger.redreader.navigation.Navigator
import org.quantumbadger.redreader.navigation.PostList
import org.quantumbadger.redreader.navigation.RedditTerms
import org.quantumbadger.redreader.navigation.Settings
import org.quantumbadger.redreader.navigation.SubredditSearch
import org.quantumbadger.redreader.navigation.TOP_LEVEL_ROUTES
import org.quantumbadger.redreader.navigation.UserProfile

/**
 * Compose-based MainActivity using Navigation 3.
 * Replaces the legacy Fragment-based MainActivity.
 *
 * The [NavigationState] is owned by the Activity (not the composition) so the
 * system back button can pop it: Navigation 3 at these androidx versions does
 * not expose a back API to the Activity, so back navigation is driven through
 * [baseActivityOnBackPressed].
 */
@AndroidEntryPoint
class MainActivityCompose : ComposeBaseActivity() {

    private val navigationState = NavigationState(
        startRoute = Main,
        topLevelRoute = mutableStateOf(Main),
        backStacks = TOP_LEVEL_ROUTES.associateWith { key ->
            mutableStateListOf<NavKey>(key)
        }
    )

    private val navigator = Navigator(navigationState)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cold-start deep link: e.g. the new-message notification opens the app
        // directly on the Compose inbox (Main top level + Inbox child).
        if (savedInstanceState == null) {
            intent?.getStringExtra(EXTRA_DEEP_LINK)
                ?.let { deepLinkRoute(it) }
        }

        setContentCompose {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AppNavGraph(navigationState)
            }
        }
    }

    /**
     * Route system back into the Navigation 3 back stack. Consumes the press
     * while there is anything to go back to; at the root it falls through to
     * the default behaviour (finish the activity). This is the back path on
     * every API level, since the legacy OnBackPressedCallback (see
     * BaseActivity) is the registered handler for the system back button.
     */
    override fun baseActivityOnBackPressed(): Boolean {
        if (navigationState.canGoBack()) {
            navigator.goBack()
            return true
        }
        return false
    }

    override fun baseActivityMustInterceptBack(): Boolean {
        return navigationState.canGoBack()
    }

    /**
     * Handle a cold-start deep-link extra. Currently the inbox, changelog,
     * subreddit search and album are wired: a notification tap opens the Compose
     * inbox (Main + Inbox), the legacy settings' changelog link opens the
     * Compose changelog (Settings + Changelog, so back returns to settings),
     * the legacy main-menu search item opens the Compose subreddit search
     * (Main + SubredditSearch), and a tapped album/gallery link opens the
     * Compose album (Main + Album(url), with the URL in [EXTRA_ALBUM_URL]).
     * Unknown routes fall back to the default main screen.
     */
    private fun deepLinkRoute(route: String) {
        when (route) {
            DEEP_LINK_INBOX -> navigationState.navigateTo(Main, Inbox)
            DEEP_LINK_CHANGELOG -> navigationState.navigateTo(Settings, Changelog)
            DEEP_LINK_SEARCH -> navigationState.navigateTo(Main, SubredditSearch)
            DEEP_LINK_ALBUM -> {
                val albumUrl = intent?.getStringExtra(EXTRA_ALBUM_URL)
                if (albumUrl != null) {
                    navigationState.navigateTo(Main, Album(albumUrl))
                }
            }
            DEEP_LINK_COMMENT_REPLY -> {
                val idAndType = intent?.getStringExtra(EXTRA_COMMENT_REPLY_ID_AND_TYPE)
                if (idAndType != null) {
                    val (postId, commentId) = parseCommentReplyIds(idAndType)
                    navigationState.navigateTo(Main, CommentReply(postId, commentId))
                }
            }
            DEEP_LINK_TERMS -> {
                navigationState.navigateTo(Settings, RedditTerms)
            }
            DEEP_LINK_POST_LISTING -> {
                val subreddit = intent?.getStringExtra(EXTRA_POST_LISTING_SUBREDDIT)
                if (subreddit != null) {
                    navigationState.navigateTo(Main, PostList(subreddit))
                }
            }
            DEEP_LINK_COMMENT_LISTING -> {
                val listingPath = intent?.getStringExtra(EXTRA_COMMENT_LISTING_POST_ID)
                if (listingPath != null) {
                    navigationState.navigateTo(Main, CommentList(listingPath))
                }
            }
            DEEP_LINK_USER_PROFILE -> {
                val username = intent?.getStringExtra(EXTRA_USER_PROFILE_USERNAME)
                if (username != null) {
                    navigationState.navigateTo(Main, UserProfile(username))
                }
            }
        }
    }

    /**
     * Split a Reddit id-and-type string into the [CommentReply] route's
     * (postId, commentId) parameters. "t3_<id>" (a post) yields that post id
     * with a null comment id; "t1_<postId>_<commentId>" (a comment) yields
     * "t3_<postId>" as the post id and the comment id, since Reddit comment
     * ids nest under the post id.
     */
    private fun parseCommentReplyIds(idAndType: String): Pair<String, String?> {
        if (idAndType.startsWith("t1_")) {
            val commentId = idAndType.removePrefix("t1_")
            val underscore = commentId.lastIndexOf('_')
            val postId = if (underscore > 0) "t3_" + commentId.substring(0, underscore) else ""
            return postId to commentId
        }
        return idAndType to null
    }

    companion object {
        /** Intent extra carrying a cold-start deep-link route name. */
        const val EXTRA_DEEP_LINK = "org.quantumbadger.redreader.extra.DEEP_LINK"

        /** Intent extra carrying the album URL for the album deep link. */
        const val EXTRA_ALBUM_URL = "org.quantumbadger.redreader.extra.ALBUM_URL"

        /** Deep-link route: the inbox (Main top level + Inbox child). */
        const val DEEP_LINK_INBOX = "inbox"

        /** Deep-link route: the changelog (Settings top level + Changelog child). */
        const val DEEP_LINK_CHANGELOG = "changelog"

        /** Deep-link route: subreddit search (Main top level + SubredditSearch child). */
        const val DEEP_LINK_SEARCH = "search"

        /** Deep-link route: an album/gallery (Main top level + Album child). */
        const val DEEP_LINK_ALBUM = "album"

        /** Intent extra carrying the comment id-and-type for the comment-reply deep link. */
        const val EXTRA_COMMENT_REPLY_ID_AND_TYPE =
            "org.quantumbadger.redreader.extra.COMMENT_REPLY_ID_AND_TYPE"

        /** Deep-link route: reply to a post or comment (Main top level + CommentReply child). */
        const val DEEP_LINK_COMMENT_REPLY = "comment_reply"

        /** Deep-link route: Reddit terms of service (Settings top level + RedditTerms child). */
        const val DEEP_LINK_TERMS = "terms"

        /** Intent extra carrying the listing path for the post-listing deep link (a subreddit name, or frontpage / popular / all / u/<user>/submitted). */
        const val EXTRA_POST_LISTING_SUBREDDIT =
            "org.quantumbadger.redreader.extra.POST_LISTING_SUBREDDIT"

        /** Deep-link route: a post listing (Main top level + PostList child). */
        const val DEEP_LINK_POST_LISTING = "post_listing"

        /** Intent extra carrying the listing path for the comment-listing deep link (a post id, or u/<user>/comments). */
        const val EXTRA_COMMENT_LISTING_POST_ID =
            "org.quantumbadger.redreader.extra.COMMENT_LISTING_POST_ID"

        /** Deep-link route: a comment listing (Main top level + CommentList child). */
        const val DEEP_LINK_COMMENT_LISTING = "comment_listing"

        /** Intent extra carrying the username for the user-profile deep link. */
        const val EXTRA_USER_PROFILE_USERNAME =
            "org.quantumbadger.redreader.extra.USER_PROFILE_USERNAME"

        /** Deep-link route: a user profile (Main top level + UserProfile child). */
        const val DEEP_LINK_USER_PROFILE = "user_profile"
    }
}
