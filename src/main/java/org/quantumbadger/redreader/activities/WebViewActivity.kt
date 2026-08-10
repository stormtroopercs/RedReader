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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.IntentCompat
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.account.RedditAccount.equals
import org.quantumbadger.redreader.activities.BugReportActivity.Companion.handleGlobalError
import org.quantumbadger.redreader.common.General.quickToast
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.common.LinkHandler.shareText
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.fragments.WebViewFragment
import org.quantumbadger.redreader.reddit.kthings.RedditPost
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL
import org.quantumbadger.redreader.views.RedditPostView.PostSelectionListener

class WebViewActivity : ViewsBaseActivity(), PostSelectionListener {
    private var webView: WebViewFragment?=null
    private var mPost: RedditPost?=null

    public override fun onCreate(savedInstanceState: Bundle?) {
        PrefsUtility.applyTheme(this)

        super.onCreate(savedInstanceState)

        val intent = getIntent()

        val url = IntentCompat.getParcelableExtra<UriString?>(intent, "url", UriString::class.java)
        mPost = IntentCompat.getParcelableExtra<RedditPost?>(intent, "post", RedditPost::class.java)

        if (url == null) {
            handleGlobalError(this, "No URL")
        }

        webView = WebViewFragment.Companion.newInstance(url, mPost)

        setBaseActivityListing(View.inflate(this, R.layout.main_single, null))

        getSupportFragmentManager().beginTransaction()
            .add(R.id.main_single_frame, webView!!)
            .commit()
    }

    override fun baseActivityMustInterceptBack(): Boolean {
        // Always intercept, as the WebView may need to navigate back through
        // its history. This is deliberately not conditional on canGoBack(), as
        // that would have to be rechecked on every navigation (including ones
        // the page makes itself via the history API), and missing one would
        // close the browser instead of going back.
        return true
    }

    override fun baseActivityOnBackPressed(): Boolean {
        return webView!!.onBackButtonPressed()
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val currentUrl = webView!!.getCurrentUrl()

        when (item.getItemId()) {
            VIEW_IN_BROWSER -> {
                if (currentUrl != null) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setData(currentUrl.toUri())
                        startActivity(intent)
                        finish() //to clear from backstack
                    } catch (e: Exception) {
                        Toast.makeText(
                            this,
                            "Error: could not launch browser.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                return true
            }

            CLEAR_CACHE -> {
                webView!!.clearCache()
                Toast.makeText(
                    this,
                    string.web_view_clear_cache_success_toast,
                    Toast.LENGTH_LONG
                ).show()
                return true
            }

            USE_HTTPS -> {
                if (currentUrl != null) {
                    if (currentUrl.value.startsWith("https://")) {
                        quickToast(this, string.webview_https_already)
                        return true
                    }

                    if (!currentUrl.value.startsWith("http://")) {
                        quickToast(this, string.webview_https_unknownprotocol)
                        return true
                    }

                    onLinkClicked(
                        this,
                        UriString(currentUrl.value.replace("http://", "https://")),
                        true,
                        mPost
                    )
                    return true
                }

                if (currentUrl != null) {
                    shareText(
                        this,
                        if (mPost != null) mPost!!.title!!.decoded else null,
                        currentUrl.value
                    )
                }
                return true
            }

            SHARE -> {
                if (currentUrl != null) {
                    shareText(
                        this,
                        if (mPost != null) mPost!!.title!!.decoded else null,
                        currentUrl.value
                    )
                }
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, VIEW_IN_BROWSER, 0, string.web_view_open_browser)
        menu.add(0, CLEAR_CACHE, 1, string.web_view_clear_cache)
        menu.add(0, USE_HTTPS, 2, string.webview_use_https)
        menu.add(0, SHARE, 3, string.action_share)
        return super.onCreateOptionsMenu(menu)
    }

    val currentUrl: UriString?
        get() = webView!!.getCurrentUrl()

    companion object {
        const val VIEW_IN_BROWSER: Int = 10
        const val CLEAR_CACHE: Int = 20
        const val USE_HTTPS: Int = 30
        const val SHARE: Int = 40
    }
}
