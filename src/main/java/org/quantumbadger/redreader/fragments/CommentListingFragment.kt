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
package org.quantumbadger.redreader.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.activities.OptionsMenuUtility
import org.quantumbadger.redreader.activities.OptionsMenuUtility.AppbarItemsPref
import org.quantumbadger.redreader.activities.OptionsMenuUtility.OptionsMenuCommentsListener
import org.quantumbadger.redreader.adapters.FilteredCommentListingManager
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategy
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyAlways
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfTimestampOutsideBounds
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.common.General.isNetworkConnected
import org.quantumbadger.redreader.common.General.isTablet
import org.quantumbadger.redreader.common.General.quickToast
import org.quantumbadger.redreader.common.General.setLayoutMatchParent
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.PrefsUtility.CommentAction
import org.quantumbadger.redreader.common.PrefsUtility.SelfpostAction
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.RRThemeAttributes
import org.quantumbadger.redreader.common.TimestampBound
import org.quantumbadger.redreader.common.time.TimeDuration.Companion.minutes
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.common.time.TimestampUTC.Companion.now
import org.quantumbadger.redreader.reddit.CommentListingRequest
import org.quantumbadger.redreader.reddit.RedditCommentListItem
import org.quantumbadger.redreader.reddit.api.RedditAPICommentAction
import org.quantumbadger.redreader.reddit.api.RedditPostActions
import org.quantumbadger.redreader.reddit.api.RedditPostActions.generateToolbar
import org.quantumbadger.redreader.reddit.api.RedditPostActions.setupAccessibilityActions
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost
import org.quantumbadger.redreader.reddit.url.RedditURLParser
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL
import org.quantumbadger.redreader.views.AccessibilityActionManager
import org.quantumbadger.redreader.views.RedditCommentView
import org.quantumbadger.redreader.views.RedditCommentView.CommentListener
import org.quantumbadger.redreader.views.RedditPostHeaderView
import org.quantumbadger.redreader.views.RedditPostView.PostSelectionListener
import org.quantumbadger.redreader.views.ScrollbarRecyclerViewManager
import org.quantumbadger.redreader.views.bezelmenu.BezelSwipeOverlay
import org.quantumbadger.redreader.views.bezelmenu.BezelSwipeOverlay.BezelSwipeListener
import org.quantumbadger.redreader.views.bezelmenu.SideToolbarOverlay
import org.quantumbadger.redreader.views.bezelmenu.SideToolbarOverlay.SideToolbarPosition
import org.quantumbadger.redreader.views.liststatus.CommentSubThreadView
import org.quantumbadger.redreader.views.liststatus.ErrorView
import java.util.LinkedList
import java.util.UUID
import org.quantumbadger.redreader.common.General

