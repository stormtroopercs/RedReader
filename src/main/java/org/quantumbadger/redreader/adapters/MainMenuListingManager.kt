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
package org.quantumbadger.redreader.adapters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.view.View.OnLongClickListener
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.Constants.Reddit
import org.quantumbadger.redreader.common.General.checkThisIsUIThread
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.common.General.getSharedPrefs
import org.quantumbadger.redreader.common.General.quickToast
import org.quantumbadger.redreader.common.General.setLayoutMatchWidthWrapHeight
import org.quantumbadger.redreader.common.LinkHandler.getPreferredRedditUriString
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.common.LinkHandler.shareText
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.PrefsUtility.BlockedSubredditSort
import org.quantumbadger.redreader.common.PrefsUtility.PinnedSubredditSort
import org.quantumbadger.redreader.common.ScreenreaderPronunciation
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.fragments.MainMenuFragment
import org.quantumbadger.redreader.fragments.MainMenuFragment.MainMenuAction
import org.quantumbadger.redreader.fragments.MainMenuFragment.MainMenuShortcutItems
import org.quantumbadger.redreader.fragments.MainMenuFragment.MainMenuUserItems
import org.quantumbadger.redreader.receivers.announcements.AnnouncementDownloader
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager
import org.quantumbadger.redreader.reddit.api.SubredditSubscriptionState
import org.quantumbadger.redreader.reddit.things.SubredditCanonicalId
import org.quantumbadger.redreader.reddit.url.MultiredditPostListURL
import org.quantumbadger.redreader.reddit.url.PostListingURL
import org.quantumbadger.redreader.reddit.url.SubredditPostListURL
import org.quantumbadger.redreader.views.AnnouncementView
import org.quantumbadger.redreader.views.LoadingSpinnerView
import org.quantumbadger.redreader.views.list.GroupedRecyclerViewItemListItemView
import org.quantumbadger.redreader.views.list.GroupedRecyclerViewItemListSectionHeaderView
import org.quantumbadger.redreader.views.liststatus.ErrorView
import java.util.Collections
import java.util.Objects
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.ArrayList
import kotlin.collections.MutableCollection
import kotlin.collections.indices
import kotlin.text.startsWith
import org.quantumbadger.redreader.common.General

