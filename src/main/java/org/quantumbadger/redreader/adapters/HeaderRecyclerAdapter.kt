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
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by veyndan on 18/04/2016.
 */
abstract class HeaderRecyclerAdapter<VH : RecyclerView.ViewHolder?>
    : RecyclerView.Adapter<VH?>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        when (viewType) {
            TYPE_HEADER -> return onCreateHeaderItemViewHolder(parent)
            TYPE_CONTENT -> return onCreateContentItemViewHolder(parent)
            else -> throw IllegalStateException()
        }
    }

    protected abstract fun onCreateHeaderItemViewHolder(parent : ViewGroup): VH?

    protected abstract fun onCreateContentItemViewHolder(parent : ViewGroup): VH?

    override fun onBindViewHolder(holder: VH, position: Int) {
        if (position == 0) {
            onBindHeaderItemViewHolder(holder, position)
        } else {
            onBindContentItemViewHolder(holder, position - HEADER_SIZE)
        }
    }

    protected abstract fun onBindHeaderItemViewHolder(holder: VH?, position: Int)

    protected abstract fun onBindContentItemViewHolder(holder: VH?, position: Int)

    override fun getItemCount(): Int {
        return this.contentItemCount + HEADER_SIZE
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_HEADER else TYPE_CONTENT
    }

    protected abstract val contentItemCount: Int

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CONTENT = 1

        protected const val HEADER_SIZE: Int = 1
    }
}