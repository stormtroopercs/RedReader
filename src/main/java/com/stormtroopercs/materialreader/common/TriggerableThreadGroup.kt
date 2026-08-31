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
package com.stormtroopercs.materialreader.common

class TriggerableThreadGroup(threads: Int, task: Runnable) {
	private val mThreads: Array<TriggerableThread?>
	private var mNextThreadToTrigger = 0

	init {
		mThreads = arrayOfNulls<TriggerableThread>(threads)

		for (i in 0..<threads) {
			mThreads[i] = TriggerableThread(task, 0)
		}
	}

	fun triggerOne() {
		mThreads[mNextThreadToTrigger]!!.trigger()
		mNextThreadToTrigger = (mNextThreadToTrigger + 1) % mThreads.size
	}
}
