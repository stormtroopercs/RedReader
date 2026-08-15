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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import org.quantumbadger.redreader.common.MutableFloatPoint2D
import org.quantumbadger.redreader.common.UIThreadRepeatingTimer
import org.quantumbadger.redreader.common.collections.Stack
import org.quantumbadger.redreader.views.glview.Refreshable
import org.quantumbadger.redreader.views.glview.displaylist.RRGLDisplayList
import org.quantumbadger.redreader.views.glview.displaylist.RRGLDisplayListRenderer.DisplayListManager
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderableGroup
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderableScale
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderableTexturedQuad
import org.quantumbadger.redreader.views.glview.displaylist.RRGLRenderableTranslation
import org.quantumbadger.redreader.views.glview.program.RRGLContext
import org.quantumbadger.redreader.views.glview.program.RRGLTexture
import org.quantumbadger.redreader.views.imageview.FingerTracker.Finger
import java.util.Arrays
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow

class ImageViewDisplayListManager(
    private val mImageTileSource: ImageTileSource,
    private val mListener: Listener
) : DisplayListManager, UIThreadRepeatingTimer.Listener, ImageViewTileLoader.Listener {
    interface Listener : BasicGestureHandler.Listener {
        fun onImageViewDLMOutOfMemory()

        fun onImageViewDLMException(t: Throwable?)
    }

    private var mOverallTranslation: RRGLRenderableTranslation?=null
    private var mOverallScale: RRGLRenderableScale?=null

    private val mHTileCount: Int
    private val mVTileCount: Int
    private val mTileSize: Int

    private var mNotLoadedTexture: RRGLTexture?=null

    private var mResolutionX = 0
    private var mResolutionY = 0

    private val mTileLoaders: Array<Array<MultiScaleTileManager?>?>
    private val mTiles: Array<Array<RRGLRenderableTexturedQuad?>?>
    private var mTileVisibility: Array<BooleanArray?>
    private var mTileLoaded: Array<BooleanArray>
    private var mLastSampleSize = 1

    private var mRefreshable: Refreshable?=null

    private enum class TouchState {
        ONE_FINGER_DOWN,
        ONE_FINGER_DRAG,
        TWO_FINGER_PINCH,
        DOUBLE_TAP_WAIT_NO_FINGERS_DOWN,
        DOUBLE_TAP_ONE_FINGER_DOWN,
        DOUBLE_TAP_ONE_FINGER_DRAG
    }

    private val mCoordinateHelper = CoordinateHelper()
    private var mBoundsHelper: BoundsHelper?=null

    private var mCurrentTouchState: TouchState?=null

    private var mDragFinger: Finger?=null
    private var mPinchFinger1: Finger?=null
    private var mPinchFinger2: Finger?=null
    private val mSpareFingers = Stack<Finger?>(8)

    private val mDoubleTapGapTimer = UIThreadRepeatingTimer(50, this)

    private var mFirstTapReleaseTime: Long = -1

    private var mScaleAnimation: ImageViewScaleAnimation?=null

    private var mScrollbars: ImageViewScrollbars?=null

    private var mScreenDensity = 1f

    @Synchronized
    override fun onGLSceneCreate(
        scene: RRGLDisplayList,
        glContext: RRGLContext,
        refreshable: Refreshable
    ) {
        mTileVisibility = Array<BooleanArray?>(mHTileCount) { BooleanArray(mVTileCount) }
        mTileLoaded = Array<BooleanArray?>(mHTileCount) { BooleanArray(mVTileCount) }
        mRefreshable = refreshable
        mScreenDensity = glContext.screenDensity

        mNotLoadedTexture = RRGLTexture(glContext, NOT_LOADED_BITMAP, false)

        val group = RRGLRenderableGroup()

        mOverallScale = RRGLRenderableScale(group)
        mOverallTranslation = RRGLRenderableTranslation(mOverallScale)
        scene.add(mOverallTranslation)

        for (x in 0..<mHTileCount) {
            for (y in 0..<mVTileCount) {
                val quad =                     RRGLRenderableTexturedQuad(glContext, mNotLoadedTexture)
                mTiles[x]!![y] = quad

                val scale = RRGLRenderableScale(quad)

                var tileWidth = mTileSize
                var tileHeight = mTileSize

                val imageWidth = mImageTileSource.width
                val imageHeight = mImageTileSource.height

                if (x == mHTileCount - 1 && imageWidth % mTileSize != 0) {
                    tileWidth = imageWidth % mTileSize
                }

                if (y == mVTileCount - 1 && imageHeight % mTileSize != 0) {
                    tileHeight = imageHeight % mTileSize
                }

                scale.setScale(tileWidth.toFloat(), tileHeight.toFloat())

                val translation =                     RRGLRenderableTranslation(scale)
                translation.setPosition((x * mTileSize).toFloat(), (y * mTileSize).toFloat())

                group.add(translation)
            }
        }

        mScrollbars = ImageViewScrollbars(
            glContext,
            mCoordinateHelper,
            mImageTileSource.width,
            mImageTileSource.height
        )

        scene.add(mScrollbars)
    }

    @Synchronized
    override fun onGLSceneResolutionChange(
        scene: RRGLDisplayList?,
        context: RRGLContext?,
        width: Int,
        height: Int
    ) {
        mResolutionX = width
        mResolutionY = height

        val setInitialScale = (mBoundsHelper == null)

        mBoundsHelper = BoundsHelper(
            width, height,
            mImageTileSource.width, mImageTileSource.height,
            mCoordinateHelper
        )

        if (setInitialScale) {
            mBoundsHelper!!.applyMinScale()
        }

        mScrollbars!!.setResolution(width, height)
        mScrollbars!!.showBars()
    }

    @Synchronized
    override fun onGLSceneUpdate(
        scene: RRGLDisplayList?,
        context: RRGLContext?
    ): Boolean {
        if (mScaleAnimation != null) {
            if (!mScaleAnimation!!.onStep()) {
                mScaleAnimation = null
            }
        }

        if (mBoundsHelper != null) {
            mBoundsHelper!!.applyBounds()
        }

        val positionOffset = mCoordinateHelper.getPositionOffset()
        val scale = mCoordinateHelper.scale

        mOverallTranslation!!.setPosition(positionOffset)
        mOverallScale!!.setScale(scale, scale)

        mScrollbars!!.update()

        val sampleSize = pickSampleSize()

        if (mLastSampleSize != sampleSize) {
            for (arr in mTileLoaded) {
                Arrays.fill(arr, false)
            }

            mLastSampleSize = sampleSize
        }

        val firstVisiblePixelX = -positionOffset.x / scale
        val firstVisiblePixelY = -positionOffset.y / scale

        val firstVisibleTileX = floor((firstVisiblePixelX / mTileSize).toDouble()).toInt()
        val firstVisibleTileY = floor((firstVisiblePixelY / mTileSize).toDouble()).toInt()

        val lastVisiblePixelX = firstVisiblePixelX + mResolutionX.toFloat() / scale
        val lastVisiblePixelY = firstVisiblePixelY + mResolutionY.toFloat() / scale

        val lastVisibleTileX = ceil((lastVisiblePixelX / mTileSize).toDouble()).toInt()
        val lastVisibleTileY = ceil((lastVisiblePixelY / mTileSize).toDouble()).toInt()

        val desiredScaleIndex: Int =             MultiScaleTileManager.Companion.sampleSizeToScaleIndex(sampleSize)

        for (x in 0..<mHTileCount) {
            for (y in 0..<mVTileCount) {
                val isTileVisible =                     x >= firstVisibleTileX && y >= firstVisibleTileY && x <= lastVisibleTileX && y <= lastVisibleTileY

                val isTileWanted =                     x >= firstVisibleTileX - 1 && y >= firstVisibleTileY - 1 && x <= lastVisibleTileX + 1 && y <= lastVisibleTileY + 1

                if (isTileWanted && !mTileLoaded[x][y]) {
                    mTileLoaders[x]!![y]!!.markAsWanted(desiredScaleIndex)
                } else {
                    mTileLoaders[x]!![y]!!.markAsUnwanted()
                }

                if (isTileVisible != mTileVisibility[x]!![y] || !mTileLoaded[x][y]) {
                    if (isTileVisible && !mTileLoaded[x][y]) {
                        val tile = mTileLoaders[x]!![y]!!.atDesiredScale

                        if (tile != null) {
                            try {
                                val texture =                                     RRGLTexture(context, tile, true)
                                mTiles[x]!![y]!!.setTexture(texture)
                                texture.releaseReference()
                                mTileLoaded[x][y] = true
                                tile.recycle()
                            } catch (e: Exception) {
                                Log.e(
                                    "ImageViewDisplayListMan",
                                    "Exception when creating texture",
                                    e
                                )
                            }
                        }
                    } else if (!isTileWanted) {
                        mTiles[x]!![y]!!.setTexture(mNotLoadedTexture)
                    }

                    mTileVisibility[x]!![y] = isTileVisible
                }
            }
        }

        if (mScaleAnimation != null) {
            mScrollbars!!.showBars()
        }

        return mScaleAnimation != null
    }

    override fun onUIAttach() {
    }

    override fun onUIDetach() {
        mImageTileSource.dispose()
    }

    @Synchronized
    override fun onFingerDown(finger: Finger?) {
        if (mScrollbars == null) {
            return
        }

        mScaleAnimation = null
        mScrollbars!!.showBars()

        if (mCurrentTouchState == null) {
            mCurrentTouchState = TouchState.ONE_FINGER_DOWN
            mDragFinger = finger
        } else {
            when (mCurrentTouchState) {
                TouchState.DOUBLE_TAP_WAIT_NO_FINGERS_DOWN -> {
                    mCurrentTouchState = TouchState.DOUBLE_TAP_ONE_FINGER_DOWN
                    mDragFinger = finger
                    mDoubleTapGapTimer.stopTimer()
                }

                TouchState.ONE_FINGER_DRAG -> {
                    mListener.onHorizontalSwipeEnd()

                    mCurrentTouchState = TouchState.TWO_FINGER_PINCH
                    mPinchFinger1 = mDragFinger
                    mPinchFinger2 = finger
                    mDragFinger = null
                }

                TouchState.ONE_FINGER_DOWN, TouchState.DOUBLE_TAP_ONE_FINGER_DOWN, TouchState.DOUBLE_TAP_ONE_FINGER_DRAG -> {
                    mCurrentTouchState = TouchState.TWO_FINGER_PINCH
                    mPinchFinger1 = mDragFinger
                    mPinchFinger2 = finger
                    mDragFinger = null
                }

                else -> mSpareFingers.push(finger)
            }
        }
    }

    private val mTmpPoint1_onFingersMoved = MutableFloatPoint2D()
    private val mTmpPoint2_onFingersMoved = MutableFloatPoint2D()

    init {
        mHTileCount = mImageTileSource.hTileCount
        mVTileCount = mImageTileSource.vTileCount
        mTileSize = mImageTileSource.tileSize
        mTiles = Array<Array<RRGLRenderableTexturedQuad?>?>(mHTileCount) {
            arrayOfNulls<RRGLRenderableTexturedQuad>(mVTileCount)
        }

        mTileLoaders = Array<Array<MultiScaleTileManager?>?>(mHTileCount) {
            arrayOfNulls<MultiScaleTileManager>(mVTileCount)
        }
        val thread = ImageViewTileLoaderThread()

        for (x in 0..<mHTileCount) {
            for (y in 0..<mVTileCount) {
                mTileLoaders[x]!![y] =                     MultiScaleTileManager(mImageTileSource, thread, x, y, this)
            }
        }
    }

    @Synchronized
    override fun onFingersMoved() {
        if (mCurrentTouchState == null) {
            return
        }

        if (mScrollbars == null) {
            return
        }

        mScaleAnimation = null
        mScrollbars!!.showBars()

        when (mCurrentTouchState) {
            TouchState.DOUBLE_TAP_ONE_FINGER_DOWN -> {
                if (mDragFinger!!.mTotalPosDifference.distanceSquared()
                    >= 400f * mScreenDensity * mScreenDensity
                ) {
                    mCurrentTouchState = TouchState.DOUBLE_TAP_ONE_FINGER_DRAG
                }
            }

            TouchState.DOUBLE_TAP_ONE_FINGER_DRAG -> {
                val screenCentre = mTmpPoint1_onFingersMoved
                screenCentre.set((mResolutionX / 2).toFloat(), (mResolutionY / 2).toFloat())

                mCoordinateHelper.scaleAboutScreenPoint(
                    screenCentre,
                    1.01.pow((mDragFinger!!.mPosDifference.y / mScreenDensity).toDouble()).toFloat()
                )
            }

            TouchState.ONE_FINGER_DOWN -> {
                run {
                    if (mDragFinger!!.mTotalPosDifference.distanceSquared()
                        >= 100f * mScreenDensity * mScreenDensity
                    ) {
                        mCurrentTouchState = TouchState.ONE_FINGER_DRAG
                    }
                }
                if (mBoundsHelper!!.isMinScale) {
                    mListener.onHorizontalSwipe(mDragFinger!!.mTotalPosDifference.x)
                } else {
                    mCoordinateHelper.translateScreen(
                        mDragFinger!!.mLastPos,
                        mDragFinger!!.mCurrentPos
                    )
                }
            }

            TouchState.ONE_FINGER_DRAG -> if (mBoundsHelper!!.isMinScale) {
                mListener.onHorizontalSwipe(mDragFinger!!.mTotalPosDifference.x)
            } else {
                mCoordinateHelper.translateScreen(
                    mDragFinger!!.mLastPos,
                    mDragFinger!!.mCurrentPos
                )
            }

            TouchState.TWO_FINGER_PINCH -> {
                val oldDistance =                     mPinchFinger1!!.mLastPos.euclideanDistanceTo(mPinchFinger2!!.mLastPos)
                val newDistance =                     mPinchFinger1!!.mCurrentPos.euclideanDistanceTo(mPinchFinger2!!.mCurrentPos)

                val oldCentre = mTmpPoint1_onFingersMoved
                mPinchFinger1!!.mLastPos.add(mPinchFinger2!!.mLastPos, oldCentre)
                oldCentre.scale(0.5)

                val newCentre = mTmpPoint2_onFingersMoved
                mPinchFinger1!!.mCurrentPos.add(mPinchFinger2!!.mCurrentPos, newCentre)
                newCentre.scale(0.5)

                val scaleDifference = (newDistance / oldDistance).toFloat()

                mCoordinateHelper.scaleAboutScreenPoint(newCentre, scaleDifference)
                mCoordinateHelper.translateScreen(oldCentre, newCentre)
            }
        }
    }

    @Synchronized
    override fun onFingerUp(finger: Finger) {
        if (mScrollbars == null) {
            return
        }

        mScaleAnimation = null
        mScrollbars!!.showBars()

        if (mSpareFingers.remove(finger)) {
            return
        }

        if (mCurrentTouchState == null) {
            return
        }

        when (mCurrentTouchState) {
            TouchState.DOUBLE_TAP_ONE_FINGER_DOWN -> {
                if (finger.mDownDuration < TAP_MAX_DURATION_MS) {
                    onDoubleTap(finger.mCurrentPos)
                }

                mCurrentTouchState = null
                mDragFinger = null
            }

            TouchState.ONE_FINGER_DOWN -> {
                if (finger.mDownDuration < TAP_MAX_DURATION_MS) {
                    // Maybe a single tap

                    mDoubleTapGapTimer.startTimer()

                    mCurrentTouchState = TouchState.DOUBLE_TAP_WAIT_NO_FINGERS_DOWN
                    mFirstTapReleaseTime = System.currentTimeMillis()
                } else {
                    mCurrentTouchState = null
                }

                mDragFinger = null
            }

            TouchState.ONE_FINGER_DRAG -> {
                mListener.onHorizontalSwipeEnd()

                if (mSpareFingers.isEmpty) {
                    mCurrentTouchState = null
                    mDragFinger = null
                } else {
                    mDragFinger = mSpareFingers.pop()
                }
            }

            TouchState.DOUBLE_TAP_ONE_FINGER_DRAG -> if (mSpareFingers.isEmpty) {
                mCurrentTouchState = null
                mDragFinger = null
            } else {
                mDragFinger = mSpareFingers.pop()
            }

            TouchState.TWO_FINGER_PINCH -> if (mSpareFingers.isEmpty) {
                mCurrentTouchState = TouchState.ONE_FINGER_DRAG
                mDragFinger =                     if (mPinchFinger1 === finger) mPinchFinger2 else mPinchFinger1
                mPinchFinger1 = null
                mPinchFinger2 = null
            } else {
                if (mPinchFinger1 === finger) {
                    mPinchFinger1 = mSpareFingers.pop()
                } else {
                    mPinchFinger2 = mSpareFingers.pop()
                }
            }
        }
    }

    private fun onDoubleTap(position: MutableFloatPoint2D?) {
        val minScale = mBoundsHelper!!.getMinScale()
        val currentScale = mCoordinateHelper.scale

        var targetScale: Float

        if (currentScale > minScale * 1.01) {
            targetScale = minScale
        } else {
            targetScale = max(
                mResolutionX.toFloat() / mImageTileSource.width.toFloat(),
                mResolutionY.toFloat() / mImageTileSource.height.toFloat()
            )

            if (abs((targetScale / currentScale) - 1.0) < 0.05) {
                targetScale = currentScale * 3
            }
        }

        mScaleAnimation =             ImageViewScaleAnimation(targetScale, mCoordinateHelper, 15, position)
    }

    override fun onUIThreadRepeatingTimer(timer: UIThreadRepeatingTimer?) {
        if (mCurrentTouchState == TouchState.DOUBLE_TAP_WAIT_NO_FINGERS_DOWN) {
            if (System.currentTimeMillis() - mFirstTapReleaseTime
                > DOUBLE_TAP_MAX_GAP_DURATION_MS
            ) {
                mListener.onSingleTap()
                mCurrentTouchState = null
                mDoubleTapGapTimer.stopTimer()
            }
        } else {
            mDoubleTapGapTimer.stopTimer()
        }
    }

    private fun pickSampleSize(): Int {
        var result = 1

        while (result <= MultiScaleTileManager.Companion.MAX_SAMPLE_SIZE
            && (1.0 / (result * 2)) > mCoordinateHelper.scale
        ) {
            result *= 2
        }

        return result
    }

    override fun onTileLoaded(x: Int, y: Int, sampleSize: Int) {
        mRefreshable!!.refresh()
    }

    override fun onTileLoaderOutOfMemory() {
        mListener.onImageViewDLMOutOfMemory()
    }

    override fun onTileLoaderException(t: Throwable?) {
        mListener.onImageViewDLMException(t)
    }

    fun resetTouchState() {
        mCurrentTouchState = null
    }

    companion object {
        private const val TAP_MAX_DURATION_MS: Long = 225
        private const val DOUBLE_TAP_MAX_GAP_DURATION_MS: Long = 275

        @Suppress("PropertyName")
        private val NOT_LOADED_BITMAP: Bitmap

        init {
            NOT_LOADED_BITMAP = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)

            val notLoadedCanvas = Canvas(NOT_LOADED_BITMAP)
            notLoadedCanvas.drawRGB(70, 70, 70)
        }
    }
}
