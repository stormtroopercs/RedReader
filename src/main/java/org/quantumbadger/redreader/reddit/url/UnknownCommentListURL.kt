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
package org.quantumbadger.redreader.reddit.url

import android.net.Uri

class UnknownCommentListURL internal constructor(private val uri: Uri) : CommentListingURL() {
    override fun after(after: String?): CommentListingURL {
        return UnknownCommentListURL(
            uri.buildUpon()
                .appendQueryParameter("after", after)
                .build()
        )
    }

    override fun limit(limit: Int?): CommentListingURL {
        return UnknownCommentListURL(
            uri.buildUpon()
                .appendQueryParameter(
                    "limit",
                    limit.toString()
                )
                .build()
        )
    }

    // TODO handle this better
    override fun generateJsonUri(): Uri? {
        if (uri.getPath()!!.endsWith(".json")) {
            return uri
        } else {
            return uri.buildUpon().appendEncodedPath(".json").build()
        }
    }

    @RedditURLParser.PathType
    override fun pathType(): Int {
        return RedditURLParser.UNKNOWN_COMMENT_LISTING_URL
    }
}
