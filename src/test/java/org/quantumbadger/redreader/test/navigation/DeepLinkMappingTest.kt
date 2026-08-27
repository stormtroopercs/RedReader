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

package org.quantumbadger.redreader.test.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.quantumbadger.redreader.activities.MainActivityCompose
import org.quantumbadger.redreader.navigation.Accounts
import org.quantumbadger.redreader.navigation.Album
import org.quantumbadger.redreader.navigation.BugReport
import org.quantumbadger.redreader.navigation.Changelog
import org.quantumbadger.redreader.navigation.CommentEdit
import org.quantumbadger.redreader.navigation.CommentList
import org.quantumbadger.redreader.navigation.CommentReply
import org.quantumbadger.redreader.navigation.DeepLinkDestination
import org.quantumbadger.redreader.navigation.DeepLinkExtras
import org.quantumbadger.redreader.navigation.HtmlView
import org.quantumbadger.redreader.navigation.Image
import org.quantumbadger.redreader.navigation.Inbox
import org.quantumbadger.redreader.navigation.Main
import org.quantumbadger.redreader.navigation.PMSend
import org.quantumbadger.redreader.navigation.PostList
import org.quantumbadger.redreader.navigation.PostSubmit
import org.quantumbadger.redreader.navigation.RedditTerms
import org.quantumbadger.redreader.navigation.Settings
import org.quantumbadger.redreader.navigation.SubredditSearch
import org.quantumbadger.redreader.navigation.UserProfile
import org.quantumbadger.redreader.navigation.WebViewRoute
import org.quantumbadger.redreader.navigation.deepLinkDestination

/**
 * Tests for [deepLinkDestination] — the pure cold-start deep-link route
 * mapping used by [MainActivityCompose.onCreate]. Covers every wired route,
 * the settings-scoped children, and the null fallback for unknown routes and
 * for routes whose required extra is missing.
 */
class DeepLinkMappingTest {

    private val noExtras = DeepLinkExtras()

    // --- Main-scoped children --------------------------------------------

