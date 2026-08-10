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
package org.quantumbadger.redreader.views

class SwipeHistory(len: Int) {
    private val positions: FloatArray
    private val timestamps: LongArray
    private var start = 0
    private var len = 0

    init {
        positions = FloatArray(len)
        timestamps = LongArray(len)
    }

    fun add(position: Float, timestamp: Long) {
        if (len >= positions.size) {
            positions[start] = position
            timestamps[start] = timestamp
            start = (start + 1) % positions.size
        } else {
            positions[(start + len) % positions.size] = position
            timestamps[(start + len) % timestamps.size] = timestamp
            len++
        }
    }

    val mostRecent: Float
        get() = positions[getNthMostRecentIndex(0)]

    fun getAtTimeAgoMs(timeAgo: Long): Float {
        val timestamp = timestamps[getNthMostRecentIndex(0)] - timeAgo
        var result = this.mostRecent

        for (i in 0..<len) {
            val index = getNthMostRecentIndex(i)

            if (timestamp > timestamps[index]) {
                return result
            } else {
                result = positions[index]
            }
        }

        return result
    }

    private fun getNthMostRecentIndex(n: Int): Int {
        if (n >= len || n < 0) {
            throw ArrayIndexOutOfBoundsException(n)
        }
        return (start + len - n - 1) % positions.size
    }

    fun clear() {
        len = 0
        start = 0
    }

    fun size(): Int {
        return len
    }
}
