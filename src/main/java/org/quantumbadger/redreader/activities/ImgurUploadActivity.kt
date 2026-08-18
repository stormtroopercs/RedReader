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
package org.quantumbadger.redreader.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.cache.CacheRequest
import org.quantumbadger.redreader.cache.CacheRequest.DownloadQueueType
import org.quantumbadger.redreader.cache.CacheRequest.RequestFailureType
import org.quantumbadger.redreader.cache.CacheRequestJSONParser
import org.quantumbadger.redreader.cache.downloadstrategy.DownloadStrategyAlways
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.Constants
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.General.closeSafely
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.common.General.getGeneralErrorForFailure
import org.quantumbadger.redreader.common.General.quickToast
import org.quantumbadger.redreader.common.General.setAllMarginsDp
import org.quantumbadger.redreader.common.General.showResultDialog
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.Priority
import org.quantumbadger.redreader.common.RRError
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.http.FailedRequestBody
import org.quantumbadger.redreader.http.body.HTTPRequestBody.Multipart
import org.quantumbadger.redreader.http.body.multipart.Part.FormDataBinary
import org.quantumbadger.redreader.image.ThumbnailScaler
import org.quantumbadger.redreader.jsonwrap.JsonValue
import org.quantumbadger.redreader.views.LoadingSpinnerView
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import kotlin.ByteArray
import kotlin.Exception
import kotlin.Int
import kotlin.Long
import kotlin.RuntimeException
import kotlin.Throwable
import kotlin.run

class ImgurUploadActivity : ViewsBaseActivity() {
    private var mTextView: TextView?=null

    private var mThumbnailView: ImageView?=null

    private var mImageData: ByteArray?

    private var mUploadButton: Button?=null

    private var mLoadingOverlay: View?=null

    protected override fun onCreate(savedInstanceState: Bundle?) {
        PrefsUtility.applyTheme(this)

        super.onCreate(savedInstanceState)

        setTitle(string.upload_to_imgur)

        val outerLayout = FrameLayout(this)

        val layout = LinearLayout(this)
        layout.setOrientation(LinearLayout.VERTICAL)

        mTextView = TextView(this)
        mTextView!!.setText(string.no_file_selected)
        layout.addView(mTextView)

        General.setAllMarginsDp(this, mTextView!!, 10)

        mUploadButton = Button(this)
        mUploadButton!!.setText(string.button_upload)
        mUploadButton!!.setEnabled(false)
        layout.addView(mUploadButton)
        updateUploadButtonVisibility()

        val browseButton = Button(this)
        browseButton.setText(string.button_browse)
        layout.addView(browseButton)

        mThumbnailView = ImageView(this)
        layout.addView(mThumbnailView)
        General.setAllMarginsDp(this, mThumbnailView!!, 20)

        browseButton.setOnClickListener(View.OnClickListener { v: View? ->
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.setType("image/*")
            startActivityForResultWithCallback(
                intent,
                BaseActivity.ActivityResultCallback { resultCode: Int, data: Intent? ->
                    if (data == null || data.getData() == null) {
                        return@ActivityResultCallback
                    }
                    if (resultCode != RESULT_OK) {
                        return@ActivityResultCallback
                    }
                    onImageSelected(data.getData()!!)
                })
        })

        mUploadButton!!.setOnClickListener(View.OnClickListener { v: View? ->
            if (mImageData != null) {
                uploadImage()
            } else {
                quickToast(this, string.no_file_selected)
            }
        })

        val sv = ScrollView(this)
        sv.addView(layout)
        outerLayout.addView(sv)

        run {
            mLoadingOverlay = LoadingSpinnerView(this)
            outerLayout.addView(mLoadingOverlay)
            mLoadingOverlay!!.setBackgroundColor(Color.argb(220, 50, 50, 50))

            General.setLayoutMatchParent(mLoadingOverlay!!)

            mLoadingOverlay!!.setOnClickListener(View.OnClickListener { v: View? -> })
            mLoadingOverlay!!.setVisibility(View.GONE)
        }

        setBaseActivityListing(outerLayout)

        setAllMarginsDp(this, layout, 20)
    }

    private fun showLoadingOverlay() {
        mLoadingOverlay!!.setVisibility(View.VISIBLE)
    }

    private fun hideLoadingOverlay() {
        mLoadingOverlay!!.setVisibility(View.GONE)
    }

    private fun updateUploadButtonVisibility() {
        mUploadButton!!.setVisibility(
            if (mImageData != null)
                View.VISIBLE
            else
                View.GONE
        )
    }

