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

import android.opengl.Matrix

class RRGLMatrixStack(private val mGLContext: RRGLContext) {
    private var mTopMatrixPos = 0
    private val mMatrices = FloatArray(16 * 128)

    init {
        setIdentity()
    }

    fun pushAndTranslate(offsetX: Float, offsetY: Float): Int {
        mTopMatrixPos += 16
        Matrix.translateM(
            mMatrices,
            mTopMatrixPos,
            mMatrices,
            mTopMatrixPos - 16,
            offsetX,
            offsetY,
            0f
        )
        return mTopMatrixPos - 16
    }

    fun pushAndScale(factorX: Float, factorY: Float): Int {
        mTopMatrixPos += 16
        Matrix.scaleM(
            mMatrices,
            mTopMatrixPos,
            mMatrices,
            mTopMatrixPos - 16,
            factorX,
            factorY,
            0f
        )
        return mTopMatrixPos - 16
    }

    fun pop(): Int {
        mTopMatrixPos -= 16
        return mTopMatrixPos
    }

    fun setIdentity() {
        Matrix.setIdentityM(mMatrices, mTopMatrixPos)
    }

    fun scale(factorX: Float, factorY: Float, factorZ: Float) {
        Matrix.scaleM(mMatrices, mTopMatrixPos, factorX, factorY, factorZ)
    }

    fun flush() {
        mGLContext.activateMatrix(mMatrices, mTopMatrixPos)
    }

    fun assertAtRoot() {
        if (mTopMatrixPos != 0) {
            throw RuntimeException("assertAtRoot() failed!")
        }

        for (i in 0..15) {
            when (i) {
                0, 5, 10, 15 -> if (mMatrices[i] != 1f) {
                    throw RuntimeException("Root matrix is not identity!")
                }

                else -> if (mMatrices[i] != 0f) {
                    throw RuntimeException("Root matrix is not identity!")
                }
            }
        }
    }
}
