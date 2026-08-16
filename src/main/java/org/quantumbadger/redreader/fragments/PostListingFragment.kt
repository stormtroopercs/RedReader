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

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import org.apache.commons.text.StringEscapeUtils
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.activities.BugReportActivity.Companion.handleGlobalError
import org.quantumbadger.redreader.activities.OptionsMenuUtility.OptionsMenuPostsListener
import org.quantumbadger.redreader.activities.SessionChangeListener
import org.quantumbadger.redreader.activities.SessionChangeListener.SessionChangeType
import org.quantumbadger.redreader.adapters.MainMenuListingManager
import org.quantumbadger.redreader.adapters.PostListingManager
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequest.DownloadQueueType
import org.quantumbadger.redreader.cache.CacheRequest.RequestFailureType
import org.quantumbadger.redreader.cache.CacheRequestCallbacks
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategy
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyAlways
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfTimestampOutsideBounds
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyNever
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.AndroidCommon.runOnUiThread
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.FileUtils
import org.quantumbadger.redreader.common.General.checkThisIsUIThread
import org.quantumbadger.redreader.common.General.getGeneralErrorForFailure
import org.quantumbadger.redreader.common.General.isConnectionWifi
import org.quantumbadger.redreader.common.General.isNetworkConnected
import org.quantumbadger.redreader.common.General.isSensitiveDebugLoggingEnabled
import org.quantumbadger.redreader.common.General.setLayoutMatchParent
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.LinkHandler.getImageInfo
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.PrefsUtility.GifViewMode
import org.quantumbadger.redreader.common.PrefsUtility.ImageViewMode
import org.quantumbadger.redreader.common.PrefsUtility.PostCount
import org.quantumbadger.redreader.common.PrefsUtility.VideoViewMode
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.TimestampBound
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.UriString.Companion.from
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import org.quantumbadger.redreader.common.time.TimeDuration.Companion.hours
import org.quantumbadger.redreader.common.time.TimeDuration.Companion.minutes
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.http.FailedRequestBody
import org.quantumbadger.redreader.image.GetImageInfoListener
import org.quantumbadger.redreader.image.ImageInfo
import org.quantumbadger.redreader.io.RequestResponseHandler
import org.quantumbadger.redreader.listingcontrollers.CommentListingController
import org.quantumbadger.redreader.reddit.PostSort
import org.quantumbadger.redreader.reddit.RedditPostListItem
import org.quantumbadger.redreader.reddit.RedditSubredditManager
import org.quantumbadger.redreader.reddit.api.RedditPostActions.ActionDescriptionPair.Companion.from
import org.quantumbadger.redreader.reddit.api.RedditPostActions.showActionMenu
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager
import org.quantumbadger.redreader.reddit.kthings.JsonUtils.decodeRedditThingFromStream
import org.quantumbadger.redreader.reddit.kthings.MaybeParseError
import org.quantumbadger.redreader.reddit.kthings.RedditComment.subreddit
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import org.quantumbadger.redreader.reddit.kthings.RedditPost.subreddit
import org.quantumbadger.redreader.reddit.kthings.RedditThing
import org.quantumbadger.redreader.reddit.kthings.RedditThing.Listing
import org.quantumbadger.redreader.reddit.kthings.RedditThing.Post
import org.quantumbadger.redreader.reddit.prepared.RedditParsedPost
import org.quantumbadger.redreader.reddit.prepared.RedditParsedPost.subreddit
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost
import org.quantumbadger.redreader.reddit.things.InvalidSubredditNameException
import org.quantumbadger.redreader.reddit.things.RedditSubreddit
import org.quantumbadger.redreader.reddit.things.SubredditCanonicalId
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL
import org.quantumbadger.redreader.reddit.url.PostListingURL
import org.quantumbadger.redreader.reddit.url.RedditURLParser
import org.quantumbadger.redreader.reddit.url.SearchPostListURL
import org.quantumbadger.redreader.reddit.url.SubredditPostListURL
import org.quantumbadger.redreader.views.PostListingHeader
import org.quantumbadger.redreader.views.RedditPostView.PostSelectionListener
import org.quantumbadger.redreader.views.ScrollbarRecyclerViewManager
import org.quantumbadger.redreader.views.SearchListingHeader
import org.quantumbadger.redreader.views.liststatus.ErrorView
import java.io.IOException
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import org.quantumbadger.redreader.common.General

