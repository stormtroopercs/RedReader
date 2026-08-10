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
import org.quantumbadger.redreader.reddit.RedditPostListItem
import java.util.Collections

class PostListingManager(context: Context?) : RedditListingManager(context) {
    var postCount: Int = 0
        private set

    fun addPosts(posts: MutableCollection<RedditPostListItem?>) {
        addItems(Collections.unmodifiableCollection<GroupedRecyclerViewAdapter.Item<*>?>(posts))
        this.postCount += posts.size
    }
}
