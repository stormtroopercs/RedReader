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

package com.stormtroopercs.materialreader.compose.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.os.Message
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.stormtroopercs.materialreader.BuildConfig
import com.stormtroopercs.materialreader.MaterialReader
import com.stormtroopercs.materialreader.common.GlobalConfig
import com.stormtroopercs.materialreader.common.PrefsUtility
import com.stormtroopercs.materialreader.common.TorCommon
import info.guardianproject.netcipher.webkit.WebkitProxy

/**
 * Compose OAuth Login Screen — wraps the Reddit OAuth WebView in Compose.
 *
 * Handles:
 * - Cookie consent auto-dismiss
 * - Tor proxy if enabled
 * - OAuth callback detection (rr_oauth_redir host)
 * - Multi-window popup support (ReCAPTCHA)
 *
 * [onOAuthComplete] is called with the callback URL when OAuth succeeds.
 * [onOAuthError] is called with an error message if OAuth fails.
 */
@Composable
fun OAuthLoginScreen(
	onOAuthComplete: (String) -> Unit,
	onOAuthError: (String) -> Unit,
) {
	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current
	var webViewRef by remember { mutableStateOf<WebView?>(null) }

	// WebView lifecycle management
	DisposableEffect(lifecycleOwner) {
		val observer = LifecycleEventObserver { _, event ->
			when (event) {
				// resumeTimers() alone only resumes JS timers — without the
				// paired WebView.onResume() the WebView stays frozen after
				// onPause() (no input, no rendering) until the process dies.
				Lifecycle.Event.ON_RESUME -> {
					webViewRef?.onResume()
					webViewRef?.resumeTimers()
				}
				Lifecycle.Event.ON_PAUSE -> {
					webViewRef?.pauseTimers()
					webViewRef?.onPause()
				}
				Lifecycle.Event.ON_DESTROY -> {
					webViewRef?.destroy()
					webViewRef = null
				}
				else -> {}
			}
		}
		lifecycleOwner.lifecycle.addObserver(observer)
		onDispose {
			lifecycleOwner.lifecycle.removeObserver(observer)
		}
	}

	Box(modifier = Modifier.fillMaxSize()) {
		// fillMaxSize is critical: without it the AndroidView holder measures the
		// WebView with AT_MOST (wrap) constraints, so the WebView only grows to its
		// initial (near-empty) content height — the page renders in a sliver at the
		// top of the screen. Forcing an exact full-screen size makes the WebView
		// fill the screen regardless of content.
		AndroidView(
			factory = { ctx ->
				createOAuthWebView(ctx, onOAuthComplete, onOAuthError).also { webViewRef = it }
			},
			update = {},
			modifier = Modifier.fillMaxSize(),
		)
	}
}

