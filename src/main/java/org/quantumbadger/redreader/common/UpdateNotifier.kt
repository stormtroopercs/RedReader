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
package org.quantumbadger.redreader.common

import java.lang.ref.WeakReference
import java.util.LinkedList

abstract class UpdateNotifier<E> {
    private val listeners = LinkedList<WeakReference<E>>()

    @Synchronized
    fun addListener(updateListener: E) {
        listeners.add(WeakReference<E>(updateListener))
    }

    @Synchronized
    fun removeListener(updateListener: E) {
        val iter = listeners.iterator()

        while (iter.hasNext()) {
            val listener = iter.next()!!.get()

            if (listener == null || listener === updateListener) {
                iter.remove()
            }
        }
    }

    @Synchronized
    fun updateAllListeners() {
        val iter = listeners.iterator()

        while (iter.hasNext()) {
            val listener = iter.next()!!.get()

            if (listener == null) {
                iter.remove()
            } else {
                notifyListener(listener)
            }
        }
    }

    protected abstract fun notifyListener(listener: E)
}
