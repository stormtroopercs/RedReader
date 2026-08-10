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

import android.view.MotionEvent
import kotlin.math.sqrt

class MutableFloatPoint2D {
    var x: Float = 0f
    var y: Float = 0f

    fun reset() {
        x = 0f
        y = 0f
    }

    fun set(event: MotionEvent, pointerIndex: Int) {
        x = event.getX(pointerIndex)
        y = event.getY(pointerIndex)
    }

    fun set(other: MutableFloatPoint2D) {
        x = other.x
        y = other.y
    }

    fun set(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    @JvmOverloads
    fun add(rhs: MutableFloatPoint2D, result: MutableFloatPoint2D = this) {
        result.x = x + rhs.x
        result.y = y + rhs.y
    }

    @JvmOverloads
    fun sub(rhs: MutableFloatPoint2D, result: MutableFloatPoint2D = this) {
        result.x = x - rhs.x
        result.y = y - rhs.y
    }

    fun scale(factor: Double) {
        x *= factor.toFloat()
        y *= factor.toFloat()
    }

    fun euclideanDistanceTo(other: MutableFloatPoint2D): Double {
        val xDistance = x - other.x
        val yDistance = y - other.y
        return sqrt((xDistance * xDistance + yDistance * yDistance).toDouble())
    }

    fun distanceSquared(): Float {
        return x * x + y * y
    }
}
