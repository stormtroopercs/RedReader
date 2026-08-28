/*******************************************************************************
 * This file is part of MaterialReader.
 *
 * MaterialReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MaterialReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MaterialReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.stormtroopercs.materialreader.common.collections

import com.stormtroopercs.materialreader.common.collections.WeakReferenceListManager.ArgOperator

class WeakReferenceListHashMapManager<K, V> {
    private val mData = HashMap<K, WeakReferenceListManager<V>>()

    private var mCleanupCounter: Byte = 0

    @Synchronized
    fun add(key: K, value: V) {
        var list = mData.get(key)

        if (list == null) {
            list = WeakReferenceListManager<V>()
            mData.put(key, list)
        }

        list.add(value)

        // Perform cleanup once for each 256 values which are added
        if ((++mCleanupCounter).toInt() == 0) {
            clean()
        }
    }

    @Synchronized
    fun remove(key: K, value: V) {
        val list = mData.get(key)

        if (list != null) {
            list.remove(value)
        }
    }

    @Synchronized
    fun map(
        key: K,
        operator: WeakReferenceListManager.Operator<V>
    ) {
        val list = mData.get(key)

        if (list != null) {
            list.map(operator)
        }
    }

    @Synchronized
    fun <A> map(
        key: K,
        operator: ArgOperator<V, A>,
        arg: A
    ) {
        val list = mData.get(key)

        if (list != null) {
            list.map<A>(operator, arg)
        }
    }

    @Synchronized
    fun clean() {
        val iterator = mData.entries.iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()

            val list = entry.value
            list.clean()

            if (list.isEmpty) {
                iterator.remove()
            }
        }
    }
}
