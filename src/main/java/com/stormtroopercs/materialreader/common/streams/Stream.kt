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
package com.stormtroopercs.materialreader.common.streams

import com.stormtroopercs.materialreader.common.Consumer

interface Stream<E> {
	fun hasNext(): Boolean

	fun next(): E

	fun filter(predicate: Predicate<E>): Stream<E> = FilterStream<E>(this, predicate)

	fun forEach(consumer: Consumer<E>) {
		while (hasNext()) {
			consumer.consume(next())
		}
	}

	companion object {
		fun <E> from(iterator: MutableIterator<E>): Stream<E> = IteratorStream<E>(iterator)

		fun <E> from(iterable: MutableIterable<E>): Stream<E> = IteratorStream<E>(iterable.iterator())
	}
}