class PostListingFragment(
    parent: AppCompatActivity,
    savedInstanceState: Bundle?,
    url: Uri,
    session: UUID?,
    forceDownload: Boolean
) : RRFragment(parent, savedInstanceState), PostSelectionListener {
    private var mPostListingURL: PostListingURL?=null

    var subreddit: RedditSubreddit?=null
        private set

    private var mSession: UUID?
    private val mPostCountLimit: Int
    private var mLoadMoreView: TextView?=null

    private val mPostListingManager: PostListingManager
    private val mRecyclerView: RecyclerView

    private val mOuter: View?

    private var mAfter: RedditIdAndType?=null
    private var mLastAfter: RedditIdAndType?=null
    private var mRequest: CacheRequest?
    private var mReadyToDownloadMore = false
    private var mTimestamp: TimestampUTC?=null

    private var mPostCount = 0
    private var mPostsNotShown = false
    private val mPostRefreshCount = AtomicInteger(0)

    private val mPostIds = HashSet<String?>(200)

    private var mPreviousFirstVisibleItemPosition: Int?=null

    // Session may be null
    init {
        mPostListingManager = PostListingManager(parent)

        if (savedInstanceState != null) {
            mPreviousFirstVisibleItemPosition = savedInstanceState.getInt(
                SAVEDSTATE_FIRST_VISIBLE_POS
            )
        }

        try {
            mPostListingURL = RedditURLParser.parseProbablePostListing(url) as PostListingURL
        } catch (e: ClassCastException) {
            Toast.makeText(getActivity(), "Invalid post listing URL.", Toast.LENGTH_LONG)
                .show()
            // TODO proper error handling -- show error view
            throw RuntimeException(e)
        }

        mSession = session

        val context = context

        // TODO output failed URL
        if (mPostListingURL == null) {
            mPostListingManager.addFooterError(
                ErrorView(
                    getActivity(),
                    RRError(
                        "Invalid post listing URL",
                        "Could not navigate to that URL.",
                        true,
                        RuntimeException(),
                        null,
                        from(url),
                        null
                    )
                )
            )
            // TODO proper error handling
            throw RuntimeException("Invalid post listing URL")
        }

        when (PrefsUtility.pref_behaviour_post_count()) {
            PostCount.ALL -> mPostCountLimit = -1
            PostCount.R25 -> mPostCountLimit = 25
            PostCount.R50 -> mPostCountLimit = 50
            PostCount.R100 -> mPostCountLimit = 100
            else -> mPostCountLimit = 0
        }

        if (mPostCountLimit > 0) {
            restackRefreshCount()
        }

        val recyclerViewManager = ScrollbarRecyclerViewManager(context, null, false)

        if (parent is OptionsMenuPostsListener
            && PrefsUtility.pref_behaviour_enable_swipe_refresh()
        ) {
            recyclerViewManager.enablePullToRefresh(
                OnRefreshListener { (parent as OptionsMenuPostsListener).onRefreshPosts() })
        }

        mRecyclerView = recyclerViewManager.recyclerView
        mPostListingManager.setLayoutManager(mRecyclerView.getLayoutManager() as LinearLayoutManager?)

        mRecyclerView.setAdapter(mPostListingManager.adapter)

        mOuter = recyclerViewManager.outerView

        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(
                recyclerView: RecyclerView,
                dx: Int,
                dy: Int
            ) {
                onLoadMoreItemsCheck()
            }
        })

        setLayoutMatchParent(mRecyclerView)

        val downloadStrategy: DownloadStrategy

        if (forceDownload) {
            downloadStrategy = DownloadStrategyAlways.Companion.INSTANCE
        } else if (session == null && savedInstanceState == null && isNetworkConnected(context)) {
            val maxAge = PrefsUtility.pref_cache_rerequest_postlist_age()
            downloadStrategy = DownloadStrategyIfTimestampOutsideBounds(
                TimestampBound.Companion.notOlderThan(maxAge)
            )
        } else {
            downloadStrategy = DownloadStrategyIfNotCached.Companion.INSTANCE
        }

        mRequest = createPostListingRequest(
            from(mPostListingURL!!.generateJsonUri()),
            RedditAccountManager.Companion.getInstance(context).getDefaultAccount(),
            session,
            downloadStrategy,
            true
        )

        // The request doesn't go ahead until the header is in place.
        when (mPostListingURL!!.pathType()) {
            RedditURLParser.SEARCH_POST_LISTING_URL -> {
                setHeader(
                    SearchListingHeader(
                        getActivity(),
                        mPostListingURL as SearchPostListURL?
                    )
                )
                CacheManager.Companion.getInstance(context).makeRequest(mRequest)
            }

            RedditURLParser.USER_POST_LISTING_URL, RedditURLParser.MULTIREDDIT_POST_LISTING_URL -> {
                setHeader(
                    mPostListingURL!!.humanReadableName(getActivity(), true),
                    mPostListingURL!!.humanReadableUrl(),
                    null
                )
                CacheManager.Companion.getInstance(context).makeRequest(mRequest)
            }

            RedditURLParser.SUBREDDIT_POST_LISTING_URL -> {
                val subredditPostListURL = mPostListingURL as SubredditPostListURL

                when (subredditPostListURL.type) {
                    SubredditPostListURL.Type.FRONTPAGE, SubredditPostListURL.Type.ALL, SubredditPostListURL.Type.SUBREDDIT_COMBINATION, SubredditPostListURL.Type.ALL_SUBTRACTION, SubredditPostListURL.Type.POPULAR -> {
                        setHeader(
                            mPostListingURL.humanReadableName(getActivity(), true),
                            mPostListingURL!!.humanReadableUrl(),
                            null
                        )
                        CacheManager.Companion.getInstance(context).makeRequest(mRequest)
                    }

                    SubredditPostListURL.Type.SUBREDDIT -> {
                        // Request the subreddit data
                        val subredditHandler: RequestResponseHandler<RedditSubreddit?, RRError?> =                             object : RequestResponseHandler<RedditSubreddit?, RRError?> {
                                override fun onRequestFailed(
                                    failureReason: RRError?
                                ) {
                                    // Ignore
                                    AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                                        CacheManager.Companion.getInstance(
                                            context
                                        ).makeRequest(mRequest)
                                    })
                                }

                                override fun onRequestSuccess(
                                    result: RedditSubreddit?,
                                    timeCached: TimestampUTC?
                                ) {
                                    AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                                        this.subreddit = result
                                        if (subreddit!!.over18
                                            && !PrefsUtility.pref_behaviour_nsfw()
                                        ) {
                                            mPostListingManager.setLoadingVisible(false)

                                            val title = string.error_nsfw_subreddits_disabled_title

                                            val message =                                                 string.error_nsfw_subreddits_disabled_message

                                            mPostListingManager.addFooterError(
                                                ErrorView(
                                                    getActivity(),
                                                    RRError(
                                                        context.getString(title),
                                                        context.getString(message),
                                                        false
                                                    )
                                                )
                                            )
                                        } else {
                                            onSubredditReceived()
                                            CacheManager.Companion.getInstance(context)
                                                .makeRequest(mRequest)
                                        }
                                    })
                                }
                            }

                        try {
                            RedditSubredditManager.Companion.getInstance(
                                getActivity(),
                                RedditAccountManager.Companion.getInstance(getActivity())
                                    .getDefaultAccount()
                            )
                                .getSubreddit(
                                    SubredditCanonicalId(
                                        subredditPostListURL.subreddit!!
                                    ),
                                    TimestampBound.Companion.NONE,
                                    subredditHandler,
                                    null
                                )
                        } catch (e: InvalidSubredditNameException) {
                            throw RuntimeException(e)
                        }
                    }
                }
            }

            RedditURLParser.POST_COMMENT_LISTING_URL, RedditURLParser.UNKNOWN_COMMENT_LISTING_URL, RedditURLParser.UNKNOWN_POST_LISTING_URL, RedditURLParser.USER_COMMENT_LISTING_URL, RedditURLParser.USER_PROFILE_URL, RedditURLParser.COMPOSE_MESSAGE_URL, RedditURLParser.OPAQUE_SHARED_URL -> handleGlobalError(
                getActivity(), RuntimeException(
                    ("Unknown url type "
                            + mPostListingURL!!.pathType()
                            + ": "
                            + mPostListingURL.toString())
                )
            )
        }
    }

    override val listingView: View? get() = mOuter

    override fun onSaveInstanceState(): Bundle {
        val bundle = Bundle()

        val layoutManager = mRecyclerView.getLayoutManager() as LinearLayoutManager?
        bundle.putInt(
            SAVEDSTATE_FIRST_VISIBLE_POS,
            layoutManager!!.findFirstVisibleItemPosition()
        )

        return bundle
    }

    fun cancel() {
        if (mRequest != null) {
            mRequest!!.cancel()
        }
    }

    @Synchronized
    fun restackRefreshCount() {
        while (mPostRefreshCount.get() <= 0) {
            mPostRefreshCount.addAndGet(mPostCountLimit)
        }
    }

    private fun onSubredditReceived() {
        val subtitle: String

        if (mPostListingURL!!.order == null
            || mPostListingURL!!.order == PostSort.HOT
        ) {
            if (subreddit!!.subscribers == null) {
                subtitle = getString(string.header_subscriber_count_unknown)
            } else {
                subtitle = context.getString(
                    string.header_subscriber_count,
                    NumberFormat.getNumberInstance(Locale.getDefault())
                        .format(subreddit!!.subscribers)
                )
            }
        } else {
            subtitle = mPostListingURL!!.humanReadableUrl()
        }

        getActivity().runOnUiThread(Runnable {
            setHeader(
                StringEscapeUtils.unescapeHtml4(subreddit!!.title),
                subtitle,
                this.subreddit
            )
            getActivity().invalidateOptionsMenu()
        })
    }

    private fun setHeader(
        title: String,
        subtitle: String,
        subreddit: RedditSubreddit?
    ) {
        val postListingHeader = PostListingHeader(
            getActivity(),
            title,
            subtitle,
            mPostListingURL,
            subreddit
        )

        setHeader(postListingHeader)

        if (subreddit != null) {
            postListingHeader.setOnLongClickListener(OnLongClickListener { view: View? ->
                try {
                    MainMenuListingManager.Companion.showActionMenu(
                        getActivity(),
                        subreddit.canonicalId
                    )
                } catch (e: InvalidSubredditNameException) {
                    throw RuntimeException(e)
                }
                true
            })
        }
    }

    private fun setHeader(view: View?) {
        getActivity().runOnUiThread(Runnable { mPostListingManager.addPostListingHeader(view) })
    }


    override fun onPostSelected(post: RedditPreparedPost) {
        (getActivity() as PostSelectionListener).onPostSelected(post)

        object : Thread() {
            override fun run() {
                post.markAsRead(getActivity())
            }
        }.start()
    }

    override fun onPostCommentsSelected(post: RedditPreparedPost) {
        (getActivity() as PostSelectionListener).onPostCommentsSelected(post)

        object : Thread() {
            override fun run() {
                post.markAsRead(getActivity())
            }
        }.start()
    }

    private fun onLoadMoreItemsCheck() {
        checkThisIsUIThread()

        if (mReadyToDownloadMore && mAfter != null && (mAfter != mLastAfter)) {
            val layoutManager = mRecyclerView.getLayoutManager() as LinearLayoutManager?

            if (((layoutManager!!.getItemCount() - layoutManager.findLastVisibleItemPosition()
                        < 20)
                        && (mPostCountLimit <= 0 || mPostRefreshCount.get() > 0)
                        || (mPreviousFirstVisibleItemPosition != null
                        && (layoutManager.getItemCount()
                        <= mPreviousFirstVisibleItemPosition!!)))
            ) {
                mLastAfter = mAfter
                mReadyToDownloadMore = false

                val newUri = mPostListingURL!!.after(mAfter).generateJsonUri()

                // TODO customise (currently 3 hrs)
                val strategy: DownloadStrategy = if (mTimestamp!!.elapsed()
                        .isLessThan(hours(3))
                )
                    DownloadStrategyIfNotCached.Companion.INSTANCE
                else
                    DownloadStrategyNever.Companion.INSTANCE

                mRequest = createPostListingRequest(
                    from(newUri),
                    RedditAccountManager.Companion.getInstance(getActivity())
                        .getDefaultAccount(),
                    mSession,
                    strategy,
                    false
                )
                mPostListingManager.setLoadingVisible(true)
                CacheManager.Companion.getInstance(getActivity()).makeRequest(mRequest)
            } else if (mPostCountLimit > 0 && mPostRefreshCount.get() <= 0) {
                if (mLoadMoreView == null) {
                    mLoadMoreView = LayoutInflater.from(context)
                        .inflate(R.layout.load_more_posts, null) as TextView?
                    mLoadMoreView!!.setOnClickListener(View.OnClickListener { view: View? ->
                        mPostListingManager.removeLoadMoreButton()
                        mLoadMoreView = null
                        restackRefreshCount()
                        onLoadMoreItemsCheck()
                    })

                    mPostListingManager.addLoadMoreButton(mLoadMoreView)
                }
            }
        }
    }

    fun onSubscribe() {
        if (mPostListingURL!!.pathType() != RedditURLParser.SUBREDDIT_POST_LISTING_URL) {
            return
        }

        try {
            RedditSubredditSubscriptionManager.Companion.getSingleton(
                getActivity(),
                RedditAccountManager.Companion.getInstance(getActivity())
                    .getDefaultAccount()
            )
                .subscribe(
                    SubredditCanonicalId(
                        mPostListingURL!!.asSubredditPostListURL().subreddit!!
                    ),
                    getActivity()
                )
        } catch (e: InvalidSubredditNameException) {
            throw RuntimeException(e)
        }
    }

    fun onUnsubscribe() {
        if (this.subreddit == null) {
            return
        }

        try {
            RedditSubredditSubscriptionManager.Companion.getSingleton(
                getActivity(),
                RedditAccountManager.Companion.getInstance(getActivity())
                    .getDefaultAccount()
            )
                .unsubscribe(subreddit!!.canonicalId, getActivity())
        } catch (e: InvalidSubredditNameException) {
            throw RuntimeException(e)
        }
    }

    val postListingURL: PostListingURL
        get() = mPostListingURL!!

    fun onPostsAdded() {
        if (mPreviousFirstVisibleItemPosition == null) {
            return
        }

        val layoutManager = mRecyclerView.getLayoutManager() as LinearLayoutManager?

        if (layoutManager!!.getItemCount() > mPreviousFirstVisibleItemPosition!!) {
            layoutManager.scrollToPositionWithOffset(
                mPreviousFirstVisibleItemPosition!!,
                0
            )
            mPreviousFirstVisibleItemPosition = null
        } else {
            layoutManager.scrollToPosition(layoutManager.getItemCount() - 1)
        }
    }

    private fun createPostListingRequest(
        url: UriString,
        user: RedditAccount,
        requestSession: UUID?,
        downloadStrategy: DownloadStrategy,
        firstDownload: Boolean
    ): CacheRequest {
        val activity = getActivity()

        return CacheRequest(
            url,
            user,
            requestSession,
            Priority(Constants.Priority.API_POST_LIST),
            downloadStrategy,
            Constants.FileType.POST_LIST,
            DownloadQueueType.REDDIT_API,
            activity,
            object : CacheRequestCallbacks {
                override fun onDataStreamComplete(
                    streamFactory: GenericFactory<SeekableInputStream, IOException?>,
                    timestamp: TimestampUTC,
                    session: UUID,
                    fromCache: Boolean,
                    mimetype: String?
                ) {
                    val activity = getActivity() as BaseActivity

                    // One hour (matches default refresh value)
                    if (firstDownload && fromCache) {
                        if (timestamp.elapsedPeriod()
                                .asDuration().isGreaterThan(hours(1))
                        ) {
                            AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                                val cacheNotif = LayoutInflater.from(activity).inflate(
                                    R.layout.cached_header,
                                    null,
                                    false
                                ) as TextView
                                cacheNotif.setText(
                                    getActivity().getString(
                                        string.listing_cached,
                                        timestamp.format()
                                    )
                                )
                                mPostListingManager.addNotification(cacheNotif)
                            })
                        } // TODO resuming a copy
                    }

                    if (firstDownload) {
                        (activity as SessionChangeListener).onSessionChanged(
                            session,
                            SessionChangeType.POSTS,
                            timestamp
                        )
                        this@PostListingFragment.mSession = session
                        this@PostListingFragment.mTimestamp = timestamp
                    }

                    // TODO {"error": 403} is received for unauthorized subreddits
                    try {
                        val thing = decodeRedditThingFromStream(streamFactory.create())

                        if (thing !is Listing) {
                            throw RuntimeException(
                                "Expected listing, got "
                                        + thing.javaClass.getName()
                            )
                        }

                        val listing = thing.data

                        val posts: ArrayList<MaybeParseError<RedditThing?>?> = listing.children

                        val isNsfwAllowed = PrefsUtility.pref_behaviour_nsfw()

                        val hideReadPosts = PrefsUtility.pref_behaviour_hide_read_posts()
                                && (mPostListingURL!!.pathType()
                                != RedditURLParser.USER_POST_LISTING_URL)

                        val isConnectionWifi = isConnectionWifi(activity)

                        val inlinePreviews = PrefsUtility.images_inline_image_previews()
                            .isEnabled(isConnectionWifi)

                        val showNsfwPreviews = PrefsUtility.images_inline_image_previews_nsfw()

                        val showSpoilerPreviews =                             PrefsUtility.images_inline_image_previews_spoiler()

                        val downloadThumbnails = PrefsUtility.appearance_thumbnails_show()
                            .isEnabled(isConnectionWifi)

                        val allowHighResThumbnails = downloadThumbnails
                                && PrefsUtility.images_high_res_thumbnails()
                            .isEnabled(isConnectionWifi)

                        val showNsfwThumbnails = PrefsUtility.appearance_thumbnails_nsfw_show()

                        val showSpoilerThumbnails =                             PrefsUtility.appearance_thumbnails_spoiler_show()

                        val precacheImages = !inlinePreviews && PrefsUtility.cache_precache_images()
                            .isEnabled(isConnectionWifi)
                                && !FileUtils.isCacheDiskFull(activity)

                        val precacheComments = PrefsUtility.cache_precache_comments()
                            .isEnabled(isConnectionWifi)

                        val imageViewMode = PrefsUtility.pref_behaviour_imageview_mode()

                        val gifViewMode = PrefsUtility.pref_behaviour_gifview_mode()

                        val videoViewMode = PrefsUtility.pref_behaviour_videoview_mode()

                        val leftHandedMode = PrefsUtility.pref_appearance_left_handed()

                        val subredditFilteringEnabled =                             (mPostListingURL!!.pathType()
                                    == RedditURLParser.SUBREDDIT_POST_LISTING_URL)
                                    && ((mPostListingURL!!.asSubredditPostListURL().type
                                    == SubredditPostListURL.Type.ALL) || (mPostListingURL!!.asSubredditPostListURL().type
                                    == SubredditPostListURL.Type.ALL_SUBTRACTION) || (mPostListingURL!!.asSubredditPostListURL().type
                                    == SubredditPostListURL.Type.POPULAR) || (mPostListingURL!!.asSubredditPostListURL().type
                                    == SubredditPostListURL.Type.FRONTPAGE))

                        // Grab this so we don't have to pull from the prefs every post
                        val blockedSubreddits =                             HashSet<SubredditCanonicalId?>(PrefsUtility.pref_blocked_subreddits())

                        Log.i(
                            TAG, "Inline previews: "
                                    + (if (inlinePreviews) "ON" else "OFF")
                        )

                        Log.i(
                            TAG, "Precaching images: "
                                    + (if (precacheImages) "ON" else "OFF")
                        )

                        Log.i(
                            TAG, "Precaching comments: "
                                    + (if (precacheComments) "ON" else "OFF")
                        )

                        val cm: CacheManager? = CacheManager.Companion.getInstance(activity)

                        val showSubredditName =                             !(mPostListingURL != null && (mPostListingURL!!.pathType()
                                    == RedditURLParser.SUBREDDIT_POST_LISTING_URL) && (mPostListingURL!!.asSubredditPostListURL().type
                                    == SubredditPostListURL.Type.SUBREDDIT))

                        val downloadedPosts = ArrayList<RedditPostListItem?>(25)

                        for (postThingValue in posts) {
                            if (postThingValue !is MaybeParseError.Ok<*>) {
                                // TODO handle this
                                continue
                            }

                            val postThing = (postThingValue as MaybeParseError.Ok<RedditThing>)
                                .value

                            if (postThing !is Post) {
                                continue
                            }

                            val post = postThing.data

                            mAfter = post.name

                            val isPostBlocked = subredditFilteringEnabled
                                    && blockedSubreddits.contains(
                                SubredditCanonicalId(post.subreddit.decoded)
                            )

                            if (!isPostBlocked && (!post.over_18 || isNsfwAllowed)
                                && mPostIds.add(post.idAlone)
                            ) {
                                val downloadThisThumbnail = downloadThumbnails
                                        && (!post.over_18 || showNsfwThumbnails)
                                        && (!post.spoiler || showSpoilerThumbnails)

                                val downloadThisPreview = inlinePreviews
                                        && (!post.over_18 || showNsfwPreviews)
                                        && (!post.spoiler || showSpoilerPreviews)

                                val positionInList = mPostCount

                                val parsedPost = RedditParsedPost(
                                    activity,
                                    post,
                                    false
                                )

                                val preparedPost = RedditPreparedPost(
                                    activity,
                                    cm,
                                    positionInList,
                                    parsedPost,
                                    timestamp,
                                    showSubredditName,
                                    downloadThisThumbnail,
                                    allowHighResThumbnails,
                                    downloadThisPreview
                                )

                                // Skip adding this post (go to next iteration) if it
                                // has been clicked on AND read posts should be hidden
                                if (hideReadPosts && preparedPost.isRead) {
                                    mPostsNotShown = true
                                    continue
                                }

                                if (precacheComments) {
                                    precacheComments(activity, preparedPost, positionInList)
                                }

                                getImageInfo(
                                    activity,
                                    parsedPost.url,
                                    Priority(
                                        Constants.Priority.IMAGE_PRECACHE,
                                        positionInList
                                    ),
                                    object : GetImageInfoListener {
                                        override fun onFailure(
                                            error: RRError
                                        ) {
                                        }

                                        override fun onNotAnImage() {
                                        }

                                        override fun onSuccess(info: ImageInfo) {
                                            if (!precacheImages) {
                                                return
                                            }

                                            precacheImage(
                                                activity,
                                                info,
                                                positionInList,
                                                gifViewMode,
                                                imageViewMode,
                                                videoViewMode
                                            )
                                        }
                                    })

                                downloadedPosts.add(
                                    RedditPostListItem(
                                        preparedPost,
                                        this@PostListingFragment,
                                        activity,
                                        leftHandedMode
                                    )
                                )

                                mPostCount++
                                mPostRefreshCount.decrementAndGet()
                            } else {
                                mPostsNotShown = true
                            }
                        }

                        runOnUiThread(Runnable {
                            mPostListingManager.addPosts(downloadedPosts)
                            mPostListingManager.setLoadingVisible(false)

                            if (mPostCount == 0
                                && (mAfter == null || mAfter == mLastAfter)
                            ) {
                                @StringRes val emptyViewText: Int

                                if (mPostsNotShown) {
                                    if (mPostListingURL!!.pathType()
                                        == RedditURLParser.SEARCH_POST_LISTING_URL
                                    ) {
                                        emptyViewText = string.no_search_results_hidden
                                    } else {
                                        emptyViewText = string.no_posts_yet_hidden
                                    }
                                } else {
                                    if (mPostListingURL!!.pathType()
                                        == RedditURLParser.SEARCH_POST_LISTING_URL
                                    ) {
                                        emptyViewText = string.no_search_results
                                    } else {
                                        emptyViewText = string.no_posts_yet
                                    }
                                }

                                val emptyView =                                     LayoutInflater.from(context).inflate(
                                        R.layout.no_items_yet,
                                        mRecyclerView,
                                        false
                                    )

                                (emptyView.findViewById<View?>(R.id.empty_view_text) as TextView)
                                    .setText(emptyViewText)

                                mPostListingManager.addViewToItems(emptyView)
                            }

                            onPostsAdded()

                            mRequest = null
                            mReadyToDownloadMore = true
                            onLoadMoreItemsCheck()
                        })
                    } catch (t: Throwable) {
                        onFailure(
                            getGeneralErrorForFailure(
                                activity,
                                RequestFailureType.PARSE,
                                t,
                                null,
                                url,
                                FailedRequestBody.Companion.from(streamFactory)
                            )
                        )
                    }
                }

                override fun onFailure(error: RRError) {
                    AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                        mPostListingManager.setLoadingVisible(false)
                        mPostListingManager.addFooterError(
                            ErrorView(
                                activity,
                                error
                            )
                        )
                    })
                }
            })
    }

    private fun precacheComments(
        activity: Activity,
        preparedPost: RedditPreparedPost,
        positionInList: Int
    ) {
        val controller = CommentListingController(
            PostCommentListingURL.Companion.forPostId(preparedPost.src.idAlone)
        )

        val url = from(controller.uri)

        CacheManager.Companion.getInstance(activity)
            .makeRequest(
                CacheRequest(
                    url,
                    RedditAccountManager.Companion.getInstance(activity).getDefaultAccount(),
                    null,
                    Priority(
                        Constants.Priority.COMMENT_PRECACHE,
                        positionInList
                    ),
                    DownloadStrategyIfTimestampOutsideBounds(
                        TimestampBound.Companion.notOlderThan(minutes(15))
                    ),
                    Constants.FileType.COMMENT_LIST,
                    DownloadQueueType.REDDIT_API,  // Don't parse the JSON
                    activity,
                    object : CacheRequestCallbacks {
                        override fun onFailure(error: RRError) {
                            if (isSensitiveDebugLoggingEnabled) {
                                Log.e(
                                    TAG,
                                    ("Failed to precache "
                                            + url
                                            + " ("
                                            + error
                                            + ")")
                                )
                            }
                        }

                        override fun onCacheFileWritten(
                            cacheFile: ReadableCacheFile,
                            timestamp: TimestampUTC?,
                            session: UUID,
                            fromCache: Boolean,
                            mimetype: String?
                        ) {
                            // Successfully precached
                        }
                    })
            )
    }

    private fun precacheImage(
        activity: Activity,
        info: ImageInfo,
        positionInList: Int,
        gifViewMode: GifViewMode,
        imageViewMode: ImageViewMode,
        videoViewMode: VideoViewMode
    ) {
        // Don't precache huge images

        if (info.original.sizeBytes != null
            && info.original.sizeBytes > 15 * 1024 * 1024
        ) {
            if (isSensitiveDebugLoggingEnabled) {
                Log.i(
                    TAG, String.format(
                        "Not precaching '%s': too big (%d kB)",
                        info.original.url,
                        info.original.sizeBytes / 1024
                    )
                )
            }
            return
        }

        // Don't precache gifs if they're opened externally
        if (ImageInfo.MediaType.GIF == info.mediaType
            && !gifViewMode.downloadInApp
        ) {
            if (isSensitiveDebugLoggingEnabled) {
                Log.i(
                    TAG, String.format(
                        "Not precaching '%s': GIFs opened externally",
                        info.original.url
                    )
                )
            }
            return
        }

        // Don't precache images if they're opened externally
        if (ImageInfo.MediaType.IMAGE == info.mediaType
            && !imageViewMode.downloadInApp
        ) {
            if (isSensitiveDebugLoggingEnabled) {
                Log.i(
                    TAG, String.format(
                        "Not precaching '%s': images opened externally",
                        info.original.url
                    )
                )
            }
            return
        }


        // Don't precache videos if they're opened externally
        if (ImageInfo.MediaType.VIDEO == info.mediaType
            && !videoViewMode.downloadInApp
        ) {
            if (isSensitiveDebugLoggingEnabled) {
                Log.i(
                    TAG, String.format(
                        "Not precaching '%s': videos opened externally",
                        info.original.url
                    )
                )
            }
            return
        }

        precacheImage(
            activity,
            info.original.url,
            positionInList
        )

        if (info.urlAudioStream != null) {
            precacheImage(
                activity,
                info.urlAudioStream,
                positionInList
            )
        }
    }

    private fun precacheImage(
        activity: Activity,
        url: UriString,
        positionInList: Int
    ) {
        CacheManager.Companion.getInstance(activity).makeRequest(
            CacheRequest(
                url,
                RedditAccountManager.Companion.getAnon(),
                null,
                Priority(
                    Constants.Priority.IMAGE_PRECACHE,
                    positionInList
                ),
                DownloadStrategyIfNotCached.Companion.INSTANCE,
                Constants.FileType.IMAGE,
                DownloadQueueType.IMAGE_PRECACHE,
                activity,
                object : CacheRequestCallbacks {
                    override fun onFailure(error: RRError) {
                        if (isSensitiveDebugLoggingEnabled) {
                            Log.e(
                                TAG, String.format(
                                    Locale.US,
                                    "Failed to precache %s (%s)",
                                    url,
                                    error
                                )
                            )
                        }
                    }

                    override fun onCacheFileWritten(
                        cacheFile: ReadableCacheFile,
                        timestamp: TimestampUTC?,
                        session: UUID,
                        fromCache: Boolean,
                        mimetype: String?
                    ) {
                        // Successfully precached
                    }
                })
        )
    }

    companion object {
        private const val TAG = "PostListingFragment"

        private const val SAVEDSTATE_FIRST_VISIBLE_POS = "firstVisiblePosition"
    }
}
