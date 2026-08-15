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
package org.quantumbadger.redreader.adapters

import android.content.Context
import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.reddit.RedditCommentListItem
import java.util.Collections

class FilteredCommentListingManager(
    context: Context?,
    private val mSearchString: String?
) : RedditListingManager(context) {
    var commentCount: Int = 0
        private set

    fun addComments(comments: MutableCollection<RedditCommentListItem>) {
        val filteredComments = filter(
            comments
        )
        addItems(filteredComments)
        this.commentCount += filteredComments.size
    }

    private fun filter(
        comments: MutableCollection<RedditCommentListItem>
    ): MutableCollection<GroupedRecyclerViewAdapter.Item<*>?> {
        val searchComments: MutableCollection<RedditCommentListItem>

        if (mSearchString == null) {
            searchComments = comments
        } else {
            searchComments = ArrayList<RedditCommentListItem>()
            for (comment in comments) {
                if (!comment.isComment) {
                    continue
                }
                val body = comment.asComment()
                    .parsedComment
                    .getRawComment().body
                if (body != null) {
                    if (StringUtils.asciiLowercase(body.decoded).contains(mSearchString)) {
                        searchComments.add(comment)
                    }
                }
            }
        }

        return Collections.unmodifiableCollection<GroupedRecyclerViewAdapter.Item<*>?>(
            searchComments
        )
    }

    val isSearchListing: Boolean
        get() = mSearchString != null
}
