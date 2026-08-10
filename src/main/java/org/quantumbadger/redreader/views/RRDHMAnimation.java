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

abstract class RRDHMAnimation(params: LiveDHM.Params) : RRAnimation() {
    private val mDHM: LiveDHM

    init {
        mDHM = LiveDHM(params)
    }

    override fun handleFrame(nanosSinceAnimationStart: Long): Boolean {
        val microsSinceAnimationStart = nanosSinceAnimationStart / 1000
        val stepLengthMicros = ((LiveDHM.Params.Companion.stepLengthSeconds
                * 1000.0
                * 1000.0)).toLong()

        val desiredStepNumber = ((microsSinceAnimationStart + (stepLengthMicros
                / 2))
                / stepLengthMicros).toInt()

        while (mDHM.getCurrentStep() < desiredStepNumber) {
            mDHM.calculateStep()

            if (mDHM.isEndThresholdReached()) {
                onEndPosition(mDHM.getParams().endPosition)
                return false
            }
        }

        onUpdatedPosition(mDHM.getCurrentPosition())
        return true
    }

    val currentVelocity: Float
        get() = mDHM.getCurrentVelocity()

    protected abstract fun onUpdatedPosition(position: Float)

    protected abstract fun onEndPosition(endPosition: Float)
}