@SuppressLint("SetJavaScriptEnabled")
private fun createOAuthWebView(
	context: android.content.Context,
	onOAuthComplete: (String) -> Unit,
	onOAuthError: (String) -> Unit,
): WebView {
	// The default WebView constructor uses WRAP_CONTENT layout params, which the
	// Compose AndroidView measures to the (near-empty) content height — the page
	// renders only in a sliver at the top of the screen. Force MATCH_PARENT so the
	// WebView fills the screen.
	val webView = WebView(context)
	webView.layoutParams = ViewGroup.LayoutParams(
		ViewGroup.LayoutParams.MATCH_PARENT,
		ViewGroup.LayoutParams.MATCH_PARENT,
	)

	val cookieManager = CookieManager.getInstance()
	cookieManager.removeAllCookies(null)
	cookieManager.setAcceptCookie(true)
	cookieManager.setAcceptThirdPartyCookies(webView, true)

	// Tor proxy support
	if (TorCommon.isTorEnabled) {
		try {
			val result = WebkitProxy.setProxy(
				MaterialReader::class.java.getCanonicalName(),
				context.applicationContext,
				webView,
				"127.0.0.1",
				8118,
			)
			if (!result) {
				onOAuthError("Tor proxy setup failed")
			}
		} catch (e: Exception) {
			onOAuthError("Tor proxy error: ${e.message}")
		}
	}

	val settings = webView.settings
	settings.javaScriptEnabled = true
	settings.useWideViewPort = true
	settings.loadWithOverviewMode = true
	settings.domStorageEnabled = true
	settings.builtInZoomControls = false
	settings.displayZoomControls = false
	settings.cacheMode = WebSettings.LOAD_NO_CACHE
	settings.databaseEnabled = false
	if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
		settings.saveFormData = false
	}
	// ReCAPTCHA support
	settings.setSupportMultipleWindows(true)
	settings.javaScriptCanOpenWindowsAutomatically = true

	// Enable WebView (Chrome DevTools) inspection so the OAuth page can be
	// driven/verified over CDP. Debug builds only — release is untouched.
	if (BuildConfig.DEBUG) {
		WebView.setWebContentsDebuggingEnabled(true)
	}

	// Multi-window support for ReCAPTCHA
	val webViewStack = mutableListOf(webView)

	webView.webChromeClient = object : WebChromeClient() {
		override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean = true

		override fun onCreateWindow(
			view: WebView?,
			isDialog: Boolean,
			isUserGesture: Boolean,
			resultMsg: Message,
		): Boolean {
			Log.i("OAuthLogin", "New window created (ReCAPTCHA?)")
			val newWebView = createOAuthWebView(context, onOAuthComplete, onOAuthError)
			webViewStack.add(newWebView)
			// Note: In Compose AndroidView, we can't swap views mid-flight easily.
			// The popup will load but won't be visible. This is a limitation.
			val transport = resultMsg.obj as WebView.WebViewTransport
			transport.setWebView(newWebView)
			resultMsg.sendToTarget()
			return true
		}

		override fun onCloseWindow(window: WebView?) {
			if (webViewStack.size > 1) {
				val removed = webViewStack.removeAt(webViewStack.size - 1)
				removed.destroy()
			}
		}
	}

	webView.webViewClient = object : WebViewClient() {
		override fun onPageFinished(view: WebView, url: String?) {
			// Auto-dismiss Reddit cookie consent dialog
			view.evaluateJavascript(
				"""(function() {
                    if(window.rrCookieWorkaround) return;
                    window.rrCookieWorkaround = true;
                    var attempts = 0;
                    var clicked = false;
                    var timer = setInterval(function() {
                        attempts++;
                        var button = document.querySelector(
                            '#data-protection-consent-dialog button[slot=secondary-button]');
                        if(button) {
                            button.click();
                            clicked = true;
                        } else {
                            var sheet = document.getElementById('data-protection-consent-sheet');
                            if(clicked && (!sheet || !sheet.open)) {
                                clearInterval(timer);
                                return;
                            }
                            if(attempts > 20 && sheet && sheet.open
                                && typeof sheet.hide === 'function') {
                                sheet.hide();
                            }
                        }
                        if(attempts > 120) clearInterval(timer);
                    }, 250);
                })()""",
				null,
			)
		}

		override fun shouldOverrideUrlLoading(
			view: WebView?,
			request: WebResourceRequest,
		): Boolean {
			val url = request.url
			if (url.host == OAUTH_HOST &&
				(url.scheme == REDREADER_SCHEME || url.scheme == HTTP_SCHEME)
			) {
				// OAuth callback detected
				onOAuthComplete(url.toString())
				return true
			}
			return false
		}

		override fun onReceivedHttpError(
			view: WebView?,
			request: WebResourceRequest,
			errorResponse: WebResourceResponse,
		) {
			// Shreddit login 401 = invalid credentials
			if (request.url.toString() == "https://www.reddit.com/svc/shreddit/account/login" &&
				errorResponse.statusCode / 100 == 4
			) {
				Log.e("OAuthLogin", "Shreddit login failed: ${errorResponse.statusCode}")
				onOAuthError("Invalid credentials. Please try again.")
			}
		}
	}

	// Build the OAuth authorize URI (mirrors RedditOAuth.promptUri)
	val appId = PrefsUtility.pref_reddit_client_id_override() ?: GlobalConfig.appId
	val promptUri = Uri.Builder()
		.scheme("https")
		.authority("www.reddit.com")
		.path("/api/v1/authorize.compact")
		.appendQueryParameter("response_type", "code")
		.appendQueryParameter("duration", "permanent")
		.appendQueryParameter("state", "Texas")
		.appendQueryParameter("redirect_uri", OAUTH_REDIRECT_URI)
		.appendQueryParameter("client_id", appId)
		.appendQueryParameter("scope", OAUTH_SCOPES)
		.build()
	webView.loadUrl(promptUri.toString())
	return webView
}

private const val OAUTH_REDIRECT_URI = "redreader://rr_oauth_redir"
private const val OAUTH_SCOPES =
	(
		"identity edit flair history modconfig modflair modlog modposts " +
			"modwiki mysubreddits privatemessages read report save submit " +
			"subscribe vote wikiedit wikiread account"
		)
private const val OAUTH_HOST = "rr_oauth_redir"
private const val REDREADER_SCHEME = "redreader"
private const val HTTP_SCHEME = "http"
