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

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import org.quantumbadger.redreader.common.General.setLayoutMatchWidthWrapHeight
import org.quantumbadger.redreader.common.General

internal class GroupedRecyclerViewItemFrameLayout(private val mChildView: View) :
    GroupedRecyclerViewAdapter.Item<Any?>() {
    private var mHidden = false

    private var mParent: FrameLayout?=null

    override fun getViewType(): Class<*> {
        return this.javaClass
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup): RecyclerView.ViewHolder {
        setLayoutMatchWidthWrapHeight(viewGroup)

        val frameLayout = FrameLayout(viewGroup.getContext())
        return object : RecyclerView.ViewHolder(frameLayout) {}
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        val view = viewHolder.itemView as FrameLayout
        view.removeAllViews()

        if (mParent != null && mChildView.getParent() === mParent) {
            mParent!!.removeAllViews()
        }

        mParent = view

        view.addView(mChildView)
        setLayoutMatchWidthWrapHeight(mChildView)
    }

    override fun isHidden(): Boolean {
        return mHidden
    }

    fun setHidden(hidden: Boolean) {
        mHidden = hidden
    }
}
