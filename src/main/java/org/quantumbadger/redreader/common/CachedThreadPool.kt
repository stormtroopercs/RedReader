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
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.quantumbadger.redreader.common

import java.util.ArrayDeque

class CachedThreadPool(private val mMaxThreads: Int, private val mThreadName: String?) {
    private val mTasks = ArrayDeque<Runnable>(16)
    private val mExecutor: Executor by lazy { Executor() }

    private var mThreadNameCount = 0

    private var mRunningThreads = 0
    private var mIdleThreads = 0

    fun add(task: Runnable?) {
        synchronized(mTasks) {
            mTasks.addLast(task)
            (mTasks as Object).notifyAll()
            if (mIdleThreads < 1 && mRunningThreads < mMaxThreads) {
                mRunningThreads++
                Thread(mExecutor, mThreadName + " " + (mThreadNameCount++)).start()
            }
        }
    }

    private inner class Executor : Runnable {
        override fun run() {
            while (true) {
                val taskToRun: Runnable

                synchronized(mTasks) {
                    if (mTasks.isEmpty()) {
                        mIdleThreads++

                        try {
                            (mTasks as Object).wait(30000)
                        } catch (e: InterruptedException) {
                            throw RuntimeException(e)
                        } finally {
                            mIdleThreads--
                        }

                        if (mTasks.isEmpty()) {
                            mRunningThreads--
                            return
                        }
                    }
                    taskToRun = mTasks.removeFirst()
                }

                taskToRun.run()
            }
        }
    }
}
