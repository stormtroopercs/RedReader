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

class RRGLProgramColour : RRGLProgramVertices(vertexShaderSource, fragmentShaderSource) {
    private val mColorHandle: Int

    fun activateColour(
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        GLES20.glUniform4f(mColorHandle, r, g, b, a)
    }

    public override fun onActivated() {
        super.onActivated()
        GLES20.glEnableVertexAttribArray(mColorHandle)
    }

    public override fun onDeactivated() {
        super.onDeactivated()
        GLES20.glDisableVertexAttribArray(mColorHandle)
    }

    init {
        setVertexBufferHandle(getAttributeHandle("a_Position"))
        setMatrixUniformHandle(getUniformHandle("u_Matrix"))
        setPixelMatrixHandle(getUniformHandle("u_PixelMatrix"))

        mColorHandle = getUniformHandle("u_Color")
    }

    companion object {
        private val vertexShaderSource = ("uniform mat4 u_Matrix; \n"
                + "uniform mat4 u_PixelMatrix; \n"
                + "attribute vec4 a_Position; \n"
                + "attribute vec2 a_TexCoordinate; \n"
                + "varying vec2 v_TexCoordinate; \n"
                + "void main() {\n"
                + "  v_TexCoordinate = a_TexCoordinate; \n"
                + "  gl_Position = u_PixelMatrix * (u_Matrix * a_Position);\n"
                + "} \n")

        private val fragmentShaderSource = ("precision mediump float; \n"
                + "uniform vec4 u_Color; \n"
                + "void main() { \n"
                + "  gl_FragColor = u_Color; \n"
                + "} \n")
    }
}
