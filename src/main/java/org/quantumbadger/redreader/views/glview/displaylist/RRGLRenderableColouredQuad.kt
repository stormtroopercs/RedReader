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

import org.quantumbadger.redreader.views.glview.program.RRGLContext
import org.quantumbadger.redreader.views.glview.program.RRGLMatrixStack
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class RRGLRenderableColouredQuad(private val mGLContext: RRGLContext) : RRGLRenderable() {
    private var mRed = 0f
    private var mGreen = 0f
    private var mBlue = 0f
    private var mAlpha = 0f
    private var mOverallAlpha = 1f

    fun setColour(r: Float, g: Float, b: Float, a: Float) {
        mRed = r
        mGreen = g
        mBlue = b
        mAlpha = a
    }

    override fun setOverallAlpha(alpha: Float) {
        mOverallAlpha = alpha
    }

    override fun renderInternal(matrixStack: RRGLMatrixStack, time: Long) {
        mGLContext.activateProgramColour()

        matrixStack.flush()

        mGLContext.activateVertexBuffer(mVertexBuffer)
        mGLContext.activateColour(mRed, mGreen, mBlue, mAlpha * mOverallAlpha)

        mGLContext.drawTriangleStrip(4)
    }

    companion object {
        private val mVertexBuffer: FloatBuffer

        private val vertexData = floatArrayOf(
            0f, 0f, 0f,
            0f, 1f, 0f,
            1f, 0f, 0f,
            1f, 1f, 0f
        )

        init {
            mVertexBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            mVertexBuffer.put(vertexData).position(0)
        }
    }
}
