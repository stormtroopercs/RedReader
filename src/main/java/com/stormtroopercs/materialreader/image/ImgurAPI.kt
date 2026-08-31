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
package com.stormtroopercs.materialreader.image

import android.content.Context
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.cache.CacheManager
import com.stormtroopercs.materialreader.cache.CacheRequest
import com.stormtroopercs.materialreader.cache.CacheRequest.DownloadQueueType
import com.stormtroopercs.materialreader.cache.CacheRequest.RequestFailureType
import com.stormtroopercs.materialreader.cache.CacheRequestJSONParser
import com.stormtroopercs.materialreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import com.stormtroopercs.materialreader.common.Constants
import com.stormtroopercs.materialreader.common.General.getGeneralErrorForFailure
import com.stormtroopercs.materialreader.common.Optional
import com.stormtroopercs.materialreader.common.Priority
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.http.FailedRequestBody
import com.stormtroopercs.materialreader.image.ImageInfo.Companion.parseImgur
import com.stormtroopercs.materialreader.jsonwrap.JsonValue
import java.util.UUID

object ImgurAPI {
	fun getAlbumInfo(
		context: Context,
		albumUrl: UriString,
		albumId: String?,
		priority: Priority,
		listener: GetAlbumInfoListener,
	) {
		val apiUrl = UriString("https://api.imgur.com/2/album/" + albumId + ".json")

		CacheManager.Companion.getInstance(context).makeRequest(
			CacheRequest(
				apiUrl,
				RedditAccountManager.Companion.getAnon(),
				null,
				priority,
				DownloadStrategyIfNotCached.Companion.INSTANCE,
				Constants.FileType.IMAGE_INFO,
				DownloadQueueType.IMMEDIATE,
				context,
				CacheRequestJSONParser(
					context,
					object : CacheRequestJSONParser.Listener {
						override fun onJsonParsed(
							result: JsonValue,
							timestamp: TimestampUTC,
							session: UUID,
							fromCache: Boolean,
						) {
							try {
								val outer = result.asObject()!!.getObject("album")
								listener.onSuccess(AlbumInfo.parseImgur(albumUrl, outer!!))
							} catch (t: Throwable) {
								listener.onFailure(
									getGeneralErrorForFailure(
										context,
										RequestFailureType.PARSE,
										t,
										null,
										apiUrl,
										Optional.Companion.of<FailedRequestBody>(
											FailedRequestBody(
												result,
											),
										),
									),
								)
							}
						}

						override fun onFailure(error: RRError) {
							listener.onFailure(error)
						}
					},
				),
			),
		)
	}

	fun getImageInfo(
		context: Context,
		imageId: String?,
		priority: Priority,
		listener: GetImageInfoListener,
	) {
		val apiUrl = UriString("https://api.imgur.com/2/image/" + imageId + ".json")

		CacheManager.Companion.getInstance(context).makeRequest(
			CacheRequest(
				apiUrl,
				RedditAccountManager.Companion.getAnon(),
				null,
				priority,
				DownloadStrategyIfNotCached.Companion.INSTANCE,
				Constants.FileType.IMAGE_INFO,
				DownloadQueueType.IMMEDIATE,
				context,
				CacheRequestJSONParser(
					context,
					object : CacheRequestJSONParser.Listener {
						override fun onJsonParsed(
							result: JsonValue,
							timestamp: TimestampUTC,
							session: UUID,
							fromCache: Boolean,
						) {
							try {
								val outer = result.asObject()!!.getObject("image")
								listener.onSuccess(parseImgur(outer))
							} catch (t: Throwable) {
								listener.onFailure(
									getGeneralErrorForFailure(
										context,
										RequestFailureType.PARSE,
										t,
										null,
										apiUrl,
										Optional.Companion.of<FailedRequestBody>(
											FailedRequestBody(
												result,
											),
										),
									),
								)
							}
						}

						override fun onFailure(error: RRError) {
							listener.onFailure(error)
						}
					},
				),
			),
		)
	}
}
