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
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.RedReader.Companion.getInstance
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountChangeListener
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.BugReportActivity.Companion.handleGlobalError
import org.quantumbadger.redreader.activities.OptionsMenuUtility.OptionsMenuPostsListener
import org.quantumbadger.redreader.activities.SessionChangeListener.SessionChangeType
import org.quantumbadger.redreader.common.DialogUtils
import org.quantumbadger.redreader.common.DialogUtils.OnSearchListener
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.fragments.PostListingFragment
import org.quantumbadger.redreader.fragments.SessionListDialog
import org.quantumbadger.redreader.listingcontrollers.PostListingController
import org.quantumbadger.redreader.reddit.PostSort
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager.ListenerContext
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager.SubredditSubscriptionStateChangeListener
import org.quantumbadger.redreader.reddit.api.SubredditSubscriptionState
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost
import org.quantumbadger.redreader.reddit.things.InvalidSubredditNameException
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL
import org.quantumbadger.redreader.reddit.url.PostListingURL
import org.quantumbadger.redreader.reddit.url.RedditURLParser
import org.quantumbadger.redreader.reddit.url.SearchPostListURL
import org.quantumbadger.redreader.views.RedditPostView.PostSelectionListener
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference


class PostListingActivity : RefreshableActivity(), RedditAccountChangeListener,
    PostSelectionListener, OptionsMenuPostsListener, SessionChangeListener,
    SubredditSubscriptionStateChangeListener {
    private var fragment: PostListingFragment? = null
    private var controller: PostListingController? = null

    private val mSubredditSubscriptionListenerContext = AtomicReference<ListenerContext?>(null)

    private var mDoubleTapBack_lastTapMs: Long = -1

    public override fun onCreate(savedInstanceState: Bundle?) {
        PrefsUtility.applyTheme(this)

        super.onCreate(savedInstanceState)

        val typedArray = obtainStyledAttributes(intArrayOf(R.attr.rrListBackgroundCol))

        try {
            getWindow().setBackgroundDrawable(
                ColorDrawable(typedArray.getColor(0, 0))
            )
        } finally {
            typedArray.recycle()
        }

        RedditAccountManager.Companion.getInstance(this).addUpdateListener(this)

        if (getIntent() != null) {
            val intent = getIntent()

            val url = RedditURLParser.parseProbablePostListing(intent.getData())

            if (url !is PostListingURL) {
                throw RuntimeException(
                    String.format(
                        Locale.US,
                        "'%s' is not a post listing URL!",
                        url.generateJsonUri()
                    )
                )
            }

            controller = PostListingController(url, this)

            var fragmentSavedInstanceState: Bundle? = null

            if (savedInstanceState != null) {
                if (savedInstanceState.containsKey(SAVEDSTATE_SESSION)) {
                    controller!!.setSession(
                        UUID.fromString(
                            savedInstanceState.getString(
                                SAVEDSTATE_SESSION
                            )
                        )
                    )
                }

                if (savedInstanceState.containsKey(SAVEDSTATE_SORT)) {
                    controller!!.setSort(
                        PostSort.valueOf(
                            savedInstanceState.getString(SAVEDSTATE_SORT)!!
                        )
                    )
                }

                if (savedInstanceState.containsKey(SAVEDSTATE_FRAGMENT)) {
                    fragmentSavedInstanceState = savedInstanceState.getBundle(
                        SAVEDSTATE_FRAGMENT
                    )
                }
            }

            setTitle(url.humanReadableName(this, false))

            setBaseActivityListing(R.layout.main_single)
            doRefresh(RefreshableFragment.POSTS, false, fragmentSavedInstanceState)
        } else {
            throw RuntimeException("Nothing to show!")
        }

        recreateSubscriptionListener()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val session = controller!!.getSession()
        if (session != null) {
            outState.putString(SAVEDSTATE_SESSION, session.toString())
        }

        val sort = controller!!.getSort()
        if (sort != null) {
            outState.putString(SAVEDSTATE_SORT, sort.name)
        }

        if (fragment != null) {
            outState.putBundle(SAVEDSTATE_FRAGMENT, fragment!!.onSaveInstanceState())
        }
    }

    protected override fun onDestroy() {
        super.onDestroy()

        val listenerContext = mSubredditSubscriptionListenerContext.get()

        if (listenerContext != null) {
            listenerContext.removeListener()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val user: RedditAccount = RedditAccountManager.Companion.getInstance(this)
            .getDefaultAccount()
        val subredditSubscriptionState: SubredditSubscriptionState?

        val subredditSubscriptionManager
                : RedditSubredditSubscriptionManager =
            RedditSubredditSubscriptionManager.Companion.getSingleton(this, user)

        if (!user.isAnonymous && controller!!.isSubreddit()
            && subredditSubscriptionManager.areSubscriptionsReady()
            && fragment != null && fragment!!.getSubreddit() != null
        ) {
            subredditSubscriptionState = subredditSubscriptionManager.getSubscriptionState(
                controller!!.subredditCanonicalName()
            )
        } else {
            subredditSubscriptionState = null
        }

        val subredditDescription = if (fragment != null
            && fragment!!.getSubreddit() != null
        )
            fragment!!.getSubreddit()!!.description_html
        else
            null

        var subredditPinState: Boolean? = null
        var subredditBlockedState: Boolean? = null

        if (controller!!.isSubreddit()
            && fragment != null && fragment!!.getSubreddit() != null
        ) {
            try {
                subredditPinState = PrefsUtility.pref_pinned_subreddits_check(
                    fragment!!.getSubreddit()!!.getCanonicalId()
                )

                subredditBlockedState = PrefsUtility.pref_blocked_subreddits_check(
                    fragment!!.getSubreddit()!!.getCanonicalId()
                )
            } catch (e: InvalidSubredditNameException) {
                subredditPinState = null
                subredditBlockedState = null
            }
        }

        OptionsMenuUtility.prepare<PostListingActivity?>(
            this,
            menu,
            false,
            true,
            false,
            controller!!.isSearchResults(),
            controller!!.isUserPostListing(),
            false,
            controller!!.isSortable(),
            true,
            controller!!.isFrontPage(),
            subredditSubscriptionState,
            subredditDescription != null && !subredditDescription.isEmpty(),
            false,
            subredditPinState,
            subredditBlockedState
        )

        return true
    }

    private fun recreateSubscriptionListener() {
        val oldContext = mSubredditSubscriptionListenerContext.getAndSet(
            RedditSubredditSubscriptionManager.Companion.getSingleton(
                this,
                RedditAccountManager.Companion.getInstance(this)
                    .getDefaultAccount()
            )
                .addListener(this)
        )

        if (oldContext != null) {
            oldContext.removeListener()
        }
    }

    override fun onRedditAccountChanged() {
        recreateSubscriptionListener()
        postInvalidateOptionsMenu()
        requestRefresh(RefreshableFragment.ALL, false)
    }

    override fun doRefresh(
        which: RefreshableFragment?,
        force: Boolean,
        savedInstanceState: Bundle?
    ) {
        if (fragment != null) {
            fragment!!.cancel()
        }

        fragment = controller!!.get(this, force, savedInstanceState)
        fragment!!.setBaseActivityContent(this)
    }

    override fun onPostSelected(post: RedditPreparedPost) {
        onLinkClicked(this, post.src.url, false, post.src.src)
    }

    override fun onPostCommentsSelected(post: RedditPreparedPost) {
        onLinkClicked(
            this,
            PostCommentListingURL.Companion.forPostId(post.src.getIdAlone()).toUriString(),
            false
        )
    }

    override fun onRefreshPosts() {
        controller!!.setSession(null)
        requestRefresh(RefreshableFragment.POSTS, true)
    }

    override fun onPastPosts() {
        val sessionListDialog: SessionListDialog = SessionListDialog.Companion.newInstance(
            controller!!.getUri(),
            controller!!.getSession(),
            SessionChangeType.POSTS
        )
        sessionListDialog.show(getSupportFragmentManager(), "SessionListDialog")
    }

    override fun onSubmitPost() {
        val intent = Intent(this, PostSubmitActivity::class.java)

        if (controller!!.isSubreddit()) {
            intent.putExtra("subreddit", controller!!.subredditCanonicalName().toString())
        }

        startActivity(intent)
    }

    override fun onSortSelected(order: PostSort?) {
        controller!!.setSort(order)
        requestRefresh(RefreshableFragment.POSTS, false)
        invalidateOptionsMenu()
    }

    override fun onSearchPosts() {
        onSearchPosts(controller, this)
    }

    override fun onSubscribe() {
        fragment!!.onSubscribe()
    }

    override fun onUnsubscribe() {
        fragment!!.onUnsubscribe()
    }

    override fun onSidebar() {
        if (fragment!!.getSubreddit() != null) {
            val intent = Intent(this, HtmlViewActivity::class.java)
            intent.putExtra(
                "html",
                fragment!!.getSubreddit()!!
                    .getSidebarHtml(PrefsUtility.isNightMode())
            )
            intent.putExtra(
                "title", String.format(
                    Locale.US, "%s: %s",
                    getString(string.sidebar_activity_title),
                    fragment!!.getSubreddit()!!.url
                )
            )
            startActivityForResult(intent, 1)
        }
    }

    override fun onPin() {
        if (fragment == null) {
            return
        }

        if (fragment!!.getSubreddit() == null) {
            handleGlobalError(
                this,
                RuntimeException(
                    "Can't pin post listing "
                            + fragment!!.getPostListingURL()
                )
            )
            return
        }

        try {
            PrefsUtility.pref_pinned_subreddits_add(
                this,
                fragment!!.getSubreddit()!!.getCanonicalId()
            )
        } catch (e: InvalidSubredditNameException) {
            throw RuntimeException(e)
        }

        invalidateOptionsMenu()
    }

    override fun onUnpin() {
        if (fragment == null) {
            return
        }

        if (fragment!!.getSubreddit() == null) {
            handleGlobalError(
                this,
                RuntimeException(
                    "Can't unpin post listing "
                            + fragment!!.getPostListingURL()
                )
            )
            return
        }

        try {
            PrefsUtility.pref_pinned_subreddits_remove(
                this,
                fragment!!.getSubreddit()!!.getCanonicalId()
            )
        } catch (e: InvalidSubredditNameException) {
            throw RuntimeException(e)
        }

        invalidateOptionsMenu()
    }

    override fun onBlock() {
        if (fragment == null) {
            return
        }

        if (fragment!!.getSubreddit() == null) {
            handleGlobalError(
                this,
                RuntimeException(
                    "Can't block post listing "
                            + fragment!!.getPostListingURL()
                )
            )
            return
        }

        try {
            PrefsUtility.pref_blocked_subreddits_add(
                this,
                fragment!!.getSubreddit()!!.getCanonicalId()
            )
        } catch (e: InvalidSubredditNameException) {
            throw RuntimeException(e)
        }

        invalidateOptionsMenu()
    }

    override fun onUnblock() {
        if (fragment == null) {
            return
        }

        if (fragment!!.getSubreddit() == null) {
            handleGlobalError(
                this,
                RuntimeException(
                    "Can't unblock post listing "
                            + fragment!!.getPostListingURL()
                )
            )
            return
        }

        try {
            PrefsUtility.pref_blocked_subreddits_remove(
                this,
                fragment!!.getSubreddit()!!.getCanonicalId()
            )
        } catch (e: InvalidSubredditNameException) {
            throw RuntimeException(e)
        }

        invalidateOptionsMenu()
    }

    override fun onSessionSelected(session: UUID?, type: SessionChangeType?) {
        controller!!.setSession(session)
        requestRefresh(RefreshableFragment.POSTS, false)
    }

    override fun onSessionRefreshSelected(type: SessionChangeType?) {
        onRefreshPosts()
    }

    override fun onSessionChanged(
        session: UUID?,
        type: SessionChangeType?,
        timestamp: TimestampUTC?
    ) {
        controller!!.setSession(session)
    }

    override fun baseActivityMustInterceptBack(): Boolean {
        return PrefsUtility.pref_behaviour_back_again()
    }

    override fun baseActivityOnBackPressed(): Boolean {
        if (PrefsUtility.pref_behaviour_back_again()
            && (mDoubleTapBack_lastTapMs < SystemClock.uptimeMillis() - 5000)
        ) {
            mDoubleTapBack_lastTapMs = SystemClock.uptimeMillis()
            Toast.makeText(this, string.press_back_again, Toast.LENGTH_SHORT).show()
            return true
        }

        return false
    }

    override fun onSubredditSubscriptionListUpdated(
        subredditSubscriptionManager: RedditSubredditSubscriptionManager?
    ) {
        postInvalidateOptionsMenu()
    }

    override fun onSubredditSubscriptionAttempted(
        subredditSubscriptionManager: RedditSubredditSubscriptionManager?
    ) {
        postInvalidateOptionsMenu()
    }

    override fun onSubredditUnsubscriptionAttempted(
        subredditSubscriptionManager: RedditSubredditSubscriptionManager?
    ) {
        postInvalidateOptionsMenu()
    }

    private fun postInvalidateOptionsMenu() {
        runOnUiThread(Runnable { this.invalidateOptionsMenu() })
    }

    override fun baseActivityAllowToolbarHideOnScroll(): Boolean {
        return true
    }

    override fun getPostSort(): PostSort? {
        return controller!!.getSort()
    }

    companion object {
        private const val SAVEDSTATE_SESSION = "pla_session"
        private const val SAVEDSTATE_SORT = "pla_sort"
        private const val SAVEDSTATE_FRAGMENT = "pla_fragment"

        fun onSearchPosts(
            controller: PostListingController?,
            activity: AppCompatActivity
        ) {
            DialogUtils.showSearchDialog(activity, OnSearchListener { query: String? ->
                if (query == null) {
                    return@showSearchDialog
                }
                val url: SearchPostListURL

                if (controller != null && (controller.isSubreddit()
                            || controller.isSubredditCombination()
                            || controller.isSubredditSearchResults())
                ) {
                    val subredditCanonicalId = controller.subredditCanonicalName()

                    if (subredditCanonicalId == null) {
                        handleGlobalError(
                            activity,
                            RuntimeException(
                                "Can't search post listing "
                                        + controller.getUri()
                            )
                        )
                        return@showSearchDialog
                    }

                    url = SearchPostListURL.Companion.build(
                        subredditCanonicalId.toString(),
                        query
                    )
                } else if (controller != null && controller.isMultireddit()) {
                    val multiName = controller.multiredditName()
                    val multiUsername = controller.multiredditUsername()

                    url = SearchPostListURL.Companion.build(multiUsername, multiName, query)
                } else {
                    url = SearchPostListURL.Companion.build(null, query)
                }

                val intent = Intent(activity, PostListingActivity::class.java)
                intent.setData(url.generateJsonUri())
                activity.startActivity(intent)
            })
        }
    }
}
