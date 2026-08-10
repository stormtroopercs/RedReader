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
package org.quantumbadger.redreader.common

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.StatFs
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.RedReader.Companion.getInstance
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.activities.BugReportActivity.Companion.handleGlobalError
import org.quantumbadger.redreader.cache.CacheCompressionType
import org.quantumbadger.redreader.cache.CacheContentProvider
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequest.DownloadQueueType
import org.quantumbadger.redreader.cache.CacheRequest.RequestFailureType
import org.quantumbadger.redreader.cache.CacheRequestCallbacks
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyIfNotCached
import org.quantumbadger.redreader.common.General.copyStream
import org.quantumbadger.redreader.common.General.filenameFromString
import org.quantumbadger.redreader.common.General.getGeneralErrorForFailure
import org.quantumbadger.redreader.common.General.quickToast
import org.quantumbadger.redreader.common.General.showResultDialog
import org.quantumbadger.redreader.common.LinkHandler.getImageInfo
import org.quantumbadger.redreader.common.PrefsUtility.SaveLocation
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.fragments.AccountListDialog.Companion.show
import org.quantumbadger.redreader.fragments.ReportDialog.Companion.show
import org.quantumbadger.redreader.fragments.ShareOrderDialog
import org.quantumbadger.redreader.http.FailedRequestBody
import org.quantumbadger.redreader.image.GetImageInfoListener
import org.quantumbadger.redreader.image.ImageInfo
import org.quantumbadger.redreader.image.LegacySaveImageCallback
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.util.UUID

object FileUtils {
    private const val TAG = "FileUtils"

    private val MIMETYPE_TO_EXTENSION = HashMap<String?, String?>()

