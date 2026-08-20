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

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.account.RedditAccountChangeListener
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.OptionsMenuUtility.OptionsMenuCommentsListener
import org.quantumbadger.redreader.common.DialogUtils
import org.quantumbadger.redreader.common.DialogUtils.OnSearchListener
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.fragments.CommentListingFragment
import org.quantumbadger.redreader.reddit.PostCommentSort
import org.quantumbadger.redreader.reddit.UserCommentSort
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL
import org.quantumbadger.redreader.views.RedditPostView.PostSelectionListener

class MoreCommentsListingActivity : RefreshableActivity(), RedditAccountChangeListener,
    OptionsMenuCommentsListener, PostSelectionListener {
    private val mUrls = ArrayList<RedditURL>(32)

    private var mFragment: CommentListingFragment?=null

    private var mSearchString: String?=null

    override fun baseActivityAllowToolbarHideOnScroll(): Boolean {
        return true
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        PrefsUtility.applyTheme(this)

        super.onCreate(savedInstanceState)

        setTitle(string.app_name)

        // TODO load from savedInstanceState
        val layout = getLayoutInflater().inflate(R.layout.main_single, null)
        setBaseActivityListing(layout)

        RedditAccountManager.Companion.getInstance(this).addUpdateListener(this)

        if (getIntent() != null) {
            val intent = getIntent()
            mSearchString = intent.getStringExtra(EXTRA_SEARCH_STRING)

            val commentIds = intent.getStringArrayListExtra(
                "commentIds"
            )
            val postId = intent.getStringExtra("postId")

            for (commentId in commentIds!!) {
                mUrls.add(PostCommentListingURL.Companion.forPostId(postId).commentId(commentId))
            }

            doRefresh(RefreshableFragment.COMMENTS, false, null)
        } else {
            throw RuntimeException("Nothing to show! (should load from bundle)") // TODO
        }
    }

    // TODO save instance state
    // @Override
    // protected void onSaveInstanceState(final Bundle outState) {
    // 	super.onSaveInstanceState(outState);
    // }
    override fun onCreateOptionsMenu(menu : Menu): Boolean {
        OptionsMenuUtility.prepare<MoreCommentsListingActivity?>(
            this,
            menu,
            false,
            false,
            true,
            false,
            false,
            false,
            false,
            false,
            false,
            null,
            false,
            false,
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
        mFragment = CommentListingFragment(
            this,
            savedInstanceState,
            mUrls,
            null,
            mSearchString,
            force
        )

        mFragment!!.setBaseActivityContent(this)

        setTitle("More Comments")
    }

    override fun onRefreshComments() {
        requestRefresh(RefreshableFragment.COMMENTS, true)
    }

    override fun onPastComments() {
    }

    override fun onSortSelected(order: PostCommentSort?) {
    }

    override fun onSortSelected(order: UserCommentSort?) {
    }

    override fun onSearchComments() {
        DialogUtils.showSearchDialog(this, OnSearchListener { query: String? ->
            val searchIntent = getIntent()
            searchIntent.putExtra(EXTRA_SEARCH_STRING, query)
            startActivity(searchIntent)
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mFragment != null && mFragment!!.onOptionsItemSelected(item)) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPostSelected(post: RedditPreparedPost) {
        onLinkClicked(this, post.src.url, false, post.src.src)
    }

    override fun onPostCommentsSelected(post: RedditPreparedPost) {
        onLinkClicked(
            this,
            PostCommentListingURL.Companion.forPostId(post.src.idAlone).toUriString(),
            false
        )
    }

    override val commentSort: OptionsMenuUtility.Sort? get() = null

    override val suggestedCommentSort: PostCommentSort? get() = null

    companion object {
        private const val EXTRA_SEARCH_STRING = "mcla_search_string"
    }
}
