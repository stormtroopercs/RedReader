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
package org.quantumbadger.redreader.common.streams

class FilterStream<E>(
    private val mInner: Stream<E>,
    private val mPredicate: Predicate<E>
) : Stream<E> {
    private var mHasNext = true
    private var mNext: E? = null

    init {
        moveToNext()
    }

    private fun moveToNext() {
        while (mInner.hasNext()) {
            val next = mInner.next()

            if (mPredicate.matches(next)) {
                mNext = next
                return
            }
        }

        mNext = null
        mHasNext = false
    }

    override fun hasNext(): Boolean {
        return mHasNext
    }

    override fun next(): E {
        val result = mNext!!
        moveToNext()
        return result
    }
}