    init {
        MIMETYPE_TO_EXTENSION.put("audio/3gpp2", "3g2")
        MIMETYPE_TO_EXTENSION.put("video/3gpp2", "3g2")
        MIMETYPE_TO_EXTENSION.put("audio/3gpp", "3gp")
        MIMETYPE_TO_EXTENSION.put("video/3gpp", "3gp")
        MIMETYPE_TO_EXTENSION.put("application/x-7z-compressed", "7z")
        MIMETYPE_TO_EXTENSION.put("audio/aac", "aac")
        MIMETYPE_TO_EXTENSION.put("application/x-abiword", "abw")
        MIMETYPE_TO_EXTENSION.put("application/x-freearc", "arc")
        MIMETYPE_TO_EXTENSION.put("video/x-msvideo", "avi")
        MIMETYPE_TO_EXTENSION.put("application/vnd.amazon.ebook", "azw")
        MIMETYPE_TO_EXTENSION.put("application/octet-stream", "bin")
        MIMETYPE_TO_EXTENSION.put("image/bmp", "bmp")
        MIMETYPE_TO_EXTENSION.put("application/x-bzip2", "bz2")
        MIMETYPE_TO_EXTENSION.put("application/x-bzip", "bz")
        MIMETYPE_TO_EXTENSION.put("application/x-csh", "csh")
        MIMETYPE_TO_EXTENSION.put("text/css", "css")
        MIMETYPE_TO_EXTENSION.put("text/csv", "csv")
        MIMETYPE_TO_EXTENSION.put("application/msword", "doc")
        MIMETYPE_TO_EXTENSION.put(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "docx"
        )
        MIMETYPE_TO_EXTENSION.put("application/vnd.ms-fontobject", "eot")
        MIMETYPE_TO_EXTENSION.put("application/epub+zip", "epub")
        MIMETYPE_TO_EXTENSION.put("image/gif", "gif")
        MIMETYPE_TO_EXTENSION.put("application/gzip", "gz")
        MIMETYPE_TO_EXTENSION.put("video/h263", "h263")
        MIMETYPE_TO_EXTENSION.put("video/h264", "h264")
        MIMETYPE_TO_EXTENSION.put("video/h265", "h265")
        MIMETYPE_TO_EXTENSION.put("image/heic ", "heic")
        MIMETYPE_TO_EXTENSION.put("image/heic-sequence ", "heic")
        MIMETYPE_TO_EXTENSION.put("image/heif ", "heif")
        MIMETYPE_TO_EXTENSION.put("image/heif-sequence", "heif")
        MIMETYPE_TO_EXTENSION.put("text/html", "html")
        MIMETYPE_TO_EXTENSION.put("image/vnd.microsoft.icon", "ico")
        MIMETYPE_TO_EXTENSION.put("text/calendar", "ics")
        MIMETYPE_TO_EXTENSION.put("application/java-archive", "jar")
        MIMETYPE_TO_EXTENSION.put("image/jpeg", "jpg")
        MIMETYPE_TO_EXTENSION.put("application/json", "json")
        MIMETYPE_TO_EXTENSION.put("application/ld+json", "jsonld")
        MIMETYPE_TO_EXTENSION.put("text/javascript", "js")
        MIMETYPE_TO_EXTENSION.put("audio/midi audio/x-midi", "mid")
        MIMETYPE_TO_EXTENSION.put("audio/mpeg", "mp3")
        MIMETYPE_TO_EXTENSION.put("video/mp4", "mp4")
        MIMETYPE_TO_EXTENSION.put("application/dash+xml", "mpd")
        MIMETYPE_TO_EXTENSION.put("video/mpeg", "mpeg")
        MIMETYPE_TO_EXTENSION.put("application/vnd.apple.installer+xml", "mpkg")
        MIMETYPE_TO_EXTENSION.put("video/mpv", "mpv")
        MIMETYPE_TO_EXTENSION.put("application/vnd.oasis.opendocument.presentation", "odp")
        MIMETYPE_TO_EXTENSION.put("application/vnd.oasis.opendocument.spreadsheet", "ods")
        MIMETYPE_TO_EXTENSION.put("application/vnd.oasis.opendocument.text", "odt")
        MIMETYPE_TO_EXTENSION.put("audio/ogg", "oga")
        MIMETYPE_TO_EXTENSION.put("video/ogg", "ogv")
        MIMETYPE_TO_EXTENSION.put("application/ogg", "ogx")
        MIMETYPE_TO_EXTENSION.put("audio/opus", "opus")
        MIMETYPE_TO_EXTENSION.put("font/otf", "otf")
        MIMETYPE_TO_EXTENSION.put("application/pdf", "pdf")
        MIMETYPE_TO_EXTENSION.put("application/x-httpd-php", "php")
        MIMETYPE_TO_EXTENSION.put("image/png", "png")
        MIMETYPE_TO_EXTENSION.put("application/vnd.ms-powerpoint", "ppt")
        MIMETYPE_TO_EXTENSION.put(
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "pptx"
        )
        MIMETYPE_TO_EXTENSION.put("application/vnd.rar", "rar")
        MIMETYPE_TO_EXTENSION.put("application/rtf", "rtf")
        MIMETYPE_TO_EXTENSION.put("application/x-sh", "sh")
        MIMETYPE_TO_EXTENSION.put("image/svg+xml", "svg")
        MIMETYPE_TO_EXTENSION.put("application/x-shockwave-flash", "swf")
        MIMETYPE_TO_EXTENSION.put("application/x-tar", "tar")
        MIMETYPE_TO_EXTENSION.put("image/tiff", "tiff")
        MIMETYPE_TO_EXTENSION.put("video/mp2t", "ts")
        MIMETYPE_TO_EXTENSION.put("font/ttf", "ttf")
        MIMETYPE_TO_EXTENSION.put("text/plain", "txt")
        MIMETYPE_TO_EXTENSION.put("application/vnd.visio", "vsd")
        MIMETYPE_TO_EXTENSION.put("audio/wav", "wav")
        MIMETYPE_TO_EXTENSION.put("audio/webm", "weba")
        MIMETYPE_TO_EXTENSION.put("video/webm", "webm")
        MIMETYPE_TO_EXTENSION.put("image/webp", "webp")
        MIMETYPE_TO_EXTENSION.put("font/woff2", "woff2")
        MIMETYPE_TO_EXTENSION.put("font/woff", "woff")
        MIMETYPE_TO_EXTENSION.put("application/xhtml+xml", "xhtml")
        MIMETYPE_TO_EXTENSION.put("application/vnd.ms-excel", "xls")
        MIMETYPE_TO_EXTENSION.put(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "xlsx"
        )
        MIMETYPE_TO_EXTENSION.put("application/xml", "xml")
        MIMETYPE_TO_EXTENSION.put("text/xml", "xml")
        MIMETYPE_TO_EXTENSION.put("application/vnd.mozilla.xul+xml", "xul")
        MIMETYPE_TO_EXTENSION.put("application/zip", "zip")
    }

