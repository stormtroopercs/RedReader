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
package org.quantumbadger.redreader.views

abstract class RRAnimation : RRChoreographer.Callback {
    private var mFirstFrameNanos: Long = -1

    private var mStarted = false
    private var mStopped = false

    fun start() {
        if (mStarted) {
            throw RuntimeException("Attempted to start animation twice!")
        }

        mStarted = true

        RRChoreographer.Companion.INSTANCE.postFrameCallback(this)
    }

    fun stop() {
        if (!mStarted) {
            throw RuntimeException("Attempted to stop animation before it's started!")
        }

        if (mStopped) {
            throw RuntimeException("Attempted to stop animation twice!")
        }

        mStopped = true
    }

    // Return true to continue animating
    protected abstract fun handleFrame(nanosSinceAnimationStart: Long): Boolean

    override fun doFrame(frameTimeNanos: Long) {
        if (mStopped) {
            return
        }

        if (mFirstFrameNanos == -1L) {
            mFirstFrameNanos = frameTimeNanos
        }

        if (handleFrame(frameTimeNanos - mFirstFrameNanos)) {
            RRChoreographer.Companion.INSTANCE.postFrameCallback(this)
        }
    }
}
