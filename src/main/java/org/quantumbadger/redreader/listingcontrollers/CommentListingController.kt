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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.quantumbadger.redreader.activities.OptionsMenuUtility
import org.quantumbadger.redreader.common.General.listOfOne
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.fragments.CommentListingFragment
import org.quantumbadger.redreader.reddit.PostCommentSort
import org.quantumbadger.redreader.reddit.UserCommentSort
import org.quantumbadger.redreader.reddit.url.CommentListingURL
import org.quantumbadger.redreader.reddit.url.RedditURLParser
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL
import java.util.UUID
import android.net.Uri
import org.quantumbadger.redreader.common.General

// TODO add notification/header for abnormal sort order
class CommentListingController(url: RedditURL) {
    var commentListingUrl: CommentListingURL
        private set
    var session: UUID?=null
    var searchString: String?=null

    init {
        var url = url
        if (url.pathType() == RedditURLParser.POST_COMMENT_LISTING_URL) {
            if (url.asPostCommentListURL().order == null) {
                url = url.asPostCommentListURL().order(defaultOrder())
            }
        } else if (url.pathType() == RedditURLParser.USER_COMMENT_LISTING_URL) {
            if (url.asUserCommentListURL().order == null) {
                url = url.asUserCommentListURL().order(defaultUserOrder())
            }
            url = url.asUserCommentListURL().limit(100)
        }

        if (url !is CommentListingURL) {
            throw RuntimeException("Not comment listing URL")
        }

        this.commentListingUrl = url
    }

    private fun defaultOrder(): PostCommentSort? {
        return PrefsUtility.pref_behaviour_commentsort()
    }

    private fun defaultUserOrder(): UserCommentSort? {
        return PrefsUtility.pref_behaviour_user_commentsort()
    }

    fun setSort(s: PostCommentSort?) {
        if (commentListingUrl.pathType() == RedditURLParser.POST_COMMENT_LISTING_URL) {
            this.commentListingUrl = commentListingUrl.asPostCommentListURL().order(s)
        }
    }

    fun setSort(s: UserCommentSort?) {
        if (commentListingUrl.pathType() == RedditURLParser.USER_COMMENT_LISTING_URL) {
            this.commentListingUrl = commentListingUrl.asUserCommentListURL().order(s)
        }
    }

    val sort: OptionsMenuUtility.Sort?
        get() {
            if (commentListingUrl.pathType() == RedditURLParser.POST_COMMENT_LISTING_URL) {
                return commentListingUrl.asPostCommentListURL().order
            } else if (commentListingUrl.pathType() == RedditURLParser.USER_COMMENT_LISTING_URL) {
                return commentListingUrl.asUserCommentListURL().order
            }

            return null
        }

    val uri: Uri?
        get() = commentListingUrl.generateJsonUri()

    fun get(
        parent: AppCompatActivity,
        force: Boolean,
        savedInstanceState: Bundle?
    ): CommentListingFragment {
        if (force) {
            this.session = null
        }
        return CommentListingFragment(
            parent,
            savedInstanceState,
            listOfOne<RedditURL>(this.commentListingUrl),
            this.session,
            this.searchString,
            force
        )
    }

    val isSortable: Boolean
        get() = commentListingUrl.pathType() == RedditURLParser.POST_COMMENT_LISTING_URL
                || commentListingUrl.pathType() == RedditURLParser.USER_COMMENT_LISTING_URL

    val isUserCommentListing: Boolean
        get() = commentListingUrl.pathType() == RedditURLParser.USER_COMMENT_LISTING_URL
}
