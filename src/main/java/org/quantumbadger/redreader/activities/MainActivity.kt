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

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.apache.commons.lang3.StringUtils
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.RedReader.Companion.getInstance
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountChangeListener
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.OptionsMenuUtility.OptionsMenuCommentsListener
import org.quantumbadger.redreader.activities.OptionsMenuUtility.OptionsMenuPostsListener
import org.quantumbadger.redreader.activities.OptionsMenuUtility.OptionsMenuSubredditsListener
import org.quantumbadger.redreader.activities.RedditTermsActivity.Companion.launch
import org.quantumbadger.redreader.activities.SessionChangeListener.SessionChangeType
import org.quantumbadger.redreader.adapters.MainMenuSelectionListener
import org.quantumbadger.redreader.common.AndroidCommon.promptForNotificationPermission
import org.quantumbadger.redreader.common.Constants.Reddit
import org.quantumbadger.redreader.common.DialogUtils
import org.quantumbadger.redreader.common.DialogUtils.OnSearchListener
import org.quantumbadger.redreader.common.FeatureFlagHandler
import org.quantumbadger.redreader.common.General.getSharedPrefs
import org.quantumbadger.redreader.common.General.isTablet
import org.quantumbadger.redreader.common.General.quickToast
import org.quantumbadger.redreader.common.General.showMustReloginDialog
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.collections.CollectionStream
import org.quantumbadger.redreader.common.collections.MapStream
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.fragments.AccountListDialog.Companion.show
import org.quantumbadger.redreader.fragments.ChangelogDialog
import org.quantumbadger.redreader.fragments.CommentListingFragment
import org.quantumbadger.redreader.fragments.MainMenuFragment
import org.quantumbadger.redreader.fragments.MainMenuFragment.MainMenuAction
import org.quantumbadger.redreader.fragments.MainMenuFragment.MainMenuShortcutItems
import org.quantumbadger.redreader.fragments.PostListingFragment
import org.quantumbadger.redreader.fragments.ReportDialog.Companion.show
import org.quantumbadger.redreader.fragments.SessionListDialog
import org.quantumbadger.redreader.listingcontrollers.CommentListingController
import org.quantumbadger.redreader.listingcontrollers.PostListingController
import org.quantumbadger.redreader.reddit.PostCommentSort
import org.quantumbadger.redreader.reddit.PostSort
import org.quantumbadger.redreader.reddit.RedditSubredditHistory
import org.quantumbadger.redreader.reddit.UserCommentSort
import org.quantumbadger.redreader.reddit.api.RedditOAuth.anyNeedRelogin
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager.ListenerContext
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager.SubredditSubscriptionStateChangeListener
import org.quantumbadger.redreader.reddit.api.SubredditSubscriptionState
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost
import org.quantumbadger.redreader.reddit.things.InvalidSubredditNameException
import org.quantumbadger.redreader.reddit.things.RedditSubreddit
import org.quantumbadger.redreader.reddit.things.SubredditCanonicalId
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL
import org.quantumbadger.redreader.reddit.url.PostListingURL
import org.quantumbadger.redreader.reddit.url.RedditURLParser
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL
import org.quantumbadger.redreader.reddit.url.SearchPostListURL
import org.quantumbadger.redreader.reddit.url.SubredditPostListURL
import org.quantumbadger.redreader.reddit.url.UserPostListingURL
import org.quantumbadger.redreader.reddit.url.UserProfileURL
import org.quantumbadger.redreader.views.RedditPostView.PostSelectionListener
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import org.quantumbadger.redreader.RedReader
import org.quantumbadger.redreader.common.General

