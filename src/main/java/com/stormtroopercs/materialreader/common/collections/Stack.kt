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

class Stack<E>(initialCapacity: Int) {
	private val mData: ArrayList<E?>

	init {
		mData = ArrayList<E?>(initialCapacity)
	}

	fun push(obj: E?) {
		mData.add(obj)
	}

	fun pop(): E? = mData.removeAt(mData.size - 1)

	val isEmpty: Boolean
		get() = mData.isEmpty()

	fun remove(obj: E?): Boolean = mData.remove(obj)

	fun peek(): E? {
		if (this.isEmpty) {
			return null
		}

		return mData.get(mData.size - 1)
	}
}