    private fun onImageSelected(uri: Uri) {
        showLoadingOverlay()

        object : Thread("Image selected thread") {
            override fun run() {
                try {
                    val file = getContentResolver().openFileDescriptor(uri, "r")

                    val thumbnailBitmap: Bitmap
                    val width: Int
                    val height: Int

                    val statSize: Long

                    try {
                        statSize = file!!.getStatSize()

                        if (statSize >= 10 * 1000 * 1000) { // Use base 10 just to be safe...
                            showResultDialog(
                                this@ImgurUploadActivity,
                                RRError(
                                    getString(string.error_file_too_big_title),
                                    getString(
                                        string.error_file_too_big_message,
                                        (statSize / 1024).toString() + "kB",
                                        "10MB"
                                    ),
                                    false
                                )
                            )
                            return
                        }

                        val thumbnailSizePx = dpToPixels(this@ImgurUploadActivity, 200f)

                        val rawBitmap = BitmapFactory.decodeFileDescriptor(
                            file.getFileDescriptor()
                        )

                        width = rawBitmap.getWidth()
                        height = rawBitmap.getHeight()

                        thumbnailBitmap = ThumbnailScaler.scaleNoCrop(
                            rawBitmap,
                            thumbnailSizePx
                        )

                        rawBitmap.recycle()
                    } finally {
                        closeSafely(file)
                    }

                    val byteOutput = ByteArrayOutputStream()

                    try {
                        getContentResolver().openInputStream(uri).use { inputStream ->
                            General.copyStream(inputStream!!, byteOutput)
                            byteOutput.flush()
                        }
                    } catch (e: IOException) {
                        throw RuntimeException(e)
                    }

                    val imageData = byteOutput.toByteArray()

                    AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                        mImageData = imageData
                        mUploadButton!!.setEnabled(true)
                        mThumbnailView!!.setImageBitmap(thumbnailBitmap)
                        mTextView!!.setText(
                            getString(
                                string.image_selected_summary,
                                width,
                                height,
                                (statSize / 1024).toString() + "kB"
                            )
                        )
                        hideLoadingOverlay()
                        updateUploadButtonVisibility()
                    })
                } catch (e: Exception) {
                    showResultDialog(
                        this@ImgurUploadActivity,
                        RRError(
                            getString(string.error_file_open_failed_title),
                            getString(string.error_file_open_failed_message),
                            true,
                            e
                        )
                    )

                    AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                        mImageData = null
                        mUploadButton!!.setEnabled(false)
                        mThumbnailView!!.setImageBitmap(null)
                        mTextView!!.setText(string.no_file_selected)
                        hideLoadingOverlay()
                        updateUploadButtonVisibility()
                    })
                }
            }
        }.start()
    }

    private fun uploadImage() {
        showLoadingOverlay()

        val apiUrl = UriString("https://api.imgur.com/3/image")

        CacheManager.Companion.getInstance(this).makeRequest(
            CacheRequest(
                apiUrl,
                RedditAccountManager.Companion.getInstance(this).getDefaultAccount(),
                null,
                Priority(Constants.Priority.API_ACTION),
                DownloadStrategyAlways.Companion.INSTANCE,
                Constants.FileType.NOCACHE,
                DownloadQueueType.IMGUR_API,
                Multipart()
                    .addPart(FormDataBinary("image", mImageData!!)),
                this,
                CacheRequestJSONParser(this, object : CacheRequestJSONParser.Listener {
                    override fun onJsonParsed(
                        result: JsonValue,
                        timestamp: TimestampUTC,
                        session: UUID,
                        fromCache: Boolean
                    ) {
                        val imageUri: Uri?

                        try {
                            val root = result.asObject()

                            if (root == null) {
                                throw RuntimeException("Response root object is null")
                            }

                            val success = root.getBoolean("success")

                            if (true != success) {
                                onFailure(
                                    getGeneralErrorForFailure(
                                        this@ImgurUploadActivity,
                                        RequestFailureType.UPLOAD_FAIL_IMGUR,
                                        null,
                                        null,
                                        null,
                                        Optional.Companion.of<FailedRequestBody>(
                                            FailedRequestBody(
                                                result
                                            )
                                        )
                                    )
                                )
                                return
                            }

                            val id = root.getObject("data")!!.getString("id")
                            imageUri = Uri.parse("https://imgur.com/" + id)
                        } catch (t: Throwable) {
                            onFailure(
                                getGeneralErrorForFailure(
                                    this@ImgurUploadActivity,
                                    RequestFailureType.PARSE_IMGUR,
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
                            return
                        }

                        AndroidCommon.runOnUiThread(Runnable {
                            val resultIntent = Intent()
                            resultIntent.setData(imageUri)
                            setResult(0, resultIntent)
                            finish()
                        })
                    }

                    override fun onFailure(error: RRError) {
                        showResultDialog(
                            this@ImgurUploadActivity,
                            error
                        )

                        AndroidCommon.runOnUiThread(Runnable { this@ImgurUploadActivity.hideLoadingOverlay() })
                    }
                })
            )
        )
    }
}
