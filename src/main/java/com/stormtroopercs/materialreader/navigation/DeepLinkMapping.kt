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

package com.stormtroopercs.materialreader.navigation

import android.content.Intent
import androidx.navigation3.runtime.NavKey
import com.stormtroopercs.materialreader.activities.MainActivityCompose

/**
 * The destination a cold-start deep link resolves to: a top-level route on its
 * own ([Root]) or a child route pushed under a top-level route ([Child]).
 */
internal sealed interface DeepLinkDestination {
    data class Root(val root: NavKey) : DeepLinkDestination
    data class Child(val root: NavKey, val child: NavKey) : DeepLinkDestination
}

/**
 * The extras carried by a cold-start deep-link intent, read off by name —
 * decoupled from the [Intent] so [deepLinkDestination] stays a pure mapping
 * that is easy to test (see `DeepLinkMappingTest`). The extra keys are the
 * `const val`s on [MainActivityCompose]'s companion object, inlined at
 * compile time, so reading them here does not load the Activity class.
 */
internal data class DeepLinkExtras(
    val albumUrl: String? = null,
    val imageUrl: String? = null,
    val imageIsGif: Boolean = false,
    val imageIsVideo: Boolean = false,
    val imageAlbumUrl: String? = null,
    val imageAlbumIndex: Int = 0,
    val commentReplyIdAndType: String? = null,
    val postListingSubreddit: String? = null,
    val postListingSearchQuery: String? = null,
    val commentListingPostId: String? = null,
    val userProfileUsername: String? = null,
    val postSubmitSubreddit: String? = null,
    val postSubmitShareUrl: String? = null,
    val commentEditIdAndType: String? = null,
    val commentEditText: String = "",
    val commentEditSelfPost: Boolean = false,
    val pmSendRecipient: String? = null,
    val pmSendSubject: String? = null,
    val pmSendText: String? = null,
    val webviewUrl: String? = null,
    val htmlViewHtml: String? = null,
    val htmlViewTitle: String? = null
) {
    companion object {
        fun from(intent: Intent?): DeepLinkExtras = DeepLinkExtras(
            albumUrl = intent?.getStringExtra(MainActivityCompose.EXTRA_ALBUM_URL),
            imageUrl = intent?.getStringExtra(MainActivityCompose.EXTRA_IMAGE_URL),
            imageIsGif = intent?.getBooleanExtra(MainActivityCompose.EXTRA_IMAGE_GIF, false) ?: false,
            imageIsVideo = intent?.getBooleanExtra(MainActivityCompose.EXTRA_IMAGE_VIDEO, false) ?: false,
            imageAlbumUrl = intent?.getStringExtra(MainActivityCompose.EXTRA_IMAGE_ALBUM_URL),
            imageAlbumIndex = intent?.getIntExtra(MainActivityCompose.EXTRA_IMAGE_ALBUM_INDEX, 0) ?: 0,
            commentReplyIdAndType = intent?.getStringExtra(MainActivityCompose.EXTRA_COMMENT_REPLY_ID_AND_TYPE),
            postListingSubreddit = intent?.getStringExtra(MainActivityCompose.EXTRA_POST_LISTING_SUBREDDIT),
            postListingSearchQuery = intent?.getStringExtra(MainActivityCompose.EXTRA_POST_LISTING_SEARCH_QUERY),
            commentListingPostId = intent?.getStringExtra(MainActivityCompose.EXTRA_COMMENT_LISTING_POST_ID),
            userProfileUsername = intent?.getStringExtra(MainActivityCompose.EXTRA_USER_PROFILE_USERNAME),
            postSubmitSubreddit = intent?.getStringExtra(MainActivityCompose.EXTRA_POST_SUBMIT_SUBREDDIT),
            postSubmitShareUrl = intent?.getStringExtra(MainActivityCompose.EXTRA_POST_SUBMIT_SHARE_URL),
            commentEditIdAndType = intent?.getStringExtra(MainActivityCompose.EXTRA_COMMENT_EDIT_ID_AND_TYPE),
            commentEditText = intent?.getStringExtra(MainActivityCompose.EXTRA_COMMENT_EDIT_TEXT) ?: "",
            commentEditSelfPost = intent?.getBooleanExtra(MainActivityCompose.EXTRA_COMMENT_EDIT_SELF_POST, false) ?: false,
            pmSendRecipient = intent?.getStringExtra(MainActivityCompose.EXTRA_PM_SEND_RECIPIENT),
            pmSendSubject = intent?.getStringExtra(MainActivityCompose.EXTRA_PM_SEND_SUBJECT),
            pmSendText = intent?.getStringExtra(MainActivityCompose.EXTRA_PM_SEND_TEXT),
            webviewUrl = intent?.getStringExtra(MainActivityCompose.EXTRA_WEBVIEW_URL),
            htmlViewHtml = intent?.getStringExtra(MainActivityCompose.EXTRA_HTML_VIEW_HTML),
            htmlViewTitle = intent?.getStringExtra(MainActivityCompose.EXTRA_HTML_VIEW_TITLE)
        )
    }
}

