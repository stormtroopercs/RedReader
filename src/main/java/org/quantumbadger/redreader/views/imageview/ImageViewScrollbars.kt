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
package org.quantumbadger.redreader.views.imageview

import org.quantumbadger.redreader.common.MutableFloatPoint2D
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderable
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderableBlend
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderableColouredQuad
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderableGroup
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderableScale
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderableTranslation
import org.quantumbadger.redreader.views.glview.program.RRGLContext
import org.quantumbadger.redreader.views.glview.program.RRGLMatrixStack

class ImageViewScrollbars(
    glContext: RRGLContext,
    private val mCoordinateHelper: CoordinateHelper,
    private val mImageResX: Int,
    private val mImageResY: Int
) : RRGLRenderable() {
    private val mRenderable: RRGLRenderableBlend

    // Vertical scroll bar
    private val mVScroll: RRGLRenderableGroup
    private val mVScrollMarkerTranslation: RRGLRenderableTranslation
    private val mVScrollMarkerScale: RRGLRenderableScale
    private val mVScrollBarTranslation: RRGLRenderableTranslation
    private val mVScrollBarScale: RRGLRenderableScale
    private val mVScrollBorderTranslation: RRGLRenderableTranslation
    private val mVScrollBorderScale: RRGLRenderableScale

    // Horizontal scroll bar
    private val mHScroll: RRGLRenderableGroup
    private val mHScrollMarkerTranslation: RRGLRenderableTranslation
    private val mHScrollMarkerScale: RRGLRenderableScale
    private val mHScrollBarTranslation: RRGLRenderableTranslation
    private val mHScrollBarScale: RRGLRenderableScale
    private val mHScrollBorderTranslation: RRGLRenderableTranslation
    private val mHScrollBorderScale: RRGLRenderableScale

    private var mResX = 0
    private var mResY = 0

    private val mDimMarginSides: Int
    private val mDimMarginEnds: Int
    private val mDimBarWidth: Int
    private val mDimBorderWidth: Int

    private var mShowUntil: Long = -1
    private var mCurrentAlpha = 1f
    private var mIsVisible = true

    init {
        val group = RRGLRenderableGroup()
        mRenderable = RRGLRenderableBlend(group)

        mDimMarginSides = glContext.dpToPixels(10f)
        mDimMarginEnds = glContext.dpToPixels(20f)
        mDimBarWidth = glContext.dpToPixels(6f)
        mDimBorderWidth = glContext.dpToPixels(1f)

        // Vertical scroll bar
        run {
            mVScroll = RRGLRenderableGroup()
            group.add(mVScroll)

            val vScrollMarker =                 RRGLRenderableColouredQuad(glContext)
            val vScrollBar =                 RRGLRenderableColouredQuad(glContext)
            val vScrollBorder =                 RRGLRenderableColouredQuad(glContext)

            vScrollMarker.setColour(1f, 1f, 1f, 0.8f)
            vScrollBar.setColour(0f, 0f, 0f, 0.5f)
            vScrollBorder.setColour(1f, 1f, 1f, 0.5f)

            mVScrollMarkerScale = RRGLRenderableScale(vScrollMarker)
            mVScrollBarScale = RRGLRenderableScale(vScrollBar)
            mVScrollBorderScale = RRGLRenderableScale(vScrollBorder)

            mVScrollMarkerTranslation =                 RRGLRenderableTranslation(mVScrollMarkerScale)
            mVScrollBarTranslation = RRGLRenderableTranslation(mVScrollBarScale)
            mVScrollBorderTranslation =                 RRGLRenderableTranslation(mVScrollBorderScale)

            mVScroll.add(mVScrollBorderTranslation)
            mVScroll.add(mVScrollBarTranslation)
            mVScroll.add(mVScrollMarkerTranslation)
        }

        // Horizontal scroll bar
        run {
            mHScroll = RRGLRenderableGroup()
            group.add(mHScroll)

            val hScrollMarker =                 RRGLRenderableColouredQuad(glContext)
            val hScrollBar =                 RRGLRenderableColouredQuad(glContext)
            val hScrollBorder =                 RRGLRenderableColouredQuad(glContext)

            hScrollMarker.setColour(1f, 1f, 1f, 0.8f)
            hScrollBar.setColour(0f, 0f, 0f, 0.5f)
            hScrollBorder.setColour(1f, 1f, 1f, 0.5f)

            mHScrollMarkerScale = RRGLRenderableScale(hScrollMarker)
            mHScrollBarScale = RRGLRenderableScale(hScrollBar)
            mHScrollBorderScale = RRGLRenderableScale(hScrollBorder)

            mHScrollMarkerTranslation =                 RRGLRenderableTranslation(mHScrollMarkerScale)
            mHScrollBarTranslation = RRGLRenderableTranslation(mHScrollBarScale)
            mHScrollBorderTranslation =                 RRGLRenderableTranslation(mHScrollBorderScale)

            mHScroll.add(mHScrollBorderTranslation)
            mHScroll.add(mHScrollBarTranslation)
            mHScroll.add(mHScrollMarkerTranslation)
        }
    }

    fun update() {
        // TODO avoid GC

        val tmp1 = MutableFloatPoint2D()
        val tmp2 = MutableFloatPoint2D()

        mCoordinateHelper.convertScreenToScene(tmp1, tmp2)
        val xStart = tmp2.x / mImageResX.toFloat()
        val yStart = tmp2.y / mImageResY.toFloat()

        tmp1.set(mResX.toFloat(), mResY.toFloat())

        mCoordinateHelper.convertScreenToScene(tmp1, tmp2)
        val xEnd = tmp2.x / mImageResX.toFloat()
        val yEnd = tmp2.y / mImageResY.toFloat()

        // Vertical scroll bar
        if (yStart < EPSILON && yEnd > 1 - EPSILON) {
            mVScroll.hide()
        } else {
            mVScroll.show()

            val vScrollTotalHeight = (mResY - 2 * mDimMarginEnds).toFloat()

            val vScrollHeight = (yEnd - yStart) * vScrollTotalHeight
            val vScrollTop = yStart * vScrollTotalHeight + mDimMarginEnds
            val vScrollLeft = (mResX - mDimBarWidth - mDimMarginSides).toFloat()

            mVScrollBorderTranslation.setPosition(
                vScrollLeft - mDimBorderWidth,
                (mDimMarginEnds - mDimBorderWidth).toFloat()
            )
            mVScrollBorderScale.setScale(
                (mDimBarWidth + 2 * mDimBorderWidth).toFloat(),
                vScrollTotalHeight + 2 * mDimBorderWidth
            )

            mVScrollBarTranslation.setPosition(vScrollLeft, mDimMarginEnds.toFloat())
            mVScrollBarScale.setScale(mDimBarWidth.toFloat(), vScrollTotalHeight)

            mVScrollMarkerTranslation.setPosition(vScrollLeft, vScrollTop)
            mVScrollMarkerScale.setScale(mDimBarWidth.toFloat(), vScrollHeight)
        }

        // Horizontal scroll bar
        if (xStart < EPSILON && xEnd > 1 - EPSILON) {
            mHScroll.hide()
        } else {
            mHScroll.show()

            val hScrollTotalWidth = (mResX - 2 * mDimMarginEnds).toFloat()

            val hScrollWidth = (xEnd - xStart) * hScrollTotalWidth
            val hScrollLeft = xStart * hScrollTotalWidth + mDimMarginEnds
            val hScrollTop = (mResY - mDimBarWidth - mDimMarginSides).toFloat()

            mHScrollBorderTranslation.setPosition(
                (mDimMarginEnds - mDimBorderWidth).toFloat(),
                hScrollTop - mDimBorderWidth
            )
            mHScrollBorderScale.setScale(
                hScrollTotalWidth + 2 * mDimBorderWidth,
                (mDimBarWidth + mDimBorderWidth * 2).toFloat()
            )

            mHScrollBarTranslation.setPosition(mDimMarginEnds.toFloat(), hScrollTop)
            mHScrollBarScale.setScale(hScrollTotalWidth, mDimBarWidth.toFloat())

            mHScrollMarkerTranslation.setPosition(hScrollLeft, hScrollTop)
            mHScrollMarkerScale.setScale(hScrollWidth, mDimBarWidth.toFloat())
        }
    }

    @Synchronized
    fun setResolution(x: Int, y: Int) {
        mResX = x
        mResY = y
    }

    override fun onAdded() {
        super.onAdded()
        mRenderable.onAdded()
    }

    override fun onRemoved() {
        mRenderable.onRemoved()
        super.onRemoved()
    }

    @Synchronized
    override fun isAnimating(): Boolean {
        return mIsVisible
    }

    @Synchronized
    fun showBars() {
        mShowUntil = System.currentTimeMillis() + 600
        mIsVisible = true
        mCurrentAlpha = 1f
    }

    @Synchronized
    override fun renderInternal(stack : RRGLMatrixStack, time: Long) {
        if (mIsVisible && time > mShowUntil) {
            mCurrentAlpha -= ALPHA_STEP

            if (mCurrentAlpha < 0) {
                mIsVisible = false
                mCurrentAlpha = 0f
            }
        }

        mRenderable.setOverallAlpha(mCurrentAlpha)

        mRenderable.startRender(stack, time)
    }

    companion object {
        private const val EPSILON = 0.0001f

        private const val ALPHA_STEP = 0.05f
    }
}