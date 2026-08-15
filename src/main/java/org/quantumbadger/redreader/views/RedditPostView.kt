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
package org.quantumbadger.redreader.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.UiThread
import androidx.constraintlayout.widget.ConstraintLayout
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequest.DownloadQueueType
import org.quantumbadger.redreader.cache.CacheRequest.RequestFailureType
import org.quantumbadger.redreader.cache.CacheRequestCallbacks
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import org.quantumbadger.redreader.common.AndroidCommon.removeClickListeners
import org.quantumbadger.redreader.common.AndroidCommon.runOnUiThread
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.DisplayUtils
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.common.General.getGeneralErrorForFailure
import org.quantumbadger.redreader.common.General.getSharedPrefs
import org.quantumbadger.redreader.common.General.setLayoutMatchWidthWrapHeight
import org.quantumbadger.redreader.common.General.setLayoutWidthHeight
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.PrefsUtility.PostFlingAction
import org.quantumbadger.redreader.common.PrefsUtility.PostTapAction
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.fragments.PostListingFragment
import org.quantumbadger.redreader.http.FailedRequestBody
import org.quantumbadger.redreader.reddit.api.RedditPostActions
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost.ThumbnailLoadedCallback
import org.quantumbadger.redreader.views.liststatus.ErrorView
import java.io.IOException
import java.util.Objects
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min
import org.quantumbadger.redreader.common.General

