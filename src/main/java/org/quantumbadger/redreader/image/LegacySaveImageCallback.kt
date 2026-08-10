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
package org.quantumbadger.redreader.image

import android.content.Intent
import android.net.Uri
import android.os.Environment
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.activities.BaseActivity.PermissionCallback
import org.quantumbadger.redreader.cache.CacheManager.ReadableCacheFile
import org.quantumbadger.redreader.cache.CacheRequest.RequestFailureType
import org.quantumbadger.redreader.common.FileUtils
import org.quantumbadger.redreader.common.FileUtils.DownloadImageToSaveSuccessCallback
import org.quantumbadger.redreader.common.General.filenameFromString
import org.quantumbadger.redreader.common.General.getGeneralErrorForFailure
import org.quantumbadger.redreader.common.General.quickToast
import org.quantumbadger.redreader.common.General.showResultDialog
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.http.FailedRequestBody
import java.io.File
import java.io.IOException

class LegacySaveImageCallback(private val activity: BaseActivity, private val uri: UriString) :
    PermissionCallback {
    override fun onPermissionGranted() {
        FileUtils.downloadImageToSave(
            activity,
            uri,
            DownloadImageToSaveSuccessCallback { info: ImageInfo?, cacheFile: ReadableCacheFile?, mimetype: String? ->
                val filename = filenameFromString(info!!.original.url.value)
                var dst = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                    ),
                    filename
                )

                if (dst.exists()) {
                    var count = 0

                    while (dst.exists()) {
                        count++
                        dst = File(
                            Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES
                            ),
                            count.toString() + "_" + filename.substring(1)
                        )
                    }
                }

                try {
                    cacheFile!!.getInputStream().use { cacheFileInputStream ->
                        FileUtils.copyFile(cacheFileInputStream, dst)
                    }
                } catch (e: IOException) {
                    showResultDialog(
                        activity,
                        getGeneralErrorForFailure(
                            activity,
                            RequestFailureType.STORAGE,
                            RuntimeException("Could not copy file", e),
                            null,
                            uri,
                            Optional.Companion.empty<FailedRequestBody>()
                        )
                    )

                    return@downloadImageToSave
                }

                activity.sendBroadcast(
                    Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                        Uri.parse("file://" + dst.getAbsolutePath())
                    )
                )
                quickToast(
                    activity,
                    (activity.getString(string.action_save_image_success)
                            + " "
                            + dst.getAbsolutePath())
                )
            })
    }

    override fun onPermissionDenied() {
        quickToast(activity, string.save_image_permission_denied)
    }
}
