/*******************************************************************************
 * This file is part of MaterialReader.
 *
 * MaterialReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MaterialReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MaterialReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.stormtroopercs.materialreader.reddit.api

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import com.stormtroopercs.materialreader.R.string
import com.stormtroopercs.materialreader.account.RedditAccount
import com.stormtroopercs.materialreader.common.BugReporter.handleGlobalError
import com.stormtroopercs.materialreader.cache.CacheManager
import com.stormtroopercs.materialreader.common.FunctionOneArgNoReturn
import com.stormtroopercs.materialreader.common.General.quickToast
import com.stormtroopercs.materialreader.common.General.showResultDialog
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.TimestampBound
import com.stormtroopercs.materialreader.common.UnexpectedInternalStateException
import com.stormtroopercs.materialreader.common.collections.CollectionStream
import com.stormtroopercs.materialreader.common.collections.MapStream
import com.stormtroopercs.materialreader.common.collections.MapStreamRethrowExceptions
import com.stormtroopercs.materialreader.common.collections.WeakReferenceListManager
import com.stormtroopercs.materialreader.common.collections.WeakReferenceListManager.ArgOperator
import com.stormtroopercs.materialreader.common.time.TimeDuration.Companion.hours
import com.stormtroopercs.materialreader.common.time.TimeDuration.Companion.secs
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.common.time.TimestampUTC.Companion.now
import com.stormtroopercs.materialreader.io.RawObjectDB
import com.stormtroopercs.materialreader.io.RequestResponseHandler
import com.stormtroopercs.materialreader.io.WritableHashSet
import com.stormtroopercs.materialreader.reddit.APIResponseHandler.ActionResponseHandler
import com.stormtroopercs.materialreader.reddit.RedditAPI
import com.stormtroopercs.materialreader.reddit.RedditAPI.RedditSubredditAction
import com.stormtroopercs.materialreader.reddit.RedditSubredditHistory
import com.stormtroopercs.materialreader.reddit.RedditSubredditManager.SubredditListType
import com.stormtroopercs.materialreader.reddit.things.InvalidSubredditNameException
import com.stormtroopercs.materialreader.reddit.things.SubredditCanonicalId
import com.stormtroopercs.materialreader.common.General

/**
 * Hilt-injected subreddit subscription manager.
 * Replaces companion object singleton pattern.
 */
