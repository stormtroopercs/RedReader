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

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.account.RedditAccountChangeListener
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.OptionsMenuUtility.OptionsMenuCommentsListener
import org.quantumbadger.redreader.activities.SessionChangeListener.SessionChangeType
import org.quantumbadger.redreader.common.DialogUtils
import org.quantumbadger.redreader.common.DialogUtils.OnSearchListener
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.fragments.CommentListingFragment
import org.quantumbadger.redreader.fragments.SessionListDialog
import org.quantumbadger.redreader.listingcontrollers.CommentListingController
import org.quantumbadger.redreader.reddit.PostCommentSort
import org.quantumbadger.redreader.reddit.UserCommentSort
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL
import org.quantumbadger.redreader.reddit.url.RedditURLParser
import org.quantumbadger.redreader.views.RedditPostView.PostSelectionListener
import java.util.UUID

class CommentListingActivity : RefreshableActivity(), RedditAccountChangeListener,
    OptionsMenuCommentsListener, PostSelectionListener, SessionChangeListener {
    private var controller: CommentListingController?=null

    private var mFragment: CommentListingFragment?=null

    public override fun onCreate(savedInstanceState: Bundle?) {
        PrefsUtility.applyTheme(this)

        super.onCreate(savedInstanceState)

        setTitle(getString(R.string.app_name))

        RedditAccountManager.getInstance(this).addUpdateListener(this)

        if (getIntent() != null) {
            val intent = getIntent()

            val url = intent.getDataString()
            val searchString = intent.getStringExtra(EXTRA_SEARCH_STRING)
            controller = CommentListingController(
                RedditURLParser.parseProbableCommentListing(Uri.parse(url))
            )
            controller!!.setSearchString(searchString)

            var fragmentSavedInstanceState: Bundle?=null

            if (savedInstanceState != null) {
                if (savedInstanceState.containsKey(SAVEDSTATE_SESSION)) {
                    controller!!.session = 
                        UUID.fromString(
                            savedInstanceState.getString(
                                SAVEDSTATE_SESSION
                            )
                        )
                    
                }

                if (savedInstanceState.containsKey(SAVEDSTATE_SORT)) {
                    if (savedInstanceState.getBoolean(SAVEDSTATE_SORT_IS_USER)) {
                        controller!!.setSort(
                            UserCommentSort.valueOf(
                                savedInstanceState.getString(SAVEDSTATE_SORT)!!
                            )
                        )
                    } else {
                        controller!!.setSort(
                            PostCommentSort.valueOf(
                                savedInstanceState.getString(SAVEDSTATE_SORT)!!
                            )
                        )
                    }
                }

                if (savedInstanceState.containsKey(SAVEDSTATE_FRAGMENT)) {
                    fragmentSavedInstanceState = savedInstanceState.getBundle(
                        SAVEDSTATE_FRAGMENT
                    )
                }
            }

            doRefresh(RefreshableFragment.COMMENTS, false, fragmentSavedInstanceState)
        } else {
            throw RuntimeException("Nothing to show!")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val session = controller!!.session
        if (session != null) {
            outState.putString(SAVEDSTATE_SESSION, session.toString())
        }

        val sort = controller!!.sort
        if (sort != null) {
            outState.putBoolean(SAVEDSTATE_SORT_IS_USER, controller!!.isUserCommentListing)
            outState.putString(SAVEDSTATE_SORT, sort.name)
        }

        if (mFragment != null) {
            outState.putBundle(SAVEDSTATE_FRAGMENT, mFragment!!.onSaveInstanceState())
        }
    }

    override fun onCreateOptionsMenu(menu : Menu): Boolean {
        OptionsMenuUtility.prepare<CommentListingActivity?>(
            this,
            menu,
            false,
            false,
            true,
            false,
            false,
            controller!!.isUserCommentListing,
            false,
            controller!!.isSortable,
            false,
            null,
            false,
            true,
            null,
            null
        )

        if (mFragment != null) {
            mFragment!!.onCreateOptionsMenu(menu)
        }

        return true
    }

    override fun onRedditAccountChanged() {
        requestRefresh(RefreshableFragment.ALL, false)
    }

    override fun doRefresh(
        which: RefreshableFragment?,
        force: Boolean,
        savedInstanceState: Bundle?
    ) {
        mFragment = controller!!.get(this, force, savedInstanceState)
        mFragment!!.setBaseActivityContent(this)

        setTitle(controller!!.commentListingUrl.humanReadableName(this, false))
        invalidateOptionsMenu()
    }

    override fun onRefreshComments() {
        controller!!.setSession(null)
        requestRefresh(RefreshableFragment.COMMENTS, true)
    }

    override fun onPastComments() {
        val sessionListDialog = SessionListDialog.newInstance(
            controller!!.uri,
            controller!!.session,
            SessionChangeType.COMMENTS
        )
        sessionListDialog.show(getSupportFragmentManager(), null)
    }

    override fun onSortSelected(order: PostCommentSort?) {
        controller!!.setSort(order)
        requestRefresh(RefreshableFragment.COMMENTS, false)
    }

    override fun onSortSelected(order: UserCommentSort?) {
        controller!!.setSort(order)
        requestRefresh(RefreshableFragment.COMMENTS, false)
    }

    override fun onSearchComments() {
        DialogUtils.showSearchDialog(this, OnSearchListener { query: String? ->
            val searchIntent = getIntent()
            searchIntent.putExtra(EXTRA_SEARCH_STRING, query)
            startActivity(searchIntent)
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mFragment != null) {
            if (mFragment!!.onOptionsItemSelected(item)) {
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onSessionRefreshSelected(type: SessionChangeType?) {
        onRefreshComments()
    }

    override fun onSessionSelected(session: UUID?, type: SessionChangeType?) {
        controller!!.setSession(session)
        requestRefresh(RefreshableFragment.COMMENTS, false)
    }

    override fun onSessionChanged(
        session: UUID?,
        type: SessionChangeType,
        timestamp: TimestampUTC?
    ) {
        Log.i(
            TAG,
            type.name + " session changed to " + (if (session != null)
                session.toString()
            else
                "<null>")
        )
        controller!!.setSession(session)
    }

    override fun onPostSelected(post: RedditPreparedPost) {
        onLinkClicked(this, post.src.url, false, post.src.src)
    }

    override fun onPostCommentsSelected(post: RedditPreparedPost) {
        onLinkClicked(
            this,
            PostCommentListingURL.forPostId(post.src.idAlone).toUriString(),
            false
        )
    }

    override fun baseActivityAllowToolbarHideOnScroll(): Boolean {
        return true
    }

    override val commentSort: OptionsMenuUtility.Sort? get() = controller!!.sort

    override val suggestedCommentSort: PostCommentSort?
        get() {
        if (mFragment == null || mFragment!!.post == null) {
            return null
        }

        return mFragment!!.post.src.suggestedCommentSort
        }

    companion object {
        private const val TAG = "CommentListingActivity"

        const val EXTRA_SEARCH_STRING: String = "cla_search_string"

        private const val SAVEDSTATE_SESSION = "cla_session"
        private const val SAVEDSTATE_SORT = "cla_sort"
        private const val SAVEDSTATE_SORT_IS_USER = "cla_sort_user"
        private const val SAVEDSTATE_FRAGMENT = "cla_fragment"
    }
}
