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

class MapStream<Input, Output>(
    private val mInput: Stream<Input?>,
    private val mOperator: Operator<Input?, Output?>
) : Stream<Output?>() {
    interface Operator<Input, Output> {
        fun operate(value: Input?): Output?
    }

    override fun hasNext(): Boolean {
        return mInput.hasNext()
    }

    override fun take(): Output? {
        return mOperator.operate(mInput.take())
    }
}
