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
 ******************************************************************************/

package org.quantumbadger.redreader.compose.ui

import android.annotation.SuppressLint
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Compose WebView Screen — wraps Android WebView in Compose via AndroidView.
 * Basic URL viewer with back navigation support. This is the app's in-app
 * browser (the retired WebViewActivity was a thin wrapper over it).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    url: String,
    title: String? = null,
    onNavigateBack: () -> Unit,
    onUrlChanged: ((String?) -> Unit)? = null
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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

    // Back navigation — WebView can go back first
    BackHandler(enabled = webViewRef?.canGoBack() == true) {
        webViewRef?.goBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title ?: "Browser") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (webViewRef?.canGoBack() == true) {
                                webViewRef?.goBack()
                            } else {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AndroidView(
                factory = { ctx ->
                    createWebView(ctx, url, onUrlChanged).also { webViewRef = it }
                },
                update = { webView ->
                    if (webView.url != url) {
                        webView.loadUrl(url)
                    }
                }
            )
        }
    }
}

/**
 * Compose HTML View Screen — renders raw HTML content in a WebView.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtmlViewScreen(
    html: String,
    title: String,
    onNavigateBack: () -> Unit,
    onWebViewCreated: (WebView) -> Unit = {}
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (webViewRef?.canGoBack() == true) {
                            webViewRef?.goBack()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            AndroidView(
                factory = { ctx ->
                    val webView = WebView(ctx)
                    setupWebViewBasic(webView)
                    webView.loadDataWithBaseURL(
                        "https://reddit.com/",
                        html,
                        "text/html; charset=utf-8",
                        "utf-8",
                        null
                    )
                    webViewRef = webView
                    onWebViewCreated(webView)
                    webView
                }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createWebView(
    context: android.content.Context,
    url: String,
    onUrlChanged: ((String?) -> Unit)? = null
): WebView {
    val webView = WebView(context)
    setupWebViewBasic(webView, onUrlChanged)
    webView.loadUrl(url)
    return webView
}

@SuppressLint("SetJavaScriptEnabled")
private fun setupWebViewBasic(
    webView: WebView,
    onUrlChanged: ((String?) -> Unit)? = null
) {
    val settings = webView.settings
    settings.javaScriptEnabled = true
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.domStorageEnabled = true
    settings.builtInZoomControls = true
    settings.displayZoomControls = false

    webView.webChromeClient = WebChromeClient()
    webView.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            // Let all URLs load in this WebView
            return false
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            onUrlChanged?.invoke(url)
        }
    }
}
