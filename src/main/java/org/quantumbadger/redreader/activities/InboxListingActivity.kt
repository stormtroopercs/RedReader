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

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.RedReader.Companion.getInstance
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.BugReportActivity.Companion.addGlobalError
import org.quantumbadger.redreader.adapters.GroupedRecyclerViewAdapter
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequest.DownloadQueueType
import org.quantumbadger.redreader.cache.CacheRequest.RequestFailureType
import org.quantumbadger.redreader.cache.CacheRequestCallbacks
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyAlways
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.Constants.Reddit
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.common.General.getGeneralErrorForFailure
import org.quantumbadger.redreader.common.General.getSharedPrefs
import org.quantumbadger.redreader.common.General.handlerMessage
import org.quantumbadger.redreader.common.General.quickToast
import org.quantumbadger.redreader.common.General.recreateActivityNoAnimation
import org.quantumbadger.redreader.common.General.setLayoutMatchWidthWrapHeight
import org.quantumbadger.redreader.common.General.showResultDialog
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.RRThemeAttributes
import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.UriString.Companion.from
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import org.quantumbadger.redreader.common.time.TimeDuration.Companion.minutes
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.http.FailedRequestBody
import org.quantumbadger.redreader.reddit.APIResponseHandler.ActionResponseHandler
import org.quantumbadger.redreader.reddit.RedditAPI
import org.quantumbadger.redreader.reddit.api.RedditPostActions.ActionDescriptionPair.Companion.from
import org.quantumbadger.redreader.reddit.kthings.JsonUtils.decodeRedditThingFromStream
import org.quantumbadger.redreader.reddit.kthings.MaybeParseError
import org.quantumbadger.redreader.reddit.kthings.RedditFieldReplies.Some
import org.quantumbadger.redreader.reddit.kthings.RedditThing
import org.quantumbadger.redreader.reddit.kthings.RedditThing.Listing
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager
import org.quantumbadger.redreader.reddit.prepared.RedditParsedComment
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedMessage
import org.quantumbadger.redreader.reddit.prepared.RedditRenderableComment
import org.quantumbadger.redreader.reddit.prepared.RedditRenderableInboxItem
import org.quantumbadger.redreader.views.RedditInboxItemView
import org.quantumbadger.redreader.views.ScrollbarRecyclerViewManager
import org.quantumbadger.redreader.views.liststatus.ErrorView
import org.quantumbadger.redreader.views.liststatus.LoadingView
import java.io.IOException
import java.util.UUID

class InboxListingActivity : ViewsBaseActivity() {
    enum class InboxType {
        INBOX, SENT, MODMAIL
    }

    private var adapter: GroupedRecyclerViewAdapter? = null

    private var loadingView: LoadingView? = null
    private var notifications: LinearLayout? = null

    private var request: CacheRequest? = null

    private var inboxType: InboxType? = null
    private var mOnlyShowUnread = false

    private var mTheme: RRThemeAttributes? = null
    private var mChangeDataManager: RedditChangeDataManager? = null

