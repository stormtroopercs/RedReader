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
package org.quantumbadger.redreader.views.glview.program

import android.content.Context
import android.opengl.GLES20
import org.quantumbadger.redreader.common.General.dpToPixels
import java.nio.FloatBuffer
import org.quantumbadger.redreader.common.General

class RRGLContext(context: Context) {
    private val mProgramTexture: RRGLProgramTexture
    private val mProgramColour: RRGLProgramColour

    private var mPixelMatrix: FloatArray? = null
    private var mPixelMatrixOffset = 0

    private var mProgramCurrent: RRGLProgramVertices?=null

    private val mContext: Context

    init {
        mProgramTexture = RRGLProgramTexture()
        mProgramColour = RRGLProgramColour()
        mContext = context
    }

    fun dpToPixels(dp: Float): Int {
        return dpToPixels(mContext, dp)
    }

    val screenDensity: Float
        get() = mContext.getResources().getDisplayMetrics().density

    fun activateProgramColour() {
        if (mProgramCurrent !== mProgramColour) {
            activateProgram(mProgramColour)
        }
    }

    fun activateProgramTexture() {
        if (mProgramCurrent !== mProgramTexture) {
            activateProgram(mProgramTexture)
        }
    }

    private fun activateProgram(program: RRGLProgramVertices) {
        if (mProgramCurrent != null) {
            mProgramCurrent!!.onDeactivated()
        }

        GLES20.glUseProgram(program.handle)
        mProgramCurrent = program

        program.onActivated()

        if (mPixelMatrix != null) {
            program.activatePixelMatrix(mPixelMatrix, mPixelMatrixOffset)
        }
    }

    fun activateTextureByHandle(textureHandle: Int) {
        mProgramTexture.activateTextureByHandle(textureHandle)
    }

    fun activateVertexBuffer(vertexBuffer: FloatBuffer?) {
        mProgramCurrent!!.activateVertexBuffer(vertexBuffer)
    }

    fun activateColour(
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        mProgramColour.activateColour(r, g, b, a)
    }

    fun activateUVBuffer(uvBuffer: FloatBuffer?) {
        mProgramTexture.activateUVBuffer(uvBuffer)
    }

    fun drawTriangleStrip(vertices: Int) {
        mProgramCurrent!!.drawTriangleStrip(vertices)
    }

    fun activateMatrix(buf: FloatArray?, offset: Int) {
        mProgramCurrent!!.activateMatrix(buf, offset)
    }

    fun activatePixelMatrix(buf: FloatArray?, offset: Int) {
        mPixelMatrix = buf
        mPixelMatrixOffset = offset

        if (mProgramCurrent != null) {
            mProgramCurrent!!.activatePixelMatrix(buf, offset)
        }
    }

    fun setClearColor(r: Float, g: Float, b: Float, a: Float) {
        GLES20.glClearColor(r, g, b, a)
    }

    fun clear() {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)
    }

    fun setViewport(width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }
}
