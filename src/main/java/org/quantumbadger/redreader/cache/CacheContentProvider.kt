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

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile
import org.quantumbadger.redreader.common.FileUtils
import org.quantumbadger.redreader.common.General.filenameFromString
import org.quantumbadger.redreader.common.GenericFactory
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.StringUtils
import java.io.File
import java.io.FileNotFoundException
import java.util.Locale
import org.quantumbadger.redreader.common.General

class CacheContentProvider : ContentProvider() {
    private var mCacheManager: CacheManager?=null

    private fun getReadableCacheFile(uri: Uri): Optional<ReadableCacheFile> {
        val filename = filenameFromString(uri.toString())

        val cacheId: Optional<Long> = getCacheIdFromFilename(filename)

        if (!cacheId.isPresent) {
            return Optional.Companion.empty<ReadableCacheFile>()
        }

        return Optional.Companion.of<ReadableCacheFile>(
            mCacheManager!!.getExistingCacheFileById(
                cacheId.get(),
                CacheCompressionType.NONE
            )
        ) // No compression is used for images
    }

    private fun getFile(uri: Uri): Optional<File> {
        val readableCacheFile = getReadableCacheFile(uri)

        if (!readableCacheFile.isPresent) {
            return Optional.Companion.empty<File>()
        }

        return readableCacheFile.get().file
    }

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        return ParcelFileDescriptor.open(
            getFile(uri).orThrow<FileNotFoundException?>(GenericFactory { FileNotFoundException(uri.toString()) }),
            ParcelFileDescriptor.MODE_READ_ONLY
        )
    }

    override fun onCreate(): Boolean {
        mCacheManager = CacheManager.Companion.getInstance(getContext()!!)
        return true
    }

    override fun attachInfo(
        context: Context,
        info: ProviderInfo
    ) {
        super.attachInfo(context, info)

        // Sanity check our security
        if (info.exported) {
            throw SecurityException("Provider must not be exported")
        }

        if (!info.grantUriPermissions) {
            throw SecurityException("Provider must grant uri permissions")
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<String?>?,
        selection: String?,
        selectionArgs: Array<String?>?,
        sortOrder: String?
    ): Cursor? {
        val readableCacheFile = getReadableCacheFile(uri)

        if (!readableCacheFile.isPresent) {
            Log.e(TAG, "Couldn't get readable cache file: " + uri)
            return MatrixCursor(COLUMNS, 0)
        }

        val file = readableCacheFile.get().file

        if (!file.isPresent) {
            Log.e(TAG, "Couldn't get underlying file: " + uri)
            return MatrixCursor(COLUMNS, 0)
        }

        val mimetype = readableCacheFile.get().lookupMimetype()

        if (!mimetype.isPresent) {
            Log.e(TAG, "Couldn't get mimetype: " + uri)
            return MatrixCursor(COLUMNS, 0)
        }

        val cols = ArrayList<String?>()
        val values = ArrayList<Any?>()

        for (col in if (projection == null) COLUMNS else projection) {
            if (OpenableColumns.DISPLAY_NAME == col) {
                cols.add(OpenableColumns.DISPLAY_NAME)
                values.add(
                    generateFilename(
                        readableCacheFile.get().id,
                        mimetype.get(),
                        "jpg"
                    )
                )
            } else if (OpenableColumns.SIZE == col) {
                cols.add(OpenableColumns.SIZE)
                values.add(file.get().length())
            } else if (MediaStore.MediaColumns.MIME_TYPE == col) {
                cols.add(MediaStore.MediaColumns.MIME_TYPE)
                values.add(mimetype.get())
            }
        }

        val cursor = MatrixCursor(cols.toTypedArray<String?>(), 1)
        cursor.addRow(values)
        return cursor
    }

    override fun getType(uri: Uri): String? {
        val readableCacheFile = getReadableCacheFile(uri)

        if (!readableCacheFile.isPresent) {
            Log.e(TAG, "Couldn't get readable cache file: " + uri)
            return null
        }

        return readableCacheFile.get().lookupMimetype().orElseNull()
    }

    override fun insert(
        uri: Uri,
        values: ContentValues?
    ): Uri? {
        throw UnsupportedOperationException("No external inserts")
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String?>?
    ): Int {
        return 0
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String?>?
    ): Int {
        throw UnsupportedOperationException("No external updates")
    }

    companion object {
        private const val TAG = "CacheContentProvider"

        @Suppress("PropertyName")
        private val COLUMNS = arrayOf<String?>(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)

        private fun generateFilename(
            cacheId: Long,
            mimetype: String,
            defaultExtension: String
        ): String {
            val extension = FileUtils.getExtensionForMimetype(mimetype).orElse(defaultExtension)

            return String.format(
                Locale.US,
                "redreader_dl_%d.%s",
                cacheId,
                extension
            )
        }

        private fun getCacheIdFromFilename(filename: String): Optional<Long> {
            val filenameSplitDot: Array<String?> =                 filename.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            if (filenameSplitDot.size != 2) {
                Log.e(TAG, "Expecting one dot in filename: " + filename)
                return Optional.Companion.empty<Long>()
            }

            val prefixRemoved = StringUtils.removePrefix(filenameSplitDot[0]!!, "redreader_dl_")

            if (!prefixRemoved.isPresent) {
                Log.e(TAG, "Expecting redreader_dl_ prefix in filename: " + filename)
                return Optional.Companion.empty<Long>()
            }

            try {
                return Optional.Companion.of<Long>(prefixRemoved.get().toLong())
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Invalid number in filename: " + filename, e)
                return Optional.Companion.empty<Long>()
            }
        }

        fun getUriForFile(
            cacheId: Long,
            mimetype: String,
            defaultExtension: String
        ): Uri? {
            return Uri.Builder()
                .scheme("content")
                .authority("org.quantumbadger.redreader.cacheprovider")
                .encodedPath(generateFilename(cacheId, mimetype, defaultExtension))
                .build()
        }
    }
}
