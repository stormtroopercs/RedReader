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

import android.util.Log
import org.quantumbadger.redreader.activities.BugReportActivity.Companion.handleGlobalError
import org.quantumbadger.redreader.cache.CacheManager.WritableCacheFile
import org.quantumbadger.redreader.cache.CacheRequest.DownloadQueueType
import org.quantumbadger.redreader.cache.CacheRequest.RequestFailureType
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.General.closeSafely
import org.quantumbadger.redreader.common.General.getGeneralErrorForFailure
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.PrioritisedCachedThreadPool
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.TorCommon
import org.quantumbadger.redreader.common.datastream.ByteArrayCallback
import org.quantumbadger.redreader.common.datastream.MemoryDataStream
import org.quantumbadger.redreader.common.time.TimestampUTC.Companion.now
import org.quantumbadger.redreader.http.FailedRequestBody
import org.quantumbadger.redreader.http.HTTPBackend
import org.quantumbadger.redreader.http.HTTPBackend.Companion.backend
import org.quantumbadger.redreader.http.HTTPBackend.RequestDetails
import org.quantumbadger.redreader.image.RedgifsAPIV2
import org.quantumbadger.redreader.reddit.api.RedditOAuth
import org.quantumbadger.redreader.reddit.api.RedditOAuth.FetchAccessTokenResult
import org.quantumbadger.redreader.reddit.api.RedditOAuth.fetchAccessTokenSynchronous
import org.quantumbadger.redreader.reddit.api.RedditOAuth.fetchAnonymousAccessTokenSynchronous
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.Volatile
import org.quantumbadger.redreader.common.General

