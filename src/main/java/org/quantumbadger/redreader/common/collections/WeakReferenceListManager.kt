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
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.quantumbadger.redreader.common.collections

import java.lang.ref.WeakReference

class WeakReferenceListManager<E> {
    private val data = ArrayList<WeakReference<E>>()

    @Synchronized
    fun size(): Int {
        return data.size
    }

    @Synchronized
    fun add(`object`: E) {
        data.add(WeakReference<E>(`object`))
    }

    @Synchronized
    fun map(operator: Operator<E>) {
        val iterator = data.iterator()

        while (iterator.hasNext()) {
            val `object` = iterator.next().get()

            if (`object` == null) {
                iterator.remove()
            } else {
                operator.operate(`object`)
            }
        }
    }

    @Synchronized
    fun <A> map(operator: ArgOperator<E, A>, arg: A) {
        val iterator = data.iterator()

        while (iterator.hasNext()) {
            val `object` = iterator.next().get()

            if (`object` == null) {
                iterator.remove()
            } else {
                operator.operate(`object`, arg)
            }
        }
    }

    @Synchronized
    fun remove(`object`: E) {
        val iterator = data.iterator()

        while (iterator.hasNext()) {
            if (iterator.next().get() === `object`) {
                iterator.remove()
            }
        }
    }

    @Synchronized
    fun clean() {
        val iterator = data.iterator()

        while (iterator.hasNext()) {
            val `object` = iterator.next().get()

            if (`object` == null) {
                iterator.remove()
            }
        }
    }

    @get:Synchronized
    val isEmpty: Boolean
        get() = data.isEmpty()

    fun interface Operator<E> {
        fun operate(`object`: E)
    }

    interface ArgOperator<E, A> {
        fun operate(`object`: E, arg: A)
    }
}