class RedditPostView(
    context: Context,
    fragmentParent: PostListingFragment,
    private val mActivity: BaseActivity,
    leftHandedMode: Boolean
) : FlingableItemView(context), ThumbnailLoadedCallback {
    private val mAccessibilityActionManager: AccessibilityActionManager

    private var mPost: RedditPreparedPost?=null
    private val title: TextView
    private val subtitle: TextView

    private val mThumbnailView: ImageView
    private val mOverlayIcon: ImageView

    private val mOuterView: LinearLayout
    private val mInnerView: LinearLayout
    private val mCommentsButton: LinearLayout
    private val mCommentsText: TextView
    private val mPostErrors: LinearLayout
    private val mImagePreviewHolder: FrameLayout
    private val mImagePreviewImageView: ImageView
    private val mImagePreviewPlayOverlay: ConstraintLayout
    private val mImagePreviewOuter: LinearLayout
    private val mImagePreviewLoadingSpinner: LoadingSpinnerView
    private val mFooter: LinearLayout

    private var mUsageId = 0

    private val thumbnailHandler: Handler

    private val mLeftFlingPref: PostFlingAction
    private val mRightFlingPref: PostFlingAction
    private var mLeftFlingAction: RedditPostActions.ActionDescriptionPair?=null
    private var mRightFlingAction: RedditPostActions.ActionDescriptionPair?=null

    private val mCommentsButtonPref: Boolean

    private val rrPostTitleReadCol: Int

    private val rrPostTitleCol: Int

    private val mThumbnailSizePrefPixels: Int

    override fun onSetItemFlingPosition(position: Float) {
        mOuterView.setTranslationX(position)
    }

    protected override fun getFlingLeftText(): String {
        mLeftFlingAction = RedditPostActions.ActionDescriptionPair.from(mPost!!, mLeftFlingPref)

        if (mLeftFlingAction != null) {
            return mActivity.getString(mLeftFlingAction!!.descriptionRes)
        } else {
            return "Disabled"
        }
    }

    protected override fun getFlingRightText(): String {
        mRightFlingAction = RedditPostActions.ActionDescriptionPair.from(mPost!!, mRightFlingPref)

        if (mRightFlingAction != null) {
            return mActivity.getString(mRightFlingAction!!.descriptionRes)
        } else {
            return "Disabled"
        }
    }

    override fun allowFlingingLeft(): Boolean {
        return mLeftFlingAction != null
    }

    override fun allowFlingingRight(): Boolean {
        return mRightFlingAction != null
    }

    override fun onFlungLeft() {
        RedditPostActions.onActionMenuItemSelected(
            mPost!!,
            mActivity,
            mLeftFlingAction!!.action
        )
    }

    override fun onFlungRight() {
        RedditPostActions.onActionMenuItemSelected(
            mPost!!,
            mActivity,
            mRightFlingAction!!.action
        )
    }


    init {
        mAccessibilityActionManager = AccessibilityActionManager(
            this,
            context.getResources()
        )

        thumbnailHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (mUsageId != msg.what) {
                    return
                }
                mThumbnailView.setImageBitmap(msg.obj as Bitmap?)
            }
        }

        val dpScale = context.getResources().getDisplayMetrics().density

        val titleFontScale = PrefsUtility.appearance_fontscale_posts()
        val subtitleFontScale = PrefsUtility.appearance_fontscale_post_subtitles()

        val rootView =             LayoutInflater.from(context).inflate(R.layout.reddit_post, this, true)

        mOuterView =             Objects.requireNonNull<LinearLayout>(rootView.findViewById<LinearLayout?>(R.id.reddit_post_layout_outer))
        mInnerView =             Objects.requireNonNull<LinearLayout>(rootView.findViewById<LinearLayout?>(R.id.reddit_post_layout_inner))

        mPostErrors =             Objects.requireNonNull<LinearLayout>(rootView.findViewById<LinearLayout?>(R.id.reddit_post_errors))

        mImagePreviewHolder = Objects.requireNonNull<FrameLayout>(
            rootView.findViewById<FrameLayout?>(R.id.reddit_post_image_preview_holder)
        )

        mImagePreviewImageView = Objects.requireNonNull<ImageView>(
            rootView.findViewById<ImageView?>(R.id.reddit_post_image_preview_imageview)
        )

        mImagePreviewPlayOverlay = Objects.requireNonNull<ConstraintLayout>(
            rootView.findViewById<ConstraintLayout?>(R.id.reddit_post_image_preview_play_overlay)
        )

        mImagePreviewOuter = Objects.requireNonNull<LinearLayout>(
            rootView.findViewById<LinearLayout?>(R.id.reddit_post_image_preview_outer)
        )

        mFooter = Objects.requireNonNull<LinearLayout>(
            rootView.findViewById<LinearLayout?>(R.id.reddit_post_footer)
        )

        mImagePreviewLoadingSpinner = LoadingSpinnerView(mActivity)
        mImagePreviewHolder.addView(mImagePreviewLoadingSpinner)

        mThumbnailView = Objects.requireNonNull<ImageView>(
            rootView.findViewById<ImageView?>(R.id.reddit_post_thumbnail_view)
        )

        mOverlayIcon = Objects.requireNonNull<ImageView>(
            rootView.findViewById<ImageView?>(R.id.reddit_post_overlay_icon)
        )

        title =             Objects.requireNonNull<TextView>(rootView.findViewById<TextView?>(R.id.reddit_post_title))
        subtitle =             Objects.requireNonNull<TextView>(rootView.findViewById<TextView?>(R.id.reddit_post_subtitle))

        mCommentsButtonPref =             PrefsUtility.appearance_post_show_comments_button()

        mCommentsButton = rootView.findViewById<LinearLayout>(R.id.reddit_post_comments_button)
        mCommentsText = mCommentsButton.findViewById<TextView>(R.id.reddit_post_comments_text)

        if (!mCommentsButtonPref) {
            mInnerView.removeView(mCommentsButton)
        }

        if (leftHandedMode) {
            val innerViewElements = ArrayList<View?>(3)
            for (i in mInnerView.getChildCount() - 1 downTo 0) {
                innerViewElements.add(mInnerView.getChildAt(i))
                mInnerView.removeViewAt(i)
            }

            for (i in innerViewElements.indices) {
                mInnerView.addView(innerViewElements.get(i))
            }

            mInnerView.setNextFocusRightId(NO_ID)
            if (mCommentsButtonPref) {
                mInnerView.setNextFocusLeftId(mCommentsButton.getId())

                mCommentsButton.setNextFocusForwardId(R.id.reddit_post_layout_outer)
                mCommentsButton.setNextFocusRightId(R.id.reddit_post_layout_outer)
                mCommentsButton.setNextFocusLeftId(NO_ID)
            }
        }

        val longClickListener = OnLongClickListener { v: View? ->
            RedditPostActions.showActionMenu(mActivity, mPost!!)
            true
        }

        when (PrefsUtility.pref_behaviour_post_tap_action()) {
            PostTapAction.LINK -> {
                mOuterView.setOnClickListener(OnClickListener { v: View? ->
                    fragmentParent.onPostSelected(
                        mPost
                    )
                })
                removeClickListeners(mThumbnailView)
                removeClickListeners(mImagePreviewOuter)
                removeClickListeners(title)
            }

            PostTapAction.COMMENTS -> {
                mOuterView.setOnClickListener(OnClickListener { v: View? ->
                    fragmentParent.onPostCommentsSelected(
                        mPost
                    )
                })

                mThumbnailView.setOnClickListener(OnClickListener { v: View? ->
                    fragmentParent.onPostSelected(
                        mPost
                    )
                })
                mThumbnailView.setOnLongClickListener(longClickListener)

                mImagePreviewOuter.setOnClickListener(OnClickListener { v: View? ->
                    fragmentParent.onPostSelected(
                        mPost
                    )
                })
                mImagePreviewOuter.setOnLongClickListener(longClickListener)

                removeClickListeners(title)
            }

            PostTapAction.TITLE_COMMENTS -> {
                mOuterView.setOnClickListener(OnClickListener { v: View? ->
                    fragmentParent.onPostSelected(
                        mPost
                    )
                })

                removeClickListeners(mThumbnailView)
                removeClickListeners(mImagePreviewOuter)

                title.setOnClickListener(OnClickListener { v: View? ->
                    fragmentParent.onPostCommentsSelected(
                        mPost
                    )
                })
                title.setOnLongClickListener(longClickListener)
            }
        }

        mOuterView.setOnLongClickListener(longClickListener)

        if (mCommentsButtonPref) {
            mCommentsButton.setOnClickListener(OnClickListener { v: View? ->
                fragmentParent.onPostCommentsSelected(
                    mPost
                )
            })
        }

        title.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            title.getTextSize() * titleFontScale
        )
        subtitle.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            subtitle.getTextSize() * subtitleFontScale
        )

        mLeftFlingPref =             PrefsUtility.pref_behaviour_fling_post_left()
        mRightFlingPref =             PrefsUtility.pref_behaviour_fling_post_right()

        run {
            val attr = context.obtainStyledAttributes(
                intArrayOf(
                    R.attr.rrPostTitleCol,
                    R.attr.rrPostTitleReadCol,
                )
            )
            rrPostTitleCol = attr.getColor(0, 0)
            rrPostTitleReadCol = attr.getColor(1, 0)
            attr.recycle()
        }

        mThumbnailSizePrefPixels = (dpScale * PrefsUtility.images_thumbnail_size_dp()).toInt()
    }

    @UiThread
    fun reset(newPost: RedditPreparedPost) {
        if (newPost != mPost) {
            mThumbnailView.setImageBitmap(null)
            mImagePreviewImageView.setImageBitmap(null)
            mImagePreviewPlayOverlay.setVisibility(GONE)
            mPostErrors.removeAllViews()
            mFooter.removeAllViews()

            mUsageId++

            resetSwipeState()

            title.setText(newPost.src.title)
            if (mCommentsButtonPref) {
                mCommentsText.setText(newPost.src.src.num_comments.toString())
            }

            val showInlinePreview = newPost.shouldShowInlinePreview()

            val showThumbnail = !showInlinePreview && newPost.hasThumbnail

            if (showInlinePreview) {
                downloadInlinePreview(newPost, mUsageId)
            } else {
                mImagePreviewLoadingSpinner.setVisibility(GONE)
                mImagePreviewOuter.setVisibility(GONE)
                setBottomMargin(false)
            }

            if (showThumbnail) {
                val thumbnail = newPost.getThumbnail(this, mUsageId)
                mThumbnailView.setImageBitmap(thumbnail)

                mThumbnailView.setVisibility(VISIBLE)
                mThumbnailView.setMinimumWidth(mThumbnailSizePrefPixels)

                setLayoutWidthHeight(
                    mThumbnailView,
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.MATCH_PARENT
                )

                mInnerView.setMinimumHeight(mThumbnailSizePrefPixels)
            } else {
                mThumbnailView.setMinimumWidth(0)
                mThumbnailView.setVisibility(GONE)
                mInnerView.setMinimumHeight(dpToPixels(mActivity, 64f))
            }
        }

        if (mPost != null) {
            mPost!!.unbind(this)
        }

        newPost.bind(this)

        mPost = newPost

        updateAppearance()
    }

    fun updateAppearance() {
        mOuterView.setBackgroundResource(
            R.drawable.rr_postlist_item_selector_main
        )

        if (mCommentsButtonPref) {
            mCommentsButton.setBackgroundResource(
                R.drawable.rr_postlist_commentbutton_selector_main
            )
        }

        if (mPost!!.isRead()) {
            title.setTextColor(rrPostTitleReadCol)
        } else {
            title.setTextColor(rrPostTitleCol)
        }

        title.setContentDescription(mPost!!.buildAccessibilityTitle(mActivity, false))

        subtitle.setText(mPost!!.buildSubtitle(mActivity, false))
        subtitle.setContentDescription(mPost!!.buildAccessibilitySubtitle(mActivity, false))

        var overlayVisible = true

        if (mPost!!.isSaved()) {
            mOverlayIcon.setImageResource(R.drawable.star_dark)
        } else if (mPost!!.isHidden()) {
            mOverlayIcon.setImageResource(R.drawable.ic_action_cross_dark)
        } else if (mPost!!.isUpvoted()) {
            mOverlayIcon.setImageResource(R.drawable.arrow_up_bold_orangered)
        } else if (mPost!!.isDownvoted()) {
            mOverlayIcon.setImageResource(R.drawable.arrow_down_bold_periwinkle)
        } else {
            overlayVisible = false
        }

        if (overlayVisible) {
            mOverlayIcon.setVisibility(VISIBLE)
        } else {
            mOverlayIcon.setVisibility(GONE)
        }

        RedditPostActions.setupAccessibilityActions(
            mAccessibilityActionManager,
            mPost!!,
            mActivity,
            false
        )
    }

    override fun betterThumbnailAvailable(
        thumbnail: Bitmap?,
        callbackUsageId: Int
    ) {
        val msg = Message.obtain()
        msg.obj = thumbnail
        msg.what = callbackUsageId
        thumbnailHandler.sendMessage(msg)
    }

    interface PostSelectionListener {
        fun onPostSelected(post: RedditPreparedPost?)

        fun onPostCommentsSelected(post: RedditPreparedPost?)
    }

    private fun setBottomMargin(enabled: Boolean) {
        val layoutParams = mOuterView.getLayoutParams() as MarginLayoutParams

        if (enabled) {
            layoutParams.bottomMargin = dpToPixels(mActivity, 6f)
        } else {
            layoutParams.bottomMargin = 0
        }

        mOuterView.setLayoutParams(layoutParams)
    }

    private fun downloadInlinePreview(
        post: RedditPreparedPost,
        usageId: Int
    ) {
        val windowVisibleDisplayFrame = DisplayUtils.getWindowVisibleDisplayFrame(mActivity)

        val screenWidth = min(1080, max(720, windowVisibleDisplayFrame.width()))
        val screenHeight = min(2000, max(400, windowVisibleDisplayFrame.height()))

        val preview = post.src.getPreview(screenWidth, 0)

        if (preview == null || preview.width < 10 || preview.height < 10) {
            mImagePreviewOuter.setVisibility(GONE)
            mImagePreviewLoadingSpinner.setVisibility(GONE)
            setBottomMargin(false)
            return
        }

        val boundedImageHeight = min(
            (screenHeight * 2) / 3,
            ((preview.height.toLong() * screenWidth) / preview.width).toInt()
        )

        val imagePreviewLayoutParams =             mImagePreviewHolder.getLayoutParams() as ConstraintLayout.LayoutParams

        imagePreviewLayoutParams.dimensionRatio = screenWidth.toString() + ":" + boundedImageHeight
        mImagePreviewHolder.setLayoutParams(imagePreviewLayoutParams)

        mImagePreviewOuter.setVisibility(VISIBLE)
        mImagePreviewLoadingSpinner.setVisibility(VISIBLE)
        setBottomMargin(true)

        CacheManager.Companion.getInstance(mActivity).makeRequest(
            CacheRequest(
                preview.url,
                RedditAccountManager.Companion.getAnon(),
                null,
                Priority(Constants.Priority.INLINE_IMAGE_PREVIEW),
                DownloadStrategyIfNotCached.Companion.INSTANCE,
                Constants.FileType.INLINE_IMAGE_PREVIEW,
                DownloadQueueType.IMMEDIATE,
                mActivity,
                object : CacheRequestCallbacks {
                    override fun onDataStreamComplete(
                        stream: GenericFactory<SeekableInputStream, IOException?>,
                        timestamp: TimestampUTC?,
                        session: UUID,
                        fromCache: Boolean,
                        mimetype: String?
                    ) {
                        if (usageId != mUsageId) {
                            return
                        }

                        try {
                            stream.create().use { `is` ->
                                val data = BitmapFactory.decodeStream(`is`)
                                if (data == null) {
                                    throw IOException("Failed to decode bitmap")
                                }

                                // Avoid a crash on badly behaving Android ROMs (where the ImageView
                                // crashes if an image is too big)
                                // Should never happen as we limit the preview size to 3000x3000
                                if (data.getByteCount() > 50 * 1024 * 1024) {
                                    throw RuntimeException(
                                        ("Image was too large: "
                                                + data.getByteCount()
                                                + ", preview URL was "
                                                + preview.url
                                                + " and post was "
                                                + post.src.getIdAndType())
                                    )
                                }

                                val alreadyAcceptedPrompt = getSharedPrefs(mActivity)
                                    .getBoolean(PROMPT_PREF_KEY, false)

                                val totalPreviewsShown: Int=sInlinePreviewsShownThisSession.incrementAndGet()

                                val isVideoPreview = post.isVideoPreview()
                                runOnUiThread(Runnable {
                                    mImagePreviewImageView.setImageBitmap(data)
                                    mImagePreviewLoadingSpinner.setVisibility(GONE)

                                    if (isVideoPreview) {
                                        mImagePreviewPlayOverlay.setVisibility(VISIBLE)
                                    }

                                    // Show every 8 previews, starting at the second one
                                    if (totalPreviewsShown % 8 == 2 && !alreadyAcceptedPrompt) {
                                        showPrefPrompt()
                                    }
                                })
                            }
                        } catch (t: Throwable) {
                            onFailure(
                                getGeneralErrorForFailure(
                                    mActivity,
                                    RequestFailureType.CONNECTION,
                                    t,
                                    null,
                                    preview.url,
                                    Optional.Companion.empty<FailedRequestBody>()
                                )
                            )
                        }
                    }

                    override fun onFailure(error: RRError) {
                        Log.e(TAG, "Failed to download image preview: " + error, error.t)

                        if (usageId != mUsageId) {
                            return
                        }

                        runOnUiThread(Runnable {
                            mImagePreviewLoadingSpinner.setVisibility(GONE)
                            mImagePreviewOuter.setVisibility(GONE)

                            val errorView = ErrorView(
                                mActivity,
                                error
                            )

                            mPostErrors.addView(errorView)
                            setLayoutMatchWidthWrapHeight(errorView)
                        })
                    }
                }
            ))
    }

    private fun showPrefPrompt() {
        val sharedPrefs = getSharedPrefs(mActivity)

        LayoutInflater.from(mActivity).inflate(
            R.layout.inline_images_question_view,
            mFooter,
            true
        )

        val promptView = mFooter.findViewById<FrameLayout?>(R.id.inline_images_prompt_root)

        val keepShowing =             mFooter.findViewById<Button>(R.id.inline_preview_prompt_keep_showing_button)

        val turnOff = mFooter.findViewById<Button>(R.id.inline_preview_prompt_turn_off_button)

        keepShowing.setOnClickListener(OnClickListener { v: View? ->
            RRAnimationShrinkHeight(promptView).start()
            sharedPrefs.edit()
                .putBoolean(PROMPT_PREF_KEY, true)
                .apply()
        })

        turnOff.setOnClickListener(OnClickListener { v: View? ->
            val prefPreview = mActivity.getApplicationContext()
                .getString(
                    string.pref_images_inline_image_previews_key
                )
            sharedPrefs.edit()
                .putBoolean(PROMPT_PREF_KEY, true)
                .putString(prefPreview, "never")
                .apply()
        })
    }

    companion object {
        private const val TAG = "RedditPostView"

        private const val PROMPT_PREF_KEY = "inline_image_prompt_accepted"

        private val sInlinePreviewsShownThisSession = AtomicInteger(0)
    }
}
