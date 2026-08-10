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
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import org.quantumbadger.redreader.common.General.checkThisIsUIThread
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.views.LoadingSpinnerView
import org.quantumbadger.redreader.views.RedditPostHeaderView
import org.quantumbadger.redreader.views.liststatus.ErrorView

abstract class RedditListingManager protected constructor(context: Context) {
    private val mAdapter = GroupedRecyclerViewAdapter(7)
    private var mLayoutManager: LinearLayoutManager?=null

    private val mLoadingItem: GroupedRecyclerViewItemFrameLayout
    private var mWorkaroundDone = false

    init {
        checkThisIsUIThread()
        val loadingSpinnerView = LoadingSpinnerView(context)
        val paddingPx = dpToPixels(context, 30f)
        loadingSpinnerView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

        mLoadingItem = GroupedRecyclerViewItemFrameLayout(loadingSpinnerView)
        mAdapter.appendToGroup(GROUP_LOADING, mLoadingItem)
    }

    fun setLayoutManager(layoutManager: LinearLayoutManager?) {
        checkThisIsUIThread()
        mLayoutManager = layoutManager
    }

    // Workaround for RecyclerView scrolling behaviour
    private fun doWorkaround() {
        if (!mWorkaroundDone && mLayoutManager != null) {
            mLayoutManager!!.scrollToPositionWithOffset(0, 0)
            mWorkaroundDone = true
        }
    }

    fun addFooterError(view: ErrorView?) {
        checkThisIsUIThread()
        mAdapter.appendToGroup(
            GROUP_FOOTER_ERRORS,
            GroupedRecyclerViewItemFrameLayout(view)
        )
    }

    fun addPostHeader(view: RedditPostHeaderView?) {
        checkThisIsUIThread()
        mAdapter.appendToGroup(
            GROUP_HEADER,
            GroupedRecyclerViewItemFrameLayout(view)
        )
        doWorkaround()
    }

    fun addPostListingHeader(view: View?) {
        checkThisIsUIThread()
        mAdapter.appendToGroup(
            GROUP_HEADER,
            GroupedRecyclerViewItemFrameLayout(view)
        )
        doWorkaround()
    }

    fun addPostSelfText(view: View?) {
        checkThisIsUIThread()
        mAdapter.appendToGroup(
            GROUP_POST_SELFTEXT,
            GroupedRecyclerViewItemFrameLayout(view)
        )
        doWorkaround()
    }

    fun addNotification(view: View?) {
        checkThisIsUIThread()
        mAdapter.appendToGroup(
            GROUP_NOTIFICATIONS,
            GroupedRecyclerViewItemFrameLayout(view)
        )
        doWorkaround()
    }

    fun addItems(items: MutableCollection<GroupedRecyclerViewAdapter.Item<*>?>?) {
        checkThisIsUIThread()
        mAdapter.appendToGroup(GROUP_ITEMS, items)
        doWorkaround()
    }

    fun addViewToItems(view: View?) {
        checkThisIsUIThread()
        mAdapter.appendToGroup(GROUP_ITEMS, GroupedRecyclerViewItemFrameLayout(view))
        doWorkaround()
    }

    fun addLoadMoreButton(view: View?) {
        checkThisIsUIThread()
        mAdapter.appendToGroup(
            GROUP_LOAD_MORE_BUTTON,
            GroupedRecyclerViewItemFrameLayout(view)
        )
        doWorkaround()
    }

    fun removeLoadMoreButton() {
        checkThisIsUIThread()
        mAdapter.removeAllFromGroup(GROUP_LOAD_MORE_BUTTON)
    }

    fun setLoadingVisible(visible: Boolean) {
        checkThisIsUIThread()
        mLoadingItem.setHidden(!visible)
        mAdapter.updateHiddenStatus()
    }

    val adapter: GroupedRecyclerViewAdapter
        get() {
            checkThisIsUIThread()
            return mAdapter
        }

    fun updateHiddenStatus() {
        checkThisIsUIThread()
        mAdapter.updateHiddenStatus()
    }

    fun getItemAtPosition(position: Int): GroupedRecyclerViewAdapter.Item<*>? {
        return mAdapter.getItemAtPosition(position)
    }

    companion object {
        private const val GROUP_HEADER = 0
        private const val GROUP_NOTIFICATIONS = 1
        private const val GROUP_POST_SELFTEXT = 2
        private const val GROUP_ITEMS = 3
        private const val GROUP_LOAD_MORE_BUTTON = 4
        private const val GROUP_LOADING = 5
        private const val GROUP_FOOTER_ERRORS = 6
    }
}
