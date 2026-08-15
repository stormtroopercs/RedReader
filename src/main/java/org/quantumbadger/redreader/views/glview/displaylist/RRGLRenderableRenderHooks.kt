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

import org.quantumbadger.redreader.views.glview.program.RRGLMatrixStack

abstract class RRGLRenderableRenderHooks(private val mEntity: RRGLRenderable) : RRGLRenderable() {
    override fun renderInternal(stack: RRGLMatrixStack?, time: Long) {
        preRender(stack, time)
        mEntity.startRender(stack, time)
        postRender(stack, time)
    }

    override fun onAdded() {
        mEntity.onAdded()
        super.onAdded()
    }

    override fun onRemoved() {
        super.onRemoved()
        mEntity.onRemoved()
    }

    override fun isAnimating(): Boolean {
        return mEntity.isAnimating
    }

    protected abstract fun preRender(stack: RRGLMatrixStack?, time: Long)

    protected abstract fun postRender(stack: RRGLMatrixStack?, time: Long)

    override fun setOverallAlpha(alpha: Float) {
        mEntity.setOverallAlpha(alpha)
    }
}