class CacheDownload(
    private val mInitiator: CacheRequest,
    private val manager: CacheManager
) : PrioritisedCachedThreadPool.Task() {
    private val session: UUID

    @Volatile
    private var mCancelled = false
    private val mRequest: HTTPBackend.Request?

    init {
        if (!mInitiator.setDownload(this)) {
            mCancelled = true
        }

        if (mInitiator.requestSession != null) {
            session = mInitiator.requestSession
        } else {
            session = UUID.randomUUID()
        }

        mRequest = backend.prepareRequest(
            mInitiator.context,
            RequestDetails(
                mInitiator.url,
                mInitiator.requestBody.asNullable()
            )
        )
    }

    @Synchronized
    fun cancel() {
        mCancelled = true

        object : Thread() {
            override fun run() {
                if (mRequest != null) {
                    mRequest.cancel()
                    mInitiator.notifyFailure(
                        getGeneralErrorForFailure(
                            mInitiator.context,
                            RequestFailureType.CANCELLED,
                            null,
                            null,
                            mInitiator.url,
                            Optional.Companion.empty<FailedRequestBody>()
                        )
                    )
                }
            }
        }.start()
    }

    fun doDownload() {
        if (mCancelled) {
            return
        }

        try {
            performDownload(mRequest!!)
        } catch (t: Throwable) {
            handleGlobalError(mInitiator.context, t)
        }
    }

    private fun performDownload(request: HTTPBackend.Request) {
        if (mInitiator.queueType == DownloadQueueType.REDDIT_API) {
            if (resetUserCredentials.getAndSet(false)) {
                mInitiator.user.setAccessToken(null)
            }

            var accessToken = mInitiator.user.mostRecentAccessToken

            if (accessToken == null || accessToken.isExpired) {
                mInitiator.notifyProgress(true, 0, 0)

                val result: FetchAccessTokenResult

                if (mInitiator.user.isAnonymous) {
                    result = fetchAnonymousAccessTokenSynchronous(mInitiator.context)
                } else {
                    result = fetchAccessTokenSynchronous(
                        mInitiator.context,
                        mInitiator.user
                    )
                }

                if (result.status != RedditOAuth.FetchAccessTokenResultStatus.SUCCESS) {
                    mInitiator.notifyFailure(result.error!!)
                    return
                }

                accessToken = result.accessToken
                mInitiator.user.setAccessToken(accessToken)
            }

            request.addHeader("Authorization", "bearer " + accessToken!!.token)
        }

        if (mInitiator.queueType == DownloadQueueType.IMGUR_API) {
            request.addHeader("Authorization", "Client-ID c3713d9e7674477")
        } else if (mInitiator.queueType == DownloadQueueType.REDGIFS_API_V2) {
            request.addHeader("Authorization", "Bearer " + RedgifsAPIV2.getLatestToken())
        }

        mInitiator.notifyDownloadStarted()

        request.executeInThisThread(object : HTTPBackend.Listener {
            override fun onError(
                failureType: RequestFailureType,
                exception: Throwable?,
                httpStatus: Int?,
                body: FailedRequestBody?
            ) {
                if (mInitiator.queueType == DownloadQueueType.REDDIT_API
                    && TorCommon.isTorEnabled()
                ) {
                    backend.recreateHttpBackend()
                    resetUserCredentialsOnNextRequest()
                }

                mInitiator.notifyFailure(
                    getGeneralErrorForFailure(
                        mInitiator.context,
                        failureType,
                        exception,
                        httpStatus,
                        mInitiator.url,
                        Optional.Companion.ofNullable<FailedRequestBody>(body)
                    )
                )
            }

            override fun onSuccess(
                mimetype: String?,
                bodyBytes: Long?,
                `is`: InputStream
            ) {
                if (mCancelled) {
                    Log.i(TAG, "Request cancelled at start of onSuccess()")
                    return
                }

                val stream = MemoryDataStream(64 * 1024)

                mInitiator.notifyDataStreamAvailable(
                    GenericFactory { stream.getInputStream() },
                    now(),
                    session,
                    false,
                    mimetype
                )

                // Download the file into memory
                try {
                    val buf = ByteArray(64 * 1024)

                    var bytesRead: Int
                    var totalBytesRead: Long = 0

                    while ((tryReadFully(`is`, buf).also { bytesRead = it }) > 0) {
                        totalBytesRead += bytesRead.toLong()

                        stream.writeBytes(buf, 0, bytesRead)

                        if (bodyBytes != null) {
                            mInitiator.notifyProgress(
                                false,
                                totalBytesRead,
                                bodyBytes
                            )
                        }

                        if (mCancelled) {
                            Log.i(TAG, "Request cancelled during read loop")
                            stream.setFailed(IOException("Download cancelled"))
                            return
                        }
                    }

                    stream.setComplete()

                    mInitiator.notifyDataStreamComplete(
                        GenericFactory { stream.getInputStream() },
                        now(),
                        session,
                        false,
                        mimetype
                    )
                } catch (t: Throwable) {
                    stream.setFailed(
                        if (t is IOException)
                            t
                        else
                            IOException("Got exception during download", t)
                    )

                    mInitiator.notifyFailure(
                        getGeneralErrorForFailure(
                            mInitiator.context,
                            RequestFailureType.CONNECTION,
                            t,
                            null,
                            mInitiator.url,
                            Optional.Companion.empty<FailedRequestBody>()
                        )
                    )

                    return
                } finally {
                    closeSafely(`is`)
                }

                // Save it to the cache
                if (mInitiator.cache) {
                    val writableCacheFile: WritableCacheFile

                    val cacheCompressionType: CacheCompressionType

                    when (mInitiator.fileType) {
                        Constants.FileType.CAPTCHA, Constants.FileType.IMAGE, Constants.FileType.INLINE_IMAGE_PREVIEW, Constants.FileType.NOCACHE, Constants.FileType.THUMBNAIL ->                            // Image saving/sharing relies the file on disk being "raw"
                            cacheCompressionType = CacheCompressionType.NONE

                        Constants.FileType.COMMENT_LIST, Constants.FileType.IMAGE_INFO, Constants.FileType.INBOX_LIST, Constants.FileType.MULTIREDDIT_LIST, Constants.FileType.POST_LIST, Constants.FileType.SUBREDDIT_ABOUT, Constants.FileType.SUBREDDIT_LIST, Constants.FileType.USER_ABOUT -> cacheCompressionType =                             CacheCompressionType.ZSTD

                        else -> {
                            Log.e(TAG, "Unhandled filetype: " + mInitiator.fileType)
                            cacheCompressionType = CacheCompressionType.NONE
                        }
                    }

                    try {
                        writableCacheFile = manager.openNewCacheFile(
                            mInitiator.url,
                            mInitiator.user,
                            mInitiator.fileType,
                            session,
                            mimetype,
                            cacheCompressionType
                        )
                    } catch (e: IOException) {
                        Log.e(TAG, "Exception opening cache file for write", e)

                        val failureType: RequestFailureType

                        if (manager.getPreferredCacheLocation().exists()) {
                            failureType = RequestFailureType.STORAGE
                        } else {
                            failureType = RequestFailureType.CACHE_DIR_DOES_NOT_EXIST
                        }

                        mInitiator.notifyFailure(
                            getGeneralErrorForFailure(
                                mInitiator.context,
                                failureType,
                                e,
                                null,
                                mInitiator.url,
                                Optional.Companion.empty<FailedRequestBody>()
                            )
                        )

                        return
                    }

                    try {
                        stream.getUnderlyingByteArrayWhenComplete(
                            ByteArrayCallback { buf: ByteArray, offset: Int, length: Int ->
                                writableCacheFile.writeWholeFile(
                                    buf,
                                    offset,
                                    length
                                )
                            })

                        writableCacheFile.onWriteFinished()

                        mInitiator.notifyCacheFileWritten(
                            writableCacheFile.getReadableCacheFile(),
                            now(),
                            session,
                            false,
                            mimetype
                        )
                    } catch (e: IOException) {
                        writableCacheFile.onWriteCancelled()

                        mInitiator.notifyFailure(
                            getGeneralErrorForFailure(
                                mInitiator.context,
                                RequestFailureType.STORAGE,
                                e,
                                null,
                                mInitiator.url,
                                Optional.Companion.empty<FailedRequestBody>()
                            )
                        )
                    }
                }
            }
        })
    }

    override fun getPriority(): Priority {
        return mInitiator.priority
    }

    override fun run() {
        doDownload()
    }

    companion object {
        private const val TAG = "CacheDownload"

        private val resetUserCredentials = AtomicBoolean(false)
        fun resetUserCredentialsOnNextRequest() {
            resetUserCredentials.set(true)
        }

        @Throws(IOException::class)
        private fun tryReadFully(
            src: InputStream,
            dst: ByteArray
        ): Int {
            var totalBytesRead = 0

            while (true) {
                val bytesRead = src.read(dst, totalBytesRead, dst.size - totalBytesRead)

                if (bytesRead <= 0) {
                    return totalBytesRead
                }

                totalBytesRead += bytesRead

                if (totalBytesRead >= dst.size) {
                    return totalBytesRead
                }
            }
        }
    }
}
