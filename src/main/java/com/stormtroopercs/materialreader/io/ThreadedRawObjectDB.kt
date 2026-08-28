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
package com.stormtroopercs.materialreader.io

import com.stormtroopercs.materialreader.common.TimestampBound
import com.stormtroopercs.materialreader.common.TriggerableThread
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.common.time.TimestampUTC.Companion.oldest
import java.util.concurrent.LinkedBlockingQueue

class ThreadedRawObjectDB<K, V : WritableObject<K>, F>
    (
    private val db: RawObjectDB<K, V>,
    private val alternateSource: CacheDataSource<K, V, F>
) : CacheDataSource<K, V, F> {
    private val writeThread = TriggerableThread(Runnable { this.doWrite() }, 1500)

    private val readThread = TriggerableThread(Runnable { this.doRead() }, 0)

    private val toWrite = HashMap<K, V>()
    private val toRead: LinkedBlockingQueue<ReadOperation> = LinkedBlockingQueue<ReadOperation>()
    private val ioLock = Any()

    private fun doWrite() {
        synchronized(ioLock) {
            val values: ArrayList<V>
            synchronized(toWrite) {
                values = ArrayList<V>(toWrite.values)
                toWrite.clear()
            }
            db.putAll(values)
        }
    }

    private fun doRead() {
        synchronized(ioLock) {
            while (!toRead.isEmpty()) {
                toRead.remove().run()
            }
        }
    }

    override fun performRequest(
        key: K, timestampBound: TimestampBound?,
        handler: RequestResponseHandler<V, F>
    ) {
        toRead.offer(SingleReadOperation(timestampBound!!, handler, key))
        readThread.trigger()
    }

    override fun performRequest(
        keys: MutableCollection<K>, timestampBound: TimestampBound?,
        handler: RequestResponseHandler<HashMap<K, V>, F>
    ) {
        toRead.offer(BulkReadOperation(timestampBound!!, handler, keys))
        readThread.trigger()
    }

    override fun performWrite(value: V) {
        synchronized(toWrite) {
            toWrite.put(value.key, value)
        }

        writeThread.trigger()
    }

    override fun performWrite(values: MutableCollection<V>) {
        synchronized(toWrite) {
            for (value in values) {
                toWrite.put(value.key, value)
            }
        }

        writeThread.trigger()
    }

    private inner class BulkReadOperation(
        timestampBound: TimestampBound,
        val responseHandler: RequestResponseHandler<HashMap<K, V>, F>,
        val keys: MutableCollection<K>
    ) : ReadOperation(timestampBound) {
        override fun run() {
            val existingResult = HashMap<K, V>(keys.size)
            var oldestTimestamp: TimestampUTC? = null

            synchronized(toWrite) {
                val iter = keys.iterator()
                while (iter.hasNext()) {
                    val key = iter.next()
                    val writeCacheResult = toWrite.get(key)
                    if (writeCacheResult != null && timestampBound.verifyTimestamp(
                            writeCacheResult.timestamp
                        )
                    ) {
                        iter.remove()
                        existingResult.put(key, writeCacheResult)
                        if (oldestTimestamp == null) {
                            oldestTimestamp = writeCacheResult.timestamp
                        } else {
                            oldestTimestamp = oldest(
                                oldestTimestamp,
                                writeCacheResult.timestamp
                            )
                        }
                    }
                }
            }

            if (keys.isEmpty()) {
                responseHandler.onRequestSuccess(existingResult, oldestTimestamp)
                return
            }

            val iter = keys.iterator()

            while (iter.hasNext()) {
                val key = iter.next()
                val dbResult = db.getById(key) // TODO this is pretty inefficient
                if (dbResult != null
                    && timestampBound.verifyTimestamp(dbResult.timestamp)
                ) {
                    iter.remove()
                    existingResult.put(key, dbResult)
                    if (oldestTimestamp == null) {
                        oldestTimestamp = dbResult.timestamp
                    } else {
                        oldestTimestamp = oldest(
                            oldestTimestamp,
                            dbResult.timestamp
                        )
                    }
                }
            }

            if (keys.isEmpty()) {
                responseHandler.onRequestSuccess(existingResult, oldestTimestamp)
                return
            }

            val outerOldestTimestamp = oldestTimestamp

            alternateSource.performRequest(
                keys,
                timestampBound,
                object : RequestResponseHandler<HashMap<K, V>, F> {
                    override fun onRequestFailed(failureReason: F) {
                        responseHandler.onRequestFailed(failureReason)
                    }

                    override fun onRequestSuccess(
                        result: HashMap<K, V>,
                        timeCached: TimestampUTC?
                    ) {
                        val timestamp: TimestampUTC? = if (outerOldestTimestamp == null)
                            timeCached
                        else
                            oldest(outerOldestTimestamp, timeCached!!)

                        performWrite(result.values)
                        existingResult.putAll(result)
                        responseHandler.onRequestSuccess(
                            existingResult,
                            timestamp
                        )
                    }
                })
        }
    }

    private inner class SingleReadOperation(
        timestampBound: TimestampBound,
        val responseHandler: RequestResponseHandler<V, F>,
        val key: K
    ) : ReadOperation(timestampBound) {
        override fun run() {
            synchronized(toWrite) {
                val writeCacheResult = toWrite.get(key)
                if (writeCacheResult != null && timestampBound.verifyTimestamp(
                        writeCacheResult.timestamp
                    )
                ) {
                    responseHandler.onRequestSuccess(
                        writeCacheResult,
                        writeCacheResult.timestamp
                    )
                    return
                }
            }

            val dbResult = db.getById(key)
            if (dbResult != null
                && timestampBound.verifyTimestamp(dbResult.timestamp)
            ) {
                responseHandler.onRequestSuccess(dbResult, dbResult.timestamp)
                return
            }

            alternateSource.performRequest(
                key,
                timestampBound,
                object : RequestResponseHandler<V, F> {
                    override fun onRequestFailed(failureReason: F) {
                        responseHandler.onRequestFailed(failureReason)
                    }

                    override fun onRequestSuccess(
                        result: V,
                        timeCached: TimestampUTC?
                    ) {
                        performWrite(result)
                        responseHandler.onRequestSuccess(result, timeCached)
                    }
                })
        }
    }

    private abstract class ReadOperation(val timestampBound: TimestampBound) {
        abstract fun run()
    }
}
