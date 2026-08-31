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
package com.stormtroopercs.materialreader.reddit.prepared

import android.content.Context
import android.util.Log
import com.stormtroopercs.materialreader.account.RedditAccount
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.common.AndroidCommon
import com.stormtroopercs.materialreader.common.General.isSensitiveDebugLoggingEnabled
import com.stormtroopercs.materialreader.common.PrefsUtility
import com.stormtroopercs.materialreader.common.collections.WeakReferenceListHashMapManager
import com.stormtroopercs.materialreader.common.collections.WeakReferenceListManager.ArgOperator
import com.stormtroopercs.materialreader.common.time.TimeDuration
import com.stormtroopercs.materialreader.common.time.TimeStringsDebug
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.common.time.TimestampUTC.Companion.fromUtcMs
import com.stormtroopercs.materialreader.common.time.TimestampUTC.Companion.now
import com.stormtroopercs.materialreader.io.ExtendedDataInputStream
import com.stormtroopercs.materialreader.io.ExtendedDataOutputStream
import com.stormtroopercs.materialreader.io.RedditChangeDataIO
import com.stormtroopercs.materialreader.reddit.kthings.RedditComment
import com.stormtroopercs.materialreader.reddit.kthings.RedditIdAndType
import com.stormtroopercs.materialreader.reddit.kthings.RedditPost
import java.io.IOException
import java.util.Locale
import java.util.SortedMap
import java.util.TreeMap
import com.stormtroopercs.materialreader.common.General

class RedditChangeDataManager {
    fun interface Listener {
        fun onRedditDataChange(thingIdAndType: RedditIdAndType?)
    }

    private class Entry {
        val mTimestamp: TimestampUTC

        val isUpvoted: Boolean
        val isDownvoted: Boolean
        val isRead: Boolean
        val isSaved: Boolean
        val isHidden: Boolean?

        private constructor() {
            mTimestamp = TimestampUTC.ZERO
            this.isUpvoted = false
            this.isDownvoted = false
            this.isRead = false
            this.isSaved = false
            this.isHidden = null
        }

        private constructor(
            timestamp: TimestampUTC,
            isUpvoted: Boolean,
            isDownvoted: Boolean,
            isRead: Boolean,
            isSaved: Boolean,
            isHidden: Boolean?
        ) {
            mTimestamp = timestamp
            this.isUpvoted = isUpvoted
            this.isDownvoted = isDownvoted
            this.isRead = isRead
            this.isSaved = isSaved
            this.isHidden = isHidden
        }

        constructor(dis: ExtendedDataInputStream) {
            mTimestamp = fromUtcMs(dis.readLong())
            this.isUpvoted = dis.readBoolean()
            this.isDownvoted = dis.readBoolean()
            this.isRead = dis.readBoolean()
            this.isSaved = dis.readBoolean()
            this.isHidden = dis.readNullableBoolean()
        }

        @Throws(IOException::class)
        fun writeTo(dos: ExtendedDataOutputStream) {
            dos.writeLong(mTimestamp.toUtcMs())
            dos.writeBoolean(this.isUpvoted)
            dos.writeBoolean(this.isDownvoted)
            dos.writeBoolean(this.isRead)
            dos.writeBoolean(this.isSaved)
            dos.writeNullableBoolean(this.isHidden)
        }

        val isClear: Boolean
            get() = !this.isUpvoted && !this.isDownvoted && !this.isRead && !this.isSaved && this.isHidden == null

        fun update(
            timestamp: TimestampUTC,
            comment: RedditComment
        ): Entry {
            if (timestamp.isLessThan(mTimestamp)) {
                return this
            }

            return Entry(
                timestamp,
                java.lang.Boolean.TRUE == comment.likes,
                java.lang.Boolean.FALSE == comment.likes,
                false,
                comment.saved,
                this.isHidden
            ) // Use existing value for "collapsed"
        }

        fun update(
            timestamp: TimestampUTC,
            post: RedditPost
        ): Entry {
            if (timestamp.isLessThan(mTimestamp)) {
                return this
            }

            return Entry(
                timestamp,
                java.lang.Boolean.TRUE == post.likes,
                java.lang.Boolean.FALSE == post.likes,
                (PrefsUtility.pref_behaviour_mark_posts_as_read() && post.clicked)
                        || this.isRead,
                post.saved,
                if (post.hidden) true else null
            )
        }