class MainActivity : RefreshableActivity(), MainMenuSelectionListener, RedditAccountChangeListener,
    PostSelectionListener, OptionsMenuSubredditsListener, OptionsMenuPostsListener,
    OptionsMenuCommentsListener, SessionChangeListener, SubredditSubscriptionStateChangeListener {
    private var twoPane = false

    private var mainMenuFragment: MainMenuFragment?=null

    private var postListingController: PostListingController?=null
    private var postListingFragment: PostListingFragment?=null

    private var commentListingController: CommentListingController?=null
    private var commentListingFragment: CommentListingFragment?=null

    private var mainMenuView: View?=null
    private var postListingView: View?=null
    private var commentListingView: View?=null

    private var mLeftPane: FrameLayout?=null
    private var mRightPane: FrameLayout?=null

    private var isMenuShown = true

    private val mSubredditSubscriptionListenerContext = AtomicReference<ListenerContext?>(null)

    override fun baseActivityIsActionBarBackEnabled(): Boolean {
        return false
    }

    override fun baseActivityAllowToolbarHideOnScroll(): Boolean {
        return !isTablet(this)
    }

    protected override fun onCreate(savedInstanceState: Bundle?) {
        PrefsUtility.applyTheme(this)

        super.onCreate(savedInstanceState)

        if (!isTaskRoot() && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)
            && getIntent().getAction() != null && getIntent().getAction() == Intent.ACTION_MAIN
        ) {
            // Workaround for issue where a new MainActivity is created despite
            // the app already running

            finish()
            return
        }

        if (!PrefsUtility.isRedditUserAgreementAccepted
            && !PrefsUtility.isRedditUserAgreementDeclined
        ) {
            launch(this, true)
            finish()
            return
        }

        val sharedPreferences = getSharedPrefs(this)
        twoPane = isTablet(this)

        setTitle(string.app_name)

        RedditAccountManager.Companion.getInstance(this).addUpdateListener(this)

        val pInfo = getInstance(this).packageInfo

        val appVersion = pInfo.versionCode

        Log.i(TAG, "[Migration] App version: " + appVersion)

        if (!sharedPreferences.contains(FeatureFlagHandler.PREF_FIRST_RUN_MESSAGE_SHOWN)) {
            Log.i(TAG, "[Migration] Showing first run message")

            FeatureFlagHandler.handleFirstInstall(sharedPreferences)

            MaterialAlertDialogBuilder(this)
                .setTitle(string.firstrun_login_title)
                .setMessage(string.firstrun_login_message)
                .setPositiveButton(
                    string.firstrun_login_button_now,
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        show(
                            this
                        )
                    })
                .setNegativeButton(string.firstrun_login_button_later, null)
                .show()

            sharedPreferences.edit()
                .putString(FeatureFlagHandler.PREF_FIRST_RUN_MESSAGE_SHOWN, "true")
                .putInt(FeatureFlagHandler.PREF_LAST_VERSION, appVersion)
                .apply()
        } else if (sharedPreferences.contains(FeatureFlagHandler.PREF_LAST_VERSION)) {
            FeatureFlagHandler.handleLegacyUpgrade(this, appVersion, pInfo.versionName)
        } else {
            Log.i(TAG, "[Migration] Last version not set.")
            sharedPreferences.edit()
                .putInt(FeatureFlagHandler.PREF_LAST_VERSION, appVersion)
                .apply()
            ChangelogDialog.Companion.newInstance().show(getSupportFragmentManager(), null)
        }

        FeatureFlagHandler.handleUpgrade(this)

        if (anyNeedRelogin(this)) {
            showMustReloginDialog(this)
        } else {
            promptForNotificationPermission(this, null)
        }

        recreateSubscriptionListener()

        doRefresh(RefreshableFragment.MAIN_RELAYOUT, false, null)

        if (savedInstanceState == null
            && PrefsUtility.pref_behaviour_skiptofrontpage()
        ) {
            onSelected(SubredditPostListURL.Companion.frontPage)
        }
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

    protected override fun onDestroy() {
        super.onDestroy()

        val listenerContext = mSubredditSubscriptionListenerContext.get()

        if (listenerContext != null) {
            listenerContext.removeListener()
        }
    }

    override fun onSelected(@MainMenuAction type: Int) {
        val username: String = RedditAccountManager.Companion.getInstance(this)
            .getDefaultAccount().username

        when (type) {
            MainMenuFragment.Companion.MENU_MENU_ACTION_FRONTPAGE -> onSelected(SubredditPostListURL.Companion.frontPage)
            MainMenuFragment.Companion.MENU_MENU_ACTION_POPULAR -> onSelected(SubredditPostListURL.Companion.popular)
            MainMenuFragment.Companion.MENU_MENU_ACTION_ALL -> onSelected(SubredditPostListURL.Companion.all)
            MainMenuFragment.Companion.MENU_MENU_ACTION_SUBMITTED -> onSelected(
                UserPostListingURL.Companion.getSubmitted(
                    username
                )
            )

            MainMenuFragment.Companion.MENU_MENU_ACTION_SUBMITTED_COMMENTS -> onLinkClicked(
                this,
                Reddit.getUri("/user/" + username + "/comments.json"),
                false
            )

            MainMenuFragment.Companion.MENU_MENU_ACTION_SAVED -> onSelected(
                UserPostListingURL.Companion.getSaved(
                    username
                )
            )

            MainMenuFragment.Companion.MENU_MENU_ACTION_HIDDEN -> onSelected(
                UserPostListingURL.Companion.getHidden(
                    username
                )
            )

            MainMenuFragment.Companion.MENU_MENU_ACTION_UPVOTED -> onSelected(
                UserPostListingURL.Companion.getLiked(
                    username
                )
            )

            MainMenuFragment.Companion.MENU_MENU_ACTION_DOWNVOTED -> onSelected(
                UserPostListingURL.Companion.getDisliked(
                    username
                )
            )

            MainMenuFragment.Companion.MENU_MENU_ACTION_PROFILE -> onLinkClicked(
                this,
                UserProfileURL(username).toUriString()
            )

            MainMenuFragment.Companion.MENU_MENU_ACTION_CUSTOM -> {
                val alertBuilder = MaterialAlertDialogBuilder(this)

                val root = getLayoutInflater().inflate(
                    R.layout.dialog_mainmenu_custom,
                    null
                )

                val destinationType = root.findViewById<Spinner>(R.id.dialog_mainmenu_custom_type)
                val editText =                     root.findViewById<AutoCompleteTextView>(R.id.dialog_mainmenu_custom_value)

                val typeReturnValues = getResources().getStringArray(
                    R.array.mainmenu_custom_destination_type_return
                )

                if (PrefsUtility.pref_menus_mainmenu_shortcutitems().contains(
                        MainMenuShortcutItems.SUBREDDIT_SEARCH
                    )
                ) {
                    var i = 0
                    while (i < typeReturnValues.size) {
                        if (typeReturnValues[i] == "user") {
                            destinationType.setSelection(i)
                            break
                        }
                        i++
                    }
                }

                val subredditHistory = RedditSubredditHistory.getSubredditsSorted(
                    RedditAccountManager.Companion.getInstance(this).getDefaultAccount()
                )

                val autocompleteAdapter = ArrayAdapter<String?>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    CollectionStream<SubredditCanonicalId?>(subredditHistory)
                        .map<String?>(MapStream.Operator { obj: SubredditCanonicalId? -> obj.displayNameLowercase })
                        .collect<ArrayList<String?>?>(ArrayList<String?>())
                )

                editText.setAdapter<ArrayAdapter<String?>?>(autocompleteAdapter)
                editText.setOnEditorActionListener(OnEditorActionListener { v: TextView?, actionId: Int, event: KeyEvent? ->
                    var handled = false
                    if (actionId == EditorInfo.IME_ACTION_GO
                        || event!!.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    ) {
                        openCustomLocation(
                            typeReturnValues,
                            destinationType,
                            editText
                        )
                        handled = true
                    }
                    handled
                })

                alertBuilder.setView(root)

                editText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        if (typeReturnValues[destinationType.getSelectedItemPosition()]
                            == "search"
                        ) {
                            return
                        }

                        val value = s.toString()
                        var type: String?=null

                        if (value.startsWith("http://") || value.startsWith("https://")) {
                            type = "url"
                        } else if (value.startsWith("/r/") || value.startsWith("r/")) {
                            type = "subreddit"
                        } else if (value.startsWith("/u/") || value.startsWith("u/")) {
                            type = "user"
                        }

                        if (type != null) {
                            var i = 0
                            while (i < typeReturnValues.size) {
                                if (typeReturnValues[i] == type) {
                                    destinationType.setSelection(i)
                                    break
                                }
                                i++
                            }
                        }
                    }

                    override fun afterTextChanged(s: Editable?) {}
                })

                destinationType.setOnItemSelectedListener(object :
                    AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        adapterView: AdapterView<*>?,
                        view: View?,
                        i: Int,
                        l: Long
                    ) {
                        val typeName: String?=typeReturnValues[destinationType.getSelectedItemPosition()]

                        if ("subreddit" == typeName) {
                            editText.setAdapter<ArrayAdapter<String?>?>(autocompleteAdapter)
                        } else {
                            editText.setAdapter(null)
                        }
                    }

                    override fun onNothingSelected(adapterView: AdapterView<*>?) {
                        editText.setAdapter(null)
                    }
                })

                alertBuilder.setPositiveButton(
                    string.dialog_go,
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        openCustomLocation(
                            typeReturnValues,
                            destinationType,
                            editText
                        )
                    })

                alertBuilder.setNegativeButton(string.dialog_cancel, null)

                val alertDialog = alertBuilder.create()
                alertDialog.getWindow()!!
                    .setSoftInputMode(
                        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                                or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
                    )
                alertDialog.show()
            }

            MainMenuFragment.Companion.MENU_MENU_ACTION_INBOX -> startActivity(
                Intent(
                    this,
                    InboxListingActivity::class.java
                )
            )

            MainMenuFragment.Companion.MENU_MENU_ACTION_SENT_MESSAGES -> {
                val intent = Intent(this, InboxListingActivity::class.java)
                intent.putExtra("inboxType", "sent")
                startActivity(intent)
            }

            MainMenuFragment.Companion.MENU_MENU_ACTION_MODMAIL -> {
                val intent = Intent(this, InboxListingActivity::class.java)
                intent.putExtra("inboxType", "modmail")
                startActivity(intent)
            }

            MainMenuFragment.Companion.MENU_MENU_ACTION_FIND_SUBREDDIT -> {
                startActivity(Intent(this, SubredditSearchActivity::class.java))
            }
        }
    }

    private fun openCustomLocation(
        typeReturnValues: Array<String>,
        destinationType: Spinner,
        editText: AutoCompleteTextView
    ) {
        val typeName = typeReturnValues[destinationType.getSelectedItemPosition()]

        when (typeName) {
            "subreddit" -> {
                val subredditInput = editText.getText()
                    .toString()
                    .trim { it <= ' ' }
                    .replace(" ", "")

                try {
                    val normalizedName: String?=RedditSubreddit.Companion.stripRPrefix(
                        subredditInput
                    )
                    val redditURL: RedditURL?=SubredditPostListURL.Companion.getSubreddit(normalizedName)
                    if (redditURL == null
                        || (redditURL.pathType()
                                != RedditURLParser.SUBREDDIT_POST_LISTING_URL)
                    ) {
                        quickToast(this, string.mainmenu_custom_invalid_name)
                    } else {
                        onSelected(redditURL.asSubredditPostListURL())
                    }
                } catch (e: InvalidSubredditNameException) {
                    quickToast(this, string.mainmenu_custom_invalid_name)
                }
            }

            "user" -> {
                val userInput = editText.getText().toString().trim { it <= ' ' }.replace(" ", "")

                if (!userInput.startsWith("/u/")
                    && !userInput.startsWith("/user/")
                ) {
                    if (userInput.startsWith("u/")
                        || userInput.startsWith("user/")
                    ) {
                        userInput = "/" + userInput
                    } else {
                        userInput = "/u/" + userInput
                    }
                }

                onLinkClicked(this, UriString(userInput))
            }

            "url" -> {
                onLinkClicked(
                    this,
                    UriString(editText.getText().toString().trim { it <= ' ' })
                )
            }

            "search" -> {
                val query = editText.getText().toString().trim { it <= ' ' }

                if (StringUtils.isEmpty(query)) {
                    quickToast(this, string.mainmenu_custom_empty_search_query)
                    break
                }

                val url: SearchPostListURL = SearchPostListURL.Companion.build(null, query)

                val intent = Intent(this, PostListingActivity::class.java)
                intent.setData(url.generateJsonUri())
                this.startActivity(intent)
            }
        }
    }

    override fun onSelected(url: PostListingURL?) {
        if (url == null) {
            return
        }

        if (twoPane) {
            postListingController = PostListingController(url, this)
            requestRefresh(RefreshableFragment.POSTS, false)
        } else {
            val intent = Intent(this, PostListingActivity::class.java)
            intent.setData(url.generateJsonUri())
            startActivityForResult(intent, 1)
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
        if (which == RefreshableFragment.MAIN_RELAYOUT) {
            mainMenuFragment = null
            postListingFragment = null
            commentListingFragment = null

            mainMenuView = null
            postListingView = null
            commentListingView = null

            if (mLeftPane != null) {
                mLeftPane!!.removeAllViews()
            }
            if (mRightPane != null) {
                mRightPane!!.removeAllViews()
            }

            twoPane = isTablet(this)

            if (twoPane) {
                val layout = getLayoutInflater().inflate(R.layout.main_double, null)
                mLeftPane = layout.findViewById<FrameLayout?>(R.id.main_left_frame)
                mRightPane = layout.findViewById<FrameLayout?>(R.id.main_right_frame)
                setBaseActivityListing(layout)
            } else {
                mLeftPane = null
                mRightPane = null
            }

            invalidateBackPressedCallback()
            invalidateOptionsMenu()
            requestRefresh(RefreshableFragment.ALL, false)

            return
        }

        if (twoPane) {
            val postContainer = (if (isMenuShown) mRightPane else mLeftPane)!!

            if (isMenuShown && (which == RefreshableFragment.ALL
                        || which == RefreshableFragment.MAIN)
            ) {
                mainMenuFragment = MainMenuFragment(this, null, force)
                mainMenuView = mainMenuFragment!!.createCombinedListingAndOverlayView()
                mLeftPane!!.removeAllViews()
                mLeftPane!!.addView(mainMenuView)
            }

            if (postListingController != null && (which == RefreshableFragment.ALL
                        || which == RefreshableFragment.POSTS)
            ) {
                if (force && postListingFragment != null) {
                    postListingFragment!!.cancel()
                }
                postListingFragment = postListingController!!.get(this, force, null)
                postListingView = postListingFragment!!.createCombinedListingAndOverlayView()
                postContainer.removeAllViews()
                postContainer.addView(postListingView)
            }

            if (commentListingController != null && (which == RefreshableFragment.ALL
                        || (which
                        == RefreshableFragment.COMMENTS))
            ) {
                commentListingFragment = commentListingController!!.get(this, force, null)
                commentListingView = commentListingFragment!!.createCombinedListingAndOverlayView()
                mRightPane!!.removeAllViews()
                mRightPane!!.addView(commentListingView)
            }
        } else {
            if (which == RefreshableFragment.ALL || which == RefreshableFragment.MAIN) {
                mainMenuFragment = MainMenuFragment(this, null, force)
                mainMenuFragment!!.setBaseActivityContent(this)
            }
        }

        invalidateOptionsMenu()
    }

    override fun baseActivityMustInterceptBack(): Boolean {
        return twoPane && !isMenuShown
    }

    override fun baseActivityOnBackPressed(): Boolean {
        if (!twoPane || isMenuShown) {
            return false
        }

        isMenuShown = true

        mainMenuFragment = MainMenuFragment(
            this,
            null,
            false
        ) // TODO preserve position
        mainMenuView = mainMenuFragment!!.createCombinedListingAndOverlayView()

        commentListingFragment = null
        commentListingView = null

        mLeftPane!!.removeAllViews()
        mRightPane!!.removeAllViews()

        mLeftPane!!.addView(mainMenuView)
        mRightPane!!.addView(postListingView)

        showBackButton(false)
        invalidateOptionsMenu()
        return true
    }

    override fun onPostCommentsSelected(post: RedditPreparedPost) {
        if (twoPane) {
            commentListingController = CommentListingController(
                PostCommentListingURL.Companion.forPostId(
                    post.src
                        .idAlone
                )
            )
            showBackButton(true)

            if (isMenuShown) {
                commentListingFragment = commentListingController!!.get(this, false, null)
                commentListingView = commentListingFragment!!.createCombinedListingAndOverlayView()

                mLeftPane!!.removeAllViews()
                mRightPane!!.removeAllViews()

                mLeftPane!!.addView(postListingView)
                mRightPane!!.addView(commentListingView)

                mainMenuFragment = null
                mainMenuView = null

                isMenuShown = false

                invalidateBackPressedCallback()
                invalidateOptionsMenu()
            } else {
                requestRefresh(RefreshableFragment.COMMENTS, false)
            }
        } else {
            onLinkClicked(
                this,
                PostCommentListingURL.Companion.forPostId(post.src.idAlone).toUriString(),
                false
            )
        }
    }

    override fun onPostSelected(post: RedditPreparedPost) {
        if (post.isSelf) {
            onPostCommentsSelected(post)
        } else {
            onLinkClicked(this, post.src.url, false, post.src.src)
        }
    }

    override fun onCreateOptionsMenu(menu : Menu): Boolean {
        val postsVisible = postListingFragment != null
        val commentsVisible = commentListingFragment != null

        val postsSortable = postListingController != null
                && postListingController!!.isSortable
        val commentsSortable = commentListingController != null
                && commentListingController!!.isSortable

        val isFrontPage = postListingController != null && postListingController!!
            .isFrontPage

        val user: RedditAccount = RedditAccountManager.Companion.getInstance(this)
            .getDefaultAccount()
        val subredditSubscriptionState: SubredditSubscriptionState?

        val subredditSubscriptionManager: RedditSubredditSubscriptionManager=            RedditSubredditSubscriptionManager.Companion.getSingleton(this, user)

        var subredditPinState: Boolean?=null
        var subredditBlockedState: Boolean?=null

        if (postsVisible
            && !user.isAnonymous && postListingController!!.isSubreddit
            && subredditSubscriptionManager.areSubscriptionsReady()
            && postListingFragment != null && postListingFragment!!.subreddit != null
        ) {
            subredditSubscriptionState = subredditSubscriptionManager.getSubscriptionState(
                postListingController!!.subredditCanonicalName()
            )
        } else {
            subredditSubscriptionState = null
        }

        if (postsVisible
            && postListingController!!.isSubreddit
            && postListingFragment != null && postListingFragment!!.subreddit != null
        ) {
            try {
                subredditPinState = PrefsUtility.pref_pinned_subreddits_check(
                    postListingFragment!!.subreddit!!.canonicalId
                )

                subredditBlockedState = PrefsUtility.pref_blocked_subreddits_check(
                    postListingFragment!!.subreddit!!.canonicalId
                )
            } catch (e: InvalidSubredditNameException) {
                subredditPinState = null
                subredditBlockedState = null
            }
        }

        val subredditDescription = if (postListingFragment != null
            && postListingFragment!!.subreddit != null
        )
            postListingFragment!!.subreddit!!.description_html
        else
            null

        OptionsMenuUtility.prepare<MainActivity?>(
            this,
            menu,
            isMenuShown,
            postsVisible,
            commentsVisible,
            false,
            false,
            false,
            postsSortable,
            commentsSortable,
            isFrontPage,
            subredditSubscriptionState,
            postsVisible
                    && subredditDescription != null && !subredditDescription.isEmpty(),
            true,
            subredditPinState,
            subredditBlockedState
        )

        if (commentListingFragment != null) {
            commentListingFragment!!.onCreateOptionsMenu(menu)
        }

        return true
    }

    override fun onRefreshComments() {
        commentListingController!!.setSession(null)
        requestRefresh(RefreshableFragment.COMMENTS, true)
    }

    override fun onPastComments() {
        val sessionListDialog: SessionListDialog = SessionListDialog.Companion.newInstance(
            commentListingController!!.uri,
            commentListingController!!.session,
            SessionChangeType.COMMENTS
        )
        sessionListDialog.show(getSupportFragmentManager(), null)
    }

    override fun onSortSelected(order: PostCommentSort?) {
        commentListingController!!.setSort(order)
        requestRefresh(RefreshableFragment.COMMENTS, false)
    }

    override fun onSortSelected(order: UserCommentSort?) {
        commentListingController!!.setSort(order)
        requestRefresh(RefreshableFragment.COMMENTS, false)
    }

    override fun onSearchComments() {
        DialogUtils.showSearchDialog(
            this,
            string.action_search_comments,
            OnSearchListener { query: String? ->
                val searchIntent = Intent(this, CommentListingActivity::class.java)
                searchIntent.setData(commentListingController!!.uri)
                searchIntent.putExtra(
                    CommentListingActivity.EXTRA_SEARCH_STRING,
                    query
                )
                startActivity(searchIntent)
            })
    }

    override fun onRefreshPosts() {
        postListingController!!.setSession(null)
        requestRefresh(RefreshableFragment.POSTS, true)
    }

    override fun onPastPosts() {
        val sessionListDialog: SessionListDialog = SessionListDialog.Companion.newInstance(
            postListingController!!.uri,
            postListingController!!.session,
            SessionChangeType.POSTS
        )
        sessionListDialog.show(getSupportFragmentManager(), null)
    }

    override fun onSubmitPost() {
        val intent = Intent(this, PostSubmitActivity::class.java)
        if (postListingController!!.isSubreddit) {
            intent.putExtra(
                "subreddit",
                postListingController!!.subredditCanonicalName().toString()
            )
        }
        startActivity(intent)
    }

    override fun onSortSelected(order: PostSort?) {
        postListingController!!.setSort(order)
        requestRefresh(RefreshableFragment.POSTS, false)
    }

    override fun onSearchPosts() {
        PostListingActivity.Companion.onSearchPosts(postListingController, this)
    }

    override fun onSubscribe() {
        if (postListingFragment != null) {
            postListingFragment!!.onSubscribe()
        }
    }

    override fun onUnsubscribe() {
        if (postListingFragment != null) {
            postListingFragment!!.onUnsubscribe()
        }
    }

    override fun onSidebar() {
        postListingFragment!!.subreddit!!.showSidebarActivity(this)
    }

    override fun onPin() {
        if (postListingFragment == null) {
            return
        }

        try {
            PrefsUtility.pref_pinned_subreddits_add(
                this,
                postListingFragment!!.subreddit!!.canonicalId
            )
        } catch (e: InvalidSubredditNameException) {
            throw RuntimeException(e)
        }

        invalidateOptionsMenu()
    }

    override fun onUnpin() {
        if (postListingFragment == null) {
            return
        }

        try {
            PrefsUtility.pref_pinned_subreddits_remove(
                this,
                postListingFragment!!.subreddit!!.canonicalId
            )
        } catch (e: InvalidSubredditNameException) {
            throw RuntimeException(e)
        }

        invalidateOptionsMenu()
    }

    override fun onBlock() {
        if (postListingFragment == null) {
            return
        }

        try {
            PrefsUtility.pref_blocked_subreddits_add(
                this,
                postListingFragment!!.subreddit!!.canonicalId
            )
        } catch (e: InvalidSubredditNameException) {
            throw RuntimeException(e)
        }

        invalidateOptionsMenu()
    }

    override fun onUnblock() {
        if (postListingFragment == null) {
            return
        }

        try {
            PrefsUtility.pref_blocked_subreddits_remove(
                this,
                postListingFragment!!.subreddit!!.canonicalId
            )
        } catch (e: InvalidSubredditNameException) {
            throw RuntimeException(e)
        }

        invalidateOptionsMenu()
    }

    override fun onRefreshSubreddits() {
        requestRefresh(RefreshableFragment.MAIN, true)
    }

    protected override fun onResume() {
        super.onResume()

        if (mainMenuFragment != null) {
            mainMenuFragment!!.onUpdateAnnouncement()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (commentListingFragment != null) {
            if (commentListingFragment!!.onOptionsItemSelected(item)) {
                return true
            }
        }

        when (item.getItemId()) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onSessionSelected(session: UUID?, type: SessionChangeType) {
        when (type) {
            SessionChangeType.POSTS -> {
                postListingController!!.setSession(session)
                requestRefresh(RefreshableFragment.POSTS, false)
            }

            SessionChangeType.COMMENTS -> {
                commentListingController!!.setSession(session)
                requestRefresh(RefreshableFragment.COMMENTS, false)
            }
        }
    }

    override fun onSessionRefreshSelected(type: SessionChangeType) {
        when (type) {
            SessionChangeType.POSTS -> onRefreshPosts()
            SessionChangeType.COMMENTS -> onRefreshComments()
        }
    }

    override fun onSessionChanged(
        session: UUID?,
        type: SessionChangeType,
        timestamp: TimestampUTC?
    ) {
        when (type) {
            SessionChangeType.POSTS -> if (postListingController != null) {
                postListingController!!.setSession(session)
            }

            SessionChangeType.COMMENTS -> if (commentListingController != null) {
                commentListingController!!.setSession(session)
            }
        }
    }

    override fun onSubredditSubscriptionListUpdated(
        subredditSubscriptionManager: RedditSubredditSubscriptionManager
    ) {
        postInvalidateOptionsMenu()
    }

    override fun onSubredditSubscriptionAttempted(
        subredditSubscriptionManager: RedditSubredditSubscriptionManager
    ) {
        postInvalidateOptionsMenu()
    }

    override fun onSubredditUnsubscriptionAttempted(
        subredditSubscriptionManager: RedditSubredditSubscriptionManager
    ) {
        postInvalidateOptionsMenu()
    }

    private fun postInvalidateOptionsMenu() {
        runOnUiThread(Runnable { this.invalidateOptionsMenu() })
    }

    private fun showBackButton(isVisible: Boolean) {
        configBackButton(
            isVisible,
            View.OnClickListener { v: View? -> onBackPressedDispatcher.onBackPressed() })
    }

    override val postSort: PostSort?
        get() {
        if (postListingController == null) {
            return null
        }

        return postListingController!!.sort
        }

    override val commentSort: OptionsMenuUtility.Sort?
        get() {
        if (commentListingController == null) {
            return null
        }

        return commentListingController!!.sort
        }

    override val suggestedCommentSort: PostCommentSort?
        get() {
        if (commentListingFragment == null || commentListingFragment!!.post == null) {
            return null
        }

        return commentListingFragment!!.post.src.suggestedCommentSort
        }

    companion object {
        private const val TAG = "MainActivity"
    }
}