    fun getExtensionForMimetype(mimetype: String): Optional<String?> {
        val splitType: String

        if (mimetype.contains(";")) {
            splitType =
                mimetype.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        } else {
            splitType = mimetype
        }

        return Optional.Companion.ofNullable<String?>(
            MIMETYPE_TO_EXTENSION.get(
                StringUtils.asciiLowercase(splitType)
            )
        )
    }

    @Throws(IOException::class)
    fun moveFile(src: File, dst: File) {
        if (!src.renameTo(dst)) {
            copyFile(src, dst)

            if (!src.delete()) {
                src.deleteOnExit()
            }
        }
    }

    @Throws(IOException::class)
    fun copyFile(src: File?, dst: File?) {
        FileInputStream(src).use { fis ->
            copyFile(fis, dst)
        }
    }

    @Throws(IOException::class)
    fun copyFile(fis: InputStream, dst: File?) {
        FileOutputStream(dst).use { fos ->
            copyStream(fis, fos)
            fos.flush()
        }
    }

    fun isCacheDiskFull(context: Context): Boolean {
        val space = getFreeSpaceAvailable(PrefsUtility.pref_cache_location(context))
        return space < 128 * 1024 * 1024
    }

    /** Get the number of free bytes that are available on the external storage. */
    fun getFreeSpaceAvailable(path: String?): Long {
        val stat = StatFs(path)
        val availableBlocks = stat.getAvailableBlocksLong()
        val blockSize = stat.getBlockSizeLong()
        return availableBlocks * blockSize
    }

