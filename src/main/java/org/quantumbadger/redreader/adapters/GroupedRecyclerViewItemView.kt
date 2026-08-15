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
import androidx.recyclerview.widget.RecyclerView
import org.quantumbadger.redreader.common.FunctionOneArgWithReturn

open class GroupedRecyclerViewItemView
    (
    private val mViewType: Class<*>,
    private val mFactory: FunctionOneArgWithReturn<ViewGroup?, View>
) : GroupedRecyclerViewAdapter.Item<RecyclerView.ViewHolder?>() {
    private var mHidden = false


    override fun getViewType(): Class<*> {
        return mViewType
    }

    override fun onCreateViewHolder(viewGroup : ViewGroup): RecyclerView.ViewHolder {
        val view = mFactory.apply(viewGroup)
        return object : RecyclerView.ViewHolder(view) {}
    }

    override fun onBindViewHolder(viewHolder : RecyclerView.ViewHolder) {
        // Nothing to do here
    }

    override fun isHidden(): Boolean {
        return mHidden
    }

    fun setHidden(hidden: Boolean) {
        mHidden = hidden
    }
}