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
import java.util.Locale

abstract class RRGLProgram(
    vertexShaderSource: String?,
    fragmentShaderSource: String?
) {
    val handle: Int

    private var mFragmentShaderHandle: Int?=null
    private var mVertexShaderHandle: Int?=null

    init {
        this.handle = GLES20.glCreateProgram()

        if (this.handle == 0) {
            throw RuntimeException("Error creating program.")
        }

        compileAndAttachShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource)
        compileAndAttachShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource)

        link()
    }

    private fun compileAndAttachShader(type: Int, source: String?) {
        when (type) {
            GLES20.GL_FRAGMENT_SHADER -> if (mFragmentShaderHandle != null) {
                throw RuntimeException()
            }

            GLES20.GL_VERTEX_SHADER -> if (mVertexShaderHandle != null) {
                throw RuntimeException()
            }

            else -> throw RuntimeException("Unknown shader type.")
        }

        val shaderHandle = GLES20.glCreateShader(type)
        if (shaderHandle == 0) {
            throw RuntimeException("Error creating shader.")
        }

        GLES20.glShaderSource(shaderHandle, source)
        GLES20.glCompileShader(shaderHandle)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shaderHandle, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

        if (compileStatus[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(this.handle)
            GLES20.glDeleteShader(shaderHandle)
            throw RuntimeException(
                String.format(
                    Locale.US,
                    "Shader compile error: \"%s\".",
                    log
                )
            )
        }

        GLES20.glAttachShader(this.handle, shaderHandle)

        when (type) {
            GLES20.GL_FRAGMENT_SHADER -> mFragmentShaderHandle = shaderHandle
            GLES20.GL_VERTEX_SHADER -> mVertexShaderHandle = shaderHandle
            else -> throw RuntimeException("Unknown shader type.")
        }
    }

    private fun link() {
        if (mFragmentShaderHandle == null || mVertexShaderHandle == null) {
            throw RuntimeException()
        }

        GLES20.glLinkProgram(this.handle)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(this.handle, GLES20.GL_LINK_STATUS, linkStatus, 0)

        if (linkStatus[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(this.handle)
            GLES20.glDeleteProgram(this.handle)
            throw RuntimeException(
                String.format(
                    Locale.US,
                    "Linker error: \"%s\".",
                    log
                )
            )
        }

        GLES20.glDetachShader(this.handle, mFragmentShaderHandle!!)
        GLES20.glDetachShader(this.handle, mVertexShaderHandle!!)

        GLES20.glDeleteShader(mFragmentShaderHandle!!)
        GLES20.glDeleteShader(mVertexShaderHandle!!)

        mFragmentShaderHandle = null
        mVertexShaderHandle = null
    }

    fun getAttributeHandle(name: String?): Int {
        return GLES20.glGetAttribLocation(this.handle, name)
    }

    fun getUniformHandle(name: String?): Int {
        return GLES20.glGetUniformLocation(this.handle, name)
    }

    abstract fun onActivated()

    abstract fun onDeactivated()
}
