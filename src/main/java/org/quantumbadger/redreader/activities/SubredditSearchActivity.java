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

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.RedReader.Companion.getInstance
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.BugReportActivity.Companion.handleGlobalError
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewAdapter
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewItemLoadingSpinner
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewItemRRError
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.EventListenerSet
import org.quantumbadger.redreader.common.FunctionOneArgNoReturn
import org.quantumbadger.redreader.common.FunctionOneArgWithReturn
import org.quantumbadger.redreader.common.General.checkThisIsUIThread
import org.quantumbadger.redreader.common.GenerationalCache
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.common.ThreadCheckedVar
import org.quantumbadger.redreader.common.collections.CollectionStream
import org.quantumbadger.redreader.common.collections.MapStream
import org.quantumbadger.redreader.reddit.APIResponseHandler.ValueResponseHandler
import org.quantumbadger.redreader.reddit.RedditAPI
import org.quantumbadger.redreader.reddit.RedditAPI.SubredditListResponse
import org.quantumbadger.redreader.reddit.SubredditDetails
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager.ListenerContext
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager.SubredditSubscriptionStateChangeListener
import org.quantumbadger.redreader.reddit.things.RedditSubreddit
import org.quantumbadger.redreader.reddit.things.SubredditCanonicalId
import org.quantumbadger.redreader.viewholders.SubredditItemViewHolder
import org.quantumbadger.redreader.views.SubredditSearchQuickLinks
import java.lang.Boolean
import java.util.Collections
import kotlin.Comparator
import kotlin.String
import kotlin.Throwable
import kotlin.run

class SubredditSearchActivity : ViewsBaseActivity(), SubredditSubscriptionStateChangeListener {
    private var mSubredditSubscriptionManager: RedditSubredditSubscriptionManager? = null

    private val mSearchView = ThreadCheckedVar<SearchView?>(null)

    private val mSubscriptions =
        ThreadCheckedVar<Optional<ArrayList<SubredditDetails?>?>?>(Optional.Companion.empty<ArrayList<SubredditDetails?>?>())

    private val mQueriesPending = ThreadCheckedVar<HashSet<String?>?>(HashSet<String?>())

    private val mSubscriptionListPending = ThreadCheckedVar<Boolean?>(false)

    private val mQueryResults =
        ThreadCheckedVar<HashMap<String?, ArrayList<SubredditDetails>?>?>(HashMap<String?, ArrayList<SubredditDetails?>?>())

    private val mSubredditItemCache: GenerationalCache<SubredditDetails?, SubredditItem?> =
        GenerationalCache<SubredditDetails?, SubredditItem?>(FunctionOneArgWithReturn { subreddit: Param? ->
            SubredditItem(subreddit)
        })

    private var mSubredditSubscriptionListenerContext: Optional<ListenerContext?> =
        Optional.Companion.empty<ListenerContext?>()

    private var mRecyclerViewLayout: LinearLayoutManager? = null
    private var mLoadingItem: GroupedRecyclerViewItemLoadingSpinner? = null

    private val mSubscriptionsErrorItem =
        ThreadCheckedVar<Optional<GroupedRecyclerViewItemRRError?>?>(Optional.Companion.empty<GroupedRecyclerViewItemRRError?>())

    private val mQueryErrorItem =
        ThreadCheckedVar<Optional<GroupedRecyclerViewItemRRError?>?>(Optional.Companion.empty<GroupedRecyclerViewItemRRError?>())

    private var mRecyclerViewAdapter: GroupedRecyclerViewAdapter? = null
    override fun onSubredditSubscriptionListUpdated(
        subredditSubscriptionManager: RedditSubredditSubscriptionManager?
    ) {
        AndroidCommon.runOnUiThread(Runnable { mSubscriptions.set(Optional.Companion.empty<ArrayList<SubredditDetails?>?>()) })
    }

    override fun onSubredditSubscriptionAttempted(
        subredditSubscriptionManager: RedditSubredditSubscriptionManager?
    ) {
        // Ignore
    }

    override fun onSubredditUnsubscriptionAttempted(
        subredditSubscriptionManager: RedditSubredditSubscriptionManager?
    ) {
        // Ignore
    }

