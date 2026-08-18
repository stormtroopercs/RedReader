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

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewItemRRError.ErrorHolder
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.views.liststatus.ErrorView

class GroupedRecyclerViewItemRRError(
    private val mActivity: AppCompatActivity,
    private val mError: RRError
) : GroupedRecyclerViewAdapter.Item<ErrorHolder>() {
    inner class ErrorHolder : RecyclerView.ViewHolder(FrameLayout(mActivity)) {
        fun bind(error: RRError) {
            val itemView = this.itemView as FrameLayout
            itemView.removeAllViews()
            itemView.addView(ErrorView(mActivity, error))
        }
    }

    override val viewType: Class<*> get() = GroupedRecyclerViewItemRRError::class.java

    override fun onCreateViewHolder(viewGroup : ViewGroup): ErrorHolder {
        return ErrorHolder()
    }

    override fun onBindViewHolder(viewHolder: ErrorHolder) {
        viewHolder.bind(mError)
    }

    override val isHidden: Boolean get() = false
}
