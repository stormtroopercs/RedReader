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
import org.quantumbadger.redreader.cache.CacheRequest.DownloadQueueType
import org.quantumbadger.redreader.common.PrioritisedCachedThreadPool

internal class PrioritisedDownloadQueue(context: Context?) {
    private val redditDownloadsQueued = HashSet<CacheDownload>()

    private val mDownloadThreadPool = PrioritisedCachedThreadPool(5, "Download")

    init {
        RedditQueueProcessor().start()
    }

    @Synchronized
    fun add(request: CacheRequest, manager: CacheManager?) {
        val download = CacheDownload(request, manager)

        if (request.queueType == DownloadQueueType.REDDIT_API) {
            redditDownloadsQueued.add(download)
            (this as Object).notifyAll()
        } else if (request.queueType == DownloadQueueType.IMMEDIATE
            || request.queueType == DownloadQueueType.IMGUR_API
        ) {
            CacheDownloadThread(download, true, "Cache Download Thread: Immediate")
        } else {
            mDownloadThreadPool.add(download)
        }
    }

    @get:Synchronized
    private val nextRedditInQueue: CacheDownload?
        get() {
            while (redditDownloadsQueued.isEmpty()) {
                try {
                    (this as Object).wait()
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }
            }

            var next: CacheDownload?=null

            for (entry in redditDownloadsQueued) {
                if (next == null || entry.priority.isHigherPriorityThan(next.priority)) {
                    next = entry
                }
            }

            redditDownloadsQueued.remove(next)

            return next
        }

    private inner class RedditQueueProcessor : Thread("Reddit Queue Processor") {
        override fun run() {
            while (true) {
                synchronized(this) {
                    val download: CacheDownload?=this.nextRedditInQueue
                    CacheDownloadThread(
                        download,
                        true,
                        "Cache Download Thread: Reddit"
                    )
                }

                try {
                    sleep(1200) // Delay imposed by reddit API restrictions.
                } catch (e: InterruptedException) {
                    throw RuntimeException(e)
                }
            }
        }
    }
}
