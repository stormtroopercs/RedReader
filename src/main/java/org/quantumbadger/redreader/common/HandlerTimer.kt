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

import android.os.Handler
import android.util.SparseBooleanArray
import androidx.annotation.UiThread

class HandlerTimer(private val mHandler: Handler) {
    private var mNextId = 0

    private val mTimers = SparseBooleanArray()

    private val nextId: Int
        get() {
            mNextId++

            while (mTimers.get(mNextId, false) || mNextId == 0) {
                mNextId++
            }

            return mNextId
        }

    // Should never return 0
    @UiThread
    fun setTimer(delayMs: Long, runnable: Runnable): Int {
        val id = this.nextId
        mTimers.put(id, true)

        mHandler.postDelayed(Runnable {
            if (!mTimers.get(id, false)) {
                return@Runnable
            }
            mTimers.delete(id)
            runnable.run()
        }, delayMs)

        return id
    }

    fun cancelTimer(timerId: Int) {
        mTimers.delete(timerId)
    }
}
