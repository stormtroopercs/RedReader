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

class MapStreamRethrowExceptions<Input, Output>(
	private val mInput: Stream<Input>,
	private val mOperator: Operator<Input, Output>,
) : Stream<Output>() {
	fun interface Operator<Input, Output> {
		@Throws(Exception::class)
		fun operate(value: Input): Output
	}

	override fun hasNext(): Boolean = mInput.hasNext()

	override fun take(): Output {
		try {
			return mOperator.operate(mInput.take())
		} catch (e: Exception) {
			throw RuntimeException(e)
		}
	}
}
