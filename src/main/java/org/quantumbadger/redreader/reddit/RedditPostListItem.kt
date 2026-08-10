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

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewAdapter
import org.quantumbadger.redreader.fragments.PostListingFragment
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost
import org.quantumbadger.redreader.views.RedditPostView

class RedditPostListItem
    (
    private val mPost: RedditPreparedPost,
    private val mFragment: PostListingFragment?,
    private val mActivity: BaseActivity?,
    private val mLeftHandedMode: Boolean
) : GroupedRecyclerViewAdapter.Item<RecyclerView.ViewHolder?>() {
    override fun getViewType(): Class<RedditPostView?> {
        return RedditPostView::class.java
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup?): RecyclerView.ViewHolder {
        val view = RedditPostView(
            mActivity,
            mFragment,
            mActivity,
            mLeftHandedMode
        )

        return object : RecyclerView.ViewHolder(view) {
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        (viewHolder.itemView as RedditPostView).reset(mPost)
    }

    override fun isHidden(): Boolean {
        return false
    }
}
