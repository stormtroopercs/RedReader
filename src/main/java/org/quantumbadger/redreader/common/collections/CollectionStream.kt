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
package org.quantumbadger.redreader.common.collections

import java.util.Arrays

class CollectionStream<Type>(iterable: Iterable<Type>) : Stream<Type>() {
    private val mIterator: Iterator<Type>

    init {
        mIterator = iterable.iterator()
    }

    @SafeVarargs
    constructor(vararg array: Type) : this(Arrays.asList<Type>(*array))

    override fun hasNext(): Boolean {
        return mIterator.hasNext()
    }

    override fun take(): Type {
        return mIterator.next()
    }
}
