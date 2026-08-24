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

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.window.layout.WindowMetricsCalculator.Companion.getOrCreate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.BugReportActivity.Companion.handleGlobalError
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.common.General.getSharedPrefs
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.common.UnexpectedInternalStateException
import org.quantumbadger.redreader.fragments.AccountListDialog.Companion.show
import org.quantumbadger.redreader.reddit.PostCommentSort
import org.quantumbadger.redreader.reddit.PostSort
import org.quantumbadger.redreader.reddit.UserCommentSort
import org.quantumbadger.redreader.reddit.api.SubredditSubscriptionState
import org.quantumbadger.redreader.settings.types.AppearanceTheme
import java.util.Collections
import org.quantumbadger.redreader.common.General

object OptionsMenuUtility {
    @Suppress("PropertyName")
    val DO_NOT_SHOW: Int = -1


    fun <E> prepare(
        activity: E?,
        menu: Menu,
        subredditsVisible: Boolean,
        postsVisible: Boolean,
        commentsVisible: Boolean,
        areSearchResults: Boolean,
        isUserPostListing: Boolean,
        isUserCommentListing: Boolean,
        postsSortable: Boolean,
        commentsSortable: Boolean,
        isFrontPage: Boolean,
        subredditSubscriptionState: SubredditSubscriptionState?,
        subredditHasSidebar: Boolean,
        pastCommentsSupported: Boolean,
        subredditPinned: Boolean?,
        subredditBlocked: Boolean?
    ) where E : ViewsBaseActivity?, E : OptionsMenuListener? {
        val appbarItemsPrefs = PrefsUtility.pref_menus_appbar_items()

        if (subredditsVisible && !postsVisible && !commentsVisible) {
            OptionsMenuUtility.add(
                activity!!,
                menu,
                Option.REFRESH_SUBREDDITS,
                getOrThrow(appbarItemsPrefs, AppbarItemsPref.REFRESH),
                false
            )
        } else if (!subredditsVisible && postsVisible && !commentsVisible) {
            if (postsSortable) {
                if (areSearchResults) {
                    OptionsMenuUtility.addAllSearchSorts(
                        activity!!,
                        menu,
                        getOrThrow(appbarItemsPrefs, AppbarItemsPref.SORT)
                    )
                } else {
                    OptionsMenuUtility.addAllPostSorts(
                        activity!!,
                        menu,
                        getOrThrow(appbarItemsPrefs, AppbarItemsPref.SORT),
                        !isUserPostListing,
                        isFrontPage
                    )
                }
            }
            OptionsMenuUtility.add(
                activity!!,
                menu,
                Option.REFRESH_POSTS,
                getOrThrow(appbarItemsPrefs, AppbarItemsPref.REFRESH),
                false
            )
            OptionsMenuUtility.add(
                activity,
                menu,
                Option.PAST_POSTS,
                getOrThrow(appbarItemsPrefs, AppbarItemsPref.PAST),
                false
            )
            OptionsMenuUtility.add(
                activity,
                menu,
                Option.SUBMIT_POST,
                getOrThrow(appbarItemsPrefs, AppbarItemsPref.SUBMIT_POST),
                false
            )
            OptionsMenuUtility.add(
                activity,
                menu,
                Option.SEARCH,
                getOrThrow(appbarItemsPrefs, AppbarItemsPref.SEARCH),
                false
            )

            if (subredditPinned != null) {
                if (subredditPinned) {
                    OptionsMenuUtility.add(
                        activity,
                        menu,
                        Option.UNPIN,
                        getOrThrow(appbarItemsPrefs, AppbarItemsPref.PIN),
                        false
                    )
                } else {
                    OptionsMenuUtility.add(
                        activity,
                        menu,
                        Option.PIN,
                        getOrThrow(appbarItemsPrefs, AppbarItemsPref.PIN),
                        false
                    )
                }
            }

            if (subredditSubscriptionState != null) {
                OptionsMenuUtility.addSubscriptionItem(
                    activity,
                    menu,
                    getOrThrow(appbarItemsPrefs, AppbarItemsPref.SUBSCRIBE),
                    subredditSubscriptionState
                )
            }

            if (subredditBlocked != null) {
                if (subredditBlocked) {
                    OptionsMenuUtility.add(
                        activity,
                        menu,
                        Option.UNBLOCK,
                        getOrThrow(appbarItemsPrefs, AppbarItemsPref.BLOCK),
                        false
                    )
                } else {
                    OptionsMenuUtility.add(
                        activity,
                        menu,
                        Option.BLOCK,
                        getOrThrow(appbarItemsPrefs, AppbarItemsPref.BLOCK),
                        false
                    )
                }
            }

            if (subredditHasSidebar) {
                OptionsMenuUtility.add(
                    activity,
                    menu,
                    Option.SIDEBAR,
                    getOrThrow(appbarItemsPrefs, AppbarItemsPref.SIDEBAR),
                    false
                )
            }
        } else if (!subredditsVisible && !postsVisible && commentsVisible) {
            if (commentsSortable && !isUserCommentListing) {
                OptionsMenuUtility.addAllCommentSorts(
                    activity!!,
                    menu,
                    getOrThrow(appbarItemsPrefs, AppbarItemsPref.SORT)
                )
            } else if (commentsSortable && isUserCommentListing) {
                OptionsMenuUtility.addAllUserCommentSorts(
                    activity!!,
                    menu,
                    getOrThrow(appbarItemsPrefs, AppbarItemsPref.SORT)
                )
            }
            OptionsMenuUtility.add(
                activity!!,
                menu,
                Option.REFRESH_COMMENTS,
                getOrThrow(appbarItemsPrefs, AppbarItemsPref.REFRESH),
                false
            )
            OptionsMenuUtility.add(
                activity,
                menu,
                Option.SEARCH,
                getOrThrow(appbarItemsPrefs, AppbarItemsPref.SEARCH),
                false
            )
            if (pastCommentsSupported) {
                OptionsMenuUtility.add(
                    activity,
                    menu,
                    Option.PAST_COMMENTS,
                    getOrThrow(appbarItemsPrefs, AppbarItemsPref.PAST),
                    false
                )
            }
        } else {
            if (postsVisible && commentsVisible) {
                if (getOrThrow(appbarItemsPrefs, AppbarItemsPref.SORT) != DO_NOT_SHOW) {
                    val sortMenu = menu.addSubMenu(
                        Menu.NONE,
                        AppbarItemsPref.SORT.ordinal,
                        Menu.NONE,
                        string.options_sort
                    )
                    sortMenu.getItem().setIcon(R.drawable.ic_sort_dark)
                    sortMenu.getItem()
                        .setShowAsAction(
                            handleShowAsActionIfRoom(
                                getOrThrow(
                                    appbarItemsPrefs,
                                    AppbarItemsPref.SORT
                                )
                            )
                        )

                    if (postsSortable) {
                        if (areSearchResults) {
                            OptionsMenuUtility.addAllSearchSorts(
                                activity!!,
                                sortMenu,
                                MenuItem.SHOW_AS_ACTION_NEVER
                            )
                        } else {
                            OptionsMenuUtility.addAllPostSorts(
                                activity!!,
                                sortMenu,
                                MenuItem.SHOW_AS_ACTION_NEVER,
                                !isUserPostListing,
                                isFrontPage
                            )
                        }
                    }
                    if (commentsSortable) {
                        OptionsMenuUtility.addAllCommentSorts(
                            activity!!,
                            sortMenu,
                            MenuItem.SHOW_AS_ACTION_NEVER
                        )
                    }
                }
            } else if (postsVisible) {
                if (postsSortable) {
                    if (areSearchResults) {
                        OptionsMenuUtility.addAllSearchSorts(
                            activity!!,
                            menu,
                            getOrThrow(appbarItemsPrefs, AppbarItemsPref.SORT)
                        )
                    } else {
                        OptionsMenuUtility.addAllPostSorts(
                            activity!!,
                            menu,
                            getOrThrow(appbarItemsPrefs, AppbarItemsPref.SORT),
                            !isUserPostListing,
                            isFrontPage
                        )
                    }
                }
            }

            if (getOrThrow(appbarItemsPrefs, AppbarItemsPref.REFRESH) != DO_NOT_SHOW) {
                val refreshMenu = menu.addSubMenu(
                    Menu.NONE,
                    AppbarItemsPref.REFRESH.ordinal,
                    Menu.NONE,
                    string.options_refresh
                )
                refreshMenu.getItem().setIcon(R.drawable.ic_refresh_dark)
                refreshMenu.getItem()
                    .setShowAsAction(
                        handleShowAsActionIfRoom(
                            getOrThrow(
                                appbarItemsPrefs,
                                AppbarItemsPref.REFRESH
                            )
                        )
                    )

                if (subredditsVisible) {
                    add(activity!!, refreshMenu, Option.REFRESH_SUBREDDITS)
                }
                if (postsVisible) {
                    add(activity!!, refreshMenu, Option.REFRESH_POSTS)
                }
                if (commentsVisible) {
                    add(activity!!, refreshMenu, Option.REFRESH_COMMENTS)
                }
            }

            if (postsVisible && commentsVisible) {
                if (getOrThrow(appbarItemsPrefs, AppbarItemsPref.PAST) != DO_NOT_SHOW) {
                    val pastMenu = menu.addSubMenu(
                        Menu.NONE,
                        AppbarItemsPref.PAST.ordinal,
                        Menu.NONE,
                        string.options_past
                    )
                    pastMenu.getItem().setIcon(R.drawable.ic_time_dark)
                    pastMenu.getItem()
                        .setShowAsAction(
                            handleShowAsActionIfRoom(
                                getOrThrow(
                                    appbarItemsPrefs,
                                    AppbarItemsPref.PAST
                                )
                            )
                        )

                    add(activity!!, pastMenu, Option.PAST_POSTS)
                    if (pastCommentsSupported) {
                        add(activity, pastMenu, Option.PAST_COMMENTS)
                    }
                }

                if (getOrThrow(appbarItemsPrefs, AppbarItemsPref.SEARCH) != DO_NOT_SHOW) {
                    val searchMenu = menu.addSubMenu(
                        Menu.NONE,
                        AppbarItemsPref.SEARCH.ordinal,
                        1,
                        string.action_search
                    )
                    searchMenu.getItem().setIcon(R.drawable.ic_search_dark)
                    searchMenu.getItem()
                        .setShowAsAction(
                            handleShowAsActionIfRoom(
                                getOrThrow(
                                    appbarItemsPrefs,
                                    AppbarItemsPref.SEARCH
                                )
                            )
                        )

                    add(activity!!, searchMenu, Option.SEARCH)
                    add(activity, searchMenu, Option.SEARCH_COMMENTS)
                }
            } else if (postsVisible) {
                OptionsMenuUtility.add(
                    activity!!,
                    menu,
                    Option.SEARCH,
                    getOrThrow(appbarItemsPrefs, AppbarItemsPref.SEARCH),
                    false
                )
                OptionsMenuUtility.add(
                    activity,
                    menu,
                    Option.PAST_POSTS,
                    getOrThrow(appbarItemsPrefs, AppbarItemsPref.PAST),
                    false
                )
            }

            if (postsVisible) {
                OptionsMenuUtility.add(
                    activity!!,
                    menu,
                    Option.SUBMIT_POST,
                    getOrThrow(appbarItemsPrefs, AppbarItemsPref.SUBMIT_POST),
                    false
                )

                if (subredditPinned != null) {
                    if (subredditPinned) {
                        OptionsMenuUtility.add(
                            activity,
                            menu,
                            Option.UNPIN,
                            getOrThrow(appbarItemsPrefs, AppbarItemsPref.PIN),
                            false
                        )
                    } else {
                        OptionsMenuUtility.add(
                            activity,
                            menu,
                            Option.PIN,
                            getOrThrow(appbarItemsPrefs, AppbarItemsPref.PIN),
                            false
                        )
                    }
                }

                if (subredditSubscriptionState != null) {
                    OptionsMenuUtility.addSubscriptionItem(
                        activity,
                        menu,
                        getOrThrow(appbarItemsPrefs, AppbarItemsPref.SUBSCRIBE),
                        subredditSubscriptionState
                    )
                }

                if (subredditBlocked != null) {
                    if (subredditBlocked) {
                        OptionsMenuUtility.add(
                            activity,
                            menu,
                            Option.UNBLOCK,
                            getOrThrow(appbarItemsPrefs, AppbarItemsPref.BLOCK),
                            false
                        )
                    } else {
                        OptionsMenuUtility.add(
                            activity,
                            menu,
                            Option.BLOCK,
                            getOrThrow(appbarItemsPrefs, AppbarItemsPref.BLOCK),
                            false
                        )
                    }
                }

                if (subredditHasSidebar) {
                    OptionsMenuUtility.add(
                        activity,
                        menu,
                        Option.SIDEBAR,
                        getOrThrow(appbarItemsPrefs, AppbarItemsPref.SIDEBAR),
                        false
                    )
                }
            }
        }

        OptionsMenuUtility.addAccounts(
            activity!!,
            menu,
            getOrThrow(appbarItemsPrefs, AppbarItemsPref.ACCOUNTS)
        )
        OptionsMenuUtility.add(
            activity,
            menu,
            Option.THEMES,
            getOrThrow(appbarItemsPrefs, AppbarItemsPref.THEME),
            false
        )

        // Always show settings if the main menu is visible, to prevent user from being
        // locked out of them
        if (subredditsVisible
            && (getOrThrow(appbarItemsPrefs, AppbarItemsPref.SETTINGS)
                    == DO_NOT_SHOW)
        ) {
            OptionsMenuUtility.add(
                activity,
                menu,
                Option.SETTINGS,
                MenuItem.SHOW_AS_ACTION_NEVER,
                false
            )
        } else {
            OptionsMenuUtility.add(
                activity,
                menu,
                Option.SETTINGS,
                getOrThrow(appbarItemsPrefs, AppbarItemsPref.SETTINGS),
                false
            )
        }

        OptionsMenuUtility.add(
            activity,
            menu,
            Option.CLOSE_ALL,
            getOrThrow(appbarItemsPrefs, AppbarItemsPref.CLOSE_ALL),
            false
        )

        OptionsMenuUtility.pruneMenu(activity, menu, appbarItemsPrefs, !subredditsVisible)
    }

