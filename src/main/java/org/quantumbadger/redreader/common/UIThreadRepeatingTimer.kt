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

import android.os.Handler
import android.os.Looper
import androidx.annotation.UiThread
import org.quantumbadger.redreader.common.General.checkThisIsUIThread

class UIThreadRepeatingTimer(private val mIntervalMs: Long, private val mListener: Listener) :
    Runnable {
    interface Listener {
        fun onUIThreadRepeatingTimer(timer: UIThreadRepeatingTimer?)
    }

    private val mHandler = Handler(Looper.getMainLooper())

    private var mShouldTimerRun = false

    @UiThread
    fun startTimer() {
        checkThisIsUIThread()

        mShouldTimerRun = true
        mHandler.postDelayed(this, mIntervalMs)
    }

    @UiThread
    fun stopTimer() {
        checkThisIsUIThread()

        mShouldTimerRun = false
    }


    override fun run() {
        if (mShouldTimerRun) {
            mListener.onUIThreadRepeatingTimer(this)

            if (mShouldTimerRun) {
                mHandler.postDelayed(this, mIntervalMs)
            }
        }
    }
}
