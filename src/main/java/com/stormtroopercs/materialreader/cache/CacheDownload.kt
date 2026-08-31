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

import android.util.Log
import com.stormtroopercs.materialreader.cache.CacheManager.WritableCacheFile
import com.stormtroopercs.materialreader.cache.CacheRequest.DownloadQueueType
import com.stormtroopercs.materialreader.cache.CacheRequest.RequestFailureType
import com.stormtroopercs.materialreader.common.BugReporter.handleGlobalError
import com.stormtroopercs.materialreader.common.Constants
import com.stormtroopercs.materialreader.common.General.closeSafely
import com.stormtroopercs.materialreader.common.General.getGeneralErrorForFailure
import com.stormtroopercs.materialreader.common.GenericFactory
import com.stormtroopercs.materialreader.common.Optional
import com.stormtroopercs.materialreader.common.PrioritisedCachedThreadPool
import com.stormtroopercs.materialreader.common.Priority
import com.stormtroopercs.materialreader.common.TorCommon
import com.stormtroopercs.materialreader.common.datastream.ByteArrayCallback
import com.stormtroopercs.materialreader.common.datastream.MemoryDataStream
import com.stormtroopercs.materialreader.common.time.TimestampUTC.Companion.now
import com.stormtroopercs.materialreader.http.FailedRequestBody
import com.stormtroopercs.materialreader.http.HTTPBackend
import com.stormtroopercs.materialreader.http.HTTPBackend.Companion.backend
import com.stormtroopercs.materialreader.http.HTTPBackend.RequestDetails
import com.stormtroopercs.materialreader.image.RedgifsAPIV2
import com.stormtroopercs.materialreader.reddit.api.RedditOAuth
import com.stormtroopercs.materialreader.reddit.api.RedditOAuth.FetchAccessTokenResult
import com.stormtroopercs.materialreader.reddit.api.RedditOAuth.fetchAccessTokenSynchronous
import com.stormtroopercs.materialreader.reddit.api.RedditOAuth.fetchAnonymousAccessTokenSynchronous
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.Volatile

class CacheDownload(
	private val mInitiator: CacheRequest,
	private val manager: CacheManager,
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
				mInitiator.requestBody.asNullable(),
			),
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
							Optional.Companion.empty<FailedRequestBody>(),
						),
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
						mInitiator.user,
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
			request.addHeader("Authorization", "Bearer " + RedgifsAPIV2.latestToken)
		}

		mInitiator.notifyDownloadStarted()

		request.executeInThisThread(object : HTTPBackend.Listener {
			override fun onError(
				failureType: RequestFailureType,
				exception: Throwable?,
				httpStatus: Int?,
				body: FailedRequestBody?,
			) {
				if (mInitiator.queueType == DownloadQueueType.REDDIT_API &&
					TorCommon.isTorEnabled
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
						Optional.Companion.ofNullable<FailedRequestBody>(body),
					),
				)
			}

			override fun onSuccess(
				mimetype: String?,
				bodyBytes: Long?,
				`is`: InputStream,
			) {
				if (mCancelled) {
					Log.i(TAG, "Request cancelled at start of onSuccess()")
					return
				}

				val stream = MemoryDataStream(64 * 1024)

				mInitiator.notifyDataStreamAvailable(
					GenericFactory { stream.inputStream },
					now(),
					session,
					false,
					mimetype,
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
								bodyBytes,
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
						GenericFactory { stream.inputStream },
						now(),
						session,
						false,
						mimetype,
					)
				} catch (t: Throwable) {
					stream.setFailed(
						if (t is IOException) {
							t
						} else {
							IOException("Got exception during download", t)
						},
					)

					mInitiator.notifyFailure(
						getGeneralErrorForFailure(
							mInitiator.context,
							RequestFailureType.CONNECTION,
							t,
							null,
							mInitiator.url,
							Optional.Companion.empty<FailedRequestBody>(),
						),
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
						Constants.FileType.CAPTCHA, Constants.FileType.IMAGE, Constants.FileType.INLINE_IMAGE_PREVIEW, Constants.FileType.NOCACHE, Constants.FileType.THUMBNAIL -> // Image saving/sharing relies the file on disk being "raw"
							cacheCompressionType = CacheCompressionType.NONE

						Constants.FileType.COMMENT_LIST, Constants.FileType.IMAGE_INFO, Constants.FileType.INBOX_LIST, Constants.FileType.MULTIREDDIT_LIST, Constants.FileType.POST_LIST, Constants.FileType.SUBREDDIT_ABOUT, Constants.FileType.SUBREDDIT_LIST, Constants.FileType.USER_ABOUT -> cacheCompressionType = CacheCompressionType.ZSTD

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
							cacheCompressionType,
						)
					} catch (e: IOException) {
						Log.e(TAG, "Exception opening cache file for write", e)

						val failureType: RequestFailureType

						if (manager.preferredCacheLocation.exists()) {
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
								Optional.Companion.empty<FailedRequestBody>(),
							),
						)

						return
					}

					try {
						stream.getUnderlyingByteArrayWhenComplete(
							ByteArrayCallback { buf: ByteArray, offset: Int, length: Int ->
								writableCacheFile.writeWholeFile(
									buf,
									offset,
									length,
								)
							},
						)

						writableCacheFile.onWriteFinished()

						mInitiator.notifyCacheFileWritten(
							writableCacheFile.getReadableCacheFile(),
							now(),
							session,
							false,
							mimetype,
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
								Optional.Companion.empty<FailedRequestBody>(),
							),
						)
					}
				}
			}
		})
	}

	override val priority: Priority get() = mInitiator.priority

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
			dst: ByteArray,
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
