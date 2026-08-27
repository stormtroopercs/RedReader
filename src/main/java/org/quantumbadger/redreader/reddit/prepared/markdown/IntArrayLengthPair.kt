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
package org.quantumbadger.redreader.reddit.prepared.markdown

class IntArrayLengthPair(capacity: Int) {
    @JvmField
    val data: IntArray
    @JvmField
    var pos: Int = 0

    init {
        this.data = IntArray(capacity)
    }

    fun clear() {
        pos = 0
    }

    fun append(arr: IntArray) {
        System.arraycopy(arr, 0, data, pos, arr.size)
        pos += arr.size
    }

    fun append(arr: CharArray) {
        for (i in arr.indices) {
            data[pos + i] = arr[i].code
        }

        pos += arr.size
    }

    fun substringAsArray(start: Int): IntArray {
        val result = IntArray(pos - start)
        System.arraycopy(data, start, result, 0, result.size)
        return result
    }
}