class CommentListingFragment(
    parent: AppCompatActivity,
    savedInstanceState: Bundle?,
    urls: ArrayList<RedditURL?>?,
    session: UUID?,
    searchString: String?,
    forceDownload: Boolean
) : RRFragment(parent, savedInstanceState), PostSelectionListener, CommentListener,
    CommentListingRequest.Listener {
    private val mUser: RedditAccount?
    private val mAllUrls: ArrayList<RedditURL?>?
    private val mUrlsToDownload: LinkedList<RedditURL?>
    private val mSession: UUID?
    private val mDownloadStrategy: DownloadStrategy

    var post: RedditPreparedPost?=null
        private set

    private var mSelfTextVisible = true

    private val mCommentListingManager: FilteredCommentListingManager

    private val mRecyclerView: RecyclerView

    private val mListingView: View
    private val mOverlayFrame: FrameLayout
    private val mFloatingToolbar: LinearLayout?

    private val mSelfTextFontScale: Float
    private val mShowLinkButtons: Boolean

    private var mCachedTimestamp: TimestampUTC?=null

    private var mPreviousFirstVisibleItemPosition: Int?=null

    init {
        if (savedInstanceState != null) {
            mPreviousFirstVisibleItemPosition = savedInstanceState.getInt(
                SAVEDSTATE_FIRST_VISIBLE_POS
            )

            if (savedInstanceState.containsKey(SAVEDSTATE_SELFTEXT_VISIBLE)) {
                mSelfTextVisible = savedInstanceState.getBoolean(SAVEDSTATE_SELFTEXT_VISIBLE)
            }
        }

        mCommentListingManager = FilteredCommentListingManager(parent, searchString)
        mAllUrls = urls

        mUrlsToDownload = LinkedList<RedditURL?>(mAllUrls)

        this.mSession = session

        if (forceDownload) {
            mDownloadStrategy = DownloadStrategyAlways.Companion.INSTANCE
        } else if (session == null && savedInstanceState == null && isNetworkConnected(parent)) {
            mDownloadStrategy = DownloadStrategyIfTimestampOutsideBounds(
                TimestampBound.Companion.notOlderThan(minutes(20))
            )
        } else {
            mDownloadStrategy = DownloadStrategyIfNotCached.Companion.INSTANCE
        }

        mUser = RedditAccountManager.Companion.getInstance(getActivity()).getDefaultAccount()

        parent.invalidateOptionsMenu()

        val context: Context = getActivity()

        mSelfTextFontScale = PrefsUtility.appearance_fontscale_bodytext()

        mShowLinkButtons = PrefsUtility.pref_appearance_linkbuttons()

        mOverlayFrame = FrameLayout(context)

        val recyclerViewManager = ScrollbarRecyclerViewManager(context, null, false)

        if (parent is OptionsMenuCommentsListener
            && PrefsUtility.pref_behaviour_enable_swipe_refresh()
        ) {
            recyclerViewManager.enablePullToRefresh(
                OnRefreshListener { (parent as OptionsMenuCommentsListener).onRefreshComments() })
        }

        mRecyclerView = recyclerViewManager.recyclerView
        mCommentListingManager.setLayoutManager(
            mRecyclerView.getLayoutManager() as LinearLayoutManager?
        )

        mRecyclerView.setAdapter(mCommentListingManager.adapter)
        mListingView = recyclerViewManager.outerView

        mRecyclerView.setItemAnimator(null)

        if (!PrefsUtility.pref_appearance_comments_show_floating_toolbar()) {
            mFloatingToolbar = null
        } else {
            mFloatingToolbar = LayoutInflater.from(context).inflate(
                R.layout.floating_toolbar,
                mOverlayFrame,
                false
            ) as LinearLayout?

            if (PrefsUtility.pref_appearance_left_handed()) {
                val toolBarParams =                     mFloatingToolbar!!.getLayoutParams() as FrameLayout.LayoutParams
                toolBarParams.gravity = Gravity.START or Gravity.BOTTOM
                mFloatingToolbar.setLayoutParams(toolBarParams)
            }

            // We need a container so that setVisible() doesn't mess with the Z-order
            val floatingToolbarContainer = FrameLayout(context)

            floatingToolbarContainer.addView(mFloatingToolbar)
            mOverlayFrame.addView(floatingToolbarContainer)

            if (PrefsUtility.isNightMode) {
                mFloatingToolbar!!.setBackgroundColor(Color.argb(0xCC, 0x33, 0x33, 0x33))
            }

            val buttonVPadding = dpToPixels(context, 12f)
            val buttonHPadding = dpToPixels(context, 16f)

            run {
                val previousButton = LayoutInflater.from(
                    context
                ).inflate(
                    R.layout.flat_image_button, mFloatingToolbar, false
                ) as ImageButton
                previousButton.setPadding(
                    buttonHPadding,
                    buttonVPadding,
                    buttonHPadding,
                    buttonVPadding
                )
                previousButton.setImageResource(R.drawable.ic_ff_up_dark)
                previousButton.setContentDescription(
                    getString(string.button_prev_comment_parent)
                )
                mFloatingToolbar!!.addView(previousButton)

                previousButton.setOnClickListener(View.OnClickListener { view: View? ->
                    onPreviousParent()
                })
                previousButton.setOnLongClickListener(OnLongClickListener { view: View? ->
                    quickToast(context, string.button_prev_comment_parent)
                    true
                })
            }

            run {
                val nextButton = LayoutInflater.from(context)
                    .inflate(
                        R.layout.flat_image_button,
                        mFloatingToolbar,
                        false
                    ) as ImageButton
                nextButton.setPadding(
                    buttonHPadding,
                    buttonVPadding,
                    buttonHPadding,
                    buttonVPadding
                )
                nextButton.setImageResource(R.drawable.ic_ff_down_dark)
                nextButton.setContentDescription(getString(string.button_next_comment_parent))
                mFloatingToolbar!!.addView(nextButton)

                nextButton.setOnClickListener(View.OnClickListener { view: View? ->
                    onNextParent()
                })
                nextButton.setOnLongClickListener(OnLongClickListener { view: View? ->
                    quickToast(context, string.button_next_comment_parent)
                    true
                })
            }
        }

        val toolbarOverlay = SideToolbarOverlay(context)

        val bezelOverlay = BezelSwipeOverlay(
            context,
            object : BezelSwipeListener {
                override fun onSwipe(@BezelSwipeOverlay.SwipeEdge edge: Int): Boolean {
                    if (this.post == null) {
                        return false
                    }

                    toolbarOverlay.setContents(
                        generateToolbar(
                            this.post,
                            getActivity() as BaseActivity,
                            true,
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

        mOverlayFrame.addView(bezelOverlay)
        mOverlayFrame.addView(toolbarOverlay)

        setLayoutMatchParent(bezelOverlay)
        setLayoutMatchParent(toolbarOverlay)

        makeNextRequest(context)
    }

    fun handleCommentVisibilityToggle(view: RedditCommentView) {
        val changeDataManager: RedditChangeDataManager=RedditChangeDataManager.Companion.getInstance(mUser)
        val item = view.comment

        if (item.isComment()) {
            val comment = item.asComment()

            changeDataManager.markHidden(
                now(),
                comment.idAndType,
                !comment.isCollapsed(changeDataManager)
            )

            mCommentListingManager.updateHiddenStatus()

            val layoutManager = mRecyclerView.getLayoutManager() as LinearLayoutManager?
            val position = layoutManager!!.getPosition(view)

            if (position == layoutManager.findFirstVisibleItemPosition()) {
                layoutManager.scrollToPositionWithOffset(position, 0)
            }
        }
    }

    override val listingView: View get() = mListingView

    override val overlayView: View? get() = mOverlayFrame

    override fun onSaveInstanceState(): Bundle {
        val bundle = Bundle()

        val layoutManager = mRecyclerView.getLayoutManager() as LinearLayoutManager?
        bundle.putInt(
            SAVEDSTATE_FIRST_VISIBLE_POS,
            layoutManager!!.findFirstVisibleItemPosition()
        )

        if (this.post != null && post!!.isSelf) {
            bundle.putBoolean(SAVEDSTATE_SELFTEXT_VISIBLE, mSelfTextVisible)
        }

        return bundle
    }

    @SuppressLint("WrongConstant")
    private fun makeNextRequest(context: Context?) {
        if (!mUrlsToDownload.isEmpty()) {
            CommentListingRequest(
                context,
                this,
                getActivity() as BaseActivity,
                mUrlsToDownload.getFirst(),
                mAllUrls!!.size == 1,
                mUrlsToDownload.getFirst(),
                mUser,
                mSession,
                mDownloadStrategy,
                this
            )
        }
    }

    override fun onCommentClicked(view: RedditCommentView) {
        when (PrefsUtility.pref_behaviour_actions_comment_tap()) {
            CommentAction.COLLAPSE -> handleCommentVisibilityToggle(view)
            CommentAction.ACTION_MENU -> {
                val item = view.comment
                if (item != null && item.isComment()) {
                    RedditAPICommentAction.showActionMenu(
                        getActivity(),
                        this,
                        item.asComment(),
                        view,
                        RedditChangeDataManager.Companion.getInstance(mUser),
                        this.post != null && post!!.isLocked
                    )
                }
            }
        }
    }

    override fun onCommentLongClicked(view: RedditCommentView) {
        when (PrefsUtility.pref_behaviour_actions_comment_longclick()) {
            CommentAction.ACTION_MENU -> {
                val item = view.comment
                if (item != null && item.isComment()) {
                    RedditAPICommentAction.showActionMenu(
                        getActivity(),
                        this,
                        item.asComment(),
                        view,
                        RedditChangeDataManager.Companion.getInstance(mUser),
                        this.post != null && post!!.isLocked
                    )
                }
            }

            CommentAction.COLLAPSE -> handleCommentVisibilityToggle(view)
            CommentAction.NOTHING -> {}
        }
    }

    override fun onCommentListingRequestDownloadNecessary() {
        mCommentListingManager.setLoadingVisible(true)
    }

    override fun onCommentListingRequestFailure(error: RRError) {
        mCommentListingManager.setLoadingVisible(false)
        mCommentListingManager.addFooterError(ErrorView(getActivity(), error))
    }

    override fun onCommentListingRequestCachedCopy(timestamp: TimestampUTC?) {
        mCachedTimestamp = timestamp
    }

    override fun onCommentListingRequestParseStart() {
        mCommentListingManager.setLoadingVisible(true)
    }

    override fun onCommentListingRequestPostDownloaded(post: RedditPreparedPost) {
        val activity = getActivity() as BaseActivity

        if (this.post == null) {
            val attr = RRThemeAttributes(activity)

            this.post = post

            // Invalidate the options menu, so the suggested sort will be shown if needed.
            activity.invalidateOptionsMenu()

            val postHeader = RedditPostHeaderView(
                activity,
                this.post
            )

            mCommentListingManager.addPostHeader(postHeader)

            val layoutManager = mRecyclerView.getLayoutManager() as LinearLayoutManager?
            layoutManager!!.scrollToPositionWithOffset(0, 0)

            if (post.src.selfText != null) {
                val selfText = post.src.selfText.generateView(
                    activity,
                    attr.rrMainTextCol,
                    13f * mSelfTextFontScale,
                    mShowLinkButtons
                )
                selfText.setFocusable(false)

                if (selfText is ViewGroup) {
                    selfText.setDescendantFocusability(
                        ViewGroup.FOCUS_BLOCK_DESCENDANTS
                    )
                }

                val paddingPx = dpToPixels(activity, 10f)
                val paddingLayout = FrameLayout(activity)
                val collapsedView = TextView(activity)
                collapsedView.setText(
                    "[ + ]  "
                            + activity.getString(string.collapsed_self_post)
                )
                collapsedView.setVisibility(View.GONE)
                collapsedView.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
                paddingLayout.addView(selfText)
                paddingLayout.addView(collapsedView)
                paddingLayout.setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

                val actionOnClick = PrefsUtility.pref_behaviour_self_post_tap_actions()
                if (actionOnClick == SelfpostAction.COLLAPSE) {
                    paddingLayout.setOnClickListener(View.OnClickListener { v: View? ->
                        if (selfText.getVisibility() == View.GONE) {
                            mSelfTextVisible = true
                            selfText.setVisibility(View.VISIBLE)
                            collapsedView.setVisibility(View.GONE)
                        } else {
                            mSelfTextVisible = false
                            selfText.setVisibility(View.GONE)
                            collapsedView.setVisibility(View.VISIBLE)
                            layoutManager.scrollToPositionWithOffset(0, 0)
                        }
                    })
                }

                if (!mSelfTextVisible) {
                    selfText.setVisibility(View.GONE)
                    collapsedView.setVisibility(View.VISIBLE)
                    layoutManager.scrollToPositionWithOffset(0, 0)
                }

                paddingLayout.setOnLongClickListener(OnLongClickListener { v: View? ->
                    RedditPostActions.showActionMenu(activity, this.post!!)
                    true
                })

                setupAccessibilityActions(
                    AccessibilityActionManager(
                        paddingLayout,
                        activity.getResources()
                    ),
                    post,
                    activity,
                    true
                )

                mCommentListingManager.addPostSelfText(paddingLayout)
            }

            if (!isTablet(activity)) {
                activity.setTitle(post.src.title)
            }

            if (mCommentListingManager.isSearchListing) {
                val searchCommentThreadView = CommentSubThreadView(
                    activity,
                    mAllUrls!!.get(0)!!.asPostCommentListURL(),
                    string.comment_header_search_thread_title
                )

                mCommentListingManager.addNotification(searchCommentThreadView)
            } else if (!mAllUrls!!.isEmpty() && mAllUrls.get(0)!!
                    .pathType() == RedditURLParser.POST_COMMENT_LISTING_URL && mAllUrls.get(0)!!
                    .asPostCommentListURL().commentId != null
            ) {
                val specificCommentThreadView = CommentSubThreadView(
                    activity,
                    mAllUrls.get(0)!!.asPostCommentListURL(),
                    string.comment_header_specific_thread_title
                )

                mCommentListingManager.addNotification(specificCommentThreadView)
            }

            // 30 minutes
            if (mCachedTimestamp != null) {
                if (mCachedTimestamp!!.elapsed().isGreaterThan(minutes(30))) {
                    val cacheNotif = LayoutInflater.from(activity).inflate(
                        R.layout.cached_header,
                        null,
                        false
                    ) as TextView
                    cacheNotif.setText(
                        activity.getString(
                            string.listing_cached,
                            mCachedTimestamp!!.format()
                        )
                    )
                    mCommentListingManager.addNotification(cacheNotif)
                }
            }
        }
    }

    override fun onCommentListingRequestAllItemsDownloaded(
        items: ArrayList<RedditCommentListItem?>?
    ) {
        mCommentListingManager.addComments(items)

        if (mFloatingToolbar != null && mFloatingToolbar.getVisibility() != View.VISIBLE) {
            mFloatingToolbar.setVisibility(View.VISIBLE)
            val animation = AnimationUtils.loadAnimation(
                context,
                R.anim.slide_in_from_bottom
            )
            animation.setInterpolator(OvershootInterpolator())
            mFloatingToolbar.startAnimation(animation)
        }

        mUrlsToDownload.removeFirst()

        val layoutManager = mRecyclerView.getLayoutManager() as LinearLayoutManager?

        if (mPreviousFirstVisibleItemPosition != null
            && layoutManager!!.getItemCount() > mPreviousFirstVisibleItemPosition!!
        ) {
            layoutManager.scrollToPositionWithOffset(
                mPreviousFirstVisibleItemPosition!!,
                0
            )

            mPreviousFirstVisibleItemPosition = null
        }

        if (mUrlsToDownload.isEmpty()) {
            if (mCommentListingManager.commentCount == 0) {
                val emptyView = LayoutInflater.from(context).inflate(
                    R.layout.no_items_yet,
                    mRecyclerView,
                    false
                )

                if (mCommentListingManager.isSearchListing) {
                    (emptyView.findViewById<View?>(R.id.empty_view_text) as TextView)
                        .setText(string.no_search_results)
                } else {
                    (emptyView.findViewById<View?>(R.id.empty_view_text) as TextView)
                        .setText(string.no_comments_yet)
                }

                mCommentListingManager.addViewToItems(emptyView)
            } else {
                val blankView = View(context)
                blankView.setMinimumWidth(1)
                blankView.setMinimumHeight(dpToPixels(context, 96f))
                mCommentListingManager.addViewToItems(blankView)
            }

            mCommentListingManager.setLoadingVisible(false)
        } else {
            makeNextRequest(getActivity())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu) {
        val appbarItemsPrefs: MutableMap<AppbarItemsPref, Int> =
            PrefsUtility.pref_menus_appbar_items()
        val replyShowAsAction = OptionsMenuUtility.getOrThrow(
            appbarItemsPrefs,
            AppbarItemsPref.REPLY
        )

        if (mAllUrls != null && !mAllUrls.isEmpty() && mAllUrls.get(0)!!
                .pathType() == RedditURLParser.POST_COMMENT_LISTING_URL && replyShowAsAction != OptionsMenuUtility.DO_NOT_SHOW
        ) {
            val reply = menu.add(
                Menu.NONE,
                AppbarItemsPref.REPLY.ordinal,
                Menu.NONE,
                string.action_reply
            )

            reply.setShowAsAction(
                OptionsMenuUtility.handleShowAsActionIfRoom(
                    replyShowAsAction
                )
            )
            reply.setIcon(R.drawable.ic_action_reply_dark)

            OptionsMenuUtility.pruneMenu(getActivity(), menu, appbarItemsPrefs, true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getTitle() != null
            && (item.getTitle()
                    == getActivity().getString(string.action_reply))
        ) {
            RedditPostActions.onActionMenuItemSelected(
                this.post!!,
                getActivity() as BaseActivity,
                RedditPostActions.Action.REPLY
            )
            return true
        }

        return false
    }

    override fun onPostSelected(post : RedditPreparedPost) {
        (getActivity() as PostSelectionListener).onPostSelected(post)
    }

    override fun onPostCommentsSelected(post : RedditPreparedPost) {
        (getActivity() as PostSelectionListener).onPostCommentsSelected(post)
    }

    fun onPreviousParent() {
        val layoutManager = mRecyclerView.getLayoutManager() as LinearLayoutManager?

        for (pos in layoutManager!!.findFirstVisibleItemPosition() - 1 downTo 1) {
            val item = mCommentListingManager.getItemAtPosition(
                pos
            )
            if (item is RedditCommentListItem
                && item.isComment
                && item.indent == 0
            ) {
                layoutManager.scrollToPositionWithOffset(pos, 0)
                setFocusDelayed(pos)
                return
            }
        }

        layoutManager.scrollToPositionWithOffset(0, 0)
        setFocusDelayed(0)
    }

    fun onNextParent() {
        val layoutManager = mRecyclerView.getLayoutManager() as LinearLayoutManager?
        for (pos in layoutManager!!.findFirstVisibleItemPosition() + 1..<layoutManager.getItemCount()) {
            val item = mCommentListingManager.getItemAtPosition(
                pos
            )
            if (item is RedditCommentListItem
                && item.isComment
                && item.indent == 0
            ) {
                layoutManager.scrollToPositionWithOffset(pos, 0)
                setFocusDelayed(pos)
                break
            }
        }
    }

    @SuppressLint("AccessibilityFocus")
    private fun setFocusDelayed(pos: Int) {
        AndroidCommon.UI_THREAD_HANDLER.postDelayed(Runnable {
            val view = mRecyclerView.findViewHolderForAdapterPosition(pos)
            if (view != null) {
                val item = view.itemView
                item.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
                item.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED)
                item.performAccessibilityAction(
                    AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
                    null
                )
            }
        }, 800)
    }

    companion object {
        private const val SAVEDSTATE_FIRST_VISIBLE_POS = "firstVisiblePosition"
        private const val SAVEDSTATE_SELFTEXT_VISIBLE = "selftextVisible"
    }
}