    fun pruneMenu(
        activity: Activity,
        menu: Menu,
        appbarItemsPrefs: MutableMap<AppbarItemsPref, Int>,
        backButtonShown: Boolean
    ) {
        //Figure out how many buttons can fit

        val windowBounds = getOrCreate()
            .computeCurrentWindowMetrics(activity).bounds

        val buttonSize = dpToPixels(activity, 48f)
        val backButtonSize = dpToPixels(activity, 52f)

        var buttonSlotsRemaining = (windowBounds.width() - (if (backButtonShown)
            backButtonSize
        else
            0)) / buttonSize

        //Count show-if-room buttons, subtract always-show buttons from
        // total-remaining, see if we MUST show the overflow menu
        var optionalButtonsRequested = 0
        var overflowButtonRequired = false

        for (i in 0..<menu.size()) {
            for (pair in appbarItemsPrefs.entries) {
                if (pair.key!!.ordinal == menu.getItem(i).getItemId()) {
                    if (pair.value == MenuItem.SHOW_AS_ACTION_ALWAYS) {
                        buttonSlotsRemaining--
                    } else if (pair.value == MenuItem.SHOW_AS_ACTION_NEVER) {
                        overflowButtonRequired = true
                    } else {
                        optionalButtonsRequested++
                    }
                }
            }
        }

        //Reserve space for the overflow button if needed
        if (overflowButtonRequired || optionalButtonsRequested > buttonSlotsRemaining) {
            buttonSlotsRemaining--
        }

        //Move optional buttons to the overflow menu if there's not enough space, end to start
        if (optionalButtonsRequested > buttonSlotsRemaining) {
            for (i in menu.size() - 1 downTo 0) {
                for (pair in appbarItemsPrefs.entries) {
                    if (pair.key!!.ordinal == menu.getItem(i).getItemId()
                        && pair.value == MenuItem.SHOW_AS_ACTION_IF_ROOM
                    ) {
                        menu.getItem(i).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                        buttonSlotsRemaining++
                        break
                    }
                }
                if (optionalButtonsRequested <= buttonSlotsRemaining) {
                    break
                }
            }
        }
    }

