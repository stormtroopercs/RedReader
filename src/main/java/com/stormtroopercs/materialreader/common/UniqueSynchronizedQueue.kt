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
package com.stormtroopercs.materialreader.common

import java.util.LinkedList

class UniqueSynchronizedQueue<E> {
    private val set = HashSet<E?>()
    private val queue = LinkedList<E?>()

    @Synchronized
    fun enqueue(`object`: E?) {
        if (set.add(`object`)) {
            queue.addLast(`object`)
        }
    }

    @Synchronized
    fun dequeue(): E? {
        if (queue.isEmpty()) {
            return null
        }

        val result = queue.removeFirst()
        set.remove(result)
        return result
    }
}
