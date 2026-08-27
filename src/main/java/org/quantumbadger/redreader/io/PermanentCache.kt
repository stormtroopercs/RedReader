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
 ******************************************************************************/
package org.quantumbadger.redreader.io

import org.quantumbadger.redreader.common.TimestampBound
import org.quantumbadger.redreader.common.collections.WeakReferenceListManager
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.common.time.TimestampUTC.Companion.oldest

class PermanentCache<K, V : WritableObject<K>, F>
    (private val cacheDataSource: CacheDataSource<K, V, F>) : CacheDataSource<K, V, F> {
    private val cached: HashMap<K, CacheEntry> = HashMap<K, CacheEntry>()

    private val updatedVersionListenerNotifier = UpdatedVersionListenerNotifier<K, V>()

    override fun performRequest(
        key: K,
        timestampBound: TimestampBound?,
        handler: RequestResponseHandler<V, F>
    ) {
        performRequest(key, timestampBound, handler, null)
    }

    @Synchronized
    override fun performRequest(
        keys: MutableCollection<K>, timestampBound: TimestampBound?,
        handler: RequestResponseHandler<HashMap<K, V>, F>
    ) {
        val keysRemaining = HashSet<K>(keys)
        val cacheResult = HashMap<K, V>(keys.size)
        var oldestTimestamp: TimestampUTC? = null

        for (key in keys) {
            val entry: CacheEntry? = cached.get(key)
            if (entry != null) {
                val value: V = entry.data
                if (timestampBound!!.verifyTimestamp(value.timestamp)) {
                    keysRemaining.remove(key)
                    cacheResult.put(key, value)
                    if (oldestTimestamp == null) {
                        oldestTimestamp = value.timestamp
                    } else {
                        oldestTimestamp = oldest(
                            oldestTimestamp,
                            value.timestamp
                        )
                    }
                }
            }
        }

        if (!keysRemaining.isEmpty()) {
            val outerOldestTimestamp = oldestTimestamp

            cacheDataSource.performRequest(
                keysRemaining,
                timestampBound,
                object : RequestResponseHandler<HashMap<K, V>, F> {
                    override fun onRequestFailed(failureReason: F) {
                        handler.onRequestFailed(failureReason)
                    }

                    override fun onRequestSuccess(
                        result: HashMap<K, V>,
                        timeCached: TimestampUTC?
                    ) {
                        cacheResult.putAll(result)

                        val timestamp: TimestampUTC? = if (outerOldestTimestamp == null)
                            timeCached
                        else
                            oldest(outerOldestTimestamp, timeCached!!)

                        handler.onRequestSuccess(
                            cacheResult,
                            timestamp
                        )
                    }
                })
        } else {
            handler.onRequestSuccess(cacheResult, oldestTimestamp)
        }
    }

    @Synchronized
    override fun performWrite(value: V) {
        put(value, true)
    }

    override fun performWrite(values: MutableCollection<V>) {
        put(values, true)
    }

    @Synchronized
    fun performRequest(
        key: K, timestampBound: TimestampBound?,
        handler: RequestResponseHandler<V, F>,
        updatedVersionListener: UpdatedVersionListener<K, V>?
    ) {
        if (timestampBound != null) {
            val existingEntry: CacheEntry? = cached.get(key)
            if (existingEntry != null) {
                val existing: V = existingEntry.data
                if (timestampBound.verifyTimestamp(existing.timestamp)) {
                    handler.onRequestSuccess(existing, existing.timestamp)
                    return
                }
            }
        }

        cacheDataSource.performRequest(
            key,
            timestampBound,
            object : RequestResponseHandler<V, F> {
                override fun onRequestFailed(failureReason: F) {
                    handler.onRequestFailed(failureReason)
                }

                override fun onRequestSuccess(result: V, timeCached: TimestampUTC?) {
                    synchronized(this@PermanentCache) {
                        put(result, false)
                        if (updatedVersionListener != null) {
                            cached.get(key)!!.listeners.add(updatedVersionListener)
                        }
                        handler.onRequestSuccess(result, timeCached)
                    }
                }
            })
    }

    @Synchronized
    fun forceUpdate(key: K) {
        cacheDataSource.performRequest(key, null, object : RequestResponseHandler<V, F> {
            override fun onRequestFailed(failureReason: F) {
            }

            override fun onRequestSuccess(result: V, timeCached: TimestampUTC?) {
                put(result, false)
            }
        })
    }

    @Synchronized
    private fun put(value: V, writeDown: Boolean) {
        val oldEntry: CacheEntry? = cached.get(value.key)

        if (oldEntry != null) {
            cached.put(value.key, CacheEntry(value, oldEntry.listeners))
            oldEntry.listeners.map<V>(updatedVersionListenerNotifier, value)
        } else {
            cached.put(value.key, CacheEntry(value))
        }

        if (writeDown) {
            cacheDataSource.performWrite(value)
        }
    }

    @Synchronized
    private fun put(values: MutableCollection<V>, writeDown: Boolean) {
        for (value in values) {
            val oldEntry: CacheEntry? = cached.get(value.key)

            if (oldEntry != null) {
                cached.put(value.key, CacheEntry(value, oldEntry.listeners))
                oldEntry.listeners.map<V>(updatedVersionListenerNotifier, value)
            } else {
                cached.put(value.key, CacheEntry(value))
            }
        }

        if (writeDown) {
            cacheDataSource.performWrite(values)
        }
    }

    private inner class CacheEntry(
        val data: V,
        val listeners: WeakReferenceListManager<UpdatedVersionListener<K, V>> = WeakReferenceListManager<UpdatedVersionListener<K, V>>()
    )
}