    private fun addSubscriptionItem(
        activity: ViewsBaseActivity, menu: Menu, showAsAction: Int,
        subredditSubscriptionState: SubredditSubscriptionState?
    ) {
        if (subredditSubscriptionState == null) {
            return
        }

        when (subredditSubscriptionState) {
            SubredditSubscriptionState.NOT_SUBSCRIBED -> {
                add(activity, menu, Option.SUBSCRIBE, showAsAction, false)
                return
            }

            SubredditSubscriptionState.SUBSCRIBED -> {
                add(activity, menu, Option.UNSUBSCRIBE, showAsAction, false)
                return
            }

            SubredditSubscriptionState.SUBSCRIBING -> {
                add(activity, menu, Option.SUBSCRIBING, showAsAction, false)
                return
            }

            SubredditSubscriptionState.UNSUBSCRIBING -> {
                add(activity, menu, Option.UNSUBSCRIBING, showAsAction, false)
                return
            }

            else -> throw UnexpectedInternalStateException("Unknown subscription state")
        }
    }

    private fun add(
        activity: ViewsBaseActivity,
        menu: Menu,
        option: Option,
        showAsAction: Int = MenuItem.SHOW_AS_ACTION_NEVER,
        longText: Boolean = true
    ) {
        var showAsAction = showAsAction
        if (showAsAction == DO_NOT_SHOW) {
            return
        } else {
            showAsAction = handleShowAsActionIfRoom(showAsAction)
        }

        when (option) {
            Option.ACCOUNTS -> {
                val accounts = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.ACCOUNTS.ordinal,
                    if (longText)
                        QuickAccountsSort.MANAGER
                    else
                        Menu.NONE,
                    activity.getString(
                        if (longText)
                            string.options_account_manager
                        else
                            string.options_accounts
                    )
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        show(activity)
                        true
                    })

                accounts.setShowAsAction(showAsAction)
                if (longText) {
                    if (PrefsUtility.isNightMode) {
                        accounts.setIcon(R.drawable.ic_settings_dark)
                    } else {
                        accounts.setIcon(R.drawable.ic_settings_light)
                    }
                } else {
                    accounts.setIcon(R.drawable.ic_accounts_dark)
                }
            }

