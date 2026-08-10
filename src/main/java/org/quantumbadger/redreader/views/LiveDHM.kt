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

import kotlin.math.abs

class LiveDHM(val params: Params) {
    class Params {
        var startPosition: Float = 0f
        var endPosition: Float = 0f

        var startVelocity: Float = 0f

        companion object {
            const val accelerationCoefficient: Float = 30f
            const val velocityDamping: Float = 0.87f

            val stepLengthSeconds: Float = 1f / 60f

            const val thresholdPositionDifference: Float = 0.49f
            const val thresholdVelocity: Float = 15f
            const val thresholdMaxSteps: Int = 1000
        }
    }

    var currentStep: Int = 0
        private set

    var currentPosition: Float
        private set
    var currentVelocity: Float
        private set

    init {
        this.currentPosition = params.startPosition
        this.currentVelocity = params.startVelocity
    }

    fun calculateStep() {
        this.currentVelocity -= Params.Companion.stepLengthSeconds * ((this.currentPosition - params.endPosition)
                * Params.Companion.accelerationCoefficient)
        this.currentVelocity *= Params.Companion.velocityDamping
        this.currentPosition += this.currentVelocity * Params.Companion.stepLengthSeconds
        this.currentStep++
    }

    val isEndThresholdReached: Boolean
        get() {
            if (this.currentStep >= Params.Companion.thresholdMaxSteps) {
                return true
            }

            if (abs(this.currentPosition) > Params.Companion.thresholdPositionDifference) {
                return false
            }

            if (abs(this.currentVelocity) > Params.Companion.thresholdVelocity) {
                return false
            }

            return true
        }
}
