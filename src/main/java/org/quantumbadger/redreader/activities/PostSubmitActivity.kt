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
import android.os.Bundle
import android.util.Log
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.common.General.quickToast
import org.quantumbadger.redreader.common.General.showResultDialog
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.fragments.postsubmit.PostSubmitContentFragment
import org.quantumbadger.redreader.fragments.postsubmit.PostSubmitSubredditSelectionFragment
import org.quantumbadger.redreader.reddit.things.InvalidSubredditNameException
import org.quantumbadger.redreader.reddit.things.SubredditCanonicalId

class PostSubmitActivity : ViewsBaseActivity(), PostSubmitSubredditSelectionFragment.Listener,
    PostSubmitContentFragment.Listener {
    private var mIntentUrl: String?=null

    protected override fun onCreate(savedInstanceState: Bundle?) {
        PrefsUtility.applyTheme(this)

        super.onCreate(savedInstanceState)

        var intentSubreddit: SubredditCanonicalId?=null

        val intent = getIntent()

        if (intent != null) {
            val subreddit = intent.getStringExtra("subreddit")

            if (subreddit != null) {
                try {
                    intentSubreddit = SubredditCanonicalId(subreddit)
                } catch (e: InvalidSubredditNameException) {
                    Log.e(TAG, "Invalid subreddit name", e)
                }
            }

            if (Intent.ACTION_SEND.equals(intent.getAction(), ignoreCase = true)
                && intent.hasExtra(Intent.EXTRA_TEXT)
            ) {
                mIntentUrl = intent.getStringExtra(Intent.EXTRA_TEXT)
            }
        }

        setBaseActivityListing(R.layout.single_fragment_layout)

        getSupportFragmentManager().beginTransaction()
            .setReorderingAllowed(false)
            .add(
                R.id.single_fragment_container,
                PostSubmitSubredditSelectionFragment::class.java,
                PostSubmitSubredditSelectionFragment.Args(intentSubreddit).toBundle()
            )
            .commit()
    }

    override fun onSubredditSelected(
        username: String,
        subreddit: SubredditCanonicalId
    ) {
        getSupportFragmentManager().beginTransaction()
            .setReorderingAllowed(false)
            .replace(
                R.id.single_fragment_container,
                PostSubmitContentFragment::class.java,
                PostSubmitContentFragment.Args(
                    username,
                    subreddit,
                    mIntentUrl
                ).toBundle()
            )
            .addToBackStack("Subreddit selected")
            .commit()
    }

    override fun onNotLoggedIn() {
        quickToast(this, string.error_toast_notloggedin)
        finish()
    }

    override fun onContentFragmentSubmissionSuccess(redirectUrl: UriString?) {
        if (redirectUrl != null) {
            onLinkClicked(this, redirectUrl)
        }

        finish()
    }

    override fun onContentFragmentSubredditDoesNotExist() {
        onBackPressedDispatcher.onBackPressed()

        val applicationContext = getApplicationContext()

        showResultDialog(
            this, RRError(
                applicationContext.getString(string.error_subreddit_does_not_exist_title),
                applicationContext.getString(string.error_subreddit_does_not_exist_message),
                false,
                RuntimeException()
            )
        )
    }

    override fun onContentFragmentSubredditPermissionDenied() {
        onBackPressedDispatcher.onBackPressed()

        val applicationContext = getApplicationContext()

        showResultDialog(
            this, RRError(
                applicationContext.getString(
                    string.error_subreddit_info_permission_denied_title
                ),
                applicationContext.getString(
                    string.error_subreddit_info_permission_denied_message
                ),
                false,
                RuntimeException()
            )
        )
    }

    override fun onContentFragmentFlairRequestError(error: RRError) {
        onBackPressedDispatcher.onBackPressed()
        showResultDialog(this, error)
    }

    companion object {
        private const val TAG = "PostSubmitActivity"
    }
}