    fun shareImageAtUri(
        activity: BaseActivity,
        uri: UriString?
    ) {
        if (uri == null) {
            return
        }

        downloadImageToSave(
            activity,
            uri,
            DownloadImageToSaveSuccessCallback { info: ImageInfo, cacheFile: ReadableCacheFile?, mimetype: String? ->
                val externalUri: Uri? = CacheContentProvider.Companion.getUriForFile(
                    cacheFile!!.getId(),
                    mimetype,
                    getExtensionFromPath(info.original.url.value).orElse("jpg")
                )
                Log.i(TAG, "Sharing image with external uri: " + externalUri)

                val shareIntent = Intent()
                    .setAction(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_STREAM, externalUri)
                    .setType(mimetype)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                // Workaround for third party share apps
                shareIntent.setClipData(ClipData.newRawUri(null, externalUri))
                if (PrefsUtility.pref_behaviour_sharing_dialog()) {
                    ShareOrderDialog.Companion.newInstance(shareIntent)
                        .show(activity.getSupportFragmentManager(), null)
                } else {
                    activity.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            activity.getString(string.action_share)
                        )
                    )
                }
            })
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun mediaStoreDownloadsInsertFile(
        activity: BaseActivity,
        name: String,
        mimetype: String?,
        fileSize: Long,
        source: FileDataSource,
        onSuccess: Runnable
    ) {
        val downloads = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL)

        Log.i(TAG, "Got downloads URI: " + downloads.toString())

        val fileMetadata = ContentValues()
        fileMetadata.put(MediaStore.Downloads.DISPLAY_NAME, name)
        fileMetadata.put(MediaStore.Downloads.SIZE, fileSize)

        if (mimetype != null) {
            fileMetadata.put(MediaStore.Downloads.MIME_TYPE, mimetype)
        }

        fileMetadata.put(MediaStore.Downloads.IS_PENDING, true)

        val resolver = activity.getContentResolver()

        val fileUri = resolver.insert(downloads, fileMetadata)

        Log.i(TAG, "Got file URI: " + fileUri.toString())

        Thread(Runnable {
            try {
                resolver.openOutputStream(fileUri!!).use { os ->
                    source.writeTo(os!!)
                    os.flush()
                }
            } catch (e: IOException) {
                showUnexpectedStorageErrorDialog(
                    activity,
                    e,
                    UriString(fileUri.toString())
                )

                resolver.delete(fileUri!!, null, null)

                return@Runnable
            }
            fileMetadata.put(MediaStore.Downloads.IS_PENDING, false)
            resolver.update(fileUri, fileMetadata, null, null)
            onSuccess.run()
        }).start()
    }

    private fun createSAFDocumentWithIntent(
        activity: BaseActivity,
        filename: String,
        mimetype: String?,
        source: FileDataSource,
        onSuccess: Runnable
    ) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .setType(mimetype)
            .putExtra(Intent.EXTRA_TITLE, filename)
            .addCategory(Intent.CATEGORY_OPENABLE)

        try {
            activity.startActivityForResultWithCallback(
                intent,
                BaseActivity.ActivityResultCallback { resultCode: Int, data: Intent? ->
                    if (data == null || data.getData() == null) {
                        return@startActivityForResultWithCallback
                    }
                    Thread(Runnable {
                        try {
                            activity.getContentResolver()
                                .openOutputStream(data.getData()!!).use { outputStream ->
                                    source.writeTo(outputStream!!)
                                    onSuccess.run()
                                }
                        } catch (e: IOException) {
                            showUnexpectedStorageErrorDialog(
                                activity,
                                e,
                                UriString(data.getData().toString())
                            )
                        }
                    }).start()
                })
        } catch (e: ActivityNotFoundException) {
            DialogUtils.showDialog(
                activity,
                string.error_no_file_manager_title,
                string.error_no_file_manager_message
            )
        }
    }

    fun saveImageAtUri(
        activity: BaseActivity,
        uri: UriString?
    ) {
        if (uri == null) {
            return
        }

        val saveLocation = PrefsUtility.pref_behaviour_save_location()

        when (saveLocation) {
            SaveLocation.PROMPT_EVERY_TIME -> {
                downloadImageToSave(
                    activity,
                    uri,
                    DownloadImageToSaveSuccessCallback { info: ImageInfo, cacheFile: ReadableCacheFile?, mimetype: String? ->
                        val filename = filenameFromString(info.original.url.value)
                        createSAFDocumentWithIntent(
                            activity,
                            filename,
                            mimetype,
                            CacheFileDataSource(cacheFile!!),
                            Runnable {
                                quickToast(
                                    activity,
                                    string.action_save_image_success_no_path
                                )
                            })
                    })
            }

            SaveLocation.SYSTEM_DEFAULT -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Log.i(TAG, "Android version Q or higher, saving with MediaStore")

                    downloadImageToSave(
                        activity,
                        uri,
                        DownloadImageToSaveSuccessCallback { info: ImageInfo, cacheFile: ReadableCacheFile?, mimetype: String? ->
                            val filename = filenameFromString(info.original.url.value)
                            mediaStoreDownloadsInsertFile(
                                activity,
                                filename,
                                mimetype,
                                cacheFile!!.getFile()
                                    .map<Long?>(FunctionOneArgWithReturn { obj: Param? -> obj.length() })
                                    .orElse(0L),
                                CacheFileDataSource(cacheFile),
                                Runnable {
                                    quickToast(
                                        activity,
                                        string.action_save_image_success_no_path
                                    )
                                })
                        })
                } else {
                    Log.i(TAG, "Android version below Q, saving with legacy method")

                    activity.requestPermissionWithCallback(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        LegacySaveImageCallback(activity, uri)
                    )
                }
            }

            else -> {
                handleGlobalError(
                    activity, RuntimeException(
                        "Missing handler for preference value " + saveLocation
                    )
                )
            }
        }
    }

    private fun showUnexpectedStorageErrorDialog(
        activity: BaseActivity,
        throwable: Throwable,
        uri: UriString
    ) {
        showResultDialog(
            activity, RRError(
                activity.getString(string.error_unexpected_storage_title),
                activity.getString(string.error_unexpected_storage_message),
                true,
                throwable,
                null,
                uri,
                null
            )
        )
    }

    private fun internalDownloadImageToSaveAudio(
        activity: BaseActivity,
        info: ImageInfo,
        video: ReadableCacheFile,
        callback: DownloadImageToSaveSuccessCallback
    ) {
        val cacheManager: CacheManager = CacheManager.Companion.getInstance(activity)

        cacheManager.makeRequest(
            CacheRequest(
                info.urlAudioStream!!,
                RedditAccountManager.Companion.getAnon(),
                null,
                Priority(Constants.Priority.IMAGE_VIEW),
                DownloadStrategyIfNotCached.Companion.INSTANCE,
                Constants.FileType.IMAGE,
                DownloadQueueType.IMMEDIATE,
                activity,
                object : CacheRequestCallbacks {
                    override fun onFailure(error: RRError) {
                        showResultDialog(activity, error)
                    }

                    override fun onCacheFileWritten(
                        cacheFile: ReadableCacheFile,
                        timestamp: TimestampUTC?,
                        session: UUID,
                        fromCache: Boolean,
                        mimetype: String?
                    ) {
                        try {
                            val output = cacheManager.openNewCacheFile(
                                UriString(
                                    ("redreader://muxedmedia/"
                                            + UUID.randomUUID()
                                            + ".mp4")
                                ),
                                RedditAccountManager.Companion.getAnon(),
                                Constants.FileType.IMAGE,
                                session,
                                "video/mp4",
                                CacheCompressionType.NONE
                            )

                            val file = output.writeExternally()

                            MediaUtils.muxFiles(
                                file,
                                arrayOf<File>(
                                    cacheFile.getFile().orThrow<RuntimeException?>(GenericFactory {
                                        RuntimeException(
                                            "Audio file not found"
                                        )
                                    }),
                                    video.getFile().orThrow<RuntimeException?>(GenericFactory {
                                        RuntimeException(
                                            "Video file not found"
                                        )
                                    })
                                ),
                                Runnable {
                                    try {
                                        output.onWriteFinished()

                                        callback.onSuccess(
                                            info,
                                            output.getReadableCacheFile(),
                                            "video/mp4"
                                        )
                                    } catch (e: Exception) {
                                        showResultDialog(
                                            activity,
                                            getGeneralErrorForFailure(
                                                activity,
                                                RequestFailureType.STORAGE,
                                                e,
                                                null,
                                                info.original.url,
                                                Optional.Companion.empty<FailedRequestBody>()
                                            )
                                        )
                                    }
                                },

                                FunctionOneArgNoReturn { e: Exception? ->
                                    showResultDialog(
                                        activity,
                                        RRError(
                                            activity.getResources().getString(
                                                string.error_title_muxing_failed
                                            ),
                                            activity.getResources().getString(
                                                string.error_message_muxing_failed
                                            ),
                                            true,
                                            e,
                                            null,
                                            info.original.url,
                                            null
                                        )
                                    )
                                })
                        } catch (e: Exception) {
                            showResultDialog(
                                activity,
                                getGeneralErrorForFailure(
                                    activity,
                                    RequestFailureType.STORAGE,
                                    e,
                                    null,
                                    info.original.url,
                                    Optional.Companion.empty<FailedRequestBody>()
                                )
                            )
                        }
                    }
                })
        )
    }

    fun downloadImageToSave(
        activity: BaseActivity,
        uri: UriString,
        callback: DownloadImageToSaveSuccessCallback
    ) {
        getImageInfo(
            activity,
            uri,
            Priority(Constants.Priority.IMAGE_VIEW),
            object : GetImageInfoListener {
                override fun onFailure(error: RRError) {
                    showResultDialog(activity, error)
                }

                override fun onSuccess(info: ImageInfo) {
                    CacheManager.Companion.getInstance(activity).makeRequest(
                        CacheRequest(
                            info.original.url,
                            RedditAccountManager.Companion.getAnon(),
                            null,
                            Priority(Constants.Priority.IMAGE_VIEW),
                            DownloadStrategyIfNotCached.Companion.INSTANCE,
                            Constants.FileType.IMAGE,
                            DownloadQueueType.IMMEDIATE,
                            activity,
                            object : CacheRequestCallbacks {
                                override fun onDownloadNecessary() {
                                    quickToast(
                                        activity,
                                        string.download_downloading,
                                        Toast.LENGTH_SHORT
                                    )
                                }

                                override fun onFailure(error: RRError) {
                                    showResultDialog(activity, error)
                                }

                                override fun onCacheFileWritten(
                                    cacheFile: ReadableCacheFile,
                                    timestamp: TimestampUTC?,
                                    session: UUID,
                                    fromCache: Boolean,
                                    mimetype: String?
                                ) {
                                    if (info.urlAudioStream != null) {
                                        Log.i(TAG, "Also downloading audio stream...")

                                        internalDownloadImageToSaveAudio(
                                            activity,
                                            info,
                                            cacheFile,
                                            callback
                                        )
                                    } else {
                                        callback.onSuccess(info, cacheFile, mimetype)
                                    }
                                }
                            })
                    )
                }

                override fun onNotAnImage() {
                    quickToast(activity, string.selected_link_is_not_image)
                }
            })
    }

    @JvmStatic
    fun getExtensionFromPath(path: String): Optional<String?> {
        val pathSegments = path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        if (pathSegments.size == 0) {
            return Optional.Companion.empty<String?>()
        }

        val dotSegments = pathSegments[pathSegments.size - 1].split("\\.".toRegex())
            .dropLastWhile { it.isEmpty() }.toTypedArray()

        if (dotSegments.size < 2) {
            return Optional.Companion.empty<String?>()
        }

        if (dotSegments.size == 2 && dotSegments[0].isEmpty()) {
            return Optional.Companion.empty<String?>()
        }

        return Optional.Companion.of<String?>(dotSegments[dotSegments.size - 1])
    }

    fun buildPath(
        base: File,
        vararg components: String
    ): File {
        var result = base

        for (component in components) {
            result = File(result, component)
        }

        return result
    }

    private val sMkdirsLock = Any()

    @Throws(IOException::class)
    fun mkdirs(file: File) {
        synchronized(sMkdirsLock) {
            if (file.isDirectory()) {
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    Files.createDirectories(file.toPath())
                } catch (e: Exception) {
                    throw IOException(
                        "Failed to create dirs " + file.getAbsolutePath(),
                        e
                    )
                }
            } else {
                if (!file.mkdirs()) {
                    throw IOException("Failed to create dirs " + file.getAbsolutePath())
                }
            }
        }
    }

    private interface FileDataSource {
        @Throws(IOException::class)
        fun writeTo(outputStream: OutputStream)
    }

    private class CacheFileDataSource(private val mCacheFile: ReadableCacheFile) : FileDataSource {
        @Throws(IOException::class)
        override fun writeTo(outputStream: OutputStream) {
            mCacheFile.getInputStream().use { inputStream ->
                copyStream(inputStream, outputStream)
                outputStream.flush()
            }
        }
    }

    interface DownloadImageToSaveSuccessCallback {
        fun onSuccess(
            info: ImageInfo,
            cacheFile: ReadableCacheFile?,
            mimetype: String?
        )
    }
}
