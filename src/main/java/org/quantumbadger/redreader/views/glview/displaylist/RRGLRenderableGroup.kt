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

open class RRGLRenderableGroup : RRGLRenderable() {
    private val mChildren = ArrayList<RRGLRenderable>(16)

    fun add(child: RRGLRenderable) {
        mChildren.add(child)
        if (isAdded) {
            child.onAdded()
        }
    }

    fun remove(child: RRGLRenderable) {
        if (isAdded) {
            child.onRemoved()
        }
        mChildren.remove(child)
    }

    override fun onAdded() {
        if (!isAdded) {
            for (entity in mChildren) {
                entity.onAdded()
            }
        }

        super.onAdded()
    }

    override fun renderInternal(matrixStack : RRGLMatrixStack, time: Long) {
        for (i in mChildren.indices) {
            val entity = mChildren.get(i)
            entity.startRender(matrixStack, time)
        }
    }

    override fun onRemoved() {
        super.onRemoved()

        if (!isAdded) {
            for (entity in mChildren) {
                entity.onRemoved()
            }
        }
    }

    override val isAnimating: Boolean
        get() {
        for (i in mChildren.indices) {
            val entity = mChildren.get(i)
            if (entity.isAnimating) {
                return true
            }
        }
        return false
        }

    override fun setOverallAlpha(alpha: Float) {
        for (i in mChildren.indices) {
            val entity = mChildren.get(i)
            entity.setOverallAlpha(alpha)
        }
    }
}