    @Test
    fun inbox_mapsToMain_plusInbox() {
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_INBOX, noExtras)
        assertEquals(DeepLinkDestination.Child(Main, Inbox), dest)
    }

    @Test
    fun search_mapsToMain_plusSubredditSearch() {
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_SEARCH, noExtras)
        assertEquals(DeepLinkDestination.Child(Main, SubredditSearch), dest)
    }

    @Test
    fun accounts_mapsToMain_plusAccounts() {
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_ACCOUNTS, noExtras)
        assertEquals(DeepLinkDestination.Child(Main, Accounts), dest)
    }

    @Test
    fun album_mapsToMain_plusAlbumWithUrl() {
        val extras = DeepLinkExtras(albumUrl = "https://i.imgur.com/album/abc")
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_ALBUM, extras)
        assertEquals(DeepLinkDestination.Child(Main, Album("https://i.imgur.com/album/abc")), dest)
    }

    @Test
    fun album_withMissingUrl_returnsNull() {
        assertNull(deepLinkDestination(MainActivityCompose.DEEP_LINK_ALBUM, noExtras))
    }

    @Test
    fun image_mapsToMain_plusImageWithAllFields() {
        val extras = DeepLinkExtras(
            imageUrl = "https://i.imgur.com/x.png",
            imageIsGif = true,
            imageIsVideo = false,
            imageAlbumUrl = "https://i.imgur.com/album/x",
            imageAlbumIndex = 3
        )
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_IMAGE, extras)
        assertEquals(
            DeepLinkDestination.Child(
                Main,
                Image("https://i.imgur.com/x.png", true, false, "https://i.imgur.com/album/x", 3)
            ),
            dest
        )
    }

    @Test
    fun image_withMissingUrl_returnsNull() {
        assertNull(deepLinkDestination(MainActivityCompose.DEEP_LINK_IMAGE, noExtras))
    }

    @Test
    fun commentReply_mapsToMain_plusCommentReply() {
        val extras = DeepLinkExtras(commentReplyIdAndType = "t1_abc123")
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_COMMENT_REPLY, extras)
        assertEquals(DeepLinkDestination.Child(Main, CommentReply("t1_abc123")), dest)
    }

    @Test
    fun commentReply_withMissingId_returnsNull() {
        assertNull(deepLinkDestination(MainActivityCompose.DEEP_LINK_COMMENT_REPLY, noExtras))
    }

    @Test
    fun postListing_mapsToMain_plusPostList() {
        val extras = DeepLinkExtras(postListingSubreddit = "test", postListingSearchQuery = "hi")
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_POST_LISTING, extras)
        assertEquals(DeepLinkDestination.Child(Main, PostList("test", "hi")), dest)
    }

    @Test
    fun postListing_withoutQuery_stillMaps() {
        val extras = DeepLinkExtras(postListingSubreddit = "test")
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_POST_LISTING, extras)
        assertEquals(DeepLinkDestination.Child(Main, PostList("test", null)), dest)
    }

    @Test
    fun postListing_withMissingSubreddit_returnsNull() {
        assertNull(deepLinkDestination(MainActivityCompose.DEEP_LINK_POST_LISTING, noExtras))
    }

    @Test
    fun commentListing_mapsToMain_plusCommentList() {
        val extras = DeepLinkExtras(commentListingPostId = "t3_abc123")
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_COMMENT_LISTING, extras)
        assertEquals(DeepLinkDestination.Child(Main, CommentList("t3_abc123")), dest)
    }

    @Test
    fun commentListing_withMissingPostId_returnsNull() {
        assertNull(deepLinkDestination(MainActivityCompose.DEEP_LINK_COMMENT_LISTING, noExtras))
    }

    @Test
    fun userProfile_mapsToMain_plusUserProfile() {
        val extras = DeepLinkExtras(userProfileUsername = "someuser")
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_USER_PROFILE, extras)
        assertEquals(DeepLinkDestination.Child(Main, UserProfile("someuser")), dest)
    }

    @Test
    fun userProfile_withMissingUsername_returnsNull() {
        assertNull(deepLinkDestination(MainActivityCompose.DEEP_LINK_USER_PROFILE, noExtras))
    }

    @Test
    fun postSubmit_mapsToMain_plusPostSubmit() {
        val extras = DeepLinkExtras(postSubmitSubreddit = "test", postSubmitShareUrl = "https://example.com")
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_POST_SUBMIT, extras)
        assertEquals(DeepLinkDestination.Child(Main, PostSubmit("test", "https://example.com")), dest)
    }

    @Test
    fun postSubmit_withMissingSubreddit_returnsNull() {
        assertNull(deepLinkDestination(MainActivityCompose.DEEP_LINK_POST_SUBMIT, noExtras))
    }

    @Test
    fun commentEdit_mapsToMain_plusCommentEdit() {
        val extras = DeepLinkExtras(
            commentEditIdAndType = "t1_abc",
            commentEditText = "hello world",
            commentEditSelfPost = true
        )
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_COMMENT_EDIT, extras)
        assertEquals(DeepLinkDestination.Child(Main, CommentEdit("t1_abc", "hello world", true)), dest)
    }

    @Test
    fun commentEdit_withMissingId_returnsNull() {
        assertNull(deepLinkDestination(MainActivityCompose.DEEP_LINK_COMMENT_EDIT, noExtras))
    }

    @Test
    fun pmSend_mapsToMain_plusPMSend() {
        val extras = DeepLinkExtras(pmSendRecipient = "someuser", pmSendSubject = "Subj", pmSendText = "Body")
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_PM_SEND, extras)
        assertEquals(DeepLinkDestination.Child(Main, PMSend("someuser", "Subj", "Body")), dest)
    }

    @Test
    fun pmSend_withMissingFields_mapsWithNulls() {
        // PM send has no required extra — it always maps, even with empty fields.
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_PM_SEND, noExtras)
        assertEquals(DeepLinkDestination.Child(Main, PMSend(null, null, null)), dest)
    }

    @Test
    fun htmlView_mapsToMain_plusHtmlView() {
        val extras = DeepLinkExtras(htmlViewHtml = "<html>hi</html>", htmlViewTitle = "Title")
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_HTML_VIEW, extras)
        assertEquals(DeepLinkDestination.Child(Main, HtmlView("<html>hi</html>", "Title")), dest)
    }

    @Test
    fun htmlView_withMissingTitle_defaultsToEmpty() {
        val extras = DeepLinkExtras(htmlViewHtml = "<html>hi</html>")
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_HTML_VIEW, extras)
        assertEquals(DeepLinkDestination.Child(Main, HtmlView("<html>hi</html>", "")), dest)
    }

    @Test
    fun htmlView_withMissingHtml_returnsNull() {
        assertNull(deepLinkDestination(MainActivityCompose.DEEP_LINK_HTML_VIEW, noExtras))
    }

    @Test
    fun webview_mapsToMain_plusWebViewRoute() {
        val extras = DeepLinkExtras(webviewUrl = "https://example.com/page")
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_WEBVIEW, extras)
        assertEquals(DeepLinkDestination.Child(Main, WebViewRoute("https://example.com/page")), dest)
    }

    @Test
    fun webview_withMissingUrl_returnsNull() {
        assertNull(deepLinkDestination(MainActivityCompose.DEEP_LINK_WEBVIEW, noExtras))
    }

    // --- Settings-scoped children ----------------------------------------

    @Test
    fun settings_mapsToSettingsRoot() {
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_SETTINGS, noExtras)
        assertEquals(DeepLinkDestination.Root(Settings), dest)
    }

    @Test
    fun changelog_mapsToSettings_plusChangelog() {
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_CHANGELOG, noExtras)
        assertEquals(DeepLinkDestination.Child(Settings, Changelog), dest)
    }

    @Test
    fun terms_mapsToSettings_plusRedditTerms() {
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_TERMS, noExtras)
        assertEquals(DeepLinkDestination.Child(Settings, RedditTerms), dest)
    }

    @Test
    fun bugReport_mapsToSettings_plusBugReport() {
        val dest = deepLinkDestination(MainActivityCompose.DEEP_LINK_BUG_REPORT, noExtras)
        assertEquals(DeepLinkDestination.Child(Settings, BugReport), dest)
    }

    // --- Fallback ---------------------------------------------------------

    @Test
    fun unknownRoute_returnsNull() {
        assertNull(deepLinkDestination("nonsense_route", noExtras))
    }

    @Test
    fun emptyRoute_returnsNull() {
        assertNull(deepLinkDestination("", noExtras))
    }
}