    private val itemHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            adapter!!.appendToGroup(0, msg.obj as GroupedRecyclerViewAdapter.Item<*>?)
        }
    }

    private inner class InboxItem(
        private val mListPosition: Int,
        private val mItem: RedditRenderableInboxItem
    ) : GroupedRecyclerViewAdapter.Item<Any?>() {
        override fun getViewType(): Class<*> {
            return RedditInboxItemView::class.java
        }

        override fun onCreateViewHolder(viewGroup: ViewGroup?): RecyclerView.ViewHolder {
            val view = RedditInboxItemView(this@InboxListingActivity, mTheme)

            val layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            view.setLayoutParams(layoutParams)

            return object : RecyclerView.ViewHolder(view) {
            }
        }

        override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder) {
            (viewHolder.itemView as RedditInboxItemView).reset(
                this@InboxListingActivity,
                mChangeDataManager,
                mTheme,
                mItem,
                mListPosition != 0
            )
        }

        override fun isHidden(): Boolean {
            return false
        }
    }

    // TODO load more on scroll to bottom?
    public override fun onCreate(savedInstanceState: Bundle?) {
        PrefsUtility.applyTheme(this)
        super.onCreate(savedInstanceState)

        mTheme = RRThemeAttributes(this)
        mChangeDataManager = RedditChangeDataManager.Companion.getInstance(
            RedditAccountManager.Companion.getInstance(this).getDefaultAccount()
        )

        val sharedPreferences = getSharedPrefs(this)
        val title: String

        if (getIntent() != null) {
            val inboxTypeString = getIntent().getStringExtra("inboxType")

            if (inboxTypeString != null) {
                inboxType = InboxType.valueOf(StringUtils.asciiUppercase(inboxTypeString))
            } else {
                inboxType = InboxType.INBOX
            }
        } else {
            inboxType = InboxType.INBOX
        }

        mOnlyShowUnread = sharedPreferences.getBoolean(PREF_ONLY_UNREAD, false)

        when (inboxType) {
            InboxType.SENT -> title = getString(string.mainmenu_sent_messages)
            InboxType.MODMAIL -> title = getString(string.mainmenu_modmail)
            else -> title = getString(string.mainmenu_inbox)
        }

        setTitle(title)

        val outer = LinearLayout(this)
        outer.setOrientation(LinearLayout.VERTICAL)

        loadingView = LoadingView(
            this,
            getString(string.download_waiting),
            true,
            true
        )

        notifications = LinearLayout(this)
        notifications!!.setOrientation(LinearLayout.VERTICAL)
        notifications!!.addView(loadingView)

        val recyclerViewManager = ScrollbarRecyclerViewManager(this, null, false)

        adapter = GroupedRecyclerViewAdapter(1)
        recyclerViewManager.getRecyclerView().setAdapter(adapter)

        outer.addView(notifications)
        outer.addView(recyclerViewManager.getOuterView())

        makeFirstRequest(this)

        setBaseActivityListing(outer)
    }

    fun cancel() {
        if (request != null) {
            request!!.cancel()
        }
    }

    private fun makeFirstRequest(context: Context) {
        val user: RedditAccount = RedditAccountManager.Companion.getInstance(context)
            .getDefaultAccount()
        val cm: CacheManager = CacheManager.Companion.getInstance(context)

        val url: UriString

        if (inboxType == InboxType.SENT) {
            url = Reddit.getUri("/message/sent.json?limit=100")
        } else if (inboxType == InboxType.MODMAIL) {
            url = Reddit.getUri("/message/moderator.json?limit=100")
        } else {
            if (mOnlyShowUnread) {
                url = Reddit.getUri("/message/unread.json?mark=true&limit=100")
            } else {
                url = Reddit.getUri("/message/inbox.json?mark=true&limit=100")
            }
        }

        // TODO parameterise limit
        request = CacheRequest(
            url,
            user,
            null,
            Priority(Constants.Priority.API_INBOX_LIST),
            DownloadStrategyAlways.Companion.INSTANCE,
            Constants.FileType.INBOX_LIST,
            DownloadQueueType.REDDIT_API,
            false,
            context,
            object : CacheRequestCallbacks {
                override fun onDataStreamComplete(
                    streamFactory: GenericFactory<SeekableInputStream?, IOException?>,
                    timestamp: TimestampUTC,
                    session: UUID,
                    fromCache: Boolean,
                    mimetype: String?
                ) {
                    try {
                        val rootThing = decodeRedditThingFromStream(streamFactory.create())

                        val listing = (rootThing as Listing).data

                        if (loadingView != null) {
                            loadingView!!.setIndeterminate(string.download_downloading)
                        }

                        // TODO pref (currently 10 mins)
                        // TODO xml
                        if (fromCache) {
                            if (timestamp.elapsed().isGreaterThan(minutes(10))) {
                                AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                                    val cacheNotif = TextView(context)
                                    cacheNotif.setText(
                                        context.getString(
                                            string.listing_cached,
                                            timestamp.format()
                                        )
                                    )
                                    val paddingPx = dpToPixels(context, 6f)
                                    val sidePaddingPx = dpToPixels(context, 10f)
                                    cacheNotif.setPadding(
                                        sidePaddingPx,
                                        paddingPx,
                                        sidePaddingPx,
                                        paddingPx
                                    )
                                    cacheNotif.setTextSize(13f)
                                    notifications!!.addView(cacheNotif)
                                })
                            }
                        }

                        // TODO {"error": 403} is received for unauthorized subreddits
                        var listPosition = 0

                        if (listing.children.isEmpty()) {
                            AndroidCommon.runOnUiThread(Runnable {
                                val emptyView =
                                    LayoutInflater.from(context).inflate(
                                        R.layout.no_items_yet,
                                        notifications,
                                        true
                                    )
                                (emptyView.findViewById<View?>(R.id.empty_view_text) as TextView)
                                    .setText(string.no_messages_yet)
                                setLayoutMatchWidthWrapHeight(emptyView)
                            })
                        }

                        for (maybeThing
                        in listing.children) {
                            // TODO show error instead

                            val thing = maybeThing.ok()

                            if (thing is RedditThing.Comment) {
                                val comment = thing.data

                                val parsedComment = RedditParsedComment(
                                    comment,
                                    this@InboxListingActivity
                                )

                                val renderableComment = RedditRenderableComment(
                                    parsedComment,
                                    null,
                                    -100000,
                                    null,
                                    false,
                                    true,
                                    true
                                )

                                itemHandler.sendMessage(
                                    handlerMessage(
                                        0,
                                        InboxItem(listPosition, renderableComment)
                                    )
                                )

                                listPosition++
                            } else if (thing is RedditThing.Message) {
                                val message = RedditPreparedMessage(
                                    this@InboxListingActivity,
                                    thing.data,
                                    inboxType
                                )

                                itemHandler.sendMessage(
                                    handlerMessage(
                                        0,
                                        InboxItem(listPosition, message)
                                    )
                                )
                                listPosition++

                                if (message.src.replies
                                            is Some
                                ) {
                                    // TODO make RedditThing generic (and override data)?

                                    val replies
                                            : ArrayList<MaybeParseError<RedditThing?>> =
                                        (message.src.replies.value as Listing)
                                            .data.children

                                    for (childMsgValue
                                    in replies) {
                                        val childMsgRaw =
                                            (childMsgValue.ok() as RedditThing.Message)
                                                .data

                                        val childMsg = RedditPreparedMessage(
                                            this@InboxListingActivity,
                                            childMsgRaw,
                                            inboxType
                                        )

                                        itemHandler.sendMessage(
                                            handlerMessage(
                                                0,
                                                InboxItem(listPosition, childMsg)
                                            )
                                        )

                                        listPosition++
                                    }
                                }
                            } else {
                                throw RuntimeException("Unknown item in list.")
                            }
                        }

                        if (loadingView != null) {
                            loadingView!!.setDone(string.download_done)
                        }
                    } catch (e: Exception) {
                        onFailure(
                            getGeneralErrorForFailure(
                                context,
                                RequestFailureType.PARSE,
                                e,
                                null,
                                url,
                                FailedRequestBody.Companion.from(streamFactory)
                            )
                        )
                    }
                }

                override fun onFailure(error: RRError) {
                    Log.e(TAG, "Inbox fetch error: " + error, error.t)

                    request = null

                    if (loadingView != null) {
                        loadingView!!.setDone(string.download_failed)
                    }

                    AndroidCommon.runOnUiThread(Runnable {
                        notifications!!.addView(
                            ErrorView(this@InboxListingActivity, error)
                        )
                    })
                }
            })

        cm.makeRequest(request!!)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (inboxType == InboxType.SENT) {
            return false
        }

        val refresh = menu.add(0, OPTIONS_MENU_REFRESH, 0, string.options_refresh)
        refresh.setIcon(R.drawable.ic_refresh_dark)
        refresh.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        if (inboxType != InboxType.SENT) {
            menu.add(0, OPTIONS_MENU_MARK_ALL_AS_READ, 1, string.mark_all_as_read)
            menu.add(0, OPTIONS_MENU_SHOW_UNREAD_ONLY, 2, string.inbox_unread_only)
            menu.getItem(2).setCheckable(true)
            if (mOnlyShowUnread) {
                menu.getItem(2).setChecked(true)
            }
        }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            OPTIONS_MENU_MARK_ALL_AS_READ -> {
                RedditAPI.markAllAsRead(
                    CacheManager.Companion.getInstance(this),
                    object : ActionResponseHandler(this) {
                        override fun onSuccess() {
                            quickToast(
                                context,
                                string.mark_all_as_read_success
                            )
                        }

                        protected override fun onCallbackException(t: Throwable?) {
                            addGlobalError(
                                RRError(
                                    "Mark all as Read failed",
                                    "Callback exception",
                                    true,
                                    t
                                )
                            )
                        }

                        protected override fun onFailure(error: RRError) {
                            showResultDialog(
                                this@InboxListingActivity,
                                error
                            )
                        }
                    },
                    RedditAccountManager.Companion.getInstance(this).getDefaultAccount(),
                    this
                )

                return true
            }

            OPTIONS_MENU_SHOW_UNREAD_ONLY -> {
                val enabled = !item.isChecked()

                item.setChecked(enabled)
                mOnlyShowUnread = enabled

                getSharedPrefs(this)
                    .edit()
                    .putBoolean(PREF_ONLY_UNREAD, enabled)
                    .apply()

                recreateActivityNoAnimation(this)
                return true
            }

            OPTIONS_MENU_REFRESH -> {
                recreateActivityNoAnimation(this)
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val TAG = "InboxListingActivity"

        private const val OPTIONS_MENU_MARK_ALL_AS_READ = 0
        private const val OPTIONS_MENU_SHOW_UNREAD_ONLY = 1
        private const val OPTIONS_MENU_REFRESH = 2

        private const val PREF_ONLY_UNREAD = "inbox_only_show_unread"
    }
}
