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
import android.os.SystemClock
import android.util.Log
import com.stormtroopercs.materialreader.account.RedditAccountManager
import com.stormtroopercs.materialreader.cache.CacheManager
import com.stormtroopercs.materialreader.cache.CacheRequest
import com.stormtroopercs.materialreader.cache.CacheRequest.DownloadQueueType
import com.stormtroopercs.materialreader.cache.CacheRequest.RequestFailureType
import com.stormtroopercs.materialreader.cache.CacheRequestJSONParser
import com.stormtroopercs.materialreader.cache.downloadstrategy.DownloadStrategyAlways
import com.stormtroopercs.materialreader.cache.downloadstrategy.DownloadStrategyIfTimestampOutsideBounds
import com.stormtroopercs.materialreader.common.Constants
import com.stormtroopercs.materialreader.common.General.getGeneralErrorForFailure
import com.stormtroopercs.materialreader.common.GenericFactory
import com.stormtroopercs.materialreader.common.Optional
import com.stormtroopercs.materialreader.common.Priority
import com.stormtroopercs.materialreader.common.RRError
import com.stormtroopercs.materialreader.common.StringUtils
import com.stormtroopercs.materialreader.common.TimestampBound
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.common.time.TimeDuration.Companion.minutes
import com.stormtroopercs.materialreader.common.time.TimestampUTC
import com.stormtroopercs.materialreader.http.FailedRequestBody
import com.stormtroopercs.materialreader.http.PostField
import com.stormtroopercs.materialreader.http.body.HTTPRequestBody.PostFields
import com.stormtroopercs.materialreader.image.ImageInfo.Companion.parseRedgifsV2
import com.stormtroopercs.materialreader.jsonwrap.JsonValue
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import com.stormtroopercs.materialreader.common.General

object RedgifsAPIV2 {
    private const val TAG = "RedgifsAPIV2"

    @Suppress("PropertyName")
    private val TOKEN: AtomicReference<AuthToken?> = AtomicReference<AuthToken?>(
        AuthToken("", 0)
    )

    val latestToken: String
        get() = TOKEN.get()!!.token

    private fun requestMetadata(
        context: Context,
        imageId: String,
        priority: Priority,
        listener: GetImageInfoListener
    ) {
        val apiUrl = UriString(
            "https://api.redgifs.com/v2/gifs/"
                    + StringUtils.asciiLowercase(imageId)
        )

        CacheManager.Companion.getInstance(context).makeRequest(
            CacheRequest(
                apiUrl,
                RedditAccountManager.Companion.getAnon(),
                null,
                priority,  // RedGifs V2 links expire after an undocumented period of time
                DownloadStrategyIfTimestampOutsideBounds(
                    TimestampBound.Companion.notOlderThan(minutes(10))
                ),
                Constants.FileType.IMAGE_INFO,
                DownloadQueueType.REDGIFS_API_V2,
                context,
                CacheRequestJSONParser(context, object : CacheRequestJSONParser.Listener {
                    override fun onJsonParsed(
                        result: JsonValue,
                        timestamp: TimestampUTC,
                        session: UUID,
                        fromCache: Boolean
                    ) {
                        try {
                            listener.onSuccess(
                                parseRedgifsV2(
                                    result
                                        .getObjectAtPath("gif")
                                        .orThrow<RuntimeException>(GenericFactory {
                                            RuntimeException(
                                                "No element 'gif'"
                                            )
                                        })
                                )
                            )

                            Log.i(TAG, "Got RedGifs v2 metadata")
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
                                            result
                                        )
                                    )
                                )
                            )
                        }
                    }

                    override fun onFailure(error: RRError) {
                        listener.onFailure(error)
                    }
                })
            )
        )
    }

    fun getImageInfo(
        context: Context,
        imageId: String,
        priority: Priority,
        listener: GetImageInfoListener
    ) {
        if (TOKEN.get()!!.isValid) {
            Log.i(TAG, "Existing token still valid")
            requestMetadata(context, imageId, priority, listener)
            return
        }

        Log.i(TAG, "Retrieving new token")

        val apiUrl = UriString("https://api.redgifs.com/v2/oauth/client")

        CacheManager.Companion.getInstance(context).makeRequest(
            CacheRequest(
                apiUrl,
                RedditAccountManager.Companion.getAnon(),
                null,
                priority,
                DownloadStrategyAlways.Companion.INSTANCE,
                Constants.FileType.IMAGE_INFO,
                DownloadQueueType.IMMEDIATE,
                PostFields(
                    PostField("grant_type", "client_credentials"),
                    PostField(
                        Constants.OA_CI,
                        "1828d09da4e-1011-a880-0005-d2ecbe8daab3"
                    ),
                    PostField(
                        Constants.OA_CS,
                        "yCarP8TUpIr6J2W8YW+vgSRb8HuBd9koW/nkPtsQaP8="
                    )
                ),
                context,
                CacheRequestJSONParser(context, object : CacheRequestJSONParser.Listener {
                    override fun onJsonParsed(
                        result: JsonValue,
                        timestamp: TimestampUTC,
                        session: UUID,
                        fromCache: Boolean
                    ) {
                        val accessToken = result.getStringAtPath("access_token")

                        if (accessToken.isEmpty) {
                            Log.i(TAG, "Failed to get RedGifs v2 token: result not present")
                            listener.onFailure(
                                getGeneralErrorForFailure(
                                    context,
                                    RequestFailureType.REQUEST,
                                    null,
                                    null,
                                    apiUrl,
                                    Optional.Companion.of<FailedRequestBody>(
                                        FailedRequestBody(
                                            result
                                        )
                                    )
                                )
                            )
                            return
                        }

                        Log.i(TAG, "Got RedGifs v2 token")

                        TOKEN.set(AuthToken.expireIn10Mins(accessToken.get()))

                        requestMetadata(context, imageId, priority, listener)
                    }

                    override fun onFailure(error: RRError) {
                        Log.i(TAG, "Failed to get RedGifs v2 token")
                        listener.onFailure(error)
                    }
                })

            )
        )
    }


    private class AuthToken(val token: String, private val expireAt: Long) {
        val isValid: Boolean
            get() = !token.isEmpty() && expireAt > SystemClock.uptimeMillis()

        companion object {
            fun expireIn10Mins(token: String): AuthToken {
                return AuthToken(token, SystemClock.uptimeMillis() + 10L * 60 * 1000)
            }
        }
    }
}
