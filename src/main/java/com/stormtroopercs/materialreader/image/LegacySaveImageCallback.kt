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

import android.content.Intent
import android.net.Uri
import android.os.Environment
import com.stormtroopercs.materialreader.R.string
import com.stormtroopercs.materialreader.activities.BaseActivity
import com.stormtroopercs.materialreader.activities.BaseActivity.PermissionCallback
import com.stormtroopercs.materialreader.cache.CacheManager.ReadableCacheFile
import com.stormtroopercs.materialreader.cache.CacheRequest.RequestFailureType
import com.stormtroopercs.materialreader.common.FileUtils
import com.stormtroopercs.materialreader.common.FileUtils.DownloadImageToSaveSuccessCallback
import com.stormtroopercs.materialreader.common.General.filenameFromString
import com.stormtroopercs.materialreader.common.General.getGeneralErrorForFailure
import com.stormtroopercs.materialreader.common.General.quickToast
import com.stormtroopercs.materialreader.common.General.showResultDialog
import com.stormtroopercs.materialreader.common.Optional
import com.stormtroopercs.materialreader.common.UriString
import com.stormtroopercs.materialreader.http.FailedRequestBody
import java.io.File
import java.io.IOException
import com.stormtroopercs.materialreader.common.General

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
                    cacheFile!!.inputStream.use { cacheFileInputStream ->
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

                    return@DownloadImageToSaveSuccessCallback
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
