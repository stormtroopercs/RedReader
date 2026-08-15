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

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.OptionsMenuUtility.OptionsMenuSubredditsListener
import org.quantumbadger.redreader.adapters.MainMenuListingManager
import org.quantumbadger.redreader.adapters.MainMenuSelectionListener
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.TimestampBound
import org.quantumbadger.redreader.common.TimestampBound.MoreRecentThanBound
import org.quantumbadger.redreader.common.time.TimeDuration.Companion.hours
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.io.RequestResponseHandler
import org.quantumbadger.redreader.reddit.api.RedditMultiredditSubscriptionManager
import org.quantumbadger.redreader.reddit.api.RedditMultiredditSubscriptionManager.MultiredditListChangeListener
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager.SubredditSubscriptionStateChangeListener
import org.quantumbadger.redreader.reddit.things.SubredditCanonicalId
import org.quantumbadger.redreader.reddit.url.PostListingURL
import org.quantumbadger.redreader.views.ScrollbarRecyclerViewManager
import org.quantumbadger.redreader.views.liststatus.ErrorView
import java.lang.annotation.Retention

class MainMenuFragment(
    parent: AppCompatActivity,
    savedInstanceState: Bundle?,
    force: Boolean
) : RRFragment(parent, savedInstanceState), MainMenuSelectionListener,
    SubredditSubscriptionStateChangeListener, MultiredditListChangeListener {
    @IntDef(
        [MENU_MENU_ACTION_FRONTPAGE, MENU_MENU_ACTION_PROFILE, MENU_MENU_ACTION_INBOX, MENU_MENU_ACTION_SUBMITTED, MENU_MENU_ACTION_SUBMITTED_COMMENTS, MENU_MENU_ACTION_UPVOTED, MENU_MENU_ACTION_DOWNVOTED, MENU_MENU_ACTION_SAVED, MENU_MENU_ACTION_MODMAIL, MENU_MENU_ACTION_HIDDEN, MENU_MENU_ACTION_CUSTOM, MENU_MENU_ACTION_ALL, MENU_MENU_ACTION_POPULAR, MENU_MENU_ACTION_SENT_MESSAGES, MENU_MENU_ACTION_FIND_SUBREDDIT]
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class MainMenuAction

    private val mManager: MainMenuListingManager

    private val mOuter: View?

    init {
        val context: Context = getActivity()

        val user: RedditAccount = RedditAccountManager.Companion.getInstance(context)
            .getDefaultAccount()

        val recyclerViewManager = ScrollbarRecyclerViewManager(parent, null, false)

        mOuter = recyclerViewManager.getOuterView()
        val recyclerView = recyclerViewManager.getRecyclerView()

        if (parent is OptionsMenuSubredditsListener
            && PrefsUtility.pref_behaviour_enable_swipe_refresh()
        ) {
            recyclerViewManager.enablePullToRefresh(
                OnRefreshListener {
                    (parent as OptionsMenuSubredditsListener)
                        .onRefreshSubreddits()
                })
        }

        mManager = MainMenuListingManager(getActivity(), this, user)

        recyclerView.setAdapter(mManager.getAdapter())

        val paddingPx = dpToPixels(context, 8f)
        recyclerView.setPadding(paddingPx, 0, paddingPx, 0)
        recyclerView.setClipToPadding(false)

        run {
            val appearance =                 context.obtainStyledAttributes(intArrayOf(R.attr.rrListItemBackgroundCol))
            getActivity().getWindow().setBackgroundDrawable(
                ColorDrawable(appearance.getColor(0, General.COLOR_INVALID))
            )
            appearance.recycle()
        }

        val multiredditSubscriptionManager: RedditMultiredditSubscriptionManager=            RedditMultiredditSubscriptionManager.Companion.getSingleton(context, user)

        val subredditSubscriptionManager: RedditSubredditSubscriptionManager=            RedditSubredditSubscriptionManager.Companion.getSingleton(context, user)

        if (force) {
            multiredditSubscriptionManager.triggerUpdate(
                object : RequestResponseHandler<HashSet<String?>?, RRError?> {
                    override fun onRequestFailed(failureReason: RRError) {
                        onMultiredditError(failureReason)
                    }

                    override fun onRequestSuccess(
                        result: HashSet<String?>?,
                        timeCached: TimestampUTC?
                    ) {
                        multiredditSubscriptionManager.addListener(this@MainMenuFragment)
                        onMultiredditSubscriptionsChanged(result)
                    }
                }, TimestampBound.Companion.NONE
            )

            subredditSubscriptionManager.triggerUpdate(
                object : RequestResponseHandler<HashSet<SubredditCanonicalId?>?, RRError?> {
                    override fun onRequestFailed(failureReason: RRError) {
                        onSubredditError(failureReason)
                    }

                    override fun onRequestSuccess(
                        result: HashSet<SubredditCanonicalId?>?,
                        timeCached: TimestampUTC?
                    ) {
                        subredditSubscriptionManager.addListener(this@MainMenuFragment)
                        onSubredditSubscriptionsChanged(result)
                    }
                }, TimestampBound.Companion.NONE
            )
        } else {
            multiredditSubscriptionManager.addListener(this)
            subredditSubscriptionManager.addListener(this)

            if (multiredditSubscriptionManager.areSubscriptionsReady()) {
                onMultiredditSubscriptionsChanged(
                    multiredditSubscriptionManager.getSubscriptionList()
                )
            }

            if (subredditSubscriptionManager.areSubscriptionsReady()) {
                onSubredditSubscriptionsChanged(
                    subredditSubscriptionManager.getSubscriptionList()
                )
            }

            val oneHour: MoreRecentThanBound=TimestampBound.Companion.notOlderThan(hours(1))
            multiredditSubscriptionManager.triggerUpdate(null, oneHour)
            subredditSubscriptionManager.triggerUpdate(null, oneHour)
        }
    }

    enum class MainMenuUserItems {
        PROFILE, INBOX, SUBMITTED, SUBMITTED_COMMENTS, SAVED,
        HIDDEN, UPVOTED, DOWNVOTED, MODMAIL, SENT_MESSAGES
    }

    enum class MainMenuShortcutItems {
        FRONTPAGE, POPULAR, ALL, SUBREDDIT_SEARCH, CUSTOM
    }

    override fun getListingView(): View? {
        return mOuter
    }

    override fun onSaveInstanceState(): Bundle? {
        return null
    }

    fun onSubredditSubscriptionsChanged(
        subscriptions: MutableCollection<SubredditCanonicalId?>?
    ) {
        mManager.setSubreddits(subscriptions)
    }

    fun onMultiredditSubscriptionsChanged(subscriptions: MutableCollection<String?>?) {
        mManager.setMultireddits(subscriptions)
    }

    private fun onSubredditError(error: RRError) {
        mManager.setSubredditsError(ErrorView(getActivity(), error))
    }

    private fun onMultiredditError(error: RRError) {
        mManager.setMultiredditsError(ErrorView(getActivity(), error))
    }

    override fun onSelected(@MainMenuAction type: Int) {
        (getActivity() as MainMenuSelectionListener).onSelected(type)
    }

    override fun onSelected(postListingURL: PostListingURL?) {
        (getActivity() as MainMenuSelectionListener).onSelected(postListingURL)
    }

    override fun onSubredditSubscriptionListUpdated(
        subredditSubscriptionManager: RedditSubredditSubscriptionManager
    ) {
        onSubredditSubscriptionsChanged(subredditSubscriptionManager.getSubscriptionList())
    }

    override fun onMultiredditListUpdated(
        multiredditSubscriptionManager: RedditMultiredditSubscriptionManager
    ) {
        onMultiredditSubscriptionsChanged(multiredditSubscriptionManager.getSubscriptionList())
    }

    override fun onSubredditSubscriptionAttempted(
        subredditSubscriptionManager: RedditSubredditSubscriptionManager?
    ) {
    }

    override fun onSubredditUnsubscriptionAttempted(
        subredditSubscriptionManager: RedditSubredditSubscriptionManager?
    ) {
    }

    fun onUpdateAnnouncement() {
        mManager.onUpdateAnnouncement()
    }

    companion object {
        const val MENU_MENU_ACTION_FRONTPAGE: Int = 0
        const val MENU_MENU_ACTION_PROFILE: Int = 1
        const val MENU_MENU_ACTION_INBOX: Int = 2
        const val MENU_MENU_ACTION_SUBMITTED: Int = 3
        const val MENU_MENU_ACTION_SUBMITTED_COMMENTS: Int = 4
        const val MENU_MENU_ACTION_UPVOTED: Int = 5
        const val MENU_MENU_ACTION_DOWNVOTED: Int = 6
        const val MENU_MENU_ACTION_SAVED: Int = 7
        const val MENU_MENU_ACTION_MODMAIL: Int = 8
        const val MENU_MENU_ACTION_HIDDEN: Int = 9
        const val MENU_MENU_ACTION_CUSTOM: Int = 10
        const val MENU_MENU_ACTION_ALL: Int = 11
        const val MENU_MENU_ACTION_POPULAR: Int = 12
        const val MENU_MENU_ACTION_SENT_MESSAGES: Int = 13
        const val MENU_MENU_ACTION_FIND_SUBREDDIT: Int = 14
    }
}
