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

import android.opengl.GLES20
import java.nio.FloatBuffer

abstract class RRGLProgramVertices(vertexShaderSource: String?, fragmentShaderSource: String?) :
    RRGLProgram(vertexShaderSource, fragmentShaderSource) {
    private var mVertexBufferHandle = 0
    private var mMatrixUniformHandle = 0
    private var mPixelMatrixUniformHandle = 0

    fun activateVertexBuffer(vertexBuffer: FloatBuffer?) {
        GLES20.glVertexAttribPointer(
            mVertexBufferHandle,
            3,
            GLES20.GL_FLOAT,
            false,
            0,
            vertexBuffer
        )
    }

    fun drawTriangleStrip(vertices: Int) {
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertices)
    }

    protected fun setVertexBufferHandle(handle: Int) {
        mVertexBufferHandle = handle
    }

    protected fun setMatrixUniformHandle(handle: Int) {
        mMatrixUniformHandle = handle
    }

    protected fun setPixelMatrixHandle(handle: Int) {
        mPixelMatrixUniformHandle = handle
    }

    fun activateMatrix(buf: FloatArray?, offset: Int) {
        GLES20.glUniformMatrix4fv(mMatrixUniformHandle, 1, false, buf, offset)
    }

    fun activatePixelMatrix(buf: FloatArray?, offset: Int) {
        GLES20.glUniformMatrix4fv(mPixelMatrixUniformHandle, 1, false, buf, offset)
    }

    override fun onActivated() {
        GLES20.glEnableVertexAttribArray(mVertexBufferHandle)
    }

    override fun onDeactivated() {
        GLES20.glDisableVertexAttribArray(mVertexBufferHandle)
    }
}
