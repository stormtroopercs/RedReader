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
package org.quantumbadger.redreader.views.imageview

import org.quantumbadger.redreader.common.MutableFloatPoint2D

class CoordinateHelper {
    var scale: Float = 1.0f
    val positionOffset: MutableFloatPoint2D = MutableFloatPoint2D()

    fun getPositionOffset(result: MutableFloatPoint2D) {
        result.set(this.positionOffset)
    }

    fun convertScreenToScene(
        screenPos: MutableFloatPoint2D,
        output: MutableFloatPoint2D
    ) {
        output.x = (screenPos.x - positionOffset.x) / this.scale
        output.y = (screenPos.y - positionOffset.y) / this.scale
    }

    fun convertSceneToScreen(
        scenePos: MutableFloatPoint2D,
        output: MutableFloatPoint2D
    ) {
        output.x = scenePos.x * this.scale + positionOffset.x
        output.y = scenePos.y * this.scale + positionOffset.y
    }

    fun scaleAboutScreenPoint(
        screenPos: MutableFloatPoint2D,
        scaleFactor: Float
    ) {
        setScaleAboutScreenPoint(screenPos, this.scale * scaleFactor)
    }

    fun setScaleAboutScreenPoint(
        screenPos: MutableFloatPoint2D,
        scale: Float
    ) {
        val oldScenePos = MutableFloatPoint2D()
        convertScreenToScene(screenPos, oldScenePos)

        this.scale = scale

        val newScreenPos = MutableFloatPoint2D()
        convertSceneToScreen(oldScenePos, newScreenPos)

        translateScreen(newScreenPos, screenPos)
    }

    fun translateScreen(
        oldScreenPos: MutableFloatPoint2D?,
        newScreenPos: MutableFloatPoint2D?
    ) {
        positionOffset.add(newScreenPos)
        positionOffset.sub(oldScreenPos)
    }
}
