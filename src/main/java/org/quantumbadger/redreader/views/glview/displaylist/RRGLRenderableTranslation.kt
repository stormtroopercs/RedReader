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
package org.quantumbadger.redreader.views.glview.displaylist

import org.quantumbadger.redreader.common.MutableFloatPoint2D
import org.quantumbadger.redreader.views.glview.program.RRGLMatrixStack


class RRGLRenderableTranslation(entity: RRGLRenderable?) : RRGLRenderableRenderHooks(entity) {
    private var mPositionX = 0f
    private var mPositionY = 0f

    fun setPosition(x: Float, y: Float) {
        mPositionX = x
        mPositionY = y
    }

    override fun preRender(stack: RRGLMatrixStack, time: Long) {
        stack.pushAndTranslate(mPositionX, mPositionY)
    }

    override fun postRender(stack: RRGLMatrixStack, time: Long) {
        stack.pop()
    }

    fun setPosition(mPositionOffset: MutableFloatPoint2D) {
        mPositionX = mPositionOffset.x
        mPositionY = mPositionOffset.y
    }
}
