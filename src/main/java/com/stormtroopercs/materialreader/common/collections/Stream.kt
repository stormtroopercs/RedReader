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

abstract class Stream<Type> {
	abstract fun hasNext(): Boolean

	abstract fun take(): Type

	fun <Output> map(operator: MapStream.Operator<Type, Output>): Stream<Output> = MapStream<Type, Output>(this, operator)

	fun <Output> mapRethrowExceptions(
		operator: MapStreamRethrowExceptions.Operator<Type, Output>,
	): Stream<Output> = MapStreamRethrowExceptions<Type, Output>(this, operator)

	fun <Output : MutableCollection<in Type>> collect(
		output: Output,
	): Output {
		while (hasNext()) {
			output.add(take())
		}

		return output
	}
}
