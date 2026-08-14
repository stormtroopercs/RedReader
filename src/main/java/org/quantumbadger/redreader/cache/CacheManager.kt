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
 */
package org.quantumbadger.redreader.cache

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import com.github.luben.zstd.Zstd
import com.github.luben.zstd.ZstdInputStream
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.activities.BugReportActivity.Companion.handleGlobalError
import org.quantumbadger.redreader.cache.CacheRequest.RequestFailureType
import org.quantumbadger.redreader.common.FileUtils
import org.quantumbadger.redreader.common.General.getGeneralErrorForFailure
import org.quantumbadger.redreader.common.General.readWholeStream
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.PrioritisedCachedThreadPool
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.datastream.MemoryDataStream
import org.quantumbadger.redreader.common.datastream.SeekableFileInputStream
import org.quantumbadger.redreader.common.datastream.SeekableInputStream
import org.quantumbadger.redreader.common.time.TimeDuration
import org.quantumbadger.redreader.common.time.TimeDuration.Companion.days
import org.quantumbadger.redreader.common.time.TimeDuration.Companion.hours
import org.quantumbadger.redreader.common.time.TimeDuration.Companion.secs
import org.quantumbadger.redreader.http.FailedRequestBody
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.Objects
import java.util.UUID
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Hilt-injected CacheManager for Reddit caching.
 * Replaces companion object singleton pattern.
 */