        fun markUpvoted(timestamp: TimestampUTC): Entry {
            return Entry(
                timestamp,
                true,
                false,
                this.isRead,
                this.isSaved,
                this.isHidden
            )
        }

        fun markDownvoted(timestamp: TimestampUTC): Entry {
            return Entry(
                timestamp,
                false,
                true,
                this.isRead,
                this.isSaved,
                this.isHidden
            )
        }

        fun markUnvoted(timestamp: TimestampUTC): Entry {
            return Entry(
                timestamp,
                false,
                false,
                this.isRead,
                this.isSaved,
                this.isHidden
            )
        }

        fun markRead(timestamp: TimestampUTC, read: Boolean): Entry {
            return Entry(
                timestamp,
                this.isUpvoted,
                this.isDownvoted,
                read,
                this.isSaved,
                this.isHidden
            )
        }

        fun markSaved(timestamp: TimestampUTC, isSaved: Boolean): Entry {
            return Entry(
                timestamp,
                this.isUpvoted,
                this.isDownvoted,
                this.isRead,
                isSaved,
                this.isHidden
            )
        }

        fun markHidden(timestamp: TimestampUTC, isHidden: Boolean?): Entry {
            return Entry(
                timestamp,
                this.isUpvoted,
                this.isDownvoted,
                this.isRead,
                this.isSaved,
                isHidden
            )
        }

        companion object {
            // For posts, this means "hidden". For comments, this means "collapsed".
            @Suppress("PropertyName")
            val CLEAR_ENTRY: Entry = Entry()
        }
    }

    private class ListenerNotifyOperator

        : ArgOperator<Listener?, RedditIdAndType?> {
        override fun operate(listener: Listener?, arg: RedditIdAndType?) {
            listener?.onRedditDataChange(arg)
        }

        companion object {
            val INSTANCE: ListenerNotifyOperator = ListenerNotifyOperator()
        }
    }

    private val mEntries = HashMap<RedditIdAndType?, Entry>()
    private val mLock = Any()

    private val mListeners = WeakReferenceListHashMapManager<RedditIdAndType?, Listener?>()

    fun addListener(
        thing: RedditIdAndType?,
        listener: Listener?
    ) {
        mListeners.add(thing, listener)
    }

    fun removeListener(
        thing: RedditIdAndType?,
        listener: Listener?
    ) {
        mListeners.remove(thing, listener)
    }

    private fun get(thing: RedditIdAndType?): Entry {
        val entry = mEntries.get(thing)

        if (entry == null) {
            return Entry.Companion.CLEAR_ENTRY
        } else {
            return entry
        }
    }

