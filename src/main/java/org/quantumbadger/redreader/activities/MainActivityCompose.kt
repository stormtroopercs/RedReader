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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.AndroidEntryPoint
import org.quantumbadger.redreader.compose.activity.ComposeBaseActivity
import org.quantumbadger.redreader.common.LinkHandler
import org.quantumbadger.redreader.common.RunnableOnce
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.navigation.Album
import org.quantumbadger.redreader.navigation.AppNavGraph
import org.quantumbadger.redreader.navigation.BugReport
import org.quantumbadger.redreader.navigation.Changelog
import org.quantumbadger.redreader.navigation.CommentEdit
import org.quantumbadger.redreader.navigation.CommentList
import org.quantumbadger.redreader.navigation.CommentReply
import org.quantumbadger.redreader.navigation.HtmlView
import org.quantumbadger.redreader.navigation.HtmlViewBackHandler
import org.quantumbadger.redreader.navigation.Inbox
import org.quantumbadger.redreader.navigation.Image
import org.quantumbadger.redreader.navigation.Main
import org.quantumbadger.redreader.navigation.NavigationState
import org.quantumbadger.redreader.navigation.Navigator
import org.quantumbadger.redreader.navigation.PostList
import org.quantumbadger.redreader.navigation.PostSubmit
import org.quantumbadger.redreader.navigation.PMSend
import org.quantumbadger.redreader.navigation.RedditTerms
import org.quantumbadger.redreader.navigation.Settings
import org.quantumbadger.redreader.navigation.SubredditSearch
import org.quantumbadger.redreader.navigation.TOP_LEVEL_ROUTES
import org.quantumbadger.redreader.navigation.UserProfile
import org.quantumbadger.redreader.navigation.WebViewRoute
import org.quantumbadger.redreader.reddit.api.RedditOAuth

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

            // ACTION_SEND share launch: another app shares text to RedReader,
            // which opens the post form with the shared text pre-filled as the
            // link URL and the subreddit picker up front (the job the legacy
            // PostSubmitActivity did via its SEND intent-filter + subreddit
            // selection fragment).
            if (intent != null
                && android.content.Intent.ACTION_SEND.equals(intent.action, true)
                && intent.hasExtra(android.content.Intent.EXTRA_TEXT)
            ) {
                val sharedText = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)
                if (sharedText != null) {
                    navigationState.navigateTo(
                        Main,
                        PostSubmit("", sharedText)
                    )
                }
            }

            // ACTION_VIEW deep link: external reddit.com / redd.it / redreader://
            // links previously routed through the retired LinkDispatchActivity.
            // This launcher activity is now the direct handler: a redreader://
            // URI completes an OAuth login, everything else opens the link in-app
            // (LinkHandler starts the working Compose screen); then this instance
            // finishes, mirroring the standalone LinkDispatchActivity.
            if (intent != null
                && android.content.Intent.ACTION_VIEW.equals(intent.action, true)
                && intent.data != null
            ) {
                handleExternalViewIntent(intent)
            }
        }

        // Keep the platform back-callback's enabled state in sync with the
        // on-screen HtmlView route's WebView: when its document history
        // appears/vanishes (register/unregister/goBack), re-evaluate whether
        // the back key must be intercepted. Without this the callback would
        // be disabled while a child route or WebView history is on screen on
        // API 36+ (predictive back), and the OS would finish the activity
        // instead of popping the screen.
        HtmlViewBackHandler.onBackChanged = { invalidateBackPressedCallback() }

        // Deep-link handling above may already have moved the navigation past
        // the root before BaseActivity ran its initial callback evaluation, so
        // re-evaluate once with the up-to-date state (the WebView itself only
        // registers once its screen composes, below).
        invalidateBackPressedCallback()

        setContentCompose {
            // Keep the platform back-callback's enabled state in sync with the
            // Navigation 3 back stack while the composition is alive: pushes,
            // pops and top-level switches change canGoBack() through
            // composition, no code path re-evaluates the callback. Without
            // this, on API 36+ (predictive back) the callback would be left
            // disabled while a child screen is on screen, and the OS would
            // finish the activity instead of popping the screen.
            LaunchedEffect(Unit) {
                snapshotFlow {
                    navigationState.topLevelRoute to navigationState.activeBackStack.size
                }.collect { invalidateBackPressedCallback() }
            }
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
        // While the on-screen HtmlView route has a live WebView with
        // document history, walk that history first (legacy HtmlViewActivity
        // behaviour) before touching the Navigation 3 back stack.
        if (HtmlViewBackHandler.goBack()) {
            return true
        }
        if (navigationState.canGoBack()) {
            navigator.goBack()
            return true
        }
        return false
    }

    override fun baseActivityMustInterceptBack(): Boolean {
        // Keep the callback enabled while the WebView still has history to
        // walk, so the predictive-back path (API 36+) stays consistent too.
        return HtmlViewBackHandler.canGoBack || navigationState.canGoBack()
    }

    /**
     * Handle a cold-start deep-link extra. Currently the inbox, changelog,
     * settings root, subreddit search and album are wired: a notification tap
     * opens the Compose inbox (Main + Inbox), the legacy settings' changelog
     * link opens the Compose changelog (Settings + Changelog, so back returns
     * to settings), legacy hosts request the Compose settings root (Settings),
     * the legacy main-menu search item opens the Compose subreddit search
     * (Main + SubredditSearch), and a tapped album/gallery link opens the
     * Compose album (Main + Album(url), with the URL in [EXTRA_ALBUM_URL]).
     * Unknown routes fall back to the default main screen.
     */
    private fun deepLinkRoute(route: String) {
        when (route) {
            DEEP_LINK_INBOX -> navigationState.navigateTo(Main, Inbox)
            DEEP_LINK_CHANGELOG -> navigationState.navigateTo(Settings, Changelog)
            DEEP_LINK_SETTINGS -> navigationState.navigateTo(Settings)
            DEEP_LINK_SEARCH -> navigationState.navigateTo(Main, SubredditSearch)
            DEEP_LINK_ALBUM -> {
                val albumUrl = intent?.getStringExtra(EXTRA_ALBUM_URL)
                if (albumUrl != null) {
                    navigationState.navigateTo(Main, Album(albumUrl))
                }
            }
            DEEP_LINK_IMAGE -> {
                val imageUrl = intent?.getStringExtra(EXTRA_IMAGE_URL)
                if (imageUrl != null) {
                    val isGif = intent?.getBooleanExtra(EXTRA_IMAGE_GIF, false) ?: false
                    val isVideo = intent?.getBooleanExtra(EXTRA_IMAGE_VIDEO, false) ?: false
                    val albumUrl = intent?.getStringExtra(EXTRA_IMAGE_ALBUM_URL)
                    val albumIndex = intent?.getIntExtra(EXTRA_IMAGE_ALBUM_INDEX, 0) ?: 0
                    navigationState.navigateTo(
                        Main,
                        Image(imageUrl, isGif, isVideo, albumUrl, albumIndex)
                    )
                }
            }
            DEEP_LINK_COMMENT_REPLY -> {
                val idAndType = intent?.getStringExtra(EXTRA_COMMENT_REPLY_ID_AND_TYPE)
                if (idAndType != null) {
                    // The raw id-and-type *is* the reply's parent thing id:
                    // "t3_…" for a post, "t1_…" for a comment (both are the
                    // full id RedditAPI.comment posts against).
                    navigationState.navigateTo(Main, CommentReply(idAndType))
                }
            }
            DEEP_LINK_TERMS -> {
                navigationState.navigateTo(Settings, RedditTerms)
            }
            DEEP_LINK_POST_LISTING -> {
                val subreddit = intent?.getStringExtra(EXTRA_POST_LISTING_SUBREDDIT)
                if (subreddit != null) {
                    val searchQuery = intent?.getStringExtra(EXTRA_POST_LISTING_SEARCH_QUERY)
                    navigationState.navigateTo(Main, PostList(subreddit, searchQuery))
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
            DEEP_LINK_POST_SUBMIT -> {
                val subreddit = intent?.getStringExtra(EXTRA_POST_SUBMIT_SUBREDDIT)
                if (subreddit != null) {
                    val shareUrl = intent?.getStringExtra(EXTRA_POST_SUBMIT_SHARE_URL)
                    navigationState.navigateTo(Main, PostSubmit(subreddit, shareUrl))
                }
            }
            DEEP_LINK_COMMENT_EDIT -> {
                val idAndType = intent?.getStringExtra(EXTRA_COMMENT_EDIT_ID_AND_TYPE)
                if (idAndType != null) {
                    val text = intent?.getStringExtra(EXTRA_COMMENT_EDIT_TEXT) ?: ""
                    val isSelfPost = intent?.getBooleanExtra(EXTRA_COMMENT_EDIT_SELF_POST, false)
                        ?: false
                    navigationState.navigateTo(
                        Main,
                        CommentEdit(idAndType, text, isSelfPost)
                    )
                }
            }
            DEEP_LINK_PM_SEND -> {
                navigationState.navigateTo(
                    Main,
                    PMSend(
                        intent?.getStringExtra(EXTRA_PM_SEND_RECIPIENT),
                        intent?.getStringExtra(EXTRA_PM_SEND_SUBJECT),
                        intent?.getStringExtra(EXTRA_PM_SEND_TEXT)
                    )
                )
            }
            DEEP_LINK_BUG_REPORT -> navigationState.navigateTo(Settings, BugReport)
            DEEP_LINK_HTML_VIEW -> {
                val html = intent?.getStringExtra(EXTRA_HTML_VIEW_HTML)
                val title = intent?.getStringExtra(EXTRA_HTML_VIEW_TITLE)
                if (html != null) {
                    navigationState.navigateTo(Main, HtmlView(html, title ?: ""))
                }
            }
            DEEP_LINK_WEBVIEW -> {
                val url = intent?.getStringExtra(EXTRA_WEBVIEW_URL)
                if (url != null) {
                    navigationState.navigateTo(Main, WebViewRoute(url))
                }
            }
        }
    }

    /**
     * Handle an ACTION_VIEW intent carrying a deep-link URI. This is the logic
     * the retired LinkDispatchActivity owned, now run from this launcher
     * activity's onCreate (the app's external reddit.com / redd.it / redreader://
     * intent-filters resolve here). A redreader:// URI completes an OAuth login
     * and finishes once done; any other browsable URI is dispatched through
     * [LinkHandler.onLinkClicked] — which opens the link by starting the working
     * Compose screen — after which this trampoline instance finishes (exactly
     * what the standalone LinkDispatchActivity did).
     */
    private fun handleExternalViewIntent(intent: android.content.Intent) {
        val data = intent.data ?: return
        if ("redreader".equals(data.scheme, ignoreCase = true)) {
            RedditOAuth.completeLogin(this, data, RunnableOnce(Runnable { finish() }))
        } else {
            LinkHandler.onLinkClicked(this, UriString.from(data), true, null, null, 0, true)
            finish()
        }
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

        /** Deep-link route: the settings root (Settings top level; used by legacy
         *  hosts that start the activity with an explicit settings request). */
        const val DEEP_LINK_SETTINGS = "settings"

        /** Deep-link route: subreddit search (Main top level + SubredditSearch child). */
        const val DEEP_LINK_SEARCH = "search"

        /** Deep-link route: an album/gallery (Main top level + Album child). */
        const val DEEP_LINK_ALBUM = "album"

        /** Intent extra carrying the image URL for the image deep link. */
        const val EXTRA_IMAGE_URL = "org.quantumbadger.redreader.extra.IMAGE_URL"

        /** Intent extra flagging the image deep link as an animated GIF. */
        const val EXTRA_IMAGE_GIF = "org.quantumbadger.redreader.extra.IMAGE_GIF"

        /** Intent extra flagging the image deep link as a video. */
        const val EXTRA_IMAGE_VIDEO = "org.quantumbadger.redreader.extra.IMAGE_VIDEO"

        /** Intent extra carrying the album URL for an in-album image deep link. */
        const val EXTRA_IMAGE_ALBUM_URL = "org.quantumbadger.redreader.extra.IMAGE_ALBUM_URL"

        /** Intent extra carrying the index of the image within its album. */
        const val EXTRA_IMAGE_ALBUM_INDEX = "org.quantumbadger.redreader.extra.IMAGE_ALBUM_INDEX"

        /** Deep-link route: a full-screen image (Main top level + Image child). */
        const val DEEP_LINK_IMAGE = "image"

        /** Intent extra carrying the comment id-and-type for the comment-reply deep link. */
        const val EXTRA_COMMENT_REPLY_ID_AND_TYPE =
            "org.quantumbadger.redreader.extra.COMMENT_REPLY_ID_AND_TYPE"

        /** Deep-link route: reply to a post or comment (Main top level + CommentReply child). */
        const val DEEP_LINK_COMMENT_REPLY = "comment_reply"

        /** Deep-link route: Reddit terms of service (Settings top level + RedditTerms child). */
        const val DEEP_LINK_TERMS = "terms"

        /** Intent extra carrying the listing path for the post-listing deep link (a subreddit name, or frontpage / popular / all / u/<user>/… / m/<name>). */
        const val EXTRA_POST_LISTING_SUBREDDIT =
            "org.quantumbadger.redreader.extra.POST_LISTING_SUBREDDIT"

        /** Intent extra carrying the search query for the post-listing deep link (null for a non-search listing). */
        const val EXTRA_POST_LISTING_SEARCH_QUERY =
            "org.quantumbadger.redreader.extra.POST_LISTING_SEARCH_QUERY"

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

        /** Intent extra carrying the subreddit for the post-submit deep link. */
        const val EXTRA_POST_SUBMIT_SUBREDDIT =
            "org.quantumbadger.redreader.extra.POST_SUBMIT_SUBREDDIT"

        /** Intent extra carrying the shared text for the post-submit deep link. */
        const val EXTRA_POST_SUBMIT_SHARE_URL =
            "org.quantumbadger.redreader.extra.POST_SUBMIT_SHARE_URL"

        /** Deep-link route: the post submission form (Main top level + PostSubmit child). */
        const val DEEP_LINK_POST_SUBMIT = "post_submit"

        /** Deep-link route: the comment / post edit form (Main top level + CommentEdit child). */
        const val DEEP_LINK_COMMENT_EDIT = "comment_edit"

        /** Intent extra carrying the id-and-type for the comment-edit deep link. */
        const val EXTRA_COMMENT_EDIT_ID_AND_TYPE =
            "org.quantumbadger.redreader.extra.COMMENT_EDIT_ID_AND_TYPE"

        /** Intent extra carrying the current markdown for the comment-edit deep link. */
        const val EXTRA_COMMENT_EDIT_TEXT =
            "org.quantumbadger.redreader.extra.COMMENT_EDIT_TEXT"

        /** Intent extra: true when the thing being edited is a self post. */
        const val EXTRA_COMMENT_EDIT_SELF_POST =
            "org.quantumbadger.redreader.extra.COMMENT_EDIT_SELF_POST"

        /** Deep-link route: the PM composer (Main top level + PMSend child). */
        const val DEEP_LINK_PM_SEND = "pm_send"

        /** Deep-link route: the bug-report screen (Settings top level + BugReport
         *  child). Used by [org.quantumbadger.redreader.common.BugReporter.handleGlobalError]
         *  to surface a collected global error in the Compose bug-report screen —
         *  the in-app replacement for the retired BugReportActivity's own launch. */
        const val DEEP_LINK_BUG_REPORT = "bug_report"

        /** Deep-link route: the HTML viewer (Main top level + HtmlView child).
         *  The in-app replacement for the retired HtmlViewActivity's own launch. */
        const val DEEP_LINK_HTML_VIEW = "html_view"

        /** Deep-link route: the in-app browser (Main top level + WebView child).
         *  The in-app replacement for the retired WebViewActivity's launch —
         *  LinkHandler's internal-browser fallback now targets this. */
        const val DEEP_LINK_WEBVIEW = "webview"

        /** Intent extra carrying the URL for the webview deep link. */
        const val EXTRA_WEBVIEW_URL = "org.quantumbadger.redreader.extra.WEBVIEW_URL"

        /** Intent extra carrying the HTML body for the html_view deep link. */
        const val EXTRA_HTML_VIEW_HTML =
            "org.quantumbadger.redreader.extra.HTML_VIEW_HTML"

        /** Intent extra carrying the title for the html_view deep link. */
        const val EXTRA_HTML_VIEW_TITLE =
            "org.quantumbadger.redreader.extra.HTML_VIEW_TITLE"

        /** Intent extra carrying the recipient for the PM-send deep link. */
        const val EXTRA_PM_SEND_RECIPIENT =
            "org.quantumbadger.redreader.extra.PM_SEND_RECIPIENT"

        /** Intent extra carrying the subject for the PM-send deep link. */
        const val EXTRA_PM_SEND_SUBJECT =
            "org.quantumbadger.redreader.extra.PM_SEND_SUBJECT"

        /** Intent extra carrying the message body for the PM-send deep link. */
        const val EXTRA_PM_SEND_TEXT =
            "org.quantumbadger.redreader.extra.PM_SEND_TEXT"
    }
}
