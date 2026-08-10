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
package org.quantumbadger.redreader.views

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.button.MaterialButton
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.activities.PostListingActivity
import org.quantumbadger.redreader.common.EventListenerSet
import org.quantumbadger.redreader.common.General.findViewById
import org.quantumbadger.redreader.common.LinkHandler
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.reddit.url.SearchPostListURL
import java.util.Objects

class SubredditSearchQuickLinks @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?=null,
    defStyleAttr: Int = 0
) : FlexboxLayout(context, attrs, defStyleAttr) {
    private var mActivity: AppCompatActivity?=null

    private var mBinding: EventListenerSet<String?>? = null
    private var mBindingListener: EventListenerSet.Listener<String?>? = null

    private var mButtonSubreddit: MaterialButton?=null
    private var mButtonUser: MaterialButton?=null
    private var mButtonUrl: MaterialButton?=null
    private var mButtonSearch: MaterialButton?=null

    override fun onFinishInflate() {
        super.onFinishInflate()

        mButtonSubreddit = findViewById<MaterialButton>(R.id.button_go_to_subreddit)
        mButtonUser = findViewById<MaterialButton>(R.id.button_go_to_user)
        mButtonUrl = findViewById<MaterialButton>(R.id.button_go_to_url)
        mButtonSearch = findViewById<MaterialButton>(R.id.button_go_to_search)
    }

    fun bind(
        activity: AppCompatActivity,
        querySource: EventListenerSet<String?>
    ) {
        mActivity = activity

        if (mBinding != null) {
            throw RuntimeException("Search view already bound")
        }

        mBinding = querySource

        doBind()
    }

    private fun doBind() {
        if (mBinding != null) {
            mBindingListener = EventListenerSet.Listener { query: E? -> this.update(query) }
            update(mBinding!!.register(mBindingListener))
        }
    }

    private fun doUnbind() {
        if (mBinding != null && mBindingListener != null) {
            mBinding!!.unregister(mBindingListener)
            mBindingListener = null
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        doBind()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        doUnbind()
    }

    private fun update(query: String?) {
        var query = query
        if (query != null) {
            query = query.trim { it <= ' ' }
        }

        if (query == null || query.isEmpty()) {
            mButtonSubreddit!!.setText(string.find_location_button_goto_subreddit)

            mButtonSubreddit!!.setEnabled(false)
            mButtonUser!!.setEnabled(false)
            mButtonUrl!!.setEnabled(false)
            mButtonSearch!!.setEnabled(false)

            mButtonSubreddit!!.setVisibility(VISIBLE)
            mButtonUser!!.setVisibility(VISIBLE)
            mButtonUrl!!.setVisibility(VISIBLE)
            mButtonSearch!!.setVisibility(VISIBLE)
        } else {
            val queryProcessed = ProcessedQuery(query)

            if (queryProcessed.querySubreddit != null) {
                mButtonSubreddit!!.setVisibility(VISIBLE)

                val subredditPrefixed = "/r/" + queryProcessed.querySubreddit
                mButtonSubreddit!!.setText(subredditPrefixed)

                mButtonSubreddit!!.setOnClickListener(
                    OnClickListener { view: View? ->
                        LinkHandler.onLinkClicked(
                            mActivity!!,
                            UriString(subredditPrefixed)
                        )
                    })
            } else {
                mButtonSubreddit!!.setVisibility(GONE)
            }

            if (queryProcessed.queryUser != null) {
                mButtonUser!!.setVisibility(VISIBLE)

                mButtonUser!!.setOnClickListener(OnClickListener { view: View? ->
                    LinkHandler.onLinkClicked(
                        mActivity!!,
                        UriString("/u/" + queryProcessed.queryUser)
                    )
                })
            } else {
                mButtonUser!!.setVisibility(GONE)
            }

            if (queryProcessed.queryUrl != null) {
                mButtonUrl!!.setVisibility(VISIBLE)

                mButtonUrl!!.setOnClickListener(OnClickListener { view: View? ->
                    LinkHandler.onLinkClicked(
                        mActivity!!,
                        queryProcessed.queryUrl
                    )
                })
            } else {
                mButtonUrl!!.setVisibility(GONE)
            }

            mButtonSearch!!.setOnClickListener(OnClickListener { view: View? ->
                val url: SearchPostListURL=                    SearchPostListURL.Companion.build(null, queryProcessed.querySearch)
                val intent = Intent(mActivity, PostListingActivity::class.java)
                intent.setData(url.generateJsonUri())
                mActivity!!.startActivity(intent)
            })

            mButtonSubreddit!!.setEnabled(true)
            mButtonUser!!.setEnabled(true)
            mButtonUrl!!.setEnabled(true)
            mButtonSearch!!.setEnabled(true)
        }
    }

    private class ProcessedQuery(query: String) {
        val querySubreddit: String?
        val queryUser: String?
        val queryUrl: UriString?
        val querySearch: String?

        init {
            querySearch = query

            val startsWithSlashRSlash = query.startsWith("/r/")
            val startsWithRSlash = query.startsWith("r/")

            val startsWithSlashUSlash = query.startsWith("/u/")
            val startsWithUSlash = query.startsWith("u/")

            if (query.contains("://")) {
                querySubreddit = null
                queryUser = null
                queryUrl = UriString(query)
            } else if (startsWithSlashRSlash || startsWithRSlash) {
                if (startsWithSlashRSlash) {
                    querySubreddit = query.substring(3)
                } else {
                    querySubreddit = query.substring(2)
                }

                queryUser = null
                queryUrl = null
            } else if (startsWithSlashUSlash || startsWithUSlash) {
                if (startsWithSlashUSlash) {
                    queryUser = query.substring(3)
                } else {
                    queryUser = query.substring(2)
                }

                querySubreddit = null
                queryUrl = null
            } else if (query.startsWith("/")) {
                querySubreddit = null
                queryUser = null
                queryUrl = UriString("https://reddit.com" + query)
            } else {
                querySubreddit = query.replace("[ \t]+".toRegex(), "_")
                queryUser = querySubreddit
                queryUrl = UriString("https://" + query)
            }
        }
    }
}
