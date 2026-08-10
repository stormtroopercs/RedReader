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
package org.quantumbadger.redreader.views.webview

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import info.guardianproject.netcipher.webkit.WebkitProxy
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.RedReader
import org.quantumbadger.redreader.activities.BugReportActivity.Companion.handleGlobalError
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.TorCommon

/**
 * Fixes the onWindowFocusChanged bug, by catching NullPointerException.
 * https://groups.google.com/d/topic/android-developers/ktbwY2gtLKQ/discussion
 *
 * @author Andrew
 *
 *
 *
 *
 * This class serves as a WebView to be used in conjunction with a VideoEnabledWebChromeClient. It
 * makes possible: - To detect the HTML5 video ended event so that the VideoEnabledWebChromeClient
 * can exit full-screen.
 *
 *
 * Important notes: - Javascript is enabled by default and must not be disabled with
 * getSettings().setJavaScriptEnabled(false). - setWebChromeClient() must be called before any
 * loadData(), loadDataWithBaseURL() or loadUrl() method.
 *
 *
 * For more information, see https://github.com/cprcrack/VideoEnabledWebView
 * @author Cristian Perez (http://cpr.name)
 */
// Taken from reddit-is-fun:
// https://github.com/talklittle/reddit-is-fun/blob/master/src/com/andrewshu/android/reddit/browser/WebViewFixed.java
// Also taken from cprcrack/VideoEnabledWebView
// https://github.com/cprcrack/VideoEnabledWebView/blob/master/app/src/main/java/name/cpr/VideoEnabledWebView.java
class WebViewFixed : WebView {
    inner class JavascriptInterface {
        // Must match Javascript interface method of VideoEnabledWebChromeClient
        @android.webkit.JavascriptInterface
        @Suppress("unused")
        fun notifyVideoEnd() {
            // This code is not executed in the UI thread, so we must force that
            // to happen
            AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                if (videoEnabledWebChromeClient != null) {
                    videoEnabledWebChromeClient!!.onHideCustomView()
                }
            })
        }
    }

    private var videoEnabledWebChromeClient: VideoEnabledWebChromeClient?=null
    private var addedJavascriptInterface: Boolean

    constructor(context: Context) : super(context) {
        addedJavascriptInterface = false
        setTor(context)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle) {
        addedJavascriptInterface = false
        setTor(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        addedJavascriptInterface = false
        setTor(context)
    }

    @get:Suppress("unused")
    val isVideoFullscreen: Boolean
        /**
         * Indicates if the video is being displayed using a custom view (typically full-screen)
         *
         * @return true it the video is being displayed using a custom view (typically full-screen)
         */
        get() = videoEnabledWebChromeClient != null
                && videoEnabledWebChromeClient!!.isVideoFullscreen()

    /**
     * Pass only a VideoEnabledWebChromeClient instance.
     */
    @SuppressLint("SetJavaScriptEnabled")
    override fun setWebChromeClient(client: WebChromeClient?) {
        getSettings().setJavaScriptEnabled(true)

        if (client is VideoEnabledWebChromeClient) {
            this.videoEnabledWebChromeClient = client
        }

        super.setWebChromeClient(client)
    }

    override fun loadData(data: String, mimeType: String?, encoding: String?) {
        addJavascriptInterface()
        super.loadData(data, mimeType, encoding)
    }

    override fun loadDataWithBaseURL(
        baseUrl: String?,
        data: String,
        mimeType: String?,
        encoding: String?,
        historyUrl: String?
    ) {
        addJavascriptInterface()
        super.loadDataWithBaseURL(baseUrl, data, mimeType, encoding, historyUrl)
    }

    fun loadHtmlUTF8WithBaseURL(baseUrl: String?, html: String) {
        loadDataWithBaseURL(baseUrl, html, "text/html; charset=utf-8", "UTF-8", null)
    }

    override fun loadUrl(url: String) {
        addJavascriptInterface()
        super.loadUrl(url)
    }

    override fun loadUrl(url: String, additionalHttpHeaders: MutableMap<String?, String?>) {
        addJavascriptInterface()
        super.loadUrl(url, additionalHttpHeaders)
    }

    @SuppressLint("AddJavascriptInterface")
    private fun addJavascriptInterface() {
        if (!addedJavascriptInterface) {
            // Add javascript interface to be called when the video ends
            // (must be done before page load)
            // Must match Javascript interface name of VideoEnabledWebChromeClient
            addJavascriptInterface(WebViewFixed.JavascriptInterface(), "_VideoEnabledWebView")

            addedJavascriptInterface = true
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        try {
            super.onWindowFocusChanged(hasWindowFocus)
        } catch (ex: NullPointerException) {
            Log.e("WebView", "WebView.onWindowFocusChanged", ex)
        }
    }

    private fun setTor(context: Context) {
        if (TorCommon.isTorEnabled()) {
            try {
                clearBrowser()
                val result = WebkitProxy.setProxy(
                    RedReader::class.java.getCanonicalName(),
                    context.getApplicationContext(),
                    this,
                    "127.0.0.1",
                    8118
                )
                if (!result) {
                    handleGlobalError(
                        context,
                        getResources().getString(string.error_tor_setting_failed)
                    )
                }
            } catch (e: Exception) {
                handleGlobalError(context, e)
            }
        }
    }

    fun clearBrowser() {
        this.clearCache(true)
        this.clearFormData()
        this.clearHistory()
        CookieManager.getInstance().removeAllCookies(null)
    }
}
