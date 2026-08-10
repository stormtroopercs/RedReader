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

abstract class Stream<Type> {
    abstract fun hasNext(): Boolean

    abstract fun take(): Type?

    fun <Output> map(operator: MapStream.Operator<Type?, Output?>?): Stream<Output?> {
        return MapStream<Type?, Output?>(this, operator)
    }

    fun <Output> mapRethrowExceptions(
        operator: MapStreamRethrowExceptions.Operator<Type?, Output?>?
    ): Stream<Output?> {
        return MapStreamRethrowExceptions<Type?, Output?>(this, operator)
    }

    fun <Output : MutableCollection<in Type?>?> collect(
        output: Output?
    ): Output? {
        while (hasNext()) {
            output!!.add(take())
        }

        return output
    }
}
