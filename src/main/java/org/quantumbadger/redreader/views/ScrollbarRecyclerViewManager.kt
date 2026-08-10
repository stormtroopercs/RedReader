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
package org.quantumbadger.redreader.views

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import org.quantumbadger.redreader.R

class ScrollbarRecyclerViewManager(
    context: Context?,
    root: ViewGroup?,
    attachToRoot: Boolean
) {
    val outerView: View
    private val mSwipeRefreshLayout: SwipeRefreshLayout
    val recyclerView: RecyclerView
    private val mScrollbarFrame: FrameLayout
    private val mScrollbar: View

    private var mScrollUnnecessary = false

    init {
        this.outerView = LayoutInflater.from(context)
            .inflate(R.layout.scrollbar_recyclerview, root, attachToRoot)
        mSwipeRefreshLayout = outerView.findViewById<SwipeRefreshLayout>(R.id.scrollbar_recyclerview_refreshlayout)
        this.recyclerView =             outerView.findViewById<RecyclerView>(R.id.scrollbar_recyclerview_recyclerview)
        mScrollbar = outerView.findViewById<View>(R.id.scrollbar_recyclerview_scrollbar)
        mScrollbarFrame =             outerView.findViewById<FrameLayout>(R.id.scrollbar_recyclerview_scrollbarframe)

        mSwipeRefreshLayout.setEnabled(false)

        val linearLayoutManager = LinearLayoutManager(context)
        recyclerView.setLayoutManager(linearLayoutManager)
        recyclerView.setHasFixedSize(true)
        linearLayoutManager.setSmoothScrollbarEnabled(false)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private fun updateScroll() {
                val firstVisible = linearLayoutManager.findFirstVisibleItemPosition()
                val lastVisible = linearLayoutManager.findLastVisibleItemPosition()
                val itemsVisible = lastVisible - firstVisible + 1
                val totalCount = linearLayoutManager.getItemCount()

                val scrollUnnecessary = (itemsVisible == totalCount)

                if (scrollUnnecessary != mScrollUnnecessary) {
                    mScrollbar.setVisibility(
                        if (scrollUnnecessary)
                            View.INVISIBLE
                        else
                            View.VISIBLE
                    )
                }

                mScrollUnnecessary = scrollUnnecessary

                if (!scrollUnnecessary) {
                    val recyclerViewHeight = recyclerView.getMeasuredHeight()
                    val scrollBarHeight = mScrollbar.getMeasuredHeight()

                    val topPadding = ((firstVisible.toDouble() / (totalCount
                            - itemsVisible).toDouble())
                            * (recyclerViewHeight - scrollBarHeight))

                    mScrollbarFrame.setPadding(0, Math.round(topPadding).toInt(), 0, 0)
                }
            }

            override fun onScrolled(
                recyclerView: RecyclerView,
                dx: Int,
                dy: Int
            ) {
                updateScroll()
            }

            override fun onScrollStateChanged(
                recyclerView: RecyclerView,
                newState: Int
            ) {
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> hideScrollbar()
                    RecyclerView.SCROLL_STATE_DRAGGING, RecyclerView.SCROLL_STATE_SETTLING -> showScrollbar()
                }

                updateScroll()
            }
        })
    }

    fun enablePullToRefresh(listener: OnRefreshListener) {
        mSwipeRefreshLayout.setOnRefreshListener(listener)
        mSwipeRefreshLayout.setEnabled(true)
    }

    private fun showScrollbar() {
        mScrollbar.animate().cancel()
        mScrollbar.setAlpha(1f)
    }

    private fun hideScrollbar() {
        mScrollbar.animate().alpha(0f).setStartDelay(500).setDuration(500).start()
    }
}