    private inner class SubredditItem
        (private val mSubreddit: SubredditDetails) :
        GroupedRecyclerViewAdapter.Item<SubredditItemViewHolder?>() {
        override fun getViewType(): Class<RedditSubreddit?> {
            return RedditSubreddit::class.java
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup): SubredditItemViewHolder {
            return SubredditItemViewHolder(viewGroup, this@SubredditSearchActivity)
        }

        override fun onBindViewHolder(holder: SubredditItemViewHolder) {
            holder.bind(mSubreddit)
        }

        override fun isHidden(): Boolean {
            return false
        }
    }

    override fun baseActivityIsToolbarSearchBarEnabled(): Boolean {
        return true
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateList() {
        checkThisIsUIThread()

        Log.i(TAG, "Updating list")

        if (mSubscriptionsErrorItem.get()!!.isPresent()) {
            mRecyclerViewAdapter!!.removeAllFromGroup(GROUP_SUBREDDITS)
            mRecyclerViewAdapter!!.appendToGroup(
                GROUP_SUBREDDITS,
                mSubscriptionsErrorItem.get()!!.get()
            )

            mLoadingItem!!.setHidden(true)
            mRecyclerViewAdapter!!.updateHiddenStatus()

            return
        }

        val currentQuery = mSearchView.get()!!.getQuery().toString()

        mRecyclerViewAdapter!!.removeAllFromGroup(GROUP_SUBREDDITS)

        if (mSubscriptions.get()!!.isEmpty()) {
            Log.i(TAG, "Subscriptions not downloaded yet")

            mLoadingItem!!.setHidden(false)
            mRecyclerViewAdapter!!.updateHiddenStatus()

            if (mSubscriptionListPending.get() !== Boolean.TRUE) {
                requestSubscriptions()
            }

            mLoadingItem!!.setHidden(false)
            mRecyclerViewAdapter!!.updateHiddenStatus()
        } else {
            val shownSubreddits = HashSet<String?>(256)

            val possibleSuggestions = ArrayList<SubredditDetails?>(mSubscriptions.get()!!.get())

            Collections.sort<SubredditDetails?>(
                possibleSuggestions,
                Comparator { o1: SubredditDetails?, o2: SubredditDetails? -> o1!!.name.compareTo(o2!!.name) })

            val asciiLowercaseQuery = StringUtils.asciiLowercase(currentQuery)

            run {
                val it: MutableIterator<SubredditDetails> = possibleSuggestions.iterator()
                while (it.hasNext()) {
                    val entry = it.next()

                    val lowercaseName = StringUtils.asciiLowercase(entry.name)

                    if (lowercaseName.startsWith(asciiLowercaseQuery)
                        && shownSubreddits.add(lowercaseName)
                    ) {
                        mRecyclerViewAdapter!!.appendToGroup(
                            GROUP_SUBREDDITS,
                            mSubredditItemCache.get(entry)
                        )
                        it.remove()
                    }
                }
            }

            run {
                val it: MutableIterator<SubredditDetails> = possibleSuggestions.iterator()
                while (it.hasNext()) {
                    val entry = it.next()

                    val lowercaseName = StringUtils.asciiLowercase(entry.name)

                    if (lowercaseName.contains(asciiLowercaseQuery)
                        && shownSubreddits.add(lowercaseName)
                    ) {
                        mRecyclerViewAdapter!!.appendToGroup(
                            GROUP_SUBREDDITS,
                            mSubredditItemCache.get(entry)
                        )
                        it.remove()
                    }
                }
            }

            val currentQueryResults = mQueryResults.get()!!.get(currentQuery)

            if (currentQueryResults != null) {
                for (subreddit in currentQueryResults) {
                    val name = StringUtils.asciiLowercase(subreddit.name)
                    if (shownSubreddits.add(name)) {
                        mRecyclerViewAdapter!!.appendToGroup(
                            GROUP_SUBREDDITS,
                            mSubredditItemCache.get(subreddit)
                        )
                    }
                }

                mLoadingItem!!.setHidden(false)
                mRecyclerViewAdapter!!.updateHiddenStatus()
            } else if (!currentQuery.trim { it <= ' ' }.isEmpty()) {
                if (mQueryErrorItem.get()!!.isPresent()) {
                    mRecyclerViewAdapter!!.appendToGroup(
                        GROUP_SUBREDDITS,
                        mQueryErrorItem.get()!!.get()
                    )

                    mLoadingItem!!.setHidden(true)
                    mRecyclerViewAdapter!!.updateHiddenStatus()
                } else {
                    mLoadingItem!!.setHidden(false)
                    mRecyclerViewAdapter!!.updateHiddenStatus()
                }
            }

            mRecyclerViewAdapter!!.notifyDataSetChanged()
            mSubredditItemCache.nextGeneration()
        }
    }

    protected override fun onCreate(savedInstanceState: Bundle?) {
        PrefsUtility.applyTheme(this)
        super.onCreate(savedInstanceState)

        mSubredditSubscriptionManager = RedditSubredditSubscriptionManager.Companion.getSingleton(
            this,
            RedditAccountManager.Companion.getInstance(this).getDefaultAccount()
        )

        val queryEventListeners = EventListenerSet<String?>()

        mLoadingItem = GroupedRecyclerViewItemLoadingSpinner(this)

        val searchView: SearchView
        SearchView > findViewById<View?>(R.id.actionbar_search_view)
        mSearchView.set(searchView)
        searchView.setQueryHint(getString(string.find_location))
        searchView.requestFocus()

        setBaseActivityListing(R.layout.subreddit_search_listing)

        val recyclerView: RecyclerView
        RecyclerView > findViewById<View?>(R.id.subreddit_search_recyclerview)

        mRecyclerViewLayout = LinearLayoutManager(
            this,
            RecyclerView.VERTICAL,
            false
        )

        mRecyclerViewAdapter = GroupedRecyclerViewAdapter(3)
        mRecyclerViewAdapter!!.appendToGroup(GROUP_LOADING_SPINNER, mLoadingItem)

        recyclerView.setLayoutManager(mRecyclerViewLayout)
        recyclerView.setAdapter(mRecyclerViewAdapter)

        mRecyclerViewAdapter!!.appendToGroup(
            GROUP_QUICK_LINKS,
            object : GroupedRecyclerViewAdapter.Item<RecyclerView.ViewHolder?>() {
                override fun getViewType(): Class<*> {
                    return this.javaClass
                }

                override fun onCreateViewHolder(viewGroup: ViewGroup): RecyclerView.ViewHolder {
                    val quickLinks = LayoutInflater.from(viewGroup.getContext())
                        .inflate(
                            R.layout.subreddit_search_quick_links,
                            viewGroup,
                            false
                        ) as SubredditSearchQuickLinks

                    quickLinks.bind(this@SubredditSearchActivity, queryEventListeners)

                    return object : RecyclerView.ViewHolder(quickLinks) {}
                }

                override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder?) {}

                override fun isHidden(): kotlin.Boolean {
                    return false
                }
            })

        mSubredditSubscriptionListenerContext
        = Optional.Companion.of<ListenerContext?>(mSubredditSubscriptionManager!!.addListener(this))

        requestSubscriptions()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): kotlin.Boolean {
                handleQueryChanged(query)
                queryEventListeners.send(query)
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String): kotlin.Boolean {
                handleQueryChanged(newText)
                queryEventListeners.send(newText)
                return true
            }
        })

        updateList()
    }

    private fun handleQueryChanged(text: String) {
        mSubscriptionsErrorItem.set(Optional.Companion.empty<GroupedRecyclerViewItemRRError?>())
        mQueryErrorItem.set(Optional.Companion.empty<GroupedRecyclerViewItemRRError?>())

        updateList()
        mRecyclerViewLayout!!.scrollToPosition(0)

        if (text.isEmpty()) {
            return
        }

        if (mQueriesPending.get()!!.contains(text)) {
            // Do nothing, let's just wait for now.
            return
        }

        if (!mQueryResults.get()!!.containsKey(text)) {
            mQueriesPending.get()!!.add(text)

            // Wait 1 second to avoid sending requests too fast
            AndroidCommon.UI_THREAD_HANDLER.postDelayed(
                Runnable {
                    if (text.contentEquals(mSearchView.get()!!.getQuery())) {
                        doSearchRequest(text)
                    } else {
                        mQueriesPending.get()!!.remove(text)
                    }
                },
                1000
            )
        }
    }

    private fun doSearchRequest(text: String) {
        Log.i(TAG, "Running search")

        val cacheManager: CacheManager = CacheManager.Companion.getInstance(this)
        val user
                : RedditAccount =
            RedditAccountManager.Companion.getInstance(this).getDefaultAccount()

        RedditAPI.searchSubreddits(
            cacheManager,
            user,
            text,
            this,
            object : ValueResponseHandler<SubredditListResponse?>(this) {
                protected override fun onSuccess(
                    value: SubredditListResponse
                ) {
                    Log.i(TAG, "Search results received")

                    val results = CollectionStream<RedditSubreddit?>(value.subreddits)
                        .map<SubredditDetails?>(MapStream.Operator { subreddit: Input? ->
                            SubredditDetails.Companion.newWithRuntimeException(
                                subreddit
                            )
                        })
                        .collect<ArrayList<SubredditDetails?>>(ArrayList<SubredditDetails?>())

                    AndroidCommon.runOnUiThread(Runnable {
                        mQueryResults.get()!!.put(text, results)
                        mQueriesPending.get()!!.remove(text)
                        updateList()
                    })
                }

                protected override fun onCallbackException(t: Throwable?) {
                    handleGlobalError(
                        this@SubredditSearchActivity,
                        t
                    )

                    AndroidCommon.runOnUiThread(Runnable { mQueriesPending.get()!!.remove(text) })
                }

                protected override fun onFailure(error: RRError) {
                    Log.i(TAG, "Got error receiving search results: " + error)

                    AndroidCommon.runOnUiThread(Runnable {
                        mQueriesPending.get()!!.remove(text)
                        mQueryErrorItem.set(
                            Optional.Companion.of<GroupedRecyclerViewItemRRError?>(
                                GroupedRecyclerViewItemRRError(
                                    this@SubredditSearchActivity,
                                    error
                                )
                            )
                        )
                        updateList()
                    })
                }
            },
            Optional.Companion.empty<String?>()
        )
    }

    private fun requestSubscriptions() {
        if (mSubscriptionListPending.get() === Boolean.TRUE) {
            Log.i(TAG, "Subscription list already pending")
            return
        }

        mSubscriptionListPending.set(true)

        val onSuccess = FunctionOneArgNoReturn { list: ArrayList<SubredditCanonicalId?>? ->
            AndroidCommon.runOnUiThread(
                Runnable {
                    if (mSubscriptionListPending.get() && list != null) {
                        mSubscriptionListPending.set(false)

                        val subscriptions = CollectionStream<SubredditCanonicalId?>(list)
                            .map<SubredditDetails?>(MapStream.Operator { subreddit: Input? ->
                                SubredditDetails(
                                    subreddit
                                )
                            })
                            .collect<ArrayList<SubredditDetails?>>(ArrayList<SubredditDetails?>())

                        mSubscriptions.set(
                            Optional.Companion.of<ArrayList<SubredditDetails?>?>(
                                subscriptions
                            )
                        )
                    }
                })
        }

        mSubredditSubscriptionManager!!.triggerUpdateIfNotReady(
            FunctionOneArgNoReturn { error: RRError? ->
                AndroidCommon.runOnUiThread(Runnable {
                    mQueryErrorItem.set(
                        Optional.Companion.of<GroupedRecyclerViewItemRRError?>(
                            GroupedRecyclerViewItemRRError(this, error!!)
                        )
                    )
                    updateList()
                })
            })

        mSubredditSubscriptionManager!!.addListener(
            object : SubredditSubscriptionStateChangeListener {
                override fun onSubredditSubscriptionListUpdated(
                    subredditSubscriptionManager: RedditSubredditSubscriptionManager
                ) {
                    onSuccess.apply(subredditSubscriptionManager.getSubscriptionList())
                }

                override fun onSubredditSubscriptionAttempted(
                    subredditSubscriptionManager: RedditSubredditSubscriptionManager?
                ) {
                }

                override fun onSubredditUnsubscriptionAttempted(
                    subredditSubscriptionManager: RedditSubredditSubscriptionManager?
                ) {
                }
            })

        onSuccess.apply(mSubredditSubscriptionManager!!.getSubscriptionList())
    }

    protected override fun onDestroy() {
        super.onDestroy()
        mSubredditSubscriptionListenerContext.apply(
            FunctionOneArgNoReturn { removeListener() })
    }

    companion object {
        private const val TAG = "SubredditSearchActivity"

        private const val GROUP_QUICK_LINKS = 0
        private const val GROUP_SUBREDDITS = 1
        private const val GROUP_LOADING_SPINNER = 2
    }
}
