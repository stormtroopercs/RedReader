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

class PrioritisedCachedThreadPool(private val mMaxThreads: Int, private val mThreadName: String?) {
    private val mTasks = ArrayList<Task?>(16)
    private val mExecutor: Executor by lazy { Executor() }

    private var mThreadNameCount = 0

    private var mRunningThreads = 0
    private var mIdleThreads = 0

    fun add(task: Task?) {
        synchronized(mTasks) {
            mTasks.add(task)
            (mTasks as Object).notifyAll()
            if (mIdleThreads < 1 && mRunningThreads < mMaxThreads) {
                mRunningThreads++
                Thread(mExecutor, mThreadName + " " + (mThreadNameCount++)).start()
            }
        }
    }

    abstract class Task {
        abstract val priority: Priority

        abstract fun run()
    }

    private inner class Executor : Runnable {
        override fun run() {
            while (true) {
                var taskToRun: Task?=null

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
                    var taskIndex = -1
                    for (i in mTasks.indices) {
                        if (taskToRun == null || mTasks.get(i)!!.priority
                                .isHigherPriorityThan(taskToRun.priority)
                        ) {
                            taskToRun = mTasks.get(i)
                            taskIndex = i
                        }
                    }
                    mTasks.removeAt(taskIndex)
                }

                checkNotNull(taskToRun)
                taskToRun.run()
            }
        }
    }
}
