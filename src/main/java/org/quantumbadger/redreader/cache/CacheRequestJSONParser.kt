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
package org.quantumbadger.redreader.cache

import android.content.Context
import org.quantumbadger.redreader.activities.BugReportActivity.Companion.handleGlobalError
import org.quantumbadger.redreader.cache.CacheRequest.RequestFailureType
import org.quantumbadger.redreader.common.CachedThreadPool
import org.quantumbadger.redreader.common.FunctionOneArgWithReturn
import org.quantumbadger.redreader.common.General.getGeneralErrorForFailure
import org.quantumbadger.redreader.common.General.ignoreIOException
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.UriString.Companion.from
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.http.FailedRequestBody
import org.quantumbadger.redreader.jsonwrap.JsonValue
import org.quantumbadger.redreader.reddit.api.RedditPostActions.ActionDescriptionPair.Companion.from
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import org.quantumbadger.redreader.common.General

class CacheRequestJSONParser(
    private val mContext: Context,
    private val mListener: Listener
) : CacheRequestCallbacks {
    interface Listener {
        fun onJsonParsed(
            result: JsonValue,
            timestamp: TimestampUTC?,
            session: UUID,
            fromCache: Boolean
        )

        fun onFailure(error: RRError)

        fun onDownloadNecessary() {
            // Do nothing by default
        }
    }

    private val mNotifiedFailure = AtomicBoolean(false)

    override fun onDataStreamAvailable(
        streamFactory: GenericFactory<SeekableInputStream, IOException?>,
        timestamp: TimestampUTC?,
        session: UUID,
        fromCache: Boolean,
        mimetype: String?
    ) {
        try {
            mThreadPool.add(Runnable {
                val jsonValue: JsonValue
                try {
                    streamFactory.create().use { `is` ->
                        jsonValue = JsonValue.Companion.parse(`is`)
                    }
                } catch (e: IOException) {
                    if (!mNotifiedFailure.getAndSet(true)) {
                        mListener.onFailure(
                            getGeneralErrorForFailure(
                                mContext,
                                RequestFailureType.PARSE,
                                e,
                                null,
                                null,
                                General.ignoreIOException<SeekableInputStream?>(streamFactory)
                                    .filter<FailedRequestBody>(FunctionOneArgWithReturn { `is`: Param? ->
                                        FailedRequestBody.Companion.from(
                                            `is`
                                        )
                                    })
                            )
                        )
                    }
                    return@add
                }
                try {
                    mListener.onJsonParsed(jsonValue, timestamp, session, fromCache)
                } catch (e: Exception) {
                    handleGlobalError(mContext, e)
                }
            })
        } catch (e: Exception) {
            if (!mNotifiedFailure.getAndSet(true)) {
                onFailure(
                    getGeneralErrorForFailure(
                        mContext,
                        RequestFailureType.STORAGE,
                        e,
                        null,
                        null,
                        Optional.Companion.empty<FailedRequestBody>()
                    )
                )
            }
        }
    }

    override fun onFailure(error: RRError) {
        if (!mNotifiedFailure.getAndSet(true)) {
            mListener.onFailure(error)
        }
    }

    companion object {
        private val mThreadPool = CachedThreadPool(5, "JSONParser")
    }
}
