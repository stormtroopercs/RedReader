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

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils

class RRGLTexture(private val mGLContext: RRGLContext, bitmap: Bitmap?, smooth: Boolean) {
    private val mTextureHandle: Int
    private var mRefCount = 1

    init {
        mTextureHandle = loadTexture(bitmap, smooth)
    }

    fun addReference() {
        mRefCount++
    }

    fun releaseReference() {
        mRefCount--
        if (mRefCount == 0) {
            deleteTexture(mTextureHandle)
        }
    }

    fun activate() {
        mGLContext.activateTextureByHandle(mTextureHandle)
    }

    companion object {
        private fun loadTexture(bitmap: Bitmap?, smooth: Boolean): Int {
            val textureHandle = IntArray(1)
            GLES20.glGenTextures(1, textureHandle, 0)

            if (textureHandle[0] == 0) {
                throw RuntimeException("OpenGL error: glGenTextures failed.")
            }

            val filter = if (smooth) GLES20.GL_LINEAR else GLES20.GL_NEAREST

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                filter
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                filter
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE
            )

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

            return textureHandle[0]
        }

        private fun deleteTexture(handle: Int) {
            val handles = intArrayOf(handle)
            GLES20.glDeleteTextures(1, handles, 0)
        }
    }
}
