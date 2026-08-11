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
 */
package org.quantumbadger.redreader.views

import android.view.Choreographer
import dagger.hilt.android.scopes.Singleton
import javax.inject.Inject

/**
 * Hilt-injected choreographer for frame callbacks.
 * Replaces companion object singleton pattern.
 */
@Singleton
class RRChoreographer @Inject constructor() : Choreographer.FrameCallback {
    fun interface Callback {
        fun doFrame(frameTimeNanos: Long)
    }

    private val mCallbacks = arrayOfNulls<Callback>(128)
    private var mCallbackCount = 0
    private var mPosted = false

    @Suppress("PropertyName")
    private val CHOREOGRAPHER: Choreographer = Choreographer.getInstance()

    fun postFrameCallback(callback: Callback) {
        mCallbacks[mCallbackCount] = callback
        mCallbackCount++

        if (!mPosted) {
            CHOREOGRAPHER.postFrameCallback(this)
            mPosted = true
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        val callbackCount = mCallbackCount
        mPosted = false
        mCallbackCount = 0

        for (i in 0..<callbackCount) {
            mCallbacks[i]!!.doFrame(frameTimeNanos)
        }
    }
}