    private fun set(
        thing: RedditIdAndType?,
        existingValue: Entry,
        newValue: Entry
    ) {
        if (newValue.isClear) {
            if (!existingValue.isClear) {
                mEntries.remove(thing)
                RedditChangeDataIO.Companion.notifyUpdateStatic()
            }
        } else {
            mEntries.put(thing, newValue)
            RedditChangeDataIO.Companion.notifyUpdateStatic()
        }

        AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
            mListeners.map<RedditIdAndType?>(
                thing,
                ListenerNotifyOperator.INSTANCE,
                thing
            )
        })
    }

    private fun insertAll(entries: HashMap<RedditIdAndType?, Entry>) {
        synchronized(mLock) {
            for (entry in entries.entries) {
                val newEntry: Entry = entry.value
                val existingEntry = mEntries.get(entry.key)

                if (existingEntry == null
                    || existingEntry.mTimestamp.isLessThan(newEntry.mTimestamp)
                ) {
                    mEntries.put(entry.key, newEntry)
                }
            }
        }

        for (idAndType in entries.keys) {
            mListeners.map<RedditIdAndType?>(idAndType, ListenerNotifyOperator.INSTANCE, idAndType)
        }
    }

    fun update(timestamp: TimestampUTC, comment: RedditComment) {
        synchronized(mLock) {
            val existingEntry = get(comment.idAndType)
            val updatedEntry = existingEntry.update(timestamp, comment)
            set(comment.idAndType, existingEntry, updatedEntry)
        }
    }

    fun update(timestamp: TimestampUTC, post: RedditPost) {
        synchronized(mLock) {
            val existingEntry = get(post.idAndType)
            val updatedEntry = existingEntry.update(timestamp, post)
            set(post.idAndType, existingEntry, updatedEntry)
        }
    }

    fun markUpvoted(timestamp: TimestampUTC, thing: RedditIdAndType?) {
        synchronized(mLock) {
            val existingEntry = get(thing)
            val updatedEntry = existingEntry.markUpvoted(timestamp)
            set(thing, existingEntry, updatedEntry)
        }
    }

    fun markDownvoted(
        timestamp: TimestampUTC,
        thing: RedditIdAndType?
    ) {
        synchronized(mLock) {
            val existingEntry = get(thing)
            val updatedEntry = existingEntry.markDownvoted(timestamp)
            set(thing, existingEntry, updatedEntry)
        }
    }

    fun markUnvoted(timestamp: TimestampUTC, thing: RedditIdAndType?) {
        synchronized(mLock) {
            val existingEntry = get(thing)
            val updatedEntry = existingEntry.markUnvoted(timestamp)
            set(thing, existingEntry, updatedEntry)
        }
    }

    fun markSaved(
        timestamp: TimestampUTC,
        thing: RedditIdAndType?,
        saved: Boolean
    ) {
        synchronized(mLock) {
            val existingEntry = get(thing)
            val updatedEntry = existingEntry.markSaved(timestamp, saved)
            set(thing, existingEntry, updatedEntry)
        }
    }

    fun markHidden(
        timestamp: TimestampUTC,
        thing: RedditIdAndType?,
        hidden: Boolean?
    ) {
        synchronized(mLock) {
            val existingEntry = get(thing)
            val updatedEntry = existingEntry.markHidden(timestamp, hidden)
            set(thing, existingEntry, updatedEntry)
        }
    }

    fun markRead(
        timestamp: TimestampUTC,
        thing: RedditIdAndType?,
        read: Boolean
    ) {
        synchronized(mLock) {
            val existingEntry = get(thing)
            val updatedEntry = existingEntry.markRead(timestamp, read)
            set(thing, existingEntry, updatedEntry)
        }
    }

    fun isUpvoted(thing: RedditIdAndType?): Boolean {
        synchronized(mLock) {
            return get(thing).isUpvoted
        }
    }

    fun isDownvoted(thing: RedditIdAndType?): Boolean {
        synchronized(mLock) {
            return get(thing).isDownvoted
        }
    }

    fun isRead(thing: RedditIdAndType?): Boolean {
        synchronized(mLock) {
            return get(thing).isRead
        }
    }

    fun isSaved(thing: RedditIdAndType?): Boolean {
        synchronized(mLock) {
            return get(thing).isSaved
        }
    }

    fun isHidden(thing: RedditIdAndType?): Boolean? {
        synchronized(mLock) {
            return get(thing).isHidden
        }
    }

    private fun snapshot(): HashMap<RedditIdAndType?, Entry> {
        synchronized(mLock) {
            return HashMap<RedditIdAndType?, Entry>(mEntries)
        }
    }

    private fun prune(maxAge: TimeDuration) {
        val now = now()
        val timestampBoundary = now.subtract(maxAge)

        synchronized(mLock) {
            val iterator =                 mEntries.entries.iterator()
            val byTimestamp: SortedMap<TimestampUTC?, RedditIdAndType?> =                 TreeMap<TimestampUTC?, RedditIdAndType?>()

            while (iterator.hasNext()) {
                val entry = iterator.next()
                val timestamp = entry.value.mTimestamp
                byTimestamp.put(timestamp, entry.key)

                if (timestamp.isLessThan(timestampBoundary)) {
                    Log.i(
                        TAG, String.format(
                            "Pruning '%s' (%s old)",
                            entry.key,
                            now.elapsedPeriodSince(timestamp).format(
                                TimeStringsDebug,
                                2
                            )
                        )
                    )

                    iterator.remove()
                }
            }

            // Limit total number of entries to limit our memory usage. This is meant as a
            // safeguard, as the time-based pruning above should have removed enough already.
            val iter2 =                 byTimestamp.entries.iterator()
            while (iter2.hasNext()) {
                if (mEntries.size <= MAX_ENTRY_COUNT) {
                    break
                }

                val entry = iter2.next()

                Log.i(
                    TAG, String.format(
                        "Evicting '%s' (%s old)",
                        entry.value,
                        now.elapsedPeriodSince(entry.key!!).format(
                            TimeStringsDebug,
                            2
                        )
                    )
                )

                mEntries.remove(entry.value)
            }
        }
    }

    companion object {
        private const val TAG = "RedditChangeDataManager"

        private const val MAX_ENTRY_COUNT = 10000

        @Suppress("PropertyName")
        private val INSTANCE_MAP = HashMap<RedditAccount?, RedditChangeDataManager?>()

        fun getInstance(user: RedditAccount?): RedditChangeDataManager {
            synchronized(INSTANCE_MAP) {
                var result: RedditChangeDataManager?=INSTANCE_MAP.get(user)
                if (result == null) {
                    result = RedditChangeDataManager()
                    INSTANCE_MAP.put(user, result)
                }
                return result
            }
        }

        private fun snapshotAllUsers(): HashMap<RedditAccount?, HashMap<RedditIdAndType?, Entry>?> {
            val result = HashMap<RedditAccount?, HashMap<RedditIdAndType?, Entry>?>()

            synchronized(INSTANCE_MAP) {
                for (account in INSTANCE_MAP.keys) {
                    result.put(account, getInstance(account).snapshot())
                }
            }

            return result
        }

        @Throws(IOException::class)
        fun writeAllUsers(dos: ExtendedDataOutputStream) {
            Log.i(TAG, "Taking snapshot...")

            val data: HashMap<RedditAccount?, HashMap<RedditIdAndType?, Entry>?> =                 snapshotAllUsers()

            Log.i(TAG, "Writing to stream...")

            val userDataSet =                 data.entries

            dos.writeInt(userDataSet.size)

            for (userData
            in userDataSet) {
                val username = userData.key!!.canonicalUsername
                dos.writeUTF(username)

                val entrySet = userData.value!!.entries

                dos.writeInt(entrySet.size)

                for (entry in entrySet) {
                    dos.writeUTF(entry.key!!.value)
                    entry.value.writeTo(dos)
                }

                if (isSensitiveDebugLoggingEnabled) {
                    Log.i(
                        TAG,
                        String.format(
                            Locale.US,
                            "Wrote %d entries for user '%s'",
                            entrySet.size,
                            username
                        )
                    )
                }
            }

            Log.i(TAG, "All entries written to stream.")
        }

        @Throws(IOException::class)
        fun readAllUsers(
            dis: ExtendedDataInputStream,
            context: Context
        ) {
            Log.i(TAG, "Reading from stream...")

            val userCount = dis.readInt()

            Log.i(TAG, userCount.toString() + " users to read.")

            for (i in 0..<userCount) {
                val username = dis.readUTF()
                val entryCount = dis.readInt()

                if (isSensitiveDebugLoggingEnabled) {
                    Log.i(
                        TAG,
                        String.format(
                            Locale.US,
                            "Reading %d entries for user '%s'",
                            entryCount,
                            username
                        )
                    )
                }

                val entries = HashMap<RedditIdAndType?, Entry>(entryCount)

                for (j in 0..<entryCount) {
                    val thingId = RedditIdAndType(dis.readUTF())
                    val entry = RedditChangeDataManager.Entry(dis)
                    entries.put(thingId, entry)
                }

                Log.i(TAG, "Getting account...")

                val account: RedditAccount?=RedditAccountManager.Companion.getInstance(context).getAccount(username)

                if (account == null) {
                    if (isSensitiveDebugLoggingEnabled) {
                        Log.i(
                            TAG,
                            String.format(
                                Locale.US,
                                "Skipping user '%s' as the account no longer exists",
                                username
                            )
                        )
                    }
                } else {
                    getInstance(account).insertAll(entries)
                    if (isSensitiveDebugLoggingEnabled) {
                        Log.i(
                            TAG,
                            String.format(
                                Locale.US,
                                "Finished inserting entries for user '%s'",
                                username
                            )
                        )
                    }
                }
            }

            Log.i(TAG, "All entries read from stream.")
        }

        fun pruneAllUsersDefaultMaxAge() {
            pruneAllUsersWhereOlderThan(PrefsUtility.pref_cache_maxage_entry())
        }

        fun pruneAllUsersWhereOlderThan(maxAge: TimeDuration) {
            Log.i(TAG, "Pruning for all users...")

            val users: MutableSet<RedditAccount?>

            synchronized(INSTANCE_MAP) {
                users = HashSet<RedditAccount?>(INSTANCE_MAP.keys)
            }

            for (user in users) {
                val managerForUser: RedditChangeDataManager = getInstance(user)
                managerForUser.prune(maxAge)
            }

            Log.i(TAG, "Pruning complete.")
        }
    }
}