@ViewModelScoped
class RedditSubredditSubscriptionManager @Inject constructor(
    private val user: RedditAccount,
    @ApplicationContext private val context: Context
) {
    private val TAG = "SubscriptionManager"

    inner class ListenerContext(private val mListener: SubredditSubscriptionStateChangeListener) {
        fun removeListener() {
            synchronized(this@RedditSubredditSubscriptionManager) {
                listeners.remove(mListener)
            }
        }
    }

    private val notifier = SubredditSubscriptionStateChangeNotifier()
    private val listeners = WeakReferenceListManager<SubredditSubscriptionStateChangeListener>()

    private var subscriptions: WritableHashSet?

    private val pendingSubscriptions = HashSet<SubredditCanonicalId>()
    private val pendingUnsubscriptions = HashSet<SubredditCanonicalId>()

    private var mLastUpdateRequestTime = TimestampUTC.ZERO

    init {
        subscriptions = db!!.getById(user.canonicalUsername)

        if (subscriptions != null) {
            addToHistory(user, subscriptionList!!)
        }
    }

    @Synchronized
    fun addListener(
        listener: SubredditSubscriptionStateChangeListener
    ): ListenerContext {
        listeners.add(listener)
        return ListenerContext(listener)
    }

    @Synchronized
    fun areSubscriptionsReady(): Boolean {
        return subscriptions != null
    }

    @Synchronized
    fun getSubscriptionState(
        id: SubredditCanonicalId
    ): SubredditSubscriptionState? {
        if (subscriptions == null) {
            return null
        }

        if (pendingSubscriptions.contains(id)) {
            return SubredditSubscriptionState.SUBSCRIBING
        } else if (pendingUnsubscriptions.contains(id)) {
            return SubredditSubscriptionState.UNSUBSCRIBING
        } else if (subscriptions!!.toHashset().contains(id.toString())) {
            return SubredditSubscriptionState.SUBSCRIBED
        } else {
            return SubredditSubscriptionState.NOT_SUBSCRIBED
        }
    }

    @Synchronized
    private fun onSubscriptionAttempt(id: SubredditCanonicalId) {
        pendingSubscriptions.add(id)
        listeners.map<SubredditSubscriptionChangeType>(
            notifier,
            SubredditSubscriptionChangeType.SUBSCRIPTION_ATTEMPTED
        )
    }

    @Synchronized
    private fun onUnsubscriptionAttempt(id: SubredditCanonicalId) {
        pendingUnsubscriptions.add(id)
        listeners.map<SubredditSubscriptionChangeType>(
            notifier,
            SubredditSubscriptionChangeType.UNSUBSCRIPTION_ATTEMPTED
        )
    }

    @Synchronized
    private fun onSubscriptionChangeAttemptFailed(id: SubredditCanonicalId) {
        pendingUnsubscriptions.remove(id)
        pendingSubscriptions.remove(id)
        listeners.map<SubredditSubscriptionChangeType>(
            notifier,
            SubredditSubscriptionChangeType.LIST_UPDATED
        )
    }

    @Synchronized
    private fun onSubscriptionAttemptSuccess(id: SubredditCanonicalId) {
        quickToast(
            context, context.getApplicationContext().getString(
                string.subscription_successful,
                id.toString()
            )
        )

        pendingSubscriptions.remove(id)
        subscriptions!!.toHashset().add(id.toString())
        listeners.map<SubredditSubscriptionChangeType>(
            notifier,
            SubredditSubscriptionChangeType.LIST_UPDATED
        )
    }

    @Synchronized
    private fun onUnsubscriptionAttemptSuccess(id: SubredditCanonicalId) {
        quickToast(
            context, context.getApplicationContext().getString(
                string.unsubscription_successful,
                id.toString()
            )
        )

        pendingUnsubscriptions.remove(id)
        subscriptions!!.toHashset().remove(id.toString())
        listeners.map<SubredditSubscriptionChangeType>(
            notifier,
            SubredditSubscriptionChangeType.LIST_UPDATED
        )
    }

    @Synchronized
    private fun onNewSubscriptionListReceived(
        newSubscriptions: HashSet<SubredditCanonicalId>,
        timestamp: TimestampUTC
    ) {
        pendingSubscriptions.clear()
        pendingUnsubscriptions.clear()

        val newSubscriptionsStrings =
            CollectionStream<SubredditCanonicalId>(newSubscriptions)
                .map<String>(MapStream.Operator { obj: SubredditCanonicalId -> obj.toString() })
                .collect<HashSet<String>>(HashSet<String>())

        subscriptions = WritableHashSet(
            newSubscriptionsStrings,
            timestamp,
            user.canonicalUsername
        )

        // TODO threaded? or already threaded due to cache manager
        db!!.put(subscriptions!!)

        addToHistory(user, newSubscriptions)

        listeners.map<SubredditSubscriptionChangeType>(
            notifier,
            SubredditSubscriptionChangeType.LIST_UPDATED
        )
    }

    @get:Synchronized
    val subscriptionList: ArrayList<SubredditCanonicalId>?
        get() {
            if (subscriptions == null) {
                return null
            }

            return CollectionStream<String>(subscriptions!!.toHashset())
                .mapRethrowExceptions<SubredditCanonicalId>(MapStreamRethrowExceptions.Operator { name: String ->
                    SubredditCanonicalId(
                        name
                    )
                })
                .collect<java.util.ArrayList<SubredditCanonicalId>>(java.util.ArrayList<SubredditCanonicalId>())
        }

    @Synchronized
    fun triggerUpdateIfNotReady(
        onFailure: FunctionOneArgNoReturn<RRError>?
    ) {
        val handler: RequestResponseHandler<HashSet<SubredditCanonicalId>, RRError> =
            object : RequestResponseHandler<HashSet<SubredditCanonicalId>, RRError> {
                override fun onRequestFailed(failureReason : RRError) {
                    if (onFailure != null) {
                        onFailure.apply(failureReason)
                    }
                }

                override fun onRequestSuccess(
                    result: HashSet<SubredditCanonicalId>,
                    timeCached: TimestampUTC?
                ) {
                    // Do nothing
                }
            }

        if (!areSubscriptionsReady()
            && (mLastUpdateRequestTime === TimestampUTC.ZERO
                    || mLastUpdateRequestTime.elapsed().isGreaterThan(secs(10)))
        ) {
            triggerUpdate(handler, TimestampBound.notOlderThan(hours(1)))
        }
    }

    @Synchronized
    fun triggerUpdateIfNotReady() {
        triggerUpdateIfNotReady(null)
    }

    @Synchronized
    fun triggerUpdate(
        handler: RequestResponseHandler<HashSet<SubredditCanonicalId>, RRError>?,
        timestampBound: TimestampBound
    ) {
        if (subscriptions != null
            && timestampBound.verifyTimestamp(subscriptions!!.timestamp)
        ) {
            return
        }

        mLastUpdateRequestTime = now()

        RedditAPIIndividualSubredditListRequester(context, user).performRequest(
            SubredditListType.SUBSCRIBED,
            timestampBound,
            object : RequestResponseHandler<WritableHashSet, RRError> {
                // TODO handle failed requests properly -- retry? then notify listeners
                override fun onRequestFailed(failureReason : RRError) {
                    if (handler != null) {
                        handler.onRequestFailed(failureReason)
                    }
                }

                override fun onRequestSuccess(
                    result: WritableHashSet,
                    timeCached: TimestampUTC?
                ) {
                    val newSubscriptionStrings = result.toHashset()

                    val newSubscriptions =
                        HashSet<SubredditCanonicalId>()

                    for (id in newSubscriptionStrings) {
                        try {
                            newSubscriptions.add(SubredditCanonicalId(id))
                        } catch (e: InvalidSubredditNameException) {
                            Log.e(TAG, "Ignoring invalid subreddit name " + id, e)
                        }
                    }

                    onNewSubscriptionListReceived(newSubscriptions, timeCached!!)
                    if (handler != null) {
                        handler.onRequestSuccess(newSubscriptions, timeCached)
                    }
                }
            }
        )
    }

    fun subscribe(
        id: SubredditCanonicalId,
        activity: AppCompatActivity
    ) {
        RedditAPI.subscriptionAction(
            CacheManager.getInstance(context),
            SubredditActionResponseHandler(
                activity,
                RedditAPI.SUBSCRIPTION_ACTION_SUBSCRIBE,
                id
            ),
            user,
            id,
            RedditAPI.SUBSCRIPTION_ACTION_SUBSCRIBE,
            context
        )

        onSubscriptionAttempt(id)
    }

    fun unsubscribe(
        id: SubredditCanonicalId,
        activity: AppCompatActivity
    ) {
        RedditAPI.subscriptionAction(
            CacheManager.getInstance(context),
            SubredditActionResponseHandler(
                activity,
                RedditAPI.SUBSCRIPTION_ACTION_UNSUBSCRIBE,
                id
            ),
            user,
            id,
            RedditAPI.SUBSCRIPTION_ACTION_UNSUBSCRIBE,
            context
        )

        onUnsubscriptionAttempt(id)
    }

    private inner class SubredditActionResponseHandler
        (
        private val activity: AppCompatActivity,
        @field:RedditSubredditAction @param:RedditSubredditAction private val action: Int,
        private val canonicalName: SubredditCanonicalId
    ) : ActionResponseHandler(activity) {
        override fun onSuccess() {
            when (action) {
                RedditAPI.SUBSCRIPTION_ACTION_SUBSCRIBE -> onSubscriptionAttemptSuccess(
                    canonicalName
                )

                RedditAPI.SUBSCRIPTION_ACTION_UNSUBSCRIBE -> onUnsubscriptionAttemptSuccess(
                    canonicalName
                )
            }
        }

        override fun onCallbackException(t : Throwable) {
            handleGlobalError(context, t)
        }

        override fun onFailure(error: RRError) {
            if (error.httpStatus != null && error.httpStatus == 404) {
                // Weirdly, reddit returns a 404 if we were already subscribed/unsubscribed to
                // this subreddit.

                if (action == RedditAPI.SUBSCRIPTION_ACTION_SUBSCRIBE
                    || action == RedditAPI.SUBSCRIPTION_ACTION_UNSUBSCRIBE
                ) {
                    onSuccess()
                    return
                }
            }

            onSubscriptionChangeAttemptFailed(canonicalName)

            showResultDialog(activity, error)
        }
    }

    interface SubredditSubscriptionStateChangeListener {
        fun onSubredditSubscriptionListUpdated(
            subredditSubscriptionManager: RedditSubredditSubscriptionManager
        )

        fun onSubredditSubscriptionAttempted(
            subredditSubscriptionManager: RedditSubredditSubscriptionManager
        )

        fun onSubredditUnsubscriptionAttempted(
            subredditSubscriptionManager: RedditSubredditSubscriptionManager
        )
    }

    private enum class SubredditSubscriptionChangeType {
        LIST_UPDATED,
        SUBSCRIPTION_ATTEMPTED,
        UNSUBSCRIPTION_ATTEMPTED
    }

    private inner class SubredditSubscriptionStateChangeNotifier

        : ArgOperator<SubredditSubscriptionStateChangeListener, SubredditSubscriptionChangeType> {
        override fun operate(
            listener: SubredditSubscriptionStateChangeListener,
            changeType: SubredditSubscriptionChangeType
        ) {
            when (changeType) {
                SubredditSubscriptionChangeType.LIST_UPDATED -> listener.onSubredditSubscriptionListUpdated(
                    this@RedditSubredditSubscriptionManager
                )

                SubredditSubscriptionChangeType.SUBSCRIPTION_ATTEMPTED -> listener.onSubredditSubscriptionAttempted(
                    this@RedditSubredditSubscriptionManager
                )

                SubredditSubscriptionChangeType.UNSUBSCRIPTION_ATTEMPTED -> listener.onSubredditUnsubscriptionAttempted(
                    this@RedditSubredditSubscriptionManager
                )

                else -> throw UnexpectedInternalStateException(
                    "Invalid SubredditSubscriptionChangeType " + changeType
                )
            }
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var db: RawObjectDB<String, WritableHashSet>? = null

        @JvmStatic
        fun getSingleton(
            context: Context,
            account: RedditAccount
        ): RedditSubredditSubscriptionManager {
            if (db == null) {
                db = RawObjectDB<String, WritableHashSet>(
                    context.applicationContext,
                    "rr_subscriptions.db",
                    WritableHashSet::class.java
                )
            }
            val singleton = RedditSubredditSubscriptionManager(account, context.applicationContext)
            singleton.triggerUpdateIfNotReady()
            return singleton
        }

        private fun addToHistory(
            account: RedditAccount,
            newSubscriptions: MutableCollection<SubredditCanonicalId>
        ) {
            RedditSubredditHistory.addSubreddits(account, newSubscriptions)
        }
    }
}
