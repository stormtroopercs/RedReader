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
import org.quantumbadger.redreader.views.glview.program.RRGLTexture
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class RRGLRenderableTexturedQuad(
    private val mGLContext: RRGLContext,
    private var mTexture: RRGLTexture
) : RRGLRenderable() {
    fun setTexture(newTexture: RRGLTexture) {
        if (isAdded) {
            mTexture.releaseReference()
        }

        mTexture = newTexture

        if (isAdded) {
            mTexture.addReference()
        }
    }

    override fun onAdded() {
        super.onAdded()
        mTexture.addReference()
    }

    override fun onRemoved() {
        mTexture.releaseReference()
        super.onRemoved()
    }

    override fun renderInternal(matrixStack: RRGLMatrixStack, time: Long) {
        mGLContext.activateProgramTexture()

        mTexture.activate()
        matrixStack.flush()

        mGLContext.activateVertexBuffer(mVertexBuffer)
        mGLContext.activateUVBuffer(mUVBuffer)

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

        private val mUVBuffer: FloatBuffer

        private val uvData = floatArrayOf(
            0f, 0f,
            0f, 1f,
            1f, 0f,
            1f, 1f
        )

        init {
            mVertexBuffer = ByteBuffer.allocateDirect(vertexData.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            mVertexBuffer.put(vertexData).position(0)

            mUVBuffer = ByteBuffer.allocateDirect(uvData.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            mUVBuffer.put(uvData).position(0)
        }
    }
}
