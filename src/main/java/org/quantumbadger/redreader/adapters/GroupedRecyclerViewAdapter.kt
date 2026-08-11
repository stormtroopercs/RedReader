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
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.atomic.AtomicLong

class GroupedRecyclerViewAdapter(groups: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder?>() {
    abstract class Item<VH : RecyclerView.ViewHolder?> {
        private val mUniqueId: Long = ITEM_UNIQUE_ID_GENERATOR.incrementAndGet()
        private var mCurrentlyHidden = false

        abstract val viewType: Class<*>?

        abstract fun onCreateViewHolder(viewGroup: ViewGroup?): VH?

        abstract fun onBindViewHolder(viewHolder: VH?)

        abstract val isHidden: Boolean

        private fun onBindViewHolderInner(
            viewHolder: RecyclerView.ViewHolder?
        ) {
            onBindViewHolder(viewHolder as VH?)
        }
    }

    private val mItems: Array<ArrayList<Item<*>?>>
    private val mItemViewTypeMap = HashMap<Class<*>?, Int?>()
    private val mViewTypeItemMap = HashMap<Int?, Item<*>?>()

    init {
        mItems = arrayOfNulls<ArrayList<*>>(groups) as Array<ArrayList<Item<*>?>>

        for (i in 0..<groups) {
            mItems[i] = ArrayList<Item<*>?>()
        }

        setHasStableIds(true)
    }

    private fun getItemPositionInternal(groupId: Int, item: Item<*>?): Int {
        val group = mItems[groupId]

        for (i in group.indices) {
            if (group.get(i) === item) {
                return getItemPositionInternal(groupId, i)
            }
        }

        throw RuntimeException("Item not found")
    }

    // "positionInGroup" should include both hidden and visible items
    private fun getItemPositionInternal(group: Int, positionInGroup: Int): Int {
        var result = 0

        for (i in 0..<group) {
            result += getGroupUnhiddenCount(i)
        }

        for (i in 0..<positionInGroup) {
            if (!mItems[group].get(i).mCurrentlyHidden) {
                result++
            }
        }

        return result
    }

    private fun getItemInternal(desiredPosition: Int): Item<*> {
        if (desiredPosition < 0) {
            throw RuntimeException(
                ("Item desiredPosition "
                        + desiredPosition
                        + " is too low")
            )
        }

        var currentPosition = 0

        for (groupId in mItems.indices) {
            val group = mItems[groupId]

            for (positionInGroup in group.indices) {
                val item: Item<*> = group.get(positionInGroup)!!

                if (!item.mCurrentlyHidden) {
                    if (currentPosition == desiredPosition) {
                        return item
                    }

                    currentPosition++
                }
            }
        }

        throw RuntimeException(
            ("Item desiredPosition "
                    + desiredPosition
                    + " is too high")
        )
    }

    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val viewHolder: RecyclerView.ViewHolder = mViewTypeItemMap.get(viewType)!!
            .onCreateViewHolder(viewGroup)!!

        val layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        if (viewHolder.itemView.getLayoutParams() is MarginLayoutParams) {
            val oldLayoutParams = viewHolder.itemView.getLayoutParams() as MarginLayoutParams

            layoutParams.setMargins(
                oldLayoutParams.leftMargin,
                oldLayoutParams.topMargin,
                oldLayoutParams.rightMargin,
                oldLayoutParams.bottomMargin
            )
        }

        viewHolder.itemView.setLayoutParams(layoutParams)

        return viewHolder
    }

    override fun onBindViewHolder(
        viewHolder: RecyclerView.ViewHolder,
        position: Int
    ) {
        getItemInternal(position).onBindViewHolderInner(viewHolder)
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItemInternal(position)
        val viewTypeClass = item.viewType

        var typeId = mItemViewTypeMap.get(viewTypeClass)

        if (typeId == null) {
            typeId = mItemViewTypeMap.size
            mItemViewTypeMap.put(viewTypeClass, typeId)
            mViewTypeItemMap.put(typeId, item)
        }

        return typeId
    }

    private fun getGroupUnhiddenCount(groupId: Int): Int {
        val group = mItems[groupId]

        var result = 0

        for (i in group.indices) {
            if (!group.get(i).mCurrentlyHidden) {
                result++
            }
        }

        return result
    }

    override fun getItemId(position: Int): Long {
        return getItemInternal(position).mUniqueId
    }

    override fun getItemCount(): Int {
        var count = 0

        for (i in mItems.indices) {
            count += getGroupUnhiddenCount(i)
        }

        return count
    }

    fun getItemAtPosition(position: Int): Item<*> {
        return getItemInternal(position)
    }

    fun appendToGroup(group: Int, item: Item<*>) {
        val position = getItemPositionInternal(group + 1, 0)

        mItems[group].add(item)

        if (!item.mCurrentlyHidden) {
            notifyItemInserted(position)
        }
    }

    fun appendToGroup(group: Int, items: MutableCollection<Item<*>>) {
        val position = getItemPositionInternal(group + 1, 0)

        mItems[group].addAll(items)

        for (item in items) {
            item.mCurrentlyHidden = false
        }

        notifyItemRangeInserted(position, items.size)
    }

    fun removeAllFromGroup(groupId: Int) {
        val group = mItems[groupId]

        for (i in group.indices.reversed()) {
            val item: Item<*> = group.get(i)!!
            val position = getItemPositionInternal(groupId, i)

            group.removeAt(i)

            if (!item.mCurrentlyHidden) {
                notifyItemRemoved(position)
            }
        }
    }

    fun removeFromGroup(groupId: Int, item: Item<*>) {
        val group = mItems[groupId]

        for (i in group.indices) {
            if (group.get(i) === item) {
                val position = getItemPositionInternal(groupId, i)

                group.removeAt(i)

                if (!item.mCurrentlyHidden) {
                    notifyItemRemoved(position)
                }

                return
            }
        }

        throw RuntimeException("Item not found")
    }

    fun updateHiddenStatus() {
        var position = 0

        for (groupId in mItems.indices) {
            val group = mItems[groupId]

            for (positionInGroup in group.indices) {
                val item: Item<*> = group.get(positionInGroup)!!

                val wasHidden = item.mCurrentlyHidden
                val isHidden = item.isHidden
                item.mCurrentlyHidden = isHidden

                if (isHidden && !wasHidden) {
                    notifyItemRemoved(position)
                } else if (!isHidden && wasHidden) {
                    notifyItemInserted(position)
                }

                if (!isHidden) {
                    position++
                }
            }
        }
    }

    fun notifyItemChanged(groupId: Int, item: Item<*>?) {
        val position = getItemPositionInternal(groupId, item)
        notifyItemChanged(position)
    }

    companion object {
        @Suppress("PropertyName")
        private val ITEM_UNIQUE_ID_GENERATOR = AtomicLong(100000)
    }
}