@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dbManager: CacheDbManager

    private val requests = PriorityBlockingQueue<CacheRequest?>()

    private val downloadQueue: PrioritisedDownloadQueue
    private val mDiskCacheThreadPool = PrioritisedCachedThreadPool(2, "Disk Cache")

    init {
        if (!isAlreadyInitialized.compareAndSet(false, true)) {
            throw RuntimeException("Attempt to initialize the cache twice.")
        }

        dbManager = CacheDbManager(context)

        downloadQueue = PrioritisedDownloadQueue(context)

        val requestHandler = RequestHandlerThread()
        requestHandler.start()
    }

    companion object {
        @Volatile
        private var instance: CacheManager?=null

        fun getInstance(context: Context): CacheManager {
            return instance ?: synchronized(this) {
                instance ?: throw IllegalStateException("CacheManager not initialized by Hilt")
            }
        }

        internal fun setInstance(i: CacheManager?) {
            instance = i
        }

        private const val TAG = "CacheManager"

        private const val ext = ".rr_cache_data"
        private const val tempExt = ".rr_cache_data_tmp"

        private val isAlreadyInitialized = AtomicBoolean(false)

        private fun pruneTemp(dir: File) {
            val list = dir.list()
            if (list == null) {
                return
            }

            for (file in list) {
                if (file.endsWith(tempExt)) {
                    File(dir, file).delete()
                }
            }
        }

        fun getCacheDirs(context: Context): ArrayList<File> {
            val dirs = ArrayList<File>()

            dirs.add(context.cacheDir)

            val externalDirs = context.externalCacheDirs
            if (externalDirs != null) {
                for (dir in externalDirs) {
                    if (dir != null) {
                        dirs.add(dir)
                    }
                }
            }

            return dirs
        }

        private fun getSubdirForCacheFile(
            cacheRoot: File,
            cacheFileId: Long
        ): File {
            return FileUtils.buildPath(
                cacheRoot,
                "rr_cache_files",
                String.format(Locale.US, "%02d", cacheFileId % 100),
                String.format(Locale.US, "%d", (cacheFileId / 100) % 10)
            )
        }
    }

    private fun isCacheFile(file: File): Long? {
        val name = file.name

        if (!name.endsWith(ext)) {
            return null
        }

        val nameSplit: Array<String?> =             name.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (nameSplit.size != 2) {
            return null
        }

        try {
            return nameSplit[0]!!.toLong()
        } catch (e: Exception) {
            return null
        }
    }

    private fun getCacheFileList(dir: File, currentFiles: HashSet<Long?>) {
        val list = dir.listFiles()
        if (list == null) {
            return
        }

        for (file in list) {
            if (file.isDirectory()) {
                getCacheFileList(file, currentFiles)
            } else {
                val cacheFileId = isCacheFile(file)

                if (cacheFileId != null) {
                    currentFiles.add(cacheFileId)
                }
            }
        }
    }

    fun pruneTemp() {
        val dirs: MutableList<File> = getCacheDirs(context)
        for (dir in dirs) {
            pruneTemp(dir)
        }
    }

    @Synchronized
    fun pruneCache() {
        pruneCache(PrefsUtility.pref_cache_maxage())
    }

    @Synchronized
    fun pruneCache(
        clearListings: Boolean,
        clearThumbnails: Boolean,
        clearImages: Boolean
    ) {
        if (!clearListings && !clearThumbnails && !clearImages) {
            return
        }

        /*Use a maximum age of 0 to clear everything* in that category.
		Otherwise, use a high number as the maximum age to ensure that nothing is deleted.

		*May not clear everything if system time shenanigans have occurred.*/
        val clearEverything = secs(0)
        val clearNothing = days((365 * 10).toLong())

        pruneCache(
            PrefsUtility.createFileTypeMap<TimeDuration?>(
                if (clearListings) clearEverything else clearNothing,
                if (clearThumbnails) clearEverything else clearNothing,
                if (clearImages) clearEverything else clearNothing
            )
        )
    }

    @Synchronized
    fun pruneCache(maxAge: java.util.HashMap<Int?, TimeDuration?>?) {
        try {
            val currentFiles = HashSet<Long?>(1024)

            val dirs: MutableList<File> = getCacheDirs(context)
            for (dir in dirs) {
                getCacheFileList(dir, currentFiles)
            }

            val filesToDelete = dbManager.getFilesToPrune(
                currentFiles,
                maxAge,
                hours(72)
            )

            Log.i("CacheManager", "Pruning " + filesToDelete.size + " files")

            for (id in filesToDelete) {
                val file = getExistingCacheFile(id!!)
                if (file != null) {
                    file.delete()
                }
            }
        } catch (t: Throwable) {
            handleGlobalError(context, t)
        }
    }

    @Synchronized
    fun emptyTheWholeCache() {
        dbManager.emptyTheWholeCache()
    }

    @get:Synchronized
    val cacheDataUsages: HashMap<Int?, Long?>
        get() {
            val dataUsagePerType =                 PrefsUtility.createFileTypeMap<Long?>(0L, 0L, 0L)

            try {
                val currentFiles =                     HashSet<Long?>(128)

                val dirs: MutableList<File> =                     getCacheDirs(context)
                for (dir in dirs) {
                    getCacheFileList(dir, currentFiles)
                }

                val filesToCheckWithTypes =                     dbManager.getFilesToSize()

                for (fileEntry in filesToCheckWithTypes.entries) {
                    val id: Long = fileEntry.key!!
                    val type: Int = fileEntry.value!!

                    val file = getExistingCacheFile(id)
                    if (file != null && dataUsagePerType.containsKey(type)) {
                        dataUsagePerType.put(
                            type,
                            Objects.requireNonNull<Long?>(dataUsagePerType.get(type)) + file.length()
                        )
                    }
                }
            } catch (t: Throwable) {
                handleGlobalError(context, t)
            }

            return dataUsagePerType
        }

    fun makeRequest(request: CacheRequest) {
        requests.put(request)
    }

    fun getSessions(url: UriString?, user: RedditAccount): MutableList<CacheEntry> {
        return dbManager.select(url, user.username, null)
    }

    val preferredCacheLocation: File
        get() = File(
            PrefsUtility.pref_cache_location(context)
        )

    fun getExistingCacheFileById(
        cacheId: Long,
        cacheCompressionType: CacheCompressionType
    ): ReadableCacheFile {
        return ReadableCacheFile(cacheId, cacheCompressionType)
    }

    inner class WritableCacheFile private constructor(
        val mUrl: UriString,
        val mUser: RedditAccount,
        val mFileType: Int,
        private val mSession: UUID,
        private val mMimetype: String?,
        private val mCacheCompressionType: CacheCompressionType
    ) {
        private val mOutStream: OutputStream
        private var readableCacheFile: ReadableCacheFile?=null
        private val location: File
        private var mWriteExternally = false

        private val mTmpFile: File

        private var mUncompressedLength: Long = 0
        private var mCompressedLength: Long = 0

        init {
            location = this.preferredCacheLocation
            mTmpFile = File(location, UUID.randomUUID().toString() + tempExt)

            mOutStream = FileOutputStream(mTmpFile)
        }

        fun getReadableCacheFile(): ReadableCacheFile {
            return Objects.requireNonNull<ReadableCacheFile>(readableCacheFile)
        }

        @Throws(IOException::class)
        fun writeWholeFile(
            buf: ByteArray?,
            offset: Int,
            length: Int
        ) {
            if (mCacheCompressionType == CacheCompressionType.NONE) {
                mOutStream.write(buf, offset, length)
                mCompressedLength += length.toLong()
            } else if (mCacheCompressionType == CacheCompressionType.ZSTD) {
                val maxDestSize = Zstd.compressBound(length.toLong())

                if (maxDestSize > Int.MAX_VALUE) {
                    throw IOException("Max output size is greater than MAX_INT")
                }

                val dst = ByteArray(maxDestSize.toInt())

                val size = Zstd.compressByteArray(
                    dst,
                    0,
                    dst.size,
                    buf,
                    offset,
                    length,
                    3
                ).toInt()

                mOutStream.write(dst, 0, size)

                mCompressedLength += size.toLong()
            }

            mUncompressedLength += length.toLong()
        }

        @Throws(IOException::class)
        fun onWriteFinished() {
            if (mWriteExternally) {
                mCompressedLength = mTmpFile.length()
                mUncompressedLength = mCompressedLength
            } else {
                mOutStream.flush()
                mOutStream.close()
            }

            val cacheFileId = dbManager.newEntry(
                mUrl,
                mUser,
                mFileType,
                mSession,
                mMimetype,
                mCacheCompressionType,
                mCompressedLength,
                mUncompressedLength
            )

            val subdir: File = getSubdirForCacheFile(location, cacheFileId)
            FileUtils.mkdirs(subdir)

            val dstFile = File(subdir, cacheFileId.toString() + ext)
            FileUtils.moveFile(mTmpFile, dstFile)

            dbManager.setEntryDone(cacheFileId)

            readableCacheFile = ReadableCacheFile(cacheFileId, mCacheCompressionType)
        }

        @Throws(IOException::class)
        fun writeExternally(): File {
            mWriteExternally = true
            mOutStream.close()
            return mTmpFile
        }

        fun onWriteCancelled() {
            try {
                mOutStream.close()
                if (!mTmpFile.delete()) {
                    Log.e(TAG, "Failed to delete temp cache file " + mTmpFile.delete())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during cancel", e)
            }
        }
    }

    inner class ReadableCacheFile private constructor(
        val id: Long,
        private val mCacheCompressionType: CacheCompressionType
    ) {
        private var mCachedUri: Uri?=null

        @get:Throws(IOException::class)
        val inputStream: InputStream
            get() {
                val result: InputStream =                     getCacheFileInputStream(this.id, mCacheCompressionType)!!

                if (result == null) {
                    throw FileNotFoundException("Stream was null for id " + this.id)
                }

                return result
            }

        val uri: Uri?
            get() {
                if (mCachedUri == null) {
                    mCachedUri = getCacheFileUri(this.id)
                }

                return mCachedUri
            }

        val file: Optional<File?>
            get() = Optional.ofNullable<File?>(
                getExistingCacheFile(this.id)
            )

        fun lookupMimetype(): Optional<String?> {
            val result = dbManager.selectById(
                this.id
            )

            if (result.isPresent()) {
                return Optional.of<String?>(result.get().mimetype)
            } else {
                return Optional.empty<String?>()
            }
        }

        override fun toString(): String {
            return String.format(Locale.US, "[ReadableCacheFile : id %d]", this.id)
        }
    }

    @Throws(IOException::class)
    fun openNewCacheFile(
        url: UriString,
        user: RedditAccount,
        fileType: Int,
        session: UUID,
        mimetype: String?,
        cacheCompressionType: CacheCompressionType
    ): WritableCacheFile {
        return WritableCacheFile(url, user, fileType, session, mimetype, cacheCompressionType)
    }

    private fun getExistingCacheFile(id: Long): File? {
        val dirs: MutableList<File> = getCacheDirs(context)

        // Try new format first
        for (dir in dirs) {
            val f: File = File(getSubdirForCacheFile(dir, id), id.toString() + ext)
            if (f.exists()) {
                return f
            }
        }

        for (dir in dirs) {
            val f = File(dir, id.toString() + ext)
            if (f.exists()) {
                return f
            }
        }

        return null
    }

    @Throws(IOException::class)
    private fun getCacheFileInputStream(
        id: Long,
        cacheCompressionType: CacheCompressionType
    ): SeekableInputStream? {
        val cacheFile = getExistingCacheFile(id)

        if (cacheFile == null) {
            return null
        }

        if (cacheCompressionType == CacheCompressionType.NONE) {
            return SeekableFileInputStream(cacheFile)
        } else if (cacheCompressionType == CacheCompressionType.ZSTD) {
            ZstdInputStream(FileInputStream(cacheFile)).use { `is` ->
                return MemoryDataStream(
                    readWholeStream(`is`)
                ).getInputStream()
            }
        } else {
            throw RuntimeException("Unhandled compression type " + cacheCompressionType)
        }
    }

    private fun getCacheFileUri(id: Long): Uri? {
        val cacheFile = getExistingCacheFile(id)

        if (cacheFile == null) {
            return null
        }

        return Uri.fromFile(cacheFile)
    }

    private inner class RequestHandlerThread : Thread("Request Handler Thread") {
        override fun run() {
            try {
                var request: CacheRequest?
                while ((requests.take().also { request = it }) != null) {
                    handleRequest(request!!)
                }
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
        }

        fun handleRequest(request: CacheRequest) {
            if (request.url == null) {
                request.notifyFailure(
                    getGeneralErrorForFailure(
                        context,
                        RequestFailureType.MALFORMED_URL,
                        NullPointerException("URL was null"),
                        null,
                        null,
                        Optional.empty<FailedRequestBody>()
                    )
                )
                return
            }

            if (request.downloadStrategy.shouldDownloadWithoutCheckingCache()) {
                queueDownload(request)
            } else {
                val result = dbManager.select(
                    request.url,
                    request.user.username,
                    request.requestSession
                )

                if (result.isEmpty()) {
                    if (request.downloadStrategy.shouldDownloadIfNotCached()) {
                        queueDownload(request)
                    } else {
                        request.notifyFailure(
                            getGeneralErrorForFailure(
                                context,
                                RequestFailureType.CACHE_MISS,
                                null,
                                null,
                                request.url,
                                Optional.empty<FailedRequestBody>()
                            )
                        )
                    }
                } else {
                    val entry = mostRecentFromList(result)

                    if (request.downloadStrategy.shouldDownloadIfCacheEntryFound(entry)) {
                        queueDownload(request)
                    } else {
                        handleCacheEntryFound(entry, request)
                    }
                }
            }
        }

        fun mostRecentFromList(list: MutableList<CacheEntry>): CacheEntry {
            var entry: CacheEntry?=null

            for (e in list) {
                if (entry == null || entry.timestamp.isLessThan(e.timestamp)) {
                    entry = e
                }
            }

            return entry!!
        }

        fun queueDownload(request: CacheRequest) {
            request.notifyDownloadNecessary()

            try {
                downloadQueue.add(request, this@CacheManager)
            } catch (e: Exception) {
                request.notifyFailure(
                    getGeneralErrorForFailure(
                        context,
                        RequestFailureType.MALFORMED_URL,
                        e,
                        null,
                        request.url,
                        Optional.empty<FailedRequestBody>()
                    )
                )
            }
        }

        fun handleCacheEntryFound(
            entry: CacheEntry,
            request: CacheRequest
        ) {
            val cacheFile = getExistingCacheFile(entry.id)

            if (cacheFile == null) {
                request.notifyFailure(
                    getGeneralErrorForFailure(
                        context,
                        RequestFailureType.STORAGE,
                        RuntimeException(),
                        null,
                        request.url,
                        Optional.empty<FailedRequestBody>()
                    )
                )

                dbManager.delete(entry.id)

                return
            }

            mDiskCacheThreadPool.add(object : PrioritisedCachedThreadPool.Task() {
                override fun getPriority(): Priority {
                    return request.priority
                }

                override fun run() {
                    val streamFactory: GenericFactory<SeekableInputStream, IOException?> =                         GenericFactory {
                            val stream =                                 getCacheFileInputStream(entry.id, entry.cacheCompressionType)
                            if (stream == null) {
                                dbManager.delete(entry.id)
                                throw IOException("Failed to open file")
                            }
                            stream
                        }

                    request.notifyDataStreamAvailable(
                        streamFactory,
                        entry.timestamp,
                        entry.session,
                        true,
                        entry.mimetype
                    )

                    request.notifyDataStreamComplete(
                        streamFactory,
                        entry.timestamp,
                        entry.session,
                        true,
                        entry.mimetype
                    )

                    request.notifyCacheFileWritten(
                        ReadableCacheFile(entry.id, entry.cacheCompressionType),
                        entry.timestamp,
                        entry.session,
                        true,
                        entry.mimetype
                    )
                }
            })
        }
    }
}