            Option.SETTINGS -> {
                val settings = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.SETTINGS.ordinal,
                    Menu.NONE,
                    string.options_settings
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        val intent = Intent(
                            activity,
                            MainActivityCompose::class.java
                        )
                        intent.putExtra(
                            MainActivityCompose.EXTRA_DEEP_LINK,
                            MainActivityCompose.DEEP_LINK_SETTINGS
                        )
                        activity.startActivityForResult(
                            intent,
                            1
                        )
                        true
                    })

                settings.setShowAsAction(showAsAction)
                settings.setIcon(R.drawable.ic_settings_dark)
            }

            Option.CLOSE_ALL -> {
                val closeAll = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.CLOSE_ALL.ordinal,
                    Menu.NONE,
                    string.options_close_all
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        activity.closeAllExceptMain()
                        true
                    })

                closeAll.setShowAsAction(showAsAction)
                closeAll.setIcon(R.drawable.ic_action_cross_dark)
            }

            Option.THEMES -> {
                val themes = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.THEME.ordinal,
                    Menu.NONE,
                    string.options_theme
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        val prefs = getSharedPrefs(activity)
                        val currentTheme = PrefsUtility.appearance_theme()

                        val themeNames = activity.getResources()
                            .getStringArray(R.array.pref_appearance_theme)

                        val themeValues = activity.getResources()
                            .getStringArray(R.array.pref_appearance_theme_return)

                        var selectedPos = -1
                        var i = 0
                        while (i < themeValues.size) {
                            if (AppearanceTheme.valueOf(
                                    StringUtils.asciiUppercase(themeValues[i]!!)
                                )
                                == currentTheme
                            ) {
                                selectedPos = i
                                break
                            }
                            i++
                        }

                        val dialog = MaterialAlertDialogBuilder(activity)
                        dialog.setTitle(string.pref_appearance_theme_title)

                        dialog.setSingleChoiceItems(
                            themeNames,
                            selectedPos,
                            DialogInterface.OnClickListener { dialog1: DialogInterface?, item1: Int ->
                                prefs.edit()
                                    .putString(
                                        activity.getString(
                                            string.pref_appearance_theme_key
                                        ),
                                        themeValues[item1]
                                    )
                                    .apply()
                                dialog1!!.dismiss()
                            })

                        val alert = dialog.create()
                        alert.show()
                        true
                    })

                themes.setShowAsAction(showAsAction)
                themes.setIcon(R.drawable.ic_themes_dark)
            }

            Option.REFRESH_SUBREDDITS -> {
                val refreshSubreddits = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.REFRESH.ordinal,
                    Menu.NONE,
                    string.options_refresh_subreddits
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        (activity as OptionsMenuSubredditsListener)
                            .onRefreshSubreddits()
                        true
                    })

                refreshSubreddits.setShowAsAction(showAsAction)
                if (!longText) {
                    refreshSubreddits.setIcon(R.drawable.ic_refresh_dark)
                }
            }

            Option.REFRESH_POSTS -> {
                val refreshPosts = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.REFRESH.ordinal,
                    Menu.NONE,
                    string.options_refresh_posts
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        (activity as OptionsMenuPostsListener).onRefreshPosts()
                        true
                    })

                refreshPosts.setShowAsAction(showAsAction)
                if (!longText) {
                    refreshPosts.setIcon(R.drawable.ic_refresh_dark)
                }
            }

            Option.SUBMIT_POST -> {
                val submitPost = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.SUBMIT_POST.ordinal,
                    Menu.NONE,
                    string.options_submit_post
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        (activity as OptionsMenuPostsListener).onSubmitPost()
                        true
                    })

                submitPost.setShowAsAction(showAsAction)
                submitPost.setIcon(R.drawable.ic_action_send_dark)
            }

            Option.SEARCH -> {
                val search = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.SEARCH.ordinal,
                    1,
                    activity.getString(
                        if (longText)
                            string.action_search_posts
                        else
                            string.action_search
                    )
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        if (activity is OptionsMenuPostsListener) {
                            (activity as OptionsMenuPostsListener).onSearchPosts()
                            return@OnMenuItemClickListener true
                        } else if (activity is OptionsMenuCommentsListener) {
                            (activity as OptionsMenuCommentsListener).onSearchComments()
                            return@OnMenuItemClickListener true
                        } else {
                            return@OnMenuItemClickListener false
                        }
                    })

                search.setShowAsAction(showAsAction)
                if (!longText) {
                    search.setIcon(R.drawable.ic_search_dark)
                }
            }

            Option.SEARCH_COMMENTS -> {
                val searchComments = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.SEARCH.ordinal,
                    1,
                    activity.getString(string.action_search_comments)
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        if (activity is OptionsMenuCommentsListener) {
                            (activity as OptionsMenuCommentsListener)
                                .onSearchComments()
                            return@OnMenuItemClickListener true
                        }
                        false
                    })

                searchComments.setShowAsAction(showAsAction)
                if (!longText) {
                    searchComments.setIcon(R.drawable.ic_search_dark)
                }
            }

            Option.REFRESH_COMMENTS -> {
                val refreshComments = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.REFRESH.ordinal,
                    Menu.NONE,
                    string.options_refresh_comments
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        (activity as OptionsMenuCommentsListener)
                            .onRefreshComments()
                        true
                    })

                refreshComments.setShowAsAction(showAsAction)
                if (!longText) {
                    refreshComments.setIcon(R.drawable.ic_refresh_dark)
                }
            }

            Option.PAST_POSTS -> {
                val pastPosts = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.PAST.ordinal,
                    Menu.NONE,
                    if (longText) string.options_past_posts else string.options_past
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        (activity as OptionsMenuPostsListener).onPastPosts()
                        true
                    })

                if (showAsAction != MenuItem.SHOW_AS_ACTION_NEVER) {
                    pastPosts.setShowAsAction(showAsAction)
                    pastPosts.setIcon(R.drawable.ic_time_dark)
                }
            }

            Option.PAST_COMMENTS -> {
                val pastComments = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.PAST.ordinal,
                    Menu.NONE,
                    if (longText) string.options_past_comments else string.options_past
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        (activity as OptionsMenuCommentsListener)
                            .onPastComments()
                        true
                    })

                if (showAsAction != MenuItem.SHOW_AS_ACTION_NEVER) {
                    pastComments.setShowAsAction(showAsAction)
                    pastComments.setIcon(R.drawable.ic_time_dark)
                }
            }

            Option.SUBSCRIBE -> {
                val subscribe = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.SUBSCRIBE.ordinal,
                    Menu.NONE,
                    string.options_subscribe
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        (activity as OptionsMenuPostsListener).onSubscribe()
                        true
                    })

                subscribe.setShowAsAction(showAsAction)
                subscribe.setIcon(R.drawable.star_off_dark)
            }

            Option.UNSUBSCRIBE -> {
                val unsubscribe = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.SUBSCRIBE.ordinal,
                    Menu.NONE,
                    string.options_unsubscribe
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        (activity as OptionsMenuPostsListener).onUnsubscribe()
                        true
                    })

                unsubscribe.setShowAsAction(showAsAction)
                unsubscribe.setIcon(R.drawable.star_dark)
            }

            Option.UNSUBSCRIBING -> {
                val unsubscribing = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.SUBSCRIBE.ordinal,
                    Menu.NONE,
                    string.options_unsubscribing
                ).setEnabled(false)

                // TODO Somehow use a ButtonLoadingSpinnerView here or something?
                unsubscribing.setShowAsAction(showAsAction)
                unsubscribing.setIcon(R.drawable.ic_loading_dark)
            }

            Option.SUBSCRIBING -> {
                val subscribing = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.SUBSCRIBE.ordinal,
                    Menu.NONE,
                    string.options_subscribing
                ).setEnabled(false)

                // TODO Somehow use a ButtonLoadingSpinnerView here or something?
                subscribing.setShowAsAction(showAsAction)
                subscribing.setIcon(R.drawable.ic_loading_dark)
            }

            Option.SIDEBAR -> {
                val sidebar = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.SIDEBAR.ordinal,
                    Menu.NONE,
                    string.options_sidebar
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        (activity as OptionsMenuPostsListener).onSidebar()
                        true
                    })

                sidebar.setShowAsAction(showAsAction)
                sidebar.setIcon(R.drawable.ic_action_info_dark)
            }

            Option.PIN -> {
                val pin = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.PIN.ordinal,
                    Menu.NONE,
                    string.pin_subreddit
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        (activity as OptionsMenuPostsListener).onPin()
                        true
                    })

                pin.setShowAsAction(showAsAction)
                pin.setIcon(R.drawable.pin_off_dark)
            }

            Option.UNPIN -> {
                val unpin = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.PIN.ordinal,
                    Menu.NONE,
                    string.unpin_subreddit
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        (activity as OptionsMenuPostsListener).onUnpin()
                        true
                    })

                unpin.setShowAsAction(showAsAction)
                unpin.setIcon(R.drawable.pin_dark)
            }

            Option.BLOCK -> {
                val block = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.BLOCK.ordinal,
                    Menu.NONE,
                    string.block_subreddit
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        (activity as OptionsMenuPostsListener).onBlock()
                        true
                    })

                block.setShowAsAction(showAsAction)
                block.setIcon(R.drawable.ic_block_off_dark)
            }

            Option.UNBLOCK -> {
                val unblock = menu.add(
                    Menu.NONE,
                    AppbarItemsPref.BLOCK.ordinal,
                    Menu.NONE,
                    string.unblock_subreddit
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        (activity as OptionsMenuPostsListener).onUnblock()
                        true
                    })

                unblock.setShowAsAction(showAsAction)
                unblock.setIcon(R.drawable.ic_block_dark)
            }

            else -> handleGlobalError(
                activity,
                "Unknown menu option added"
            )
        }
    }

    // The SORTS groups are only consumed inside this class (addSortsToNewSubmenu).
    // Kotlin forbids public members exposing a private nested type, so these are private
    // (Java allowed `final static SortGroup` = package-private over a `private class`).
    @Suppress("PropertyName")
    private val CONTROVERSIAL_SORTS: SortGroup = SortGroup(
        arrayOf<PostSort>(
            PostSort.CONTROVERSIAL_HOUR,
            PostSort.CONTROVERSIAL_DAY,
            PostSort.CONTROVERSIAL_WEEK,
            PostSort.CONTROVERSIAL_MONTH,
            PostSort.CONTROVERSIAL_YEAR,
            PostSort.CONTROVERSIAL_ALL
        ),
        string.sort_posts_controversial
    )

    @Suppress("PropertyName")
    private val TOP_SORTS: SortGroup = SortGroup(
        arrayOf<PostSort>(
            PostSort.TOP_HOUR,
            PostSort.TOP_DAY,
            PostSort.TOP_WEEK,
            PostSort.TOP_MONTH,
            PostSort.TOP_YEAR,
            PostSort.TOP_ALL
        ),
        string.sort_posts_top
    )

    @Suppress("PropertyName")
    private val RELEVANCE_SORTS: SortGroup = SortGroup(
        arrayOf<PostSort>(
            PostSort.RELEVANCE_HOUR,
            PostSort.RELEVANCE_DAY,
            PostSort.RELEVANCE_WEEK,
            PostSort.RELEVANCE_MONTH,
            PostSort.RELEVANCE_YEAR,
            PostSort.RELEVANCE_ALL
        ),
        string.sort_posts_relevance
    )

    @Suppress("PropertyName")
    private val NEW_SORTS: SortGroup = SortGroup(
        arrayOf<PostSort>(
            PostSort.NEW_HOUR,
            PostSort.NEW_DAY,
            PostSort.NEW_WEEK,
            PostSort.NEW_MONTH,
            PostSort.NEW_YEAR,
            PostSort.NEW_ALL
        ),
        string.sort_posts_new
    )

    @Suppress("PropertyName")
    private val HOT_SORTS: SortGroup = SortGroup(
        arrayOf<PostSort>(
            PostSort.HOT_HOUR,
            PostSort.HOT_DAY,
            PostSort.HOT_WEEK,
            PostSort.HOT_MONTH,
            PostSort.HOT_YEAR,
            PostSort.HOT_ALL
        ),
        string.sort_posts_hot
    )

    @Suppress("PropertyName")
    private val COMMENTS_SORTS: SortGroup = SortGroup(
        arrayOf<PostSort>(
            PostSort.COMMENTS_HOUR,
            PostSort.COMMENTS_DAY,
            PostSort.COMMENTS_WEEK,
            PostSort.COMMENTS_MONTH,
            PostSort.COMMENTS_YEAR,
            PostSort.COMMENTS_ALL
        ),
        string.sort_posts_comments
    )

    private fun addAllPostSorts(
        activity: AppCompatActivity,
        menu: Menu,
        showAsAction: Int,
        includeRising: Boolean,
        includeBest: Boolean
    ) {
        if (showAsAction == DO_NOT_SHOW) {
            return
        }

        val sortPosts = addSortSubMenu(menu, string.options_sort_posts, showAsAction)

        addSort(activity, sortPosts, PostSort.HOT)
        addSort(activity, sortPosts, PostSort.NEW)

        if (includeRising) {
            addSort(activity, sortPosts, PostSort.RISING)
        }

        addSortsToNewSubmenu(activity, sortPosts, CONTROVERSIAL_SORTS)

        if (includeBest) {
            addSort(activity, sortPosts, PostSort.BEST)
        }

        addSortsToNewSubmenu(activity, sortPosts, TOP_SORTS)

        sortPosts.setGroupCheckable(Menu.NONE, true, true)
    }

    private fun addAllSearchSorts(
        activity: AppCompatActivity,
        menu: Menu,
        showAsAction: Int
    ) {
        if (showAsAction == DO_NOT_SHOW) {
            return
        }

        val sortPosts = addSortSubMenu(menu, string.options_sort_posts, showAsAction)

        addSortsToNewSubmenu(activity, sortPosts, RELEVANCE_SORTS)
        addSortsToNewSubmenu(activity, sortPosts, NEW_SORTS)
        addSortsToNewSubmenu(activity, sortPosts, HOT_SORTS)
        addSortsToNewSubmenu(activity, sortPosts, TOP_SORTS)
        addSortsToNewSubmenu(activity, sortPosts, COMMENTS_SORTS)

        sortPosts.setGroupCheckable(Menu.NONE, true, true)
    }

    private fun addAllCommentSorts(
        activity: AppCompatActivity,
        menu: Menu,
        showAsAction: Int
    ) {
        if (showAsAction == DO_NOT_SHOW) {
            return
        }

        val sortComments = addSortSubMenu(
            menu,
            string.options_sort_comments,
            showAsAction
        )

        val postCommentSorts = arrayOf<PostCommentSort?>(
            PostCommentSort.BEST,
            PostCommentSort.HOT,
            PostCommentSort.NEW,
            PostCommentSort.OLD,
            PostCommentSort.CONTROVERSIAL,
            PostCommentSort.TOP,
            PostCommentSort.QA
        )

        for (sort in postCommentSorts) {
            OptionsMenuUtility.addSort(activity, sortComments, sort!!)
        }

        sortComments.setGroupCheckable(Menu.NONE, true, true)
    }

    @Suppress("PropertyName")
    private val CONTROVERSIAL_COMMENT_SORTS: SortGroup = SortGroup(
        arrayOf<UserCommentSort>(
            UserCommentSort.CONTROVERSIAL_HOUR,
            UserCommentSort.CONTROVERSIAL_DAY,
            UserCommentSort.CONTROVERSIAL_WEEK,
            UserCommentSort.CONTROVERSIAL_MONTH,
            UserCommentSort.CONTROVERSIAL_YEAR,
            UserCommentSort.CONTROVERSIAL_ALL
        ),
        string.sort_comments_controversial
    )

    @Suppress("PropertyName")
    private val TOP_COMMENT_SORTS: SortGroup = SortGroup(
        arrayOf<UserCommentSort>(
            UserCommentSort.TOP_HOUR,
            UserCommentSort.TOP_DAY,
            UserCommentSort.TOP_WEEK,
            UserCommentSort.TOP_MONTH,
            UserCommentSort.TOP_YEAR,
            UserCommentSort.TOP_ALL
        ),
        string.sort_comments_top
    )

    private fun addAllUserCommentSorts(
        activity: AppCompatActivity,
        menu: Menu,
        showAsAction: Int
    ) {
        if (showAsAction == DO_NOT_SHOW) {
            return
        }

        val sortComments = addSortSubMenu(
            menu,
            string.options_sort_comments,
            showAsAction
        )

        addSort(activity, sortComments, UserCommentSort.HOT)
        addSort(activity, sortComments, UserCommentSort.NEW)

        addSortsToNewSubmenu(activity, sortComments, CONTROVERSIAL_COMMENT_SORTS)
        addSortsToNewSubmenu(activity, sortComments, TOP_COMMENT_SORTS)

        sortComments.setGroupCheckable(Menu.NONE, true, true)
    }

    private fun addSort(
        activity: AppCompatActivity,
        menu: Menu,
        order: Sort
    ) {
        @StringRes val menuTitle: Int
        if (activity is OptionsMenuCommentsListener
            && (activity as OptionsMenuCommentsListener).suggestedCommentSort != null && ((activity as OptionsMenuCommentsListener).suggestedCommentSort
                    == order)
        ) {
            menuTitle = order.suggestedTitle
        } else {
            menuTitle = order.menuTitle
        }

        val menuItem = menu.add(activity.getString(menuTitle))
            .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                order.onSortSelected(activity)
                true
            })

        if (activity is OptionsMenuPostsListener
            && (activity as OptionsMenuPostsListener).postSort != null && (activity as OptionsMenuPostsListener).postSort == order
        ) {
            menuItem.setChecked(true)
        } else if (activity is OptionsMenuCommentsListener
            && (activity as OptionsMenuCommentsListener).commentSort != null && (activity as OptionsMenuCommentsListener).commentSort == order
        ) {
            menuItem.setChecked(true)
        }
    }

    private fun addSortsToNewSubmenu(
        activity: AppCompatActivity,
        menu: Menu,
        sortGroup: SortGroup
    ) {
        val subMenu = menu.addSubMenu(activity.getString(sortGroup.subMenuTitle))

        for (sort in sortGroup.sorts) {
            addSort(activity, subMenu, sort)
        }

        subMenu.setGroupCheckable(Menu.NONE, true, true)

        val activeSort: Sort?
        if (sortGroup.sorts.firstOrNull() is PostSort) {
            activeSort = (activity as OptionsMenuPostsListener).postSort
        } else {
            activeSort = (activity as OptionsMenuCommentsListener).commentSort
        }

        if (sortGroup.equalsBaseAndType(activeSort)) {
            menu.getItem(menu.size() - 1).setChecked(true)
        }
    }

    private fun addSortSubMenu(
        menu: Menu,
        @StringRes subMenuTitle: Int,
        showAsAction: Int
    ): SubMenu {
        val sortMenu = menu.addSubMenu(
            Menu.NONE,
            AppbarItemsPref.SORT.ordinal,
            Menu.NONE,
            subMenuTitle
        )

        if (showAsAction != MenuItem.SHOW_AS_ACTION_NEVER) {
            sortMenu.getItem().setIcon(R.drawable.ic_sort_dark)
            sortMenu.getItem().setShowAsAction(handleShowAsActionIfRoom(showAsAction))
        }

        return sortMenu
    }

    private fun addAccounts(
        activity: ViewsBaseActivity,
        menu: Menu,
        showAsAction: Int
    ) {
        if (showAsAction == DO_NOT_SHOW) {
            return
        }

        val accountManager: RedditAccountManager =             RedditAccountManager.Companion.getInstance(activity)
        val accountsList = accountManager.accounts

        if (PrefsUtility.pref_menus_quick_account_switcher()
            && accountsList.size > 1
        ) {
            //Quick account switcher is on, create its SubMenu and add it to the main menu

            val accountsGroup = 1

            val accountsMenu = menu.addSubMenu(
                Menu.NONE,
                AppbarItemsPref.ACCOUNTS.ordinal,
                Menu.NONE,
                string.options_accounts
            )
            accountsMenu.getItem().setShowAsAction(handleShowAsActionIfRoom(showAsAction))
            accountsMenu.getItem().setIcon(R.drawable.ic_accounts_dark)

            // Sort the accounts so they don't move around
            Collections.sort(
                accountsList,
                Comparator { o1: RedditAccount, o2: RedditAccount -> o1.username.compareTo(o2.username) })

            //Add a MenuItem for each account, always putting Anonymous after the real accounts
            //Each account gets a radio button to show which one is active
            for (account in accountsList) {
                val accountsMenuItem = accountsMenu.add(
                    accountsGroup,
                    Menu.NONE,
                    if (account.isAnonymous)
                        QuickAccountsSort.ANONYMOUS
                    else
                        QuickAccountsSort.ACCOUNT,
                    if (account.isAnonymous)
                        activity.getString(string.accounts_anon)
                    else
                        account.username
                )
                    .setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener { item: MenuItem? ->
                        accountManager.defaultAccount = account
                        true
                    })

                if (account.equals(accountManager.getDefaultAccount())) {
                    accountsMenuItem.setChecked(true)
                }
            }

            accountsMenu.setGroupCheckable(accountsGroup, true, true)

            //Add a MenuItem for the full account dialog, so it's still accessible for changes
            add(activity, accountsMenu, Option.ACCOUNTS)
        } else {
            //Quick account switcher is off, just make the button go straight to the dialog

            add(activity, menu, Option.ACCOUNTS, showAsAction, false)
        }
    }

    fun handleShowAsActionIfRoom(showAsAction: Int): Int {
        if (showAsAction == MenuItem.SHOW_AS_ACTION_IF_ROOM) {
            return MenuItem.SHOW_AS_ACTION_ALWAYS
        }

        return showAsAction
    }

    // Avoids IDE warnings about null pointers
    fun getOrThrow(
        appbarItemsPref: MutableMap<AppbarItemsPref, Int>,
        key: AppbarItemsPref
    ): Int {
        val value = appbarItemsPref.get(key)

        if (value == null) {
            throw RuntimeException("appbarItemsPref value is null")
        }

        return value
    }

    enum class AppbarItemsPref {
        SORT,
        REFRESH,
        PAST,
        SUBMIT_POST,
        PIN,
        SUBSCRIBE,
        BLOCK,
        SIDEBAR,
        ACCOUNTS,
        THEME,
        SETTINGS,
        CLOSE_ALL,
        REPLY,
        SEARCH
    }

    private enum class Option {
        ACCOUNTS,
        SETTINGS,
        CLOSE_ALL,
        SUBMIT_POST,
        SEARCH,
        SEARCH_COMMENTS,
        REFRESH_SUBREDDITS,
        REFRESH_POSTS,
        REFRESH_COMMENTS,
        PAST_POSTS,
        THEMES,
        PAST_COMMENTS,
        SUBSCRIBE,
        SUBSCRIBING,
        UNSUBSCRIBING,
        UNSUBSCRIBE,
        SIDEBAR,
        PIN,
        UNPIN,
        BLOCK,
        UNBLOCK
    }

    interface Sort {
        val name: String

        @get:StringRes
        val menuTitle: Int

        fun onSortSelected(activity : AppCompatActivity)
    }

    //The sorts of a SortGroup should always be of the same "base type" (e.g. only top post sorts).
    private class SortGroup(val sorts: Array<out Sort>, @field:StringRes val subMenuTitle: Int) {
        fun equalsBaseAndType(sort: Sort?): Boolean {
            if (sort == null) {
                return false
            }

            if (sort.javaClass != sorts[0].javaClass) {
                return false
            }

            val baseSort1 = sorts[0].name.split("_".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[0]
            val baseSort2: String?=sort.name.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]

            return baseSort1 == baseSort2
        }
    }

    private object QuickAccountsSort {
        //Constants for sorting the quick accounts submenu properly
        //Real accounts first, then Anonymous, then the account dialog
        const val ACCOUNT: Int = 2
        const val ANONYMOUS: Int = 3
        const val MANAGER: Int = 4
    }

    // Base marker interface. Must be at least public because the public listener
    // sub-interfaces (OptionsMenuPostsListener, OptionsMenuCommentsListener,
    // OptionsMenuSubredditsListener) extend it — Kotlin forbids a public interface
    // exposing a private supertype. (Java allowed package-private inheritance.)
    interface OptionsMenuListener

    interface OptionsMenuSubredditsListener : OptionsMenuListener {
        fun onRefreshSubreddits()
    }

    interface OptionsMenuPostsListener : OptionsMenuListener {
        fun onRefreshPosts()

        fun onPastPosts()

        fun onSubmitPost()

        fun onSortSelected(order: PostSort?)

        fun onSearchPosts()

        fun onSubscribe()

        fun onUnsubscribe()

        fun onSidebar()

        fun onPin()

        fun onUnpin()

        fun onBlock()

        fun onUnblock()

        val postSort: PostSort?
    }

    interface OptionsMenuCommentsListener : OptionsMenuListener {
        fun onRefreshComments()

        fun onPastComments()

        fun onSortSelected(order: PostCommentSort?)

        fun onSortSelected(order: UserCommentSort?)

        fun onSearchComments()

        val commentSort: Sort?

        val suggestedCommentSort: PostCommentSort?
    }
}