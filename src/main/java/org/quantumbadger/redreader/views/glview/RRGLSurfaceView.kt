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
package org.quantumbadger.redreader.views.glview

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import org.quantumbadger.redreader.views.glview.displaylist.RRGLDisplayListRenderer
import org.quantumbadger.redreader.views.glview.displaylist.RRGLDisplayListRenderer.DisplayListManager
import org.quantumbadger.redreader.views.imageview.FingerTracker

class RRGLSurfaceView(
    context: Context?,
    displayListManager: DisplayListManager
) : GLSurfaceView(context) {
    private val mFingerTracker: FingerTracker
    private val mDisplayListManager: DisplayListManager

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        setRenderer(RRGLDisplayListRenderer(displayListManager, this))
        setRenderMode(RENDERMODE_WHEN_DIRTY)

        mFingerTracker = FingerTracker(displayListManager)
        mDisplayListManager = displayListManager
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        mFingerTracker.onTouchEvent(event)
        requestRender()
        return true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mDisplayListManager.onUIAttach()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mDisplayListManager.onUIDetach()
    }
}
