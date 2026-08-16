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
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.quantumbadger.redreader.reddit.api

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.TimestampBound
import org.quantumbadger.redreader.common.collections.WeakReferenceListManager
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.io.RawObjectDB
import org.quantumbadger.redreader.io.RequestResponseHandler
import org.quantumbadger.redreader.io.WritableHashSet

/**
 * Hilt-injected multireddit subscription manager.
 * Replaces companion object singleton pattern.
 */
@ViewModelScoped
class RedditMultiredditSubscriptionManager @Inject constructor(
    private val mUser: RedditAccount,
    @ApplicationContext private val mContext: Context
) {
    private val notifier = MultiredditListChangeNotifier()
    private val listeners = WeakReferenceListManager<MultiredditListChangeListener?>()

    private var mMultireddits: WritableHashSet?

    private val db: RawObjectDB<String?, WritableHashSet?>?

    init {
        mMultireddits = db!!.getById(mUser.canonicalUsername)
    }

    fun addListener(listener: MultiredditListChangeListener) {
        listeners.add(listener)
    }

    @Synchronized
    fun areSubscriptionsReady(): Boolean {
        return mMultireddits != null
    }

    @Synchronized
    private fun onNewSubscriptionListReceived(
        newSubscriptions: HashSet<String?>?,
        timestamp: TimestampUTC
    ) {
        mMultireddits = WritableHashSet(
            newSubscriptions,
            timestamp,
            mUser.canonicalUsername
        )

        listeners.map(notifier)

        // TODO threaded? or already threaded due to cache manager
        db!!.put(mMultireddits)
    }

    @get:Synchronized
    val subscriptionList: ArrayList<String?>
        get() = java.util.ArrayList<String?>(mMultireddits!!.toHashset())

    fun triggerUpdate(
        handler: RequestResponseHandler<HashSet<String?>?, RRError?>?,
        timestampBound: TimestampBound
    ) {
        if (mMultireddits != null
            && timestampBound.verifyTimestamp(mMultireddits!!.timestamp)
        ) {
            return
        }

        RedditAPIMultiredditListRequester(mContext, mUser).performRequest(
            RedditAPIMultiredditListRequester.Key.INSTANCE,
            timestampBound,
            object : RequestResponseHandler<WritableHashSet?, RRError?> {
                // TODO handle failed requests properly -- retry? then notify listeners
                override fun onRequestFailed(failureReason : RRError) {
                    if (handler != null) {
                        handler.onRequestFailed(failureReason)
                    }
                }

                override fun onRequestSuccess(
                    result: WritableHashSet,
                    timeCached: TimestampUTC
                ) {
                    val newSubscriptions = result.toHashset()
                    onNewSubscriptionListReceived(newSubscriptions, timeCached)
                    if (handler != null) {
                        handler.onRequestSuccess(newSubscriptions, timeCached)
                    }
                }
            }
        )
    }

    interface MultiredditListChangeListener {
        fun onMultiredditListUpdated(
            multiredditSubscriptionManager: RedditMultiredditSubscriptionManager?
        )
    }

    private inner class MultiredditListChangeNotifier

        : WeakReferenceListManager.Operator<MultiredditListChangeListener?> {
        override fun operate(listener: MultiredditListChangeListener) {
            listener.onMultiredditListUpdated(
                this@RedditMultiredditSubscriptionManager
            )
        }
    }

    companion object {
        @JvmStatic
        fun getSingleton(
            context: Context,
            account: RedditAccount
        ): RedditMultiredditSubscriptionManager {
            return RedditMultiredditSubscriptionManager(account, context)
        }
    }
}
