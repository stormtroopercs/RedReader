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
package org.quantumbadger.redreader.views.list

import android.graphics.drawable.Drawable
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewAdapter
import org.quantumbadger.redreader.common.Optional

class GroupedRecyclerViewItemListItemView(
    private val mIcon: Drawable?,
    private val mText: CharSequence,
    private val mContentDescription: String?,
    private val mHideDivider: Boolean,
    private val mClickListener: View.OnClickListener?,
    private val mLongClickListener: OnLongClickListener?,
    private val mSecondaryIcon: Optional<Drawable?>,
    private val mSecondaryAction: Optional<View.OnClickListener?>,
    private val mSecondaryContentDesc: Optional<String?>
) : GroupedRecyclerViewAdapter.Item<Any?>() {
    override val viewType: Class<*> get() = ListItemView::class.java

    override fun onCreateViewHolder(viewGroup: ViewGroup): RecyclerView.ViewHolder {
        return object : RecyclerView.ViewHolder(ListItemView(viewGroup.getContext())) {}
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        (viewHolder.itemView as ListItemView).reset(
            mIcon,
            mText,
            mContentDescription,
            mHideDivider,
            mClickListener,
            mLongClickListener,
            mSecondaryIcon,
            mSecondaryAction,
            mSecondaryContentDesc
        )
    }

    override val isHidden: Boolean get() = false
}
