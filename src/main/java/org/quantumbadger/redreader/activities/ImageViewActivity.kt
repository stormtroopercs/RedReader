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
package org.quantumbadger.redreader.activities

import android.content.Intent
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Movie
import android.net.Uri
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.annotation.UiThread
import androidx.core.content.IntentCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.github.lzyzsd.circleprogress.DonutProgress
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.RedReader.Companion.getInstance
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequest.DownloadQueueType
import org.quantumbadger.redreader.cache.CacheRequestCallbacks
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.Constants.Mime
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.General.bytesToMegabytes
import org.quantumbadger.redreader.common.General.checkThisIsUIThread
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.common.General.isThisUIThread
import org.quantumbadger.redreader.common.General.quickToast
import org.quantumbadger.redreader.common.General.setLayoutMatchParent
import org.quantumbadger.redreader.common.General.setLayoutMatchWidthWrapHeight
import org.quantumbadger.redreader.common.General.startNewThread
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.LinkHandler
import org.quantumbadger.redreader.common.LinkHandler.getAlbumInfo
import org.quantumbadger.redreader.common.LinkHandler.getImageInfo
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.common.LinkHandler.openWebBrowser
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.PrefsUtility.AppearanceStatusBarMode
import org.quantumbadger.redreader.common.PrefsUtility.GifViewMode
import org.quantumbadger.redreader.common.PrefsUtility.ImageViewMode
import org.quantumbadger.redreader.common.PrefsUtility.VideoViewMode
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.UriString.Companion.fromNullable
import org.quantumbadger.redreader.common.datastream.ByteArrayCallback
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.fragments.AccountListDialog.Companion.show
import org.quantumbadger.redreader.fragments.ImageInfoDialog
import org.quantumbadger.redreader.fragments.ReportDialog.Companion.show
import org.quantumbadger.redreader.image.AlbumInfo
import org.quantumbadger.redreader.image.GetAlbumInfoListener
import org.quantumbadger.redreader.image.GetImageInfoListener
import org.quantumbadger.redreader.image.GifDecoderThread
import org.quantumbadger.redreader.image.GifDecoderThread.OnGifLoadedListener
import org.quantumbadger.redreader.image.ImageInfo
import org.quantumbadger.redreader.image.ImageInfo.HasAudio
import org.quantumbadger.redreader.reddit.api.RedditPostActions
import org.quantumbadger.redreader.reddit.api.RedditPostActions.generateToolbar
import org.quantumbadger.redreader.reddit.api.RedditPostActions.onActionMenuItemSelected
import org.quantumbadger.redreader.reddit.kthings.RedditPost
import org.quantumbadger.redreader.reddit.prepared.RedditParsedPost
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL
import org.quantumbadger.redreader.views.GIFView
import org.quantumbadger.redreader.views.HorizontalSwipeProgressOverlay
import org.quantumbadger.redreader.views.RedditPostView.PostSelectionListener
import org.quantumbadger.redreader.views.bezelmenu.BezelSwipeOverlay
import org.quantumbadger.redreader.views.bezelmenu.BezelSwipeOverlay.BezelSwipeListener
import org.quantumbadger.redreader.views.bezelmenu.SideToolbarOverlay
import org.quantumbadger.redreader.views.bezelmenu.SideToolbarOverlay.SideToolbarPosition
import org.quantumbadger.redreader.views.glview.RRGLSurfaceView
import org.quantumbadger.redreader.views.imageview.BasicGestureHandler
import org.quantumbadger.redreader.views.imageview.ImageTileSource
import org.quantumbadger.redreader.views.imageview.ImageTileSourceWholeBitmap
import org.quantumbadger.redreader.views.imageview.ImageViewDisplayListManager
import org.quantumbadger.redreader.views.liststatus.ErrorView
import org.quantumbadger.redreader.views.video.ExoPlayerSeekableInputStreamDataSource
import org.quantumbadger.redreader.views.video.ExoPlayerSeekableInputStreamDataSourceFactory
import org.quantumbadger.redreader.views.video.ExoPlayerWrapperView
import org.quantumbadger.redreader.views.video.VideoGestureHandler
import java.io.IOException
import java.io.InputStream
import java.util.Objects
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class ImageViewActivity : ViewsBaseActivity(), PostSelectionListener,
    ImageViewDisplayListManager.Listener {
    private var mProgressText: TextView?=null

    private var surfaceView: GLSurfaceView?=null
    private var imageView: ImageView?=null
    private var gifThread: GifDecoderThread?=null

    private var mVideoPlayerWrapper: ExoPlayerWrapperView?=null

    private var mUrl: UriString?=null

    private var mIsPaused = true
    private var mIsDestroyed = false
    private val mActionsOnResume = ArrayList<Runnable>()

    private var mImageOrVideoRequest: CacheRequest?=null
    private var mAudioRequest: CacheRequest?=null

    private var mHaveReverted = false

    private var mImageViewDisplayerManager: ImageViewDisplayListManager?=null

    private var mSwipeOverlay: HorizontalSwipeProgressOverlay?=null
    private var mSwipeCancelled = false

    private var mPost: RedditPost?=null

    private var mImageInfo: ImageInfo?=null
    private var mAlbumInfo: AlbumInfo?=null
    private var mAlbumImageIndex = 0

    private var mLayout: FrameLayout?=null

    private var mGallerySwipeLengthPx = 0

    private var mFloatingToolbar: LinearLayout?=null

    override fun baseActivityIsToolbarActionBarEnabled(): Boolean {
        return false
    }

    override fun baseActivityNavigationBarColour(): Int {
        return Color.BLACK
    }

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (PrefsUtility.pref_appearance_android_status()
            == AppearanceStatusBarMode.HIDE_ON_MEDIA
        ) {
            hideStatusBar()
        }

        setTitle(string.accessibility_image_viewer_title)

        val gallerySwipeLengthDp = PrefsUtility.pref_behaviour_gallery_swipe_length_dp()
        mGallerySwipeLengthPx = dpToPixels(this, gallerySwipeLengthDp.toFloat())

        val intent = getIntent()

        mUrl = fromNullable(intent.getDataString())

        if (mUrl == null) {
            finish()
            return
        }

        mPost = IntentCompat.getParcelableExtra<RedditPost?>(intent, "post", RedditPost::class.java)

        if (intent.hasExtra("albumUrl")) {
            getAlbumInfo(
                this,
                Objects.requireNonNull<UriString?>(
                    IntentCompat.getParcelableExtra<UriString?>(
                        intent, "albumUrl",
                        UriString::class.java
                    )
                ),
                Priority(Constants.Priority.IMAGE_VIEW),
                object : GetAlbumInfoListener {
                    override fun onFailure(error: RRError) {
                        // Do nothing
                    }

                    override fun onGalleryRemoved() {
                        // Do nothing
                    }

                    override fun onGalleryDataNotPresent() {
                        // Do nothing
                    }

                    override fun onSuccess(info: AlbumInfo) {
                        AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                            mAlbumInfo = info
                            mAlbumImageIndex = intent.getIntExtra(
                                "albumImageIndex",
                                0
                            )
                        })
                    }
                }
            )
        }

        val progressBar = DonutProgress(this)
        progressBar.setIndeterminate(true)
        progressBar.setFinishedStrokeColor(Color.rgb(200, 200, 200))
        progressBar.setUnfinishedStrokeColor(Color.rgb(50, 50, 50))
        progressBar.setAspectIndicatorStrokeColor(Color.rgb(200, 200, 200))
        val progressStrokeWidthPx = dpToPixels(this, 15f)
        progressBar.setUnfinishedStrokeWidth(progressStrokeWidthPx.toFloat())
        progressBar.setFinishedStrokeWidth(progressStrokeWidthPx.toFloat())
        progressBar.setAspectIndicatorStrokeWidth(dpToPixels(this, 1f).toFloat())
        progressBar.startingDegree = -90
        progressBar.initPainters()

        val progressTextLayout = LinearLayout(this)
        progressTextLayout.setOrientation(LinearLayout.VERTICAL)
        progressTextLayout.setGravity(Gravity.CENTER_HORIZONTAL)

        progressTextLayout.addView(progressBar)
        val progressDimensionsPx = dpToPixels(this, 150f)
        progressBar.getLayoutParams().width = progressDimensionsPx
        progressBar.getLayoutParams().height = progressDimensionsPx

        mProgressText = TextView(this)
        mProgressText!!.setText(string.download_loading)
        mProgressText!!.setAllCaps(true)
        mProgressText!!.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        mProgressText!!.setGravity(Gravity.CENTER_HORIZONTAL)
        progressTextLayout.addView(mProgressText)
        mProgressText!!.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT
        mProgressText!!.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT
        (mProgressText!!.getLayoutParams() as MarginLayoutParams).topMargin = dpToPixels(this, 10f)

        val progressLayout = RelativeLayout(this)
        progressLayout.addView(progressTextLayout)
        (progressTextLayout.getLayoutParams() as RelativeLayout.LayoutParams).addRule(
            RelativeLayout.CENTER_IN_PARENT
        )
        setLayoutMatchWidthWrapHeight(progressTextLayout)

        mLayout = FrameLayout(this)
        mLayout!!.addView(progressLayout)

        // The RedGIFs API no longer provides audio, so show the embedded web
        // player instead (LinkHandler substitutes the embed URL for the link)
        if (LinkHandler.isRedGifsImage(mUrl!!)) {
            revertToWeb()
            return
        }

        getImageInfo(
            this,
            mUrl,
            Priority(Constants.Priority.IMAGE_VIEW),
            object : GetImageInfoListener {
                override fun onFailure(error: RRError) {
                    quickToast(
                        this@ImageViewActivity,
                        string.imageview_image_info_failed
                    )

                    revertToWeb()
                }

                override fun onSuccess(info: ImageInfo) {
                    mImageInfo = info
                    val audioUri = if (info.urlAudioStream == null)
                        null
                    else
                        info.urlAudioStream
                    openImage(progressBar, info.original.url, audioUri)
                }

                override fun onNotAnImage() {
                    revertToWeb()
                }
            })

        val post: RedditPreparedPost?

        if (mPost != null) {
            val parsedPost = RedditParsedPost(this, mPost!!, false)

            post = RedditPreparedPost(
                this,
                CacheManager.Companion.getInstance(this),
                0,
                parsedPost,
                TimestampUTC.ZERO,
                false,
                false,
                false,
                false
            )
        } else {
            post = null
        }

        val hiddenAccessibilityLayout = LayoutInflater.from(this)
            .inflate(R.layout.image_view_hidden_accessibility_layout, null)
        run {
            val commentsButton = hiddenAccessibilityLayout.findViewById<View>(
                R.id.image_view_hidden_accessibility_view_comments
            )
            val backButton = hiddenAccessibilityLayout.findViewById<View>(
                R.id.image_view_hidden_accessibility_go_back
            )

            if (post != null) {
                commentsButton.setOnClickListener(
                    View.OnClickListener { v: View? ->
                        onActionMenuItemSelected(
                            post,
                            this,
                            RedditPostActions.Action.COMMENTS_SWITCH
                        )
                    })
            } else {
                commentsButton.setContentDescription(null)
                commentsButton.setClickable(false)
                commentsButton.setFocusable(false)
                commentsButton.setVisibility(View.GONE)
            }

            backButton.setOnClickListener(View.OnClickListener { v: View? -> finish() })

            //Consume & ignore touch events, so loading images aren't closed by tapping.
            backButton.setOnTouchListener(OnTouchListener { v: View?, event: MotionEvent? -> true })
        }

        val outerFrame = FrameLayout(this)
        outerFrame.addView(hiddenAccessibilityLayout)
        outerFrame.addView(mLayout)
        General.setLayoutMatchParent(mLayout!!)

        if (PrefsUtility.pref_appearance_image_viewer_show_floating_toolbar()) {
            mFloatingToolbar = Objects.requireNonNull<LinearLayout?>(
                LayoutInflater.from(this).inflate(
                    R.layout.floating_toolbar,
                    outerFrame,
                    false
                ) as LinearLayout?
            )

            if (PrefsUtility.pref_appearance_left_handed()) {
                val toolBarParams =                     mFloatingToolbar!!.getLayoutParams() as FrameLayout.LayoutParams
                toolBarParams.gravity = Gravity.START or Gravity.BOTTOM
                mFloatingToolbar!!.setLayoutParams(toolBarParams)
            }

            outerFrame.addView(mFloatingToolbar)

            mFloatingToolbar!!.setVisibility(View.GONE)
        }

        if (post != null) {
            val toolbarOverlay = SideToolbarOverlay(this)

            val bezelOverlay = BezelSwipeOverlay(
                this,
                object : BezelSwipeListener {
                    override fun onSwipe(@BezelSwipeOverlay.SwipeEdge edge: Int): Boolean {
                        toolbarOverlay.setContents(
                            generateToolbar(
                                post,
                                this@ImageViewActivity,
                                false,
                                toolbarOverlay
                            )
                        )
                        toolbarOverlay.show(
                            if (edge == BezelSwipeOverlay.Companion.LEFT)
                                SideToolbarPosition.LEFT
                            else
                                SideToolbarPosition.RIGHT
                        )
                        return true
                    }

                    override fun onTap(): Boolean {
                        if (toolbarOverlay.isShown()) {
                            toolbarOverlay.hide()
                            return true
                        }

                        return false
                    }
                })

            outerFrame.addView(bezelOverlay)
            outerFrame.addView(toolbarOverlay)

            setLayoutMatchParent(bezelOverlay)
            setLayoutMatchParent(toolbarOverlay)
        }

        setBaseActivityListing(outerFrame)
    }

    private fun setMainView(v: View) {
        mLayout!!.removeAllViews()
        mLayout!!.addView(v)

        mSwipeOverlay = HorizontalSwipeProgressOverlay(this)
        mLayout!!.addView(mSwipeOverlay)

        setLayoutMatchParent(v)
    }

    private fun onImageStreamReady(
        isNetwork: Boolean,
        videoStream: GenericFactory<SeekableInputStream, IOException?>,
        audioStream: GenericFactory<SeekableInputStream, IOException?>?,
        mimetype: String?,
        videoStreamUri: Uri
    ) {
        startNewThread("ImageViewActivity", Runnable {
            Log.i(TAG, "Image stream ready")
            if (mimetype == null) {
                revertToWeb()
                return@startNewThread
            }

            val isOctetStream = Mime.isOctetStream(mimetype)

            val isImage = Mime.isImage(mimetype)
                    || (mImageInfo!!.mediaType == ImageInfo.MediaType.IMAGE && isOctetStream)

            val isVideo = Mime.isVideo(mimetype)
                    || (mImageInfo!!.mediaType == ImageInfo.MediaType.VIDEO && isOctetStream)

            val isGif = !isVideo && !isImage && Mime.isImageGif(mimetype)

            if (!isImage && !isVideo && !isGif) {
                Log.e(TAG, "Cannot play mimetype: " + mimetype)
                revertToWeb()
                return@startNewThread
            }

            if (mImageInfo != null && ((mImageInfo!!.title != null && !mImageInfo!!.title!!.isEmpty())
                        || (mImageInfo!!.caption != null && !mImageInfo!!.caption!!.isEmpty()))
            ) {
                AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                    addFloatingToolbarButton(
                        R.drawable.ic_action_info_dark,
                        string.props_image_title,
                        View.OnClickListener { view: View? ->
                            ImageInfoDialog.Companion.newInstance(mImageInfo).show(
                                getSupportFragmentManager(),
                                null
                            )
                        })
                })
            }

            val fullyDownloadBeforePlaying = PrefsUtility.pref_videos_download_before_playing()

            if (isNetwork && fullyDownloadBeforePlaying && (isVideo || isGif)) {
                Log.i(TAG, "Fully downloading before starting playback")

                try {
                    videoStream.create().use { `is` ->
                        `is`.readRemainingAsBytes(ByteArrayCallback { buf: ByteArray?, offset: Int, length: Int ->
                            Log.i(
                                TAG, "Video fully downloaded, starting playback"
                            )
                        })
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Got exception while fully buffering", e)
                    quickToast(this, string.imageview_download_failed)
                    revertToWeb()
                    return@startNewThread
                }
            }
            if (isVideo) {
                AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                    if (mIsDestroyed) {
                        return@post
                    }
                    val videoViewMode = PrefsUtility.pref_behaviour_videoview_mode()
                    if (videoViewMode == VideoViewMode.INTERNAL_BROWSER) {
                        revertToWeb()
                    } else if (videoViewMode == VideoViewMode.EXTERNAL_BROWSER) {
                        openInExternalBrowser()
                    } else if (videoViewMode == VideoViewMode.EXTERNAL_APP_VLC) {
                        cancelCacheRequests()
                        launchVlc(videoStreamUri)
                    } else {
                        playWithExoplayer(isNetwork, videoStream, audioStream)
                    }
                })
            } else if (isGif) {
                val gifViewMode = PrefsUtility.pref_behaviour_gifview_mode()

                if (gifViewMode == GifViewMode.INTERNAL_BROWSER) {
                    revertToWeb()
                    return@startNewThread
                } else if (gifViewMode == GifViewMode.EXTERNAL_BROWSER) {
                    openInExternalBrowser()
                    return@startNewThread
                }

                if (gifViewMode == GifViewMode.INTERNAL_MOVIE) {
                    playGIFWithMovie(videoStream)
                } else {
                    playGIFWithLegacyDecoder(videoStream)
                }
            } else {
                val imageViewMode = PrefsUtility.pref_behaviour_imageview_mode()

                if (imageViewMode == ImageViewMode.INTERNAL_BROWSER) {
                    revertToWeb()
                } else if (imageViewMode == ImageViewMode.EXTERNAL_BROWSER) {
                    openInExternalBrowser()
                } else {
                    showImageWithInternalViewer(videoStream)
                }
            }
        })
    }

    override fun onPostSelected(post: RedditPreparedPost) {
        onLinkClicked(this, post.src.url, false, post.src.src)
    }

    override fun onPostCommentsSelected(post: RedditPreparedPost) {
        onLinkClicked(
            this,
            UriString(
                PostCommentListingURL.Companion.forPostId(post.src.getIdAlone())
                    .generateJsonUri()
                    .toString()
            ),
            false
        )
    }

    private fun revertToWeb() {
        Log.i(TAG, "Using internal browser", RuntimeException())

        val r: Runnable = object : Runnable {
            override fun run() {
                if (mIsPaused) {
                    Log.i(TAG, "Not reverting as we are paused. Queuing for later.")
                    mActionsOnResume.add(this)
                    return
                }

                if (mIsDestroyed) {
                    Log.i(TAG, "Not reverting as we are destroyed")
                    return
                }

                if (!mHaveReverted) {
                    mHaveReverted = true
                    onLinkClicked(this@ImageViewActivity, mUrl, true)
                    this@ImageViewActivity.finish()
                }
            }
        }

        AndroidCommon.runOnUiThread(r)
    }

    private fun openInExternalBrowser() {
        Log.i(TAG, "Using external browser")

        val r = Runnable {
            openWebBrowser(this, mUrl!!.toUri(), false)
            finish()
        }

        if (isThisUIThread) {
            r.run()
        } else {
            AndroidCommon.UI_THREAD_HANDLER.post(r)
        }
    }

    public override fun onPause() {
        if (mIsPaused) {
            throw RuntimeException()
        }

        mIsPaused = true

        super.onPause()
        if (surfaceView != null) {
            surfaceView!!.onPause()
        }
    }

    public override fun onResume() {
        if (!mIsPaused) {
            throw RuntimeException()
        }

        mIsPaused = false

        super.onResume()
        if (surfaceView != null) {
            surfaceView!!.onResume()
        }

        for (runnable in mActionsOnResume) {
            runnable.run()
        }

        mActionsOnResume.clear()
    }

    public override fun onDestroy() {
        super.onDestroy()
        mIsDestroyed = true

        cancelCacheRequests()

        if (gifThread != null) {
            gifThread!!.stopPlaying()
        }

        if (mVideoPlayerWrapper != null) {
            mVideoPlayerWrapper!!.release()
            mVideoPlayerWrapper = null
        }
    }

    private fun cancelCacheRequests() {
        if (mImageOrVideoRequest != null) {
            mImageOrVideoRequest!!.cancel()
        }

        if (mAudioRequest != null) {
            mAudioRequest!!.cancel()
        }
    }

    override fun onSingleTap() {
        if (PrefsUtility.pref_behaviour_video_playback_controls()
            && mVideoPlayerWrapper != null
        ) {
            mVideoPlayerWrapper!!.handleTap()

            if (mFloatingToolbar != null) {
                if (mVideoPlayerWrapper!!.isControlViewVisible() == View.VISIBLE) {
                    mFloatingToolbar!!.setVisibility(View.GONE)
                } else {
                    mFloatingToolbar!!.setVisibility(View.VISIBLE)
                }
            }
        } else if (PrefsUtility.pref_behaviour_imagevideo_tap_close()) {
            finish()
        }
    }

    override fun onHorizontalSwipe(pixels: Float) {
        if (mSwipeCancelled) {
            return
        }

        if (mSwipeOverlay != null && mAlbumInfo != null) {
            mSwipeOverlay!!.onSwipeUpdate(pixels, mGallerySwipeLengthPx.toFloat())

            if (pixels >= mGallerySwipeLengthPx) {
                // Back

                mSwipeCancelled = true
                if (mSwipeOverlay != null) {
                    mSwipeOverlay!!.onSwipeEnd()
                }

                if (mAlbumImageIndex > 0) {
                    onLinkClicked(
                        this,
                        mAlbumInfo!!.images.get(mAlbumImageIndex - 1).original.url,
                        false,
                        mPost,
                        mAlbumInfo,
                        mAlbumImageIndex - 1
                    )

                    finish()
                } else {
                    quickToast(this, string.album_already_first_image)
                }
            } else if (pixels <= -mGallerySwipeLengthPx) {
                // Forwards

                mSwipeCancelled = true
                if (mSwipeOverlay != null) {
                    mSwipeOverlay!!.onSwipeEnd()
                }

                if (mAlbumImageIndex < mAlbumInfo!!.images.size - 1) {
                    onLinkClicked(
                        this,
                        mAlbumInfo!!.images.get(mAlbumImageIndex + 1).original.url,
                        false,
                        mPost,
                        mAlbumInfo,
                        mAlbumImageIndex + 1
                    )

                    finish()
                } else {
                    quickToast(this, string.album_already_last_image)
                }
            }
        }
    }

    override fun onHorizontalSwipeEnd() {
        mSwipeCancelled = false

        if (mSwipeOverlay != null) {
            mSwipeOverlay!!.onSwipeEnd()
        }
    }

    override fun onImageViewDLMOutOfMemory() {
        if (!mHaveReverted) {
            quickToast(this, string.imageview_oom)
            revertToWeb()
        }
    }

    override fun onImageViewDLMException(t: Throwable?) {
        if (!mHaveReverted) {
            quickToast(this, string.imageview_decode_failed)
            revertToWeb()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (mImageViewDisplayerManager != null) {
            mImageViewDisplayerManager!!.resetTouchState()
        }
    }

    private fun openImage(
        progressBar: DonutProgress,
        uri: UriString,
        audioUri: UriString?
    ) {
        if (mImageInfo!!.mediaType != null) {
            Log.i(TAG, "Media type " + mImageInfo!!.mediaType + " detected")

            if (mImageInfo!!.mediaType == ImageInfo.MediaType.IMAGE) {
                val imageViewMode = PrefsUtility.pref_behaviour_imageview_mode()

                if (imageViewMode == ImageViewMode.EXTERNAL_BROWSER) {
                    openInExternalBrowser()
                    return
                } else if (imageViewMode == ImageViewMode.INTERNAL_BROWSER) {
                    revertToWeb()
                    return
                }
            } else if (mImageInfo!!.mediaType == ImageInfo.MediaType.GIF) {
                val gifViewMode = PrefsUtility.pref_behaviour_gifview_mode()

                if (gifViewMode == GifViewMode.EXTERNAL_BROWSER) {
                    openInExternalBrowser()
                    return
                } else if (gifViewMode == GifViewMode.INTERNAL_BROWSER) {
                    revertToWeb()
                    return
                }
            } else if (mImageInfo!!.mediaType == ImageInfo.MediaType.VIDEO) {
                val videoViewMode = PrefsUtility.pref_behaviour_videoview_mode()

                if (videoViewMode == VideoViewMode.EXTERNAL_BROWSER) {
                    openInExternalBrowser()
                    return
                } else if (videoViewMode == VideoViewMode.INTERNAL_BROWSER) {
                    revertToWeb()
                    return
                } else if (videoViewMode == VideoViewMode.EXTERNAL_APP_VLC) {
                    launchVlc(Uri.parse(uri.toString()))
                }
            }
        }

        Log.i(TAG, "Proceeding with download")
        makeCacheRequest(progressBar, uri, audioUri)
    }

    private fun manageAspectRatioIndicator(progressBar: DonutProgress) {
        findAspectRatio@ if (PrefsUtility.pref_appearance_show_aspect_ratio_indicator()) {
            if (mImageInfo!!.original.size != null && mImageInfo!!.original.size!!.height > 0) {
                progressBar.setLoadingImageAspectRatio(mImageInfo!!.original.size!!.width.toFloat() / mImageInfo!!.original.size!!.height)
            } else {
                break@findAspectRatio
            }

            progressBar.setAspectIndicatorDisplay(true)
            return
        }

        progressBar.setAspectIndicatorDisplay(false)
    }

    private fun makeCacheRequest(
        progressBar: DonutProgress,
        uri: UriString,
        audioUri: UriString?
    ) {
        val resultLock = Any()

        val failed = AtomicBoolean(false)
        val audio = AtomicReference<GenericFactory<SeekableInputStream, IOException?>?>()
        val video = AtomicReference<GenericFactory<SeekableInputStream, IOException?>?>()
        val videoMimetype = AtomicReference<String?>()

        CacheManager.Companion.getInstance(this).makeRequest(
            CacheRequest(
                uri,
                RedditAccountManager.Companion.getAnon(),
                null,
                Priority(Constants.Priority.IMAGE_VIEW),
                DownloadStrategyIfNotCached.Companion.INSTANCE,
                Constants.FileType.IMAGE,
                DownloadQueueType.IMMEDIATE,
                this,
                object : CacheRequestCallbacks {
                    private var mProgressTextSet = false

                    override fun onFailure(error: RRError) {
                        synchronized(resultLock) {
                            if (!failed.getAndSet(true)) {
                                AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                                    val layout = LinearLayout(this@ImageViewActivity)
                                    val errorView = ErrorView(
                                        this@ImageViewActivity,
                                        error
                                    )
                                    layout.addView(errorView)
                                    setLayoutMatchWidthWrapHeight(errorView)
                                    setMainView(layout)
                                })
                            }
                        }
                    }

                    override fun onDownloadNecessary() {
                        AndroidCommon.runOnUiThread(Runnable {
                            progressBar.setVisibility(View.VISIBLE)
                            progressBar.setIndeterminate(true)
                            manageAspectRatioIndicator(progressBar)
                        })
                    }

                    override fun onProgress(
                        authorizationInProgress: Boolean,
                        bytesRead: Long,
                        totalBytes: Long
                    ) {
                        AndroidCommon.runOnUiThread(Runnable {
                            progressBar.setVisibility(View.VISIBLE)
                            progressBar.setIndeterminate(authorizationInProgress)
                            progressBar.progress =                                 (((1000 * bytesRead) / totalBytes).toFloat()) / 1000
                            manageAspectRatioIndicator(progressBar)
                            if (!mProgressTextSet) {
                                mProgressText!!.setText(bytesToMegabytes(totalBytes))
                                mProgressTextSet = true
                            }
                        })
                    }

                    override fun onDataStreamAvailable(
                        streamFactory: GenericFactory<SeekableInputStream, IOException?>,
                        timestamp: TimestampUTC?,
                        session: UUID,
                        fromCache: Boolean,
                        mimetype: String?
                    ) {
                        synchronized(resultLock) {
                            if (audio.get() != null || audioUri == null) {
                                onImageStreamReady(
                                    !fromCache,
                                    streamFactory,
                                    audio.get(),
                                    mimetype,
                                    Uri.parse(uri.toString())
                                )
                            } else {
                                video.set(streamFactory)
                                videoMimetype.set(mimetype)
                            }
                        }
                    }
                }).also { mImageOrVideoRequest = it })

        if (audioUri != null) {
            CacheManager.Companion.getInstance(this).makeRequest(
                CacheRequest(
                    audioUri,
                    RedditAccountManager.Companion.getAnon(),
                    null,
                    Priority(Constants.Priority.IMAGE_VIEW),
                    DownloadStrategyIfNotCached.Companion.INSTANCE,
                    Constants.FileType.IMAGE,
                    DownloadQueueType.IMMEDIATE,
                    this,
                    object : CacheRequestCallbacks {
                        override fun onFailure(error: RRError) {
                            synchronized(resultLock) {
                                if (!failed.getAndSet(true)) {
                                    AndroidCommon.runOnUiThread(Runnable {
                                        val layout = LinearLayout(this@ImageViewActivity)
                                        val errorView = ErrorView(
                                            this@ImageViewActivity,
                                            error
                                        )
                                        layout.addView(errorView)
                                        setLayoutMatchWidthWrapHeight(errorView)
                                        setMainView(layout)
                                    })
                                }
                            }
                        }

                        override fun onDataStreamAvailable(
                            streamFactory: GenericFactory<SeekableInputStream, IOException?>,
                            timestamp: TimestampUTC?,
                            session: UUID,
                            fromCache: Boolean,
                            mimetype: String?
                        ) {
                            synchronized(resultLock) {
                                if (video.get() != null) {
                                    onImageStreamReady(
                                        !fromCache,
                                        video.get(),
                                        streamFactory,
                                        videoMimetype.get(),
                                        Uri.parse(uri.toString())
                                    )
                                } else {
                                    audio.set(streamFactory)
                                }
                            }
                        }
                    }).also { mAudioRequest = it })
        }
    }

    private fun addFloatingToolbarButton(
        @DrawableRes drawable: Int,
        @StringRes description: Int,
        listener: View.OnClickListener
    ): ImageButton? {
        if (mFloatingToolbar == null) {
            return null
        }

        mFloatingToolbar!!.setVisibility(View.VISIBLE)

        val ib = LayoutInflater.from(this).inflate(
            R.layout.flat_image_button,
            mFloatingToolbar,
            false
        ) as ImageButton

        val buttonPadding = dpToPixels(this, 10f)
        ib.setPadding(buttonPadding, buttonPadding, buttonPadding, buttonPadding)
        ib.setImageResource(drawable)
        ib.setContentDescription(getResources().getString(description))

        ib.setOnClickListener(listener)

        mFloatingToolbar!!.addView(ib)

        return ib
    }

    private fun launchVlc(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)

        intent.setClassName(
            "org.videolan.vlc",
            "org.videolan.vlc.gui.video.VideoPlayerActivity"
        )

        intent.setDataAndType(uri, "video/*")

        try {
            startActivity(intent)
        } catch (t: Throwable) {
            quickToast(this, string.videoview_mode_app_vlc_launch_failed)
            Log.e(TAG, "VLC failed to launch", t)
        }

        finish()
    }

    @OptIn(markerClass = UnstableApi::class)
    @UiThread
    private fun playWithExoplayer(
        isNetwork: Boolean,
        videoStream: GenericFactory<SeekableInputStream, IOException?>,
        audioStream: GenericFactory<SeekableInputStream, IOException?>?
    ) {
        checkThisIsUIThread()

        try {
            Log.i(TAG, "Playing video using ExoPlayer")
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            val layout = RelativeLayout(this)
            layout.setGravity(Gravity.CENTER)

            val videoDataSourceFactory =                 ExoPlayerSeekableInputStreamDataSourceFactory(isNetwork, videoStream)

            val mediaSource: MediaSource

            val videoMediaSource: MediaSource=ProgressiveMediaSource.Factory(videoDataSourceFactory)
                .createMediaSource(
                    MediaItem.fromUri(
                        ExoPlayerSeekableInputStreamDataSource.Companion.URI
                    )
                )

            if (audioStream == null) {
                mediaSource = videoMediaSource
            } else {
                val audioDataSourceFactory =                     ExoPlayerSeekableInputStreamDataSourceFactory(isNetwork, audioStream)

                mediaSource = MergingMediaSource(
                    videoMediaSource,
                    ProgressiveMediaSource.Factory(audioDataSourceFactory)
                        .createMediaSource(
                            MediaItem.fromUri(
                                ExoPlayerSeekableInputStreamDataSource.Companion.URI
                            )
                        )
                )
            }

            mVideoPlayerWrapper = ExoPlayerWrapperView(
                this,
                mediaSource,
                ExoPlayerWrapperView.Listener { this.revertToWeb() },
                0
            )

            layout.addView(mVideoPlayerWrapper)
            setMainView(layout)

            setLayoutMatchParent(layout)
            General.setLayoutMatchParent(mVideoPlayerWrapper!!)

            val gestureHandler = VideoGestureHandler(this, mVideoPlayerWrapper!!)

            mVideoPlayerWrapper!!.setOnTouchListener(gestureHandler)

            layout.setOnTouchListener(gestureHandler)

            val muteByDefault = PrefsUtility.pref_behaviour_video_mute_default()

            mVideoPlayerWrapper!!.setMuted(muteByDefault)

            val iconMuted = R.drawable.ic_volume_off_white_24dp
            val iconUnmuted = R.drawable.ic_volume_up_white_24dp

            if (mImageInfo != null
                && (mImageInfo!!.hasAudio
                        != HasAudio.NO_AUDIO)
            ) {
                val muteButton = AtomicReference<ImageButton>()
                muteButton.set(
                    addFloatingToolbarButton(
                        if (muteByDefault) iconMuted else iconUnmuted,
                        if (muteByDefault) string.video_unmute else string.video_mute,
                        View.OnClickListener { view: View? ->
                            val button = muteButton.get()
                            if (mVideoPlayerWrapper!!.isMuted()) {
                                mVideoPlayerWrapper!!.setMuted(false)
                                button.setImageResource(iconUnmuted)
                                button.setContentDescription(
                                    getResources().getString(string.video_mute)
                                )
                            } else {
                                mVideoPlayerWrapper!!.setMuted(true)
                                button.setImageResource(iconMuted)
                                button.setContentDescription(
                                    getResources().getString(string.video_unmute)
                                )
                            }
                        })
                )
            }
        } catch (e: OutOfMemoryError) {
            quickToast(this, string.imageview_oom)
            revertToWeb()
        } catch (e: Throwable) {
            quickToast(this, string.imageview_invalid_video)
            revertToWeb()
        }
    }

    private fun playGIFWithMovie(
        streamFactory: GenericFactory<SeekableInputStream, IOException?>
    ) {
        Log.i(TAG, "Playing GIF using Movie API")

        try {
            streamFactory.create().use { `is` ->
                Log.i(TAG, "Got input stream of type " + `is`.javaClass.getCanonicalName())
                `is`.readRemainingAsBytes(ByteArrayCallback { buf: ByteArray?, offset: Int, length: Int ->
                    Log.i(TAG, "Got byte array")
                    val movie: Movie

                    try {
                        movie = GIFView.Companion.prepareMovie(buf, offset, length)
                    } catch (e: OutOfMemoryError) {
                        quickToast(this, string.imageview_oom)
                        revertToWeb()
                        return@readRemainingAsBytes
                    } catch (e: Throwable) {
                        quickToast(this, string.imageview_invalid_gif)
                        revertToWeb()
                        return@readRemainingAsBytes
                    }
                    AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                        if (mIsDestroyed) {
                            return@post
                        }
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

                        val gifView = GIFView(this, movie)

                        setMainView(gifView)
                        gifView.setOnTouchListener(BasicGestureHandler(this))
                    })
                })
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read GIF data", e)
            quickToast(this, string.imageview_download_failed)
            revertToWeb()
        }
    }

    private fun playGIFWithLegacyDecoder(
        streamFactory: GenericFactory<SeekableInputStream, IOException?>
    ) {
        Log.i(TAG, "Playing GIF using legacy decoder")

        // The GIF decoder thread will close this itself
        val `is`: InputStream
        try {
            `is` = streamFactory.create()
        } catch (e: IOException) {
            quickToast(this, string.imageview_download_failed)
            revertToWeb()
            return
        }

        gifThread = GifDecoderThread(
            `is`,
            object : OnGifLoadedListener {
                override fun onGifLoaded() {
                    AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                        if (mIsDestroyed) {
                            return@post
                        }
                        imageView = ImageView(this@ImageViewActivity)
                        imageView!!.setScaleType(ImageView.ScaleType.FIT_CENTER)
                        setMainView(imageView!!)
                        gifThread!!.setView(imageView)
                        imageView!!.setOnTouchListener(
                            BasicGestureHandler(
                                this@ImageViewActivity
                            )
                        )
                    })
                }

                override fun onOutOfMemory() {
                    quickToast(
                        this@ImageViewActivity,
                        string.imageview_oom
                    )
                    revertToWeb()
                }

                override fun onGifInvalid() {
                    quickToast(
                        this@ImageViewActivity,
                        string.imageview_invalid_gif
                    )
                    revertToWeb()
                }
            })

        gifThread!!.start()
    }

    private fun showImageWithInternalViewer(
        streamFactory: GenericFactory<SeekableInputStream, IOException?>
    ) {
        Log.i(TAG, "Showing image using internal viewer")

        val imageTileSource: ImageTileSource
        try {
            try {
                streamFactory.create().use { `is` ->
                    imageTileSource = ImageTileSourceWholeBitmap(
                        BitmapFactory.decodeStream(`is`)
                    )
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Exception when creating ImageTileSource", t)
                quickToast(this, string.imageview_decode_failed)
                revertToWeb()
                return
            }
        } catch (e: OutOfMemoryError) {
            quickToast(this, string.imageview_oom)
            revertToWeb()
            return
        }

        AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
            if (mIsDestroyed) {
                return@post
            }
            mImageViewDisplayerManager = ImageViewDisplayListManager(imageTileSource, this)
            surfaceView = RRGLSurfaceView(this, mImageViewDisplayerManager)
            setMainView(surfaceView!!)
            if (mIsPaused) {
                surfaceView!!.onPause()
            } else {
                surfaceView!!.onResume()
            }
        })
    }

    companion object {
        private const val TAG = "ImageViewActivity"
    }
}