/**
 * Map a cold-start deep-link route name (+ its intent extras) to the
 * destination the [NavigationState] should land on. Pure — it only reads the
 * provided extras, so it can be unit-tested directly (see
 * `DeepLinkMappingTest`) without instantiating the launcher activity. Returns
 * null for unknown routes (the app stays on the default main screen) and for
 * routes whose required extra is missing.
 *
 * The route-name and extra constants are the `const val`s on
 * [MainActivityCompose]'s companion object, inlined at compile time.
 */
internal fun deepLinkDestination(
    route: String,
    extras: DeepLinkExtras
): DeepLinkDestination? {
    return when (route) {
        MainActivityCompose.DEEP_LINK_INBOX -> DeepLinkDestination.Child(Main, Inbox)
        MainActivityCompose.DEEP_LINK_CHANGELOG -> DeepLinkDestination.Child(Settings, Changelog)
        MainActivityCompose.DEEP_LINK_SETTINGS -> DeepLinkDestination.Root(Settings)
        MainActivityCompose.DEEP_LINK_SEARCH -> DeepLinkDestination.Child(Main, SubredditSearch)
        MainActivityCompose.DEEP_LINK_ACCOUNTS -> DeepLinkDestination.Child(Main, Accounts)
        MainActivityCompose.DEEP_LINK_ALBUM -> extras.albumUrl?.let { DeepLinkDestination.Child(Main, Album(it)) }
        MainActivityCompose.DEEP_LINK_IMAGE -> extras.imageUrl?.let { url ->
            DeepLinkDestination.Child(
                Main,
                Image(url, extras.imageIsGif, extras.imageIsVideo, extras.imageAlbumUrl, extras.imageAlbumIndex)
            )
        }
        MainActivityCompose.DEEP_LINK_COMMENT_REPLY -> extras.commentReplyIdAndType?.let {
            DeepLinkDestination.Child(Main, CommentReply(it))
        }
        MainActivityCompose.DEEP_LINK_TERMS -> DeepLinkDestination.Child(Settings, RedditTerms)
        MainActivityCompose.DEEP_LINK_POST_LISTING -> extras.postListingSubreddit?.normalizeListingPath()?.let {
            DeepLinkDestination.Child(Main, PostList(it, extras.postListingSearchQuery))
        }
        MainActivityCompose.DEEP_LINK_COMMENT_LISTING -> extras.commentListingPostId?.let {
            DeepLinkDestination.Child(Main, CommentList(it))
        }
        MainActivityCompose.DEEP_LINK_USER_PROFILE -> extras.userProfileUsername?.let {
            DeepLinkDestination.Child(Main, UserProfile(it))
        }
        MainActivityCompose.DEEP_LINK_POST_SUBMIT -> extras.postSubmitSubreddit?.let {
            DeepLinkDestination.Child(Main, PostSubmit(it, extras.postSubmitShareUrl))
        }
        MainActivityCompose.DEEP_LINK_COMMENT_EDIT -> extras.commentEditIdAndType?.let {
            DeepLinkDestination.Child(Main, CommentEdit(it, extras.commentEditText, extras.commentEditSelfPost))
        }
        MainActivityCompose.DEEP_LINK_PM_SEND -> DeepLinkDestination.Child(
            Main,
            PMSend(extras.pmSendRecipient, extras.pmSendSubject, extras.pmSendText)
        )
        MainActivityCompose.DEEP_LINK_BUG_REPORT -> DeepLinkDestination.Child(Settings, BugReport)
        MainActivityCompose.DEEP_LINK_HTML_VIEW -> extras.htmlViewHtml?.let {
            DeepLinkDestination.Child(Main, HtmlView(it, extras.htmlViewTitle ?: ""))
        }
        MainActivityCompose.DEEP_LINK_WEBVIEW -> extras.webviewUrl?.let {
            DeepLinkDestination.Child(Main, WebViewRoute(it))
        }
        else -> null
    }
}
