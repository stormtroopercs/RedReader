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

import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import org.quantumbadger.redreader.views.glview.RRGLSurfaceView
import org.quantumbadger.redreader.views.glview.Refreshable
import org.quantumbadger.redreader.views.glview.program.RRGLContext
import org.quantumbadger.redreader.views.glview.program.RRGLMatrixStack
import org.quantumbadger.redreader.views.imageview.FingerTracker.FingerListener
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class RRGLDisplayListRenderer(
    private val mDisplayListManager: DisplayListManager,
    private val mSurfaceView: RRGLSurfaceView
) : GLSurfaceView.Renderer, Refreshable {
    interface DisplayListManager : FingerListener {
        fun onGLSceneCreate(
            scene: RRGLDisplayList?,
            context: RRGLContext?,
            refreshable: Refreshable?
        )

        fun onGLSceneResolutionChange(
            scene: RRGLDisplayList?,
            context: RRGLContext?,
            width: Int,
            height: Int
        )

        fun onGLSceneUpdate(scene: RRGLDisplayList?, context: RRGLContext?): Boolean

        fun onUIAttach()

        fun onUIDetach()
    }

    private val mPixelMatrix = FloatArray(16)

    private var mScene: RRGLDisplayList? = null
    private var mGLContext: RRGLContext? = null
    private var mMatrixStack: RRGLMatrixStack? = null

    override fun onSurfaceCreated(ignore: GL10?, config: EGLConfig?) {
        mGLContext = RRGLContext(mSurfaceView.getContext())
        mMatrixStack = RRGLMatrixStack(mGLContext)
        mScene = RRGLDisplayList()

        mGLContext!!.setClearColor(0f, 0f, 0f, 1f)

        mDisplayListManager.onGLSceneCreate(mScene, mGLContext, this)
    }

    override fun onSurfaceChanged(ignore: GL10?, width: Int, height: Int) {
        mGLContext!!.setViewport(width, height)

        val hScale = 2f / width.toFloat()
        val vScale = -2f / height.toFloat()

        Matrix.setIdentityM(mPixelMatrix, 0)
        Matrix.translateM(mPixelMatrix, 0, -1f, 1f, 0f)
        Matrix.scaleM(mPixelMatrix, 0, hScale, vScale, 1f)

        mDisplayListManager.onGLSceneResolutionChange(mScene, mGLContext, width, height)
    }

    private var frames = 0
    private var startTime: Long = -1

    override fun onDrawFrame(ignore: GL10?) {
        val time = System.currentTimeMillis()

        if (startTime == -1L) {
            startTime = time
        }

        frames++

        if (time - startTime >= 1000) {
            startTime = time
            Log.i("FPS", "Frames: " + frames)
            frames = 0
        }

        val animating = mDisplayListManager.onGLSceneUpdate(mScene, mGLContext)

        mGLContext!!.clear()

        mGLContext!!.activatePixelMatrix(mPixelMatrix, 0)

        mMatrixStack!!.assertAtRoot()
        mScene!!.startRender(mMatrixStack, time)
        mMatrixStack!!.assertAtRoot()

        if (animating || mScene!!.isAnimating()) {
            mSurfaceView.requestRender()
        }
    }

    override fun refresh() {
        mSurfaceView.requestRender()
    }
}
