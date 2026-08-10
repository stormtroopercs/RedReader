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

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebView.WebViewTransport
import android.webkit.WebViewClient
import info.guardianproject.netcipher.webkit.WebkitProxy
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.RedReader
import org.quantumbadger.redreader.activities.BugReportActivity.Companion.handleGlobalError
import org.quantumbadger.redreader.common.DialogUtils
import org.quantumbadger.redreader.common.LinkHandler.openCustomTab
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.TorCommon
import org.quantumbadger.redreader.reddit.api.RedditOAuth.promptUri

class OAuthLoginActivity : ViewsBaseActivity() {
    private val webViewStack = ArrayList<WebView>()

    protected override fun onDestroy() {
        super.onDestroy()

        clearBaseActivityListing()

        for (w in webViewStack) {
            w.destroy()
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView? {
        val view = WebView(this)

        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(view, true)

        if (TorCommon.isTorEnabled()) {
            try {
                val result = WebkitProxy.setProxy(
                    RedReader::class.java.getCanonicalName(),
                    getApplicationContext(),
                    view,
                    "127.0.0.1",
                    8118
                )
                if (!result) {
                    handleGlobalError(
                        this,
                        getResources().getString(string.error_tor_setting_failed)
                    )
                    return null
                }
            } catch (e: Exception) {
                handleGlobalError(this, e)
                return null
            }
        }

        val settings = view.getSettings()

        settings.setBuiltInZoomControls(false)
        settings.setJavaScriptEnabled(true)
        settings.setUseWideViewPort(true)
        settings.setLoadWithOverviewMode(true)
        settings.setDomStorageEnabled(true)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            settings.setSaveFormData(false)
        }
        settings.setDatabaseEnabled(false)
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE)
        settings.setDisplayZoomControls(false)

        // Suggested by Reddit to work around ReCAPTCHA issues
        settings.setSupportMultipleWindows(true)
        settings.setJavaScriptCanOpenWindowsAutomatically(true)

        view.setWebChromeClient(object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                return true
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                // https://stackoverflow.com/a/11280814

                Log.i(TAG, "New window created")
                val newWebView = createWebView()
                webViewStack.add(newWebView!!)
                setBaseActivityListing(newWebView)
                val transport = resultMsg.obj as WebViewTransport
                transport.setWebView(newWebView)
                resultMsg.sendToTarget()
                return true
            }

            override fun onCloseWindow(window: WebView?) {
                if (webViewStack.size > 1) {
                    val removed = webViewStack.removeAt(webViewStack.size - 1)
                    removed.destroy()
                    setBaseActivityListing(webViewStack.get(webViewStack.size - 1))
                }
            }
        })

        view.setWebViewClient(object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                // Reddit shows a modal cookie consent dialog which can appear
                // behind the login form while still blocking all input to it.
                // Dismiss it by pressing its "Reject Optional Cookies" button
                // as soon as it appears. The button is located using its slot
                // name, which is locale-independent. The consent UI is loaded
                // asynchronously after the page itself, hence the polling. As
                // a fallback, if the button can't be found after 5 seconds but
                // the consent sheet is open, hide the sheet directly.
                view.evaluateJavascript(
                    ("(function() {"
                            + "if(window.rrCookieWorkaround) return;"
                            + "window.rrCookieWorkaround = true;"
                            + "var attempts = 0;"
                            + "var clicked = false;"
                            + "var timer = setInterval(function() {"
                            + "attempts++;"
                            + "var button = document.querySelector("
                            + "'#data-protection-consent-dialog "
                            + "button[slot=secondary-button]');"
                            + "if(button) {"
                            + "button.click();"
                            + "clicked = true;"
                            + "} else {"
                            + "var sheet = document.getElementById("
                            + "'data-protection-consent-sheet');"
                            + "if(clicked && (!sheet || !sheet.open)) {"
                            + "clearInterval(timer);"
                            + "return;"
                            + "}"
                            + "if(attempts > 20 && sheet && sheet.open"
                            + " && typeof sheet.hide === 'function') {"
                            + "sheet.hide();"
                            + "}"
                            + "}"
                            + "if(attempts > 120) clearInterval(timer);"
                            + "}, 250);"
                            + "})()"),
                    null
                )
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest
            ): Boolean {
                val url = request.getUrl()
                if (url.getHost() == OAUTH_HOST &&
                    (url.getScheme() == REDREADER_SCHEME ||
                            url.getScheme() == HTTP_SCHEME)
                ) {
                    val intent = Intent()
                    intent.putExtra("url", url.toString())
                    setResult(123, intent)
                    finish()
                } else {
                    setTitle(url.getHost())
                    return false
                }

                return true
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                // onReceivedHttpError: https://www.reddit.com/svc/shreddit/account/login, error = 401

                Log.e(
                    TAG, ("onReceivedHttpError: "
                            + request.getUrl()
                            + ", error = "
                            + errorResponse.getStatusCode())
                )

                if (request.getUrl()
                        .toString() == "https://www.reddit.com/svc/shreddit/account/login"
                    && errorResponse.getStatusCode() / 100 == 4
                ) {
                    DialogUtils.showDialogPositiveNegative(
                        this@OAuthLoginActivity,
                        getString(string.login_reddit_error_title),
                        getString(string.login_reddit_error_message),
                        string.dialog_continue,
                        string.dialog_cancel,
                        Runnable {
                            openCustomTab(
                                this@OAuthLoginActivity,
                                promptUri,
                                null,
                                false
                            )
                            finish()
                        },
                        Runnable {
                            finish()
                        }
                    )
                }
            }
        })

        return view
    }

    @SuppressLint("SetJavaScriptEnabled")
    public override fun onCreate(savedInstanceState: Bundle?) {
        PrefsUtility.applyTheme(this)

        super.onCreate(savedInstanceState)

        val webView = createWebView()

        if (webView != null) {
            webViewStack.add(webView)
            setBaseActivityListing(webView)
            webView.loadUrl(promptUri.toString())
        }
    }

    override fun onPause() {
        super.onPause()

        for (w in webViewStack) {
            w.onPause()
            w.pauseTimers()
        }
    }

    protected override fun onResume() {
        super.onResume()

        for (w in webViewStack) {
            w.resumeTimers()
            w.onResume()
        }
    }

    companion object {
        private const val TAG = "OAuthLoginActivity"

        private const val OAUTH_HOST = "rr_oauth_redir"
        private const val REDREADER_SCHEME = "redreader"
        private const val HTTP_SCHEME = "http"
    }
}
