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
import android.util.Log
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.activities.BugReportActivity.Companion.addGlobalError
import org.quantumbadger.redreader.activities.BugReportActivity.Companion.handleGlobalError
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategy
import org.quantumbadger.redreader.common.General.getGeneralErrorForFailure
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.http.FailedRequestBody
import org.quantumbadger.redreader.http.body.HTTPRequestBody
import java.io.IOException
import java.util.UUID
import org.quantumbadger.redreader.common.General

class CacheRequest private constructor(
    url: UriString,
    user: RedditAccount,
    requestSession: UUID?,
    priority: Priority,
    downloadStrategy: DownloadStrategy,
    fileType: Int,
    queueType: DownloadQueueType?,
    requestBody: HTTPRequestBody?,
    cache: Boolean,
    context: Context,
    private val mCallbacks: CacheRequestCallbacks
) : Comparable<CacheRequest?> {
    enum class DownloadQueueType {
        REDDIT_API,
        IMGUR_API,
        IMMEDIATE,
        IMAGE_PRECACHE,
        REDGIFS_API_V2
    }

    enum class RequestFailureType {
        CONNECTION,
        REQUEST,
        STORAGE,
        CACHE_MISS,
        CANCELLED,
        MALFORMED_URL,
        PARSE,
        DISK_SPACE,
        REDDIT_REDIRECT,
        PARSE_IMGUR,
        UPLOAD_FAIL_IMGUR,
        CACHE_DIR_DOES_NOT_EXIST
    }

    val url: UriString
    val user: RedditAccount
    val requestSession: UUID?

    val priority: Priority

    val downloadStrategy: DownloadStrategy

    val fileType: Int

    val queueType: DownloadQueueType?
    val requestBody: Optional<HTTPRequestBody?>

    val cache: Boolean

    private var download: CacheDownload?=null
    private var cancelled = false

    val context: Context

    // Called by CacheDownload
    @Synchronized
    fun setDownload(download: CacheDownload?): Boolean {
        if (cancelled) {
            return false
        }
        this.download = download
        return true
    }

    // Can be called to cancel the request
    @Synchronized
    fun cancel() {
        cancelled = true

        if (download != null) {
            download!!.cancel()
            download = null
        }
    }

    constructor(
        url: UriString,
        user: RedditAccount,
        requestSession: UUID?,
        priority: Priority,
        downloadStrategy: DownloadStrategy,
        fileType: Int,
        queueType: DownloadQueueType?,
        cache: Boolean,
        context: Context,
        callbacks: CacheRequestCallbacks
    ) : this(
        url,
        user,
        requestSession,
        priority,
        downloadStrategy,
        fileType,
        queueType,
        null,
        cache,
        context,
        callbacks
    )

    constructor(
        url: UriString,
        user: RedditAccount,
        requestSession: UUID?,
        priority: Priority,
        downloadStrategy: DownloadStrategy,
        fileType: Int,
        queueType: DownloadQueueType?,
        context: Context,
        callbacks: CacheRequestCallbacks
    ) : this(
        url,
        user,
        requestSession,
        priority,
        downloadStrategy,
        fileType,
        queueType,
        true,
        context,
        callbacks
    )

    constructor(
        url: UriString,
        user: RedditAccount,
        requestSession: UUID?,
        priority: Priority,
        downloadStrategy: DownloadStrategy,
        fileType: Int,
        queueType: DownloadQueueType?,
        requestBody: HTTPRequestBody?,
        context: Context,
        callbacks: CacheRequestCallbacks
    ) : this(
        url,
        user,
        requestSession,
        priority,
        downloadStrategy,
        fileType,
        queueType,
        requestBody,
        false,
        context,
        callbacks
    )

    // TODO remove this huge constructor, make mutable
    init {
        this.context = context.getApplicationContext()

        if (user == null) {
            throw NullPointerException(
                "User was null - set to empty string for anonymous"
            )
        }

        require(!(!downloadStrategy.shouldDownloadWithoutCheckingCache() && requestBody != null)) { "Should not perform cache lookup for POST requests" }

        this.url = url
        this.user = user
        this.requestSession = requestSession
        this.priority = priority
        this.downloadStrategy = downloadStrategy
        this.fileType = fileType
        this.queueType = queueType
        this.requestBody = Optional.Companion.ofNullable<HTTPRequestBody?>(requestBody)
        this.cache = (requestBody == null) && cache

        if (url == null) {
            notifyFailure(
                getGeneralErrorForFailure(
                    this.context,
                    RequestFailureType.MALFORMED_URL,
                    null,
                    null,
                    null,
                    Optional.Companion.empty<FailedRequestBody>()
                )
            )
            cancel()
        }
    }

    // Queue helpers
    override fun compareTo(another: CacheRequest): Int {
        return if (priority.isHigherPriorityThan(another.priority))
            -1
        else
            (if (another.priority.isHigherPriorityThan(priority)) 1 else 0)
    }

    // Callbacks
    private fun onCallbackException(t: Throwable) {
        Log.e("CacheRequest", "Exception thrown from callback", t)
        handleGlobalError(context, t)
    }

    fun notifyDataStreamAvailable(
        streamFactory: GenericFactory<SeekableInputStream, IOException?>,
        timestamp: TimestampUTC?,
        session: UUID,
        fromCache: Boolean,
        mimetype: String?
    ) {
        mCallbacks.onDataStreamAvailable(streamFactory, timestamp, session, fromCache, mimetype)
    }

    fun notifyDataStreamComplete(
        streamFactory: GenericFactory<SeekableInputStream, IOException?>,
        timestamp: TimestampUTC,
        session: UUID,
        fromCache: Boolean,
        mimetype: String?
    ) {
        mCallbacks.onDataStreamComplete(streamFactory, timestamp, session, fromCache, mimetype)
    }

    fun notifyFailure(error: RRError) {
        try {
            mCallbacks.onFailure(error)
        } catch (t1: Throwable) {
            onCallbackException(t1)
        }
    }

    fun notifyProgress(
        authorizationInProgress: Boolean,
        bytesRead: Long,
        totalBytes: Long
    ) {
        try {
            mCallbacks.onProgress(authorizationInProgress, bytesRead, totalBytes)
        } catch (t: Throwable) {
            onCallbackException(t)
        }
    }

    fun notifyCacheFileWritten(
        cacheFile: ReadableCacheFile,
        timestamp: TimestampUTC?,
        session: UUID,
        fromCache: Boolean,
        mimetype: String?
    ) {
        try {
            mCallbacks.onCacheFileWritten(cacheFile, timestamp, session, fromCache, mimetype)
        } catch (t: Throwable) {
            onCallbackException(t)
        }
    }

    fun notifyDownloadNecessary() {
        try {
            mCallbacks.onDownloadNecessary()
        } catch (t1: Throwable) {
            Log.e("CacheRequest", "Exception thrown by onDownloadNecessary", t1)

            try {
                onCallbackException(t1)
            } catch (t2: Throwable) {
                Log.e("CacheRequest", "Exception thrown by onCallbackException", t2)
                addGlobalError(RRError(null, null, true, t1))
                handleGlobalError(context, t2)
            }
        }
    }

    fun notifyDownloadStarted() {
        try {
            mCallbacks.onDownloadStarted()
        } catch (t1: Throwable) {
            Log.e("CacheRequest", "Exception thrown by onDownloadStarted", t1)

            try {
                onCallbackException(t1)
            } catch (t2: Throwable) {
                Log.e("CacheRequest", "Exception thrown by onCallbackException", t2)
                addGlobalError(RRError(null, null, true, t1))
                handleGlobalError(context, t2)
            }
        }
    }
}
