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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.RecyclerView
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewAdapter

class GroupedRecyclerViewItemListSectionHeaderView
    (private val mText: CharSequence) : GroupedRecyclerViewAdapter.Item<RecyclerView.ViewHolder>() {
    override val viewType: Class<*>
        get() {
        // There's no wrapper class for this view, so just use the item class
        return GroupedRecyclerViewItemListSectionHeaderView::class.java
        }

    override fun onCreateViewHolder(viewGroup: ViewGroup): RecyclerView.ViewHolder {
        return object : RecyclerView.ViewHolder(
            LayoutInflater.from(viewGroup.getContext()).inflate(
                R.layout.list_sectionheader,
                viewGroup,
                false
            )
        ) {
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
        val view = viewHolder.itemView as TextView
        view.setText(mText)

        //From https://stackoverflow.com/a/54082384
        ViewCompat.setAccessibilityDelegate(view, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(
                host: View,
                info: AccessibilityNodeInfoCompat
            ) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.setHeading(true)
            }
        })
    }

    override val isHidden: Boolean get() = false
}