class MainMenuListingManager(
    activity: AppCompatActivity,
    listener: MainMenuSelectionListener,
    user: RedditAccount
) {
    val adapter: GroupedRecyclerViewAdapter=GroupedRecyclerViewAdapter(13)
    private val mContext: Context
    private val mActivity: AppCompatActivity

    private val mListener: MainMenuSelectionListener

    private var mMultiredditHeaderItem: GroupedRecyclerViewAdapter.Item<*>? = null

    private var mSubredditSubscriptions: ArrayList<SubredditCanonicalId>? = null
    private var mMultiredditSubscriptions: ArrayList<String>? = null

    private val mAnnouncementHolder: FrameLayout

    enum class SubredditAction(val descriptionResId: Int) {
        SHARE(string.action_share),
        COPY_URL(string.action_copy_link),
        BLOCK(string.block_subreddit),
        UNBLOCK(string.unblock_subreddit),
        PIN(string.pin_subreddit),
        UNPIN(string.unpin_subreddit),
        SUBSCRIBE(string.options_subscribe),
        UNSUBSCRIBE(string.options_unsubscribe),
        EXTERNAL(string.action_external)
    }

    init {
        checkThisIsUIThread()

        mActivity = activity
        mContext = activity.getApplicationContext()
        mListener = listener

        mAnnouncementHolder = FrameLayout(mActivity)
        setLayoutMatchWidthWrapHeight(mAnnouncementHolder)

        val rrIconPerson: Drawable?
        val rrIconEnvOpen: Drawable?
        val rrIconSentMessages: Drawable?
        val rrIconSend: Drawable?
        val rrIconStarFilled: Drawable?
        val rrIconCross: Drawable?
        val rrIconUpvote: Drawable?
        val rrIconDownvote: Drawable?
        val rrIconAccountSearch: Drawable

        run {
            val attr = activity.obtainStyledAttributes(
                intArrayOf(
                    R.attr.rrIconPerson,
                    R.attr.rrIconEnvOpen,
                    R.attr.rrIconSentMessages,
                    R.attr.rrIconSend,
                    R.attr.rrIconStarFilled,
                    R.attr.rrIconCross,
                    R.attr.rrIconArrowUpBold,
                    R.attr.rrIconArrowDownBold,
                    R.attr.rrIconAccountSearch
                )
            )
            rrIconPerson = AppCompatResources.getDrawable(activity, attr.getResourceId(0, 0))
            rrIconEnvOpen = AppCompatResources.getDrawable(activity, attr.getResourceId(1, 0))
            rrIconSentMessages = AppCompatResources.getDrawable(activity, attr.getResourceId(2, 0))
            rrIconSend = AppCompatResources.getDrawable(activity, attr.getResourceId(3, 0))
            rrIconStarFilled = AppCompatResources.getDrawable(
                activity,
                attr.getResourceId(4, 0)
            )
            rrIconCross = AppCompatResources.getDrawable(activity, attr.getResourceId(5, 0))
            rrIconUpvote = AppCompatResources.getDrawable(activity, attr.getResourceId(6, 0))
            rrIconDownvote = AppCompatResources.getDrawable(
                activity,
                attr.getResourceId(7, 0)
            )
            rrIconAccountSearch = Objects.requireNonNull<Drawable>(
                AppCompatResources.getDrawable(
                    activity,
                    attr.getResourceId(8, 0)
                )
            )
            attr.recycle()
        }

        run {
            val mainMenuShortcutItems = PrefsUtility.pref_menus_mainmenu_shortcutitems()
            if (mainMenuShortcutItems.contains(MainMenuShortcutItems.FRONTPAGE)) {
                adapter.appendToGroup(
                    GROUP_MAIN_ITEMS,
                    makeItem(
                        if (user.isAnonymous)
                            string.mainmenu_frontpage
                        else
                            string.mainmenu_subscribed_posts,
                        MainMenuFragment.Companion.MENU_MENU_ACTION_FRONTPAGE,
                        null,
                        true
                    )
                )
            }

            if (mainMenuShortcutItems.contains(MainMenuShortcutItems.POPULAR)) {
                adapter.appendToGroup(
                    GROUP_MAIN_ITEMS,
                    makeItem(
                        string.mainmenu_popular,
                        MainMenuFragment.Companion.MENU_MENU_ACTION_POPULAR,
                        null,
                        false
                    )
                )
            }

            if (mainMenuShortcutItems.contains(MainMenuShortcutItems.ALL)) {
                adapter.appendToGroup(
                    GROUP_MAIN_ITEMS,
                    makeItem(
                        string.mainmenu_all,
                        MainMenuFragment.Companion.MENU_MENU_ACTION_ALL,
                        null,
                        false
                    )
                )
            }
            if (mainMenuShortcutItems.contains(
                    MainMenuShortcutItems.SUBREDDIT_SEARCH
                )
            ) {
                if (mainMenuShortcutItems.contains(
                        MainMenuShortcutItems.CUSTOM
                    )
                ) {
                    val clickListener = View.OnClickListener { view: View? ->
                        mListener.onSelected(
                            MainMenuFragment.Companion.MENU_MENU_ACTION_FIND_SUBREDDIT
                        )
                    }

                    val item = GroupedRecyclerViewItemListItemView(
                        null,
                        activity.getString(string.find_location),
                        null,
                        false,
                        clickListener,
                        null,
                        Optional.Companion.of<Drawable>(rrIconAccountSearch),
                        Optional.Companion.of<View.OnClickListener>(View.OnClickListener { view: View? ->
                            mListener.onSelected(
                                MainMenuFragment.Companion.MENU_MENU_ACTION_CUSTOM
                            )
                        }),
                        Optional.Companion.of<String>(
                            activity.getString(
                                string.mainmenu_custom_destination
                            )
                        )
                    )

                    adapter.appendToGroup(GROUP_MAIN_ITEMS, item)
                } else {
                    adapter.appendToGroup(
                        GROUP_MAIN_ITEMS,
                        makeItem(
                            string.find_location,
                            MainMenuFragment.Companion.MENU_MENU_ACTION_FIND_SUBREDDIT,
                            null,
                            false
                        )
                    )
                }
            } else if (mainMenuShortcutItems.contains(
                    MainMenuShortcutItems.CUSTOM
                )
            ) {
                adapter.appendToGroup(
                    GROUP_MAIN_ITEMS,
                    makeItem(
                        string.mainmenu_custom_destination,
                        MainMenuFragment.Companion.MENU_MENU_ACTION_CUSTOM,
                        null,
                        false
                    )
                )
            }
        }

        if (PrefsUtility.pref_menus_mainmenu_dev_announcements()) {
            adapter.appendToGroup(
                GROUP_ANNOUNCEMENTS,
                GroupedRecyclerViewItemFrameLayout(mAnnouncementHolder)
            )

            onUpdateAnnouncement()
        }

        if (!user.isAnonymous) {
            val mainMenuUserItems = PrefsUtility.pref_menus_mainmenu_useritems()

            if (!mainMenuUserItems.isEmpty()) {
                if (PrefsUtility.pref_appearance_hide_username_main_menu()) {
                    adapter.appendToGroup(
                        GROUP_USER_HEADER,
                        GroupedRecyclerViewItemListSectionHeaderView(
                            activity.getString(string.mainmenu_useritems)
                        )
                    )
                } else {
                    adapter.appendToGroup(
                        GROUP_USER_HEADER,
                        GroupedRecyclerViewItemListSectionHeaderView(user.username)
                    )
                }

                val isFirst = AtomicBoolean(true)

                if (mainMenuUserItems.contains(MainMenuUserItems.PROFILE)) {
                    adapter.appendToGroup(
                        GROUP_USER_ITEMS,
                        makeItem(
                            string.mainmenu_profile,
                            MainMenuFragment.Companion.MENU_MENU_ACTION_PROFILE,
                            rrIconPerson,
                            isFirst.getAndSet(false)
                        )
                    )
                }

                if (mainMenuUserItems.contains(MainMenuUserItems.INBOX)) {
                    adapter.appendToGroup(
                        GROUP_USER_ITEMS,
                        makeItem(
                            string.mainmenu_inbox,
                            MainMenuFragment.Companion.MENU_MENU_ACTION_INBOX,
                            rrIconEnvOpen,
                            isFirst.getAndSet(false)
                        )
                    )
                }

                if (mainMenuUserItems.contains(MainMenuUserItems.SENT_MESSAGES)) {
                    adapter.appendToGroup(
                        GROUP_USER_ITEMS,
                        makeItem(
                            string.mainmenu_sent_messages,
                            MainMenuFragment.Companion.MENU_MENU_ACTION_SENT_MESSAGES,
                            rrIconSentMessages,
                            isFirst.getAndSet(false)
                        )
                    )
                }

                if (mainMenuUserItems.contains(MainMenuUserItems.SUBMITTED)) {
                    adapter.appendToGroup(
                        GROUP_USER_ITEMS,
                        makeItem(
                            string.mainmenu_submitted,
                            MainMenuFragment.Companion.MENU_MENU_ACTION_SUBMITTED,
                            rrIconSend,
                            isFirst.getAndSet(false)
                        )
                    )
                }

                if (mainMenuUserItems.contains(
                        MainMenuUserItems.SUBMITTED_COMMENTS
                    )
                ) {
                    adapter.appendToGroup(
                        GROUP_USER_ITEMS,
                        makeItem(
                            string.mainmenu_submitted_comments,
                            MainMenuFragment.Companion.MENU_MENU_ACTION_SUBMITTED_COMMENTS,
                            rrIconSend,
                            isFirst.getAndSet(false)
                        )
                    )
                }

                if (mainMenuUserItems.contains(MainMenuUserItems.SAVED)) {
                    adapter.appendToGroup(
                        GROUP_USER_ITEMS,
                        makeItem(
                            string.mainmenu_saved,
                            MainMenuFragment.Companion.MENU_MENU_ACTION_SAVED,
                            rrIconStarFilled,
                            isFirst.getAndSet(false)
                        )
                    )
                }

                if (mainMenuUserItems.contains(MainMenuUserItems.HIDDEN)) {
                    adapter.appendToGroup(
                        GROUP_USER_ITEMS,
                        makeItem(
                            string.mainmenu_hidden,
                            MainMenuFragment.Companion.MENU_MENU_ACTION_HIDDEN,
                            rrIconCross,
                            isFirst.getAndSet(false)
                        )
                    )
                }

                if (mainMenuUserItems.contains(MainMenuUserItems.UPVOTED)) {
                    adapter.appendToGroup(
                        GROUP_USER_ITEMS,
                        makeItem(
                            string.mainmenu_upvoted,
                            MainMenuFragment.Companion.MENU_MENU_ACTION_UPVOTED,
                            rrIconUpvote,
                            isFirst.getAndSet(false)
                        )
                    )
                }

                if (mainMenuUserItems.contains(MainMenuUserItems.DOWNVOTED)) {
                    adapter.appendToGroup(
                        GROUP_USER_ITEMS,
                        makeItem(
                            string.mainmenu_downvoted,
                            MainMenuFragment.Companion.MENU_MENU_ACTION_DOWNVOTED,
                            rrIconDownvote,
                            isFirst.getAndSet(false)
                        )
                    )
                }

                if (mainMenuUserItems.contains(MainMenuUserItems.MODMAIL)) {
                    adapter.appendToGroup(
                        GROUP_USER_ITEMS,
                        makeItem(
                            string.mainmenu_modmail,
                            MainMenuFragment.Companion.MENU_MENU_ACTION_MODMAIL,
                            rrIconEnvOpen,
                            isFirst.getAndSet(false)
                        )
                    )
                }
            }
        }

        setPinnedSubreddits()

        if (PrefsUtility.pref_appearance_show_blocked_subreddits_main_menu()) {
            setBlockedSubreddits()
        }

        if (!user.isAnonymous) {
            if (PrefsUtility.pref_show_multireddit_main_menu()) {
                showMultiredditsHeader(activity)

                val multiredditsLoadingSpinnerView = LoadingSpinnerView(activity)
                val paddingPx = dpToPixels(activity, 30f)
                multiredditsLoadingSpinnerView.setPadding(
                    paddingPx,
                    paddingPx,
                    paddingPx,
                    paddingPx
                )

                val multiredditsLoadingItem = GroupedRecyclerViewItemFrameLayout(
                    multiredditsLoadingSpinnerView
                )
                adapter.appendToGroup(GROUP_MULTIREDDITS_ITEMS, multiredditsLoadingItem)
            }
        }

        if (PrefsUtility.pref_show_subscribed_subreddits_main_menu()) {
            adapter.appendToGroup(
                GROUP_SUBREDDITS_HEADER,
                GroupedRecyclerViewItemListSectionHeaderView(
                    activity.getString(string.mainmenu_header_subreddits_subscribed)
                )
            )

            run {
                val subredditsLoadingSpinnerView = LoadingSpinnerView(activity)
                val paddingPx = dpToPixels(activity, 30f)
                subredditsLoadingSpinnerView.setPadding(
                    paddingPx,
                    paddingPx,
                    paddingPx,
                    paddingPx
                )

                val subredditsLoadingItem = GroupedRecyclerViewItemFrameLayout(
                    subredditsLoadingSpinnerView
                )
                adapter.appendToGroup(GROUP_SUBREDDITS_ITEMS, subredditsLoadingItem)
            }
        }
    }

    private fun setPinnedSubreddits() {
        val pinnedSubreddits = PrefsUtility.pref_pinned_subreddits()

        adapter.removeAllFromGroup(GROUP_PINNED_SUBREDDITS_ITEMS)
        adapter.removeAllFromGroup(GROUP_PINNED_SUBREDDITS_HEADER)

        if (!pinnedSubreddits.isEmpty()) {
            val pinnedSubredditsSort = PrefsUtility.pref_behaviour_pinned_subredditsort()

            adapter.appendToGroup(
                GROUP_PINNED_SUBREDDITS_HEADER,
                GroupedRecyclerViewItemListSectionHeaderView(
                    mActivity.getString(string.mainmenu_header_subreddits_pinned)
                )
            )

            if (pinnedSubredditsSort == PinnedSubredditSort.NAME) {
                Collections.sort<SubredditCanonicalId>(pinnedSubreddits)
            }

            var isFirst = true

            for (sr in pinnedSubreddits) {
                adapter.appendToGroup(
                    GROUP_PINNED_SUBREDDITS_ITEMS,
                    makeSubredditItem(sr, isFirst, true)
                )
                isFirst = false
            }
        }
    }

    private fun setBlockedSubreddits() {
        val blockedSubreddits = PrefsUtility.pref_blocked_subreddits()

        adapter.removeAllFromGroup(GROUP_BLOCKED_SUBREDDITS_ITEMS)
        adapter.removeAllFromGroup(GROUP_BLOCKED_SUBREDDITS_HEADER)

        if (!blockedSubreddits.isEmpty()) {
            val blockedSubredditsSort = PrefsUtility.pref_behaviour_blocked_subredditsort()

            adapter.appendToGroup(
                GROUP_BLOCKED_SUBREDDITS_HEADER,
                GroupedRecyclerViewItemListSectionHeaderView(
                    mActivity.getString(string.mainmenu_header_subreddits_blocked)
                )
            )

            if (blockedSubredditsSort == BlockedSubredditSort.NAME) {
                Collections.sort<SubredditCanonicalId>(blockedSubreddits)
            }

            var isFirst = true
            for (sr in blockedSubreddits) {
                adapter.appendToGroup(
                    GROUP_BLOCKED_SUBREDDITS_ITEMS,
                    makeSubredditItem(sr, isFirst, true)
                )
                isFirst = false
            }
        }
    }


    private fun showMultiredditsHeader(context: Context) {
        checkThisIsUIThread()

        if (mMultiredditHeaderItem == null) {
            val headerItem = GroupedRecyclerViewItemListSectionHeaderView(
                context.getString(string.mainmenu_header_multireddits)
            )

            mMultiredditHeaderItem = headerItem
            adapter.appendToGroup(GROUP_MULTIREDDITS_HEADER, headerItem)
        }
    }

    private fun hideMultiredditsHeader() {
        checkThisIsUIThread()

        mMultiredditHeaderItem = null
        adapter.removeAllFromGroup(GROUP_MULTIREDDITS_HEADER)
    }

    fun setMultiredditsError(errorView: ErrorView) {
        AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
            adapter.removeAllFromGroup(GROUP_MULTIREDDITS_ITEMS)
            adapter.appendToGroup(
                GROUP_MULTIREDDITS_ITEMS,
                GroupedRecyclerViewItemFrameLayout(errorView)
            )
        })
    }

    fun setSubredditsError(errorView: ErrorView) {
        AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
            adapter.removeAllFromGroup(GROUP_SUBREDDITS_ITEMS)
            adapter.appendToGroup(
                GROUP_SUBREDDITS_ITEMS,
                GroupedRecyclerViewItemFrameLayout(errorView)
            )
        })
    }

    fun setSubreddits(subscriptions: Collection<SubredditCanonicalId>) {
        val subscriptionsSorted = ArrayList<SubredditCanonicalId>(
            subscriptions
        )
        Collections.sort<SubredditCanonicalId>(subscriptionsSorted)

        AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
            if (mSubredditSubscriptions != null
                && mSubredditSubscriptions == subscriptionsSorted
            ) {
                return@Runnable
            }
            if (!PrefsUtility.pref_show_subscribed_subreddits_main_menu()) {
                adapter.removeAllFromGroup(GROUP_SUBREDDITS_HEADER)
                adapter.removeAllFromGroup(GROUP_SUBREDDITS_ITEMS)
                return@Runnable
            }

            mSubredditSubscriptions = subscriptionsSorted

            adapter.removeAllFromGroup(GROUP_SUBREDDITS_ITEMS)

            var isFirst = true
            for (subreddit in subscriptionsSorted) {
                adapter.appendToGroup(
                    GROUP_SUBREDDITS_ITEMS,
                    makeSubredditItem(subreddit, isFirst, false)
                )

                isFirst = false
            }
        })
    }

    fun setMultireddits(subscriptions: Collection<String>) {
        val subscriptionsSorted = ArrayList<String>(subscriptions)
        Collections.sort<String>(subscriptionsSorted)

        AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
            if (mMultiredditSubscriptions != null
                && mMultiredditSubscriptions == subscriptionsSorted
            ) {
                return@Runnable
            }
            if (!PrefsUtility.pref_show_multireddit_main_menu()) {
                adapter.removeAllFromGroup(GROUP_MULTIREDDITS_HEADER)
                adapter.removeAllFromGroup(GROUP_MULTIREDDITS_ITEMS)
                return@Runnable
            }

            mMultiredditSubscriptions = subscriptionsSorted

            adapter.removeAllFromGroup(GROUP_MULTIREDDITS_ITEMS)
            if (subscriptionsSorted.isEmpty()) {
                hideMultiredditsHeader()
            } else {
                showMultiredditsHeader(mContext)

                var isFirst = true

                for (multireddit in subscriptionsSorted) {
                    val item = makeMultiredditItem(multireddit, isFirst)
                    adapter.appendToGroup(GROUP_MULTIREDDITS_ITEMS, item)

                    isFirst = false
                }
            }
        })
    }

    private fun makeItem(
        nameRes: Int,
        @MainMenuAction action: Int,
        icon: Drawable?,
        hideDivider: Boolean
    ): GroupedRecyclerViewItemListItemView {
        return makeItem(mContext.getString(nameRes), action, icon, hideDivider)
    }

    private fun makeItem(
        name: String,
        @MainMenuAction action: Int,
        icon: Drawable?,
        hideDivider: Boolean
    ): GroupedRecyclerViewItemListItemView {
        val clickListener = View.OnClickListener { view: View? -> mListener.onSelected(action) }

        return GroupedRecyclerViewItemListItemView(
            icon,
            name,
            null,
            hideDivider,
            clickListener,
            null,
            Optional.Companion.empty<Drawable>(),
            Optional.Companion.empty<View.OnClickListener>(),
            Optional.Companion.empty<String>()
        )
    }

    private fun makeSubredditItem(
        subreddit: SubredditCanonicalId,
        hideDivider: Boolean,
        showRSlashPrefix: Boolean
    ): GroupedRecyclerViewItemListItemView {
        val clickListener = View.OnClickListener { view: View? ->
            if (subreddit.toString().startsWith("/r/")) {
                mListener.onSelected(
                    SubredditPostListURL.Companion.getSubreddit(
                        subreddit
                    ) as PostListingURL?
                )
            } else {
                onLinkClicked(mActivity, UriString(subreddit.toString()))
            }
        }

        val longClickListener = OnLongClickListener { view: View? ->
            showActionMenu(mActivity, subreddit)
            true
        }

        val displayName = if (showRSlashPrefix)
            subreddit.toString()
        else
            subreddit.displayNameLowercase

        return GroupedRecyclerViewItemListItemView(
            null,
            displayName,
            ScreenreaderPronunciation.getPronunciation(mContext, displayName),
            hideDivider,
            clickListener,
            longClickListener,
            Optional.Companion.empty<Drawable>(),
            Optional.Companion.empty<View.OnClickListener>(),
            Optional.Companion.empty<String>()
        )
    }

    private fun makeMultiredditItem(
        name: String,
        hideDivider: Boolean
    ): GroupedRecyclerViewItemListItemView {
        val clickListener = View.OnClickListener { view: View? ->
            mListener.onSelected(
                MultiredditPostListURL.Companion.getMultireddit(name) as PostListingURL?
            )
        }

        return GroupedRecyclerViewItemListItemView(
            null,
            name,
            ScreenreaderPronunciation.getPronunciation(mContext, name),
            hideDivider,
            clickListener,
            null,
            Optional.Companion.empty<Drawable>(),
            Optional.Companion.empty<View.OnClickListener>(),
            Optional.Companion.empty<String>()
        )
    }

    private class SubredditMenuItem(
        context: Context,
        titleRes: Int,
        val action: SubredditAction
    ) {
        val title: String

        init {
            this.title = context.getString(titleRes)
        }
    }

    fun onUpdateAnnouncement() {
        val sharedPreferences = getSharedPrefs(mContext)

        if (PrefsUtility.pref_menus_mainmenu_dev_announcements()) {
            val announcement =                 AnnouncementDownloader.getMostRecentUnreadAnnouncement(sharedPreferences)

            if (announcement.isPresent) {
                mAnnouncementHolder.removeAllViews()
                mAnnouncementHolder.addView(AnnouncementView(mActivity, announcement.get()))
            }
        }
    }

    companion object {
        @Suppress("unused")
        private const val GROUP_MAIN_HEADER = 0

        private const val GROUP_MAIN_ITEMS = 1
        private const val GROUP_USER_HEADER = 2
        private const val GROUP_USER_ITEMS = 3
        private const val GROUP_ANNOUNCEMENTS = 4
        private const val GROUP_PINNED_SUBREDDITS_HEADER = 5
        private const val GROUP_PINNED_SUBREDDITS_ITEMS = 6
        private const val GROUP_BLOCKED_SUBREDDITS_HEADER = 7
        private const val GROUP_BLOCKED_SUBREDDITS_ITEMS = 8
        private const val GROUP_MULTIREDDITS_HEADER = 9
        private const val GROUP_MULTIREDDITS_ITEMS = 10
        private const val GROUP_SUBREDDITS_HEADER = 11
        private const val GROUP_SUBREDDITS_ITEMS = 12

        fun showActionMenu(
            activity: AppCompatActivity,
            subreddit: SubredditCanonicalId
        ) {
            val itemPref = PrefsUtility.pref_menus_subreddit_context_items()

            if (itemPref.isEmpty()) {
                return
            }

            val menu: ArrayList<SubredditMenuItem?> = ArrayList<SubredditMenuItem?>()
            if (itemPref.contains(SubredditAction.COPY_URL)) {
                menu.add(
                    SubredditMenuItem(
                        activity,
                        string.action_copy_link,
                        SubredditAction.COPY_URL
                    )
                )
            }
            if (itemPref.contains(SubredditAction.EXTERNAL)) {
                menu.add(
                    SubredditMenuItem(
                        activity,
                        string.action_external,
                        SubredditAction.EXTERNAL
                    )
                )
            }
            if (itemPref.contains(SubredditAction.SHARE)) {
                menu.add(
                    SubredditMenuItem(
                        activity,
                        string.action_share,
                        SubredditAction.SHARE
                    )
                )
            }

            if (itemPref.contains(SubredditAction.BLOCK)) {
                val isBlocked = PrefsUtility.pref_blocked_subreddits_check(subreddit)

                if (isBlocked) {
                    menu.add(
                        SubredditMenuItem(
                            activity,
                            string.unblock_subreddit,
                            SubredditAction.UNBLOCK
                        )
                    )
                } else {
                    menu.add(
                        SubredditMenuItem(
                            activity,
                            string.block_subreddit,
                            SubredditAction.BLOCK
                        )
                    )
                }
            }

            if (itemPref.contains(SubredditAction.PIN)) {
                val isPinned = PrefsUtility.pref_pinned_subreddits_check(subreddit)

                if (isPinned) {
                    menu.add(
                        SubredditMenuItem(
                            activity,
                            string.unpin_subreddit,
                            SubredditAction.UNPIN
                        )
                    )
                } else {
                    menu.add(
                        SubredditMenuItem(
                            activity,
                            string.pin_subreddit,
                            SubredditAction.PIN
                        )
                    )
                }
            }

            if (!RedditAccountManager.Companion.getInstance(activity)
                    .getDefaultAccount()
                    .isAnonymous
            ) {
                if (itemPref.contains(SubredditAction.SUBSCRIBE)) {
                    val subscriptionManager: RedditSubredditSubscriptionManager=                        RedditSubredditSubscriptionManager.Companion.getSingleton(
                            activity,
                            RedditAccountManager.Companion.getInstance(activity)
                                .getDefaultAccount()
                        )

                    if (subscriptionManager.areSubscriptionsReady()) {
                        if (subscriptionManager.getSubscriptionState(subreddit)
                            == SubredditSubscriptionState.SUBSCRIBED
                        ) {
                            menu.add(
                                SubredditMenuItem(
                                    activity,
                                    string.options_unsubscribe,
                                    SubredditAction.UNSUBSCRIBE
                                )
                            )
                        } else {
                            menu.add(
                                SubredditMenuItem(
                                    activity,
                                    string.options_subscribe,
                                    SubredditAction.SUBSCRIBE
                                )
                            )
                        }
                    }
                }
            }

            val menuText = arrayOfNulls<String>(menu.size)

            for (i in menuText.indices) {
                menuText[i] = menu.get(i)!!.title
            }

            val builder = MaterialAlertDialogBuilder(activity)

            builder.setItems(
                menuText,
                DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    onSubredditActionMenuItemSelected(
                        subreddit,
                        activity,
                        menu.get(which)!!.action
                    )
                })

            val alert = builder.create()
            alert.setCanceledOnTouchOutside(true)
            alert.show()
        }

        private fun onSubredditActionMenuItemSelected(
            subredditCanonicalId: SubredditCanonicalId,
            activity: AppCompatActivity,
            action: SubredditAction
        ) {
            val url = Reddit.getNonAPIUri(subredditCanonicalId.toString())

            val subMan: RedditSubredditSubscriptionManager =                 RedditSubredditSubscriptionManager.Companion.getSingleton(
                    activity,
                    RedditAccountManager.Companion.getInstance(
                        activity
                    )
                        .getDefaultAccount()
                )

            when (action) {
                SubredditAction.SHARE -> {
                    shareText(
                        activity,
                        subredditCanonicalId.toString(),
                        getPreferredRedditUriString(url).toString()
                    )
                }

                SubredditAction.COPY_URL -> {
                    val clipboardManager =                         activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                    if (clipboardManager != null) {
                        val data = ClipData.newPlainText(null, url.value)
                        clipboardManager.setPrimaryClip(data)

                        quickToast(
                            activity.getApplicationContext(),
                            string.subreddit_link_copied_to_clipboard
                        )
                    }
                }

                SubredditAction.EXTERNAL -> {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setData(Uri.parse(url.value))
                    activity.startActivity(intent)
                }

                SubredditAction.PIN -> PrefsUtility.pref_pinned_subreddits_add(
                    activity,
                    subredditCanonicalId
                )

                SubredditAction.UNPIN -> PrefsUtility.pref_pinned_subreddits_remove(
                    activity,
                    subredditCanonicalId
                )

                SubredditAction.BLOCK -> PrefsUtility.pref_blocked_subreddits_add(
                    activity,
                    subredditCanonicalId
                )

                SubredditAction.UNBLOCK -> PrefsUtility.pref_blocked_subreddits_remove(
                    activity,
                    subredditCanonicalId
                )

                SubredditAction.SUBSCRIBE -> if (subMan.getSubscriptionState(subredditCanonicalId)
                    == SubredditSubscriptionState.NOT_SUBSCRIBED
                ) {
                    subMan.subscribe(subredditCanonicalId, activity)
                    Toast.makeText(
                        activity,
                        string.options_subscribing,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        activity,
                        string.mainmenu_toast_subscribed,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                SubredditAction.UNSUBSCRIBE -> if (subMan.getSubscriptionState(subredditCanonicalId)
                    == SubredditSubscriptionState.SUBSCRIBED
                ) {
                    subMan.unsubscribe(subredditCanonicalId, activity)
                    Toast.makeText(
                        activity,
                        string.options_unsubscribing,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        activity,
                        string.mainmenu_toast_not_subscribed,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
