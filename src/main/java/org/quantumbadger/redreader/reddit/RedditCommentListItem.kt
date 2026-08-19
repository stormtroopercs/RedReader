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

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewAdapter
import org.quantumbadger.redreader.common.RRThemeAttributes
import org.quantumbadger.redreader.fragments.CommentListingFragment
import org.quantumbadger.redreader.reddit.kthings.RedditMore
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager
import org.quantumbadger.redreader.reddit.prepared.RedditRenderableComment
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL
import org.quantumbadger.redreader.views.LoadMoreCommentsView
import org.quantumbadger.redreader.views.RedditCommentView

class RedditCommentListItem

    : GroupedRecyclerViewAdapter.Item<RecyclerView.ViewHolder> {
    enum class Type {
        COMMENT, LOAD_MORE
    }

    private val mType: Type

    val indent: Int
    val parent: RedditCommentListItem?
    private val mFragment: CommentListingFragment?
    private val mActivity: BaseActivity?
    private val mCommentListingUrl: RedditURL?

    private val mComment: RedditRenderableComment?
    private val mMoreComments: RedditMore?

    private val mChangeDataManager: RedditChangeDataManager

    constructor(
        comment: RedditRenderableComment?,
        parent: RedditCommentListItem?,
        fragment: CommentListingFragment?,
        activity: BaseActivity?,
        commentListingUrl: RedditURL?
    ) {
        this.parent = parent
        mFragment = fragment
        mActivity = activity
        mCommentListingUrl = commentListingUrl
        mType = Type.COMMENT
        mComment = comment
        mMoreComments = null

        if (parent == null) {
            this.indent = 0
        } else {
            this.indent = parent.indent + 1
        }

        mChangeDataManager = RedditChangeDataManager.Companion.getInstance(
            RedditAccountManager.Companion.getInstance(activity).getDefaultAccount()
        )
    }

    constructor(
        moreComments: RedditMore?,
        parent: RedditCommentListItem?,
        fragment: CommentListingFragment?,
        activity: BaseActivity?,
        commentListingUrl: RedditURL?
    ) {
        this.parent = parent
        mFragment = fragment
        mActivity = activity
        mCommentListingUrl = commentListingUrl
        mType = Type.LOAD_MORE
        mComment = null
        mMoreComments = moreComments

        if (parent == null) {
            this.indent = 0
        } else {
            this.indent = parent.indent + 1
        }

        mChangeDataManager = RedditChangeDataManager.Companion.getInstance(
            RedditAccountManager.Companion.getInstance(activity).getDefaultAccount()
        )
    }

    val isComment: Boolean
        get() = mType == Type.COMMENT

    val isLoadMore: Boolean
        get() = mType == Type.LOAD_MORE

    fun asComment(): RedditRenderableComment {
        if (!this.isComment) {
            throw RuntimeException("Called asComment() on non-comment item")
        }

        return mComment!!
    }

    fun asLoadMore(): RedditMore {
        if (!this.isLoadMore) {
            throw RuntimeException("Called asLoadMore() on non-load-more item")
        }

        return mMoreComments!!
    }

    fun isCollapsed(changeDataManager: RedditChangeDataManager): Boolean {
        if (!this.isComment) {
            return false
        }

        return mComment!!.isCollapsed(changeDataManager)
    }

    fun isHidden(changeDataManager: RedditChangeDataManager): Boolean {
        if (this.parent != null) {
            return parent.isCollapsed(changeDataManager) || parent.isHidden(
                changeDataManager
            )
        }

        return false
    }

    override val viewType: Class<*>
        get() {
        if (this.isComment) {
            return RedditCommentView::class.java
        }

        if (this.isLoadMore) {
            return LoadMoreCommentsView::class.java
        }

        throw RuntimeException("Unknown item type")
        }

    override fun onCreateViewHolder(viewGroup: ViewGroup): RecyclerView.ViewHolder {
        val context = viewGroup.getContext()
        val view: View

        if (this.isComment) {
            view = RedditCommentView(
                mActivity,
                RRThemeAttributes(context),
                mFragment,
                mFragment
            )
        } else if (this.isLoadMore) {
            view = LoadMoreCommentsView(
                context,
                mCommentListingUrl
            )
        } else {
            throw RuntimeException("Unknown item type")
        }

        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        if (this.isComment) {
            (viewHolder.itemView as RedditCommentView).reset(mActivity, this)
        } else if (this.isLoadMore) {
            (viewHolder.itemView as LoadMoreCommentsView).reset(this)
        } else {
            throw RuntimeException("Unknown item type")
        }
    }

    override val isHidden: Boolean get() = isHidden(mChangeDataManager)
}
