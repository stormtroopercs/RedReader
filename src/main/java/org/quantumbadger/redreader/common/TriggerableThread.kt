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
package org.quantumbadger.redreader.common

class TriggerableThread(private val task: Runnable, private val initialDelay: Long) {
    private var thread: InternalTriggerableThread?=null
    private var allowRetrigger = false
    private var shouldRetrigger = false

    @Synchronized
    fun trigger() {
        if (thread == null) {
            thread = InternalTriggerableThread()
            thread!!.start()
        } else if (allowRetrigger) {
            shouldRetrigger = true
        }
    }

    @Synchronized
    private fun onSleepEnd() {
        allowRetrigger = true
    }

    @Synchronized
    private fun shouldThreadContinue(): Boolean {
        if (shouldRetrigger) {
            shouldRetrigger = false
            return true
        } else {
            thread = null
            allowRetrigger = false
            return false
        }
    }

    private inner class InternalTriggerableThread : Thread() {
        override fun run() {
            do {
                try {
                    sleep(initialDelay)
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }

                onSleepEnd()
                task.run()
            } while (shouldThreadContinue())
        }
    }
}
