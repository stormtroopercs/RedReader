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

class EventListenerSet<E> {
    interface Listener<E> {
        fun onEvent(event: E?)
    }

    private var mMostRecentEvent: E? = null

    private val mListeners = ThreadCheckedVar<HashSet<Listener<E?>>?>(HashSet<Listener<E?>?>())

    fun send(event: E?) {
        mMostRecentEvent = event
        for (listener in mListeners.get()!!) {
            listener.onEvent(event)
        }
    }

    fun register(listener: Listener<E?>?): E? {
        mListeners.get()!!.add(listener!!)
        return mMostRecentEvent
    }

    fun unregister(listener: Listener<E?>?) {
        mListeners.get()!!.remove(listener!!)
    }
}
