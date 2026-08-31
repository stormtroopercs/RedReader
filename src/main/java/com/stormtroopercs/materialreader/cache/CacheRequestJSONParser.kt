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
package com.stormtroopercs.materialreader.cache

import android.content.Context
import com.stormtroopercs.materialreader.cache.CacheRequest.RequestFailureType
import com.stormtroopercs.materialreader.common.BugReporter.handleGlobalError
import com.stormtroopercs.materialreader.common.CachedThreadPool
import com.stormtroopercs.materialreader.common.FunctionOneArgWithReturn
import com.stormtroopercs.materialreader.common.General
import com.stormtroopercs.materialreader.common.General.getGeneralErrorForFailure
import com.stormtroopercs.materialreader.common.General.ignoreIOException
import com.stormtroopercs.materialreader.common.GenericFactory
import com.stormtroopercs.materialreader.common.Optional
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.UriString.Companion.from
import com.stormtroopercs.materialreader.common.datastream.SeekableInputStream
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.http.FailedRequestBody
import com.stormtroopercs.materialreader.jsonwrap.JsonValue
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class CacheRequestJSONParser(
	private val mContext: Context,
	private val mListener: Listener,
) : CacheRequestCallbacks {
	interface Listener {
		fun onJsonParsed(
			result: JsonValue,
			timestamp: TimestampUTC,
			session: UUID,
			fromCache: Boolean,
		)

		fun onFailure(error: RRError)

		fun onDownloadNecessary() {
			// Do nothing by default
		}
	}

	private val mNotifiedFailure = AtomicBoolean(false)

	override fun onDataStreamAvailable(
		streamFactory: GenericFactory<SeekableInputStream, IOException>,
		timestamp: TimestampUTC,
		session: UUID,
		fromCache: Boolean,
		mimetype: String?,
	) {
		try {
			mThreadPool.add(
				Runnable {
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
									General.ignoreIOException<SeekableInputStream>(streamFactory)
										.filter<FailedRequestBody>(
											FunctionOneArgWithReturn { `is` ->
												FailedRequestBody.Companion.from(
													`is`,
												)
											},
										),
								),
							)
						}
						return@Runnable
					}
					try {
						mListener.onJsonParsed(jsonValue, timestamp, session, fromCache)
					} catch (e: Exception) {
						handleGlobalError(mContext, e)
					}
				},
			)
		} catch (e: Exception) {
			if (!mNotifiedFailure.getAndSet(true)) {
				onFailure(
					getGeneralErrorForFailure(
						mContext,
						RequestFailureType.STORAGE,
						e,
						null,
						null,
						Optional.Companion.empty<FailedRequestBody>(),
					),
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
