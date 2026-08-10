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
package org.quantumbadger.redreader.fragments

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.BundleCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.RedReader.Companion.getInstance
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.cache.CacheManager
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.General.quickToast
import org.quantumbadger.redreader.common.General.setLayoutMatchParent
import org.quantumbadger.redreader.common.LinkHandler
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.PrefsUtility.AppearanceStatusBarMode
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.common.UriString.Companion.from
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.reddit.api.RedditPostActions
import org.quantumbadger.redreader.reddit.kthings.RedditPost
import org.quantumbadger.redreader.reddit.prepared.RedditParsedPost
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost
import org.quantumbadger.redreader.reddit.url.RedditURLParser
import org.quantumbadger.redreader.views.RedditPostView.PostSelectionListener
import org.quantumbadger.redreader.views.bezelmenu.BezelSwipeOverlay
import org.quantumbadger.redreader.views.bezelmenu.BezelSwipeOverlay.BezelSwipeListener
import org.quantumbadger.redreader.views.bezelmenu.SideToolbarOverlay
import org.quantumbadger.redreader.views.bezelmenu.SideToolbarOverlay.SideToolbarPosition
import org.quantumbadger.redreader.views.webview.VideoEnabledWebChromeClient
import org.quantumbadger.redreader.views.webview.VideoEnabledWebChromeClient.ToggledFullscreenCallback
import org.quantumbadger.redreader.views.webview.WebViewFixed
import java.net.URISyntaxException
import java.util.Locale
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.Volatile

class WebViewFragment : Fragment(), PostSelectionListener {
    private var mActivity: BaseActivity? = null

    private var mUrl: UriString? = null
    private var html: String? = null

    @Volatile
    private var currentUrl: UriString? = null

    @Volatile
    private var goingBack = false

    @Volatile
    private var lastBackDepthAttempt = 0

    private var webView: WebViewFixed? = null
    private var progressView: ProgressBar? = null
    private var outer: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // TODO load position/etc?
        super.onCreate(savedInstanceState)
        mUrl =
            BundleCompat.getParcelable<UriString?>(requireArguments(), "url", UriString::class.java)
        html = requireArguments().getString("html")
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mActivity = getActivity() as BaseActivity?

        outer = inflater.inflate(R.layout.web_view_fragment, null) as FrameLayout

        val srcPost = BundleCompat.getParcelable<RedditPost?>(
            requireArguments(),
            "post",
            RedditPost::class.java
        )
        val post: RedditPreparedPost?

        if (srcPost != null) {
            val parsedPost = RedditParsedPost(
                mActivity!!,
                srcPost,
                false
            )

            post = RedditPreparedPost(
                mActivity,
                CacheManager.Companion.getInstance(mActivity),
                0,
                parsedPost,
                TimestampUTC.ZERO,
                false,
                false,
                false,
                false
            )
        } else {
            post = null
        }

        webView = outer!!.findViewById<WebViewFixed>(R.id.web_view_fragment_webviewfixed)
        val loadingViewFrame =
            outer!!.findViewById<FrameLayout>(R.id.web_view_fragment_loadingview_frame)

        progressView = ProgressBar(
            mActivity,
            null,
            android.R.attr.progressBarStyleHorizontal
        )
        loadingViewFrame.addView(progressView)
        loadingViewFrame.setPadding(
            General.dpToPixels(mActivity!!, 10f),
            0,
            General.dpToPixels(mActivity!!, 10f),
            0
        )
        val fullscreenViewFrame =
            outer!!.findViewById<FrameLayout?>(R.id.web_view_fragment_fullscreen_frame)

        val chromeClient: VideoEnabledWebChromeClient = object : VideoEnabledWebChromeClient(
            loadingViewFrame,
            fullscreenViewFrame
        ) {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)

                AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                    progressView!!.setProgress(newProgress)
                    progressView!!.setVisibility(
                        if (newProgress == 100)
                            View.GONE
                        else
                            View.VISIBLE
                    )
                })
            }
        }

        chromeClient.setOnToggledFullscreen(ToggledFullscreenCallback { fullscreen: Boolean ->
            // Your code to handle the full-screen change, for example showing
            // and hiding the title bar. Example:
            val insetsController = WindowCompat.getInsetsController(
                mActivity!!.getWindow(),
                mActivity!!.getWindow().getDecorView()
            )
            if (fullscreen) {
                mActivity!!.getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
                mActivity!!.getSupportActionBar()!!.hide()
                insetsController.setSystemBarsBehavior(
                    WindowInsetsControllerCompat
                        .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                )
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
            } else {
                //only re-enable status bar if there is no contradicting preference set
                if (PrefsUtility.pref_appearance_android_status()
                    == AppearanceStatusBarMode.NEVER_HIDE
                ) {
                    insetsController.show(WindowInsetsCompat.Type.statusBars())
                }
                mActivity!!.getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
                mActivity!!.getSupportActionBar()!!.show()
            }
        })

        /*handle download links show an alert box to load this outside the internal browser*/
        webView!!.setDownloadListener(DownloadListener { url: String?, userAgent: String?, contentDisposition: String?, mimetype: String?, contentLength: Long ->
            MaterialAlertDialogBuilder(mActivity!!)
                .setTitle(string.download_link_title)
                .setMessage(string.download_link_message)
                .setPositiveButton(
                    android.R.string.yes,
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        val i = Intent(Intent.ACTION_VIEW)
                        i.setData(Uri.parse(url))
                        try {
                            getContext()!!.startActivity(i)

                            //get back from internal browser
                            mActivity!!.onBackPressedDispatcher
                                .onBackPressed()
                        } catch (e: ActivityNotFoundException) {
                            General.quickToast(
                                getContext()!!,
                                string.action_not_handled_by_installed_app_toast
                            )
                        }
                    })
                .setNegativeButton(
                    android.R.string.no,
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        //get back from internal browser
                        mActivity!!.onBackPressedDispatcher
                            .onBackPressed()
                    })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        })


        /*handle download links end*/
        val settings = webView!!.getSettings()

        settings.setBuiltInZoomControls(true)
        settings.setJavaScriptEnabled(true)
        settings.setJavaScriptCanOpenWindowsAutomatically(false)
        settings.setUseWideViewPort(true)
        settings.setLoadWithOverviewMode(true)
        settings.setDomStorageEnabled(true)
        settings.setDisplayZoomControls(false)

        // Allow the RedGifs embedded player to start playback without a tap
        settings.setMediaPlaybackRequiresUserGesture(
            mUrl == null || !LinkHandler.isRedGifsImage(mUrl!!)
        )

        // TODO handle long clicks
        webView!!.setWebChromeClient(chromeClient)


        if (mUrl != null) {
            webView!!.loadUrl(mUrl!!.value)
        } else {
            webView!!.loadHtmlUTF8WithBaseURL("https://reddit.com/", html)
        }

        webView!!.setWebViewClient(object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                if (request == null) {
                    return false
                }

                val url = request.getUrl()

                if (url.getScheme() == "data") {
                    // Prevent imgur bug where we're directed to some random data URI
                    return true
                }

                // Go back if loading same page to prevent redirect loops.
                if (goingBack && currentUrl != null && url == currentUrl) {
                    quickToast(
                        mActivity,
                        String.format(
                            Locale.US,
                            "Handling redirect loop (level %d)",
                            -lastBackDepthAttempt
                        ), Toast.LENGTH_SHORT
                    )

                    lastBackDepthAttempt--

                    if (webView!!.canGoBackOrForward(lastBackDepthAttempt)) {
                        webView!!.goBackOrForward(lastBackDepthAttempt)
                    } else {
                        mActivity!!.finish()
                    }
                } else {
                    if (RedditURLParser.parse(url) != null) {
                        LinkHandler.onLinkClicked(mActivity!!, from(url), false)
                    } else {
                        // When websites recognize the user agent is on Android, they sometimes
                        // redirect or offer deep links into native apps. These come in two flavors:
                        //
                        // 1. `intent://` URLs for arbitrary native apps. Launching these may be a
                        //    security vulnerability, because it's not clear what app is being
                        //    loaded with RedReader's permissions. Luckily, these URLs often have
                        //    fallback HTTP URLs, which can be loaded instead.
                        //
                        // 2. Custom scheme URLs, like `twitter://` or `market://` URLs. While these
                        //    can also launch arbitrary apps, the assumption is custom schemes are
                        //    only used for widely known apps (though even those can be replaced by
                        //    alternative apps). Often, these URLs don't have fallbacks, so take the
                        //    risk of loading these in their native apps.
                        //
                        // All this logic is in the `else` block because processing these URLs can
                        // fail, in which case the logic falls through and treats these URLs as
                        // HTTP URLs.

                        if (url.getScheme() == "intent") {
                            if (onEncounteredIntentUrl(url)) {
                                return true
                            }
                        } else if (url.getScheme() != "http" && url.getScheme() != "https") {
                            if (onEncounteredCustomSchemeUrl(url)) {
                                return true
                            }
                        }

                        if (!PrefsUtility.pref_behaviour_useinternalbrowser()) {
                            LinkHandler.openWebBrowser(
                                mActivity!!,
                                url,
                                true
                            )
                        } else if (PrefsUtility.pref_behaviour_usecustomtabs()) {
                            LinkHandler.openCustomTab(
                                mActivity!!,
                                url,
                                null,
                                true
                            )
                        } else {
                            webView!!.loadUrl(url.toString())
                            currentUrl = from(url)
                        }
                    }
                }

                return true
            }

            /**
             * Assumes the `url` starts with `intent://`
             */
            fun onEncounteredIntentUrl(url: Uri): Boolean {
                val nativeAppIntent: Intent?
                try {
                    nativeAppIntent = Intent.parseUri(url.toString(), Intent.URI_INTENT_SCHEME)
                } catch (e: URISyntaxException) {
                    return false
                }

                if (nativeAppIntent == null) {
                    return false
                }

                val fallbackUrl = nativeAppIntent.getStringExtra("browser_fallback_url")
                if (fallbackUrl == null) {
                    return false
                }

                webView!!.loadUrl(fallbackUrl)
                currentUrl = UriString(fallbackUrl)
                return true
            }

            /**
             * Assumes the `url` starts with something other than `intent://`, `http://` or
             * `https://`
             */
            fun onEncounteredCustomSchemeUrl(url: Uri?): Boolean {
                val nativeAppIntent = Intent(Intent.ACTION_VIEW, url)
                try {
                    startActivity(nativeAppIntent)
                    return true
                } catch (e: ActivityNotFoundException) {
                    return false
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)

                if (mUrl != null && url != null) {
                    val activity: AppCompatActivity? = mActivity

                    if (activity != null) {
                        activity.setTitle(url)
                    }
                }
            }

            override fun onPageFinished(view: WebView, url: String?) {
                super.onPageFinished(view, url)

                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        AndroidCommon.UI_THREAD_HANDLER.post(Runnable {
                            if (currentUrl == null || url == null) {
                                return@post
                            }
                            if (url != view.getUrl()) {
                                return@post
                            }
                            if (goingBack && url == currentUrl) {
                                quickToast(
                                    mActivity,
                                    String.format(
                                        Locale.US,
                                        "Handling redirect loop (level %d)",
                                        -lastBackDepthAttempt
                                    )
                                )

                                lastBackDepthAttempt--

                                if (webView!!.canGoBackOrForward(lastBackDepthAttempt)) {
                                    webView!!.goBackOrForward(lastBackDepthAttempt)
                                } else {
                                    mActivity!!.finish()
                                }
                            } else {
                                goingBack = false
                            }
                        })
                    }
                }, 1000)
            }

            override fun doUpdateVisitedHistory(
                view: WebView?,
                url: String?,
                isReload: Boolean
            ) {
                super.doUpdateVisitedHistory(view, url, isReload)
            }
        })

        val outerFrame = FrameLayout(mActivity!!)
        outerFrame.addView(outer)

        if (post != null) {
            val toolbarOverlay = SideToolbarOverlay(mActivity)

            val bezelOverlay = BezelSwipeOverlay(
                mActivity,
                object : BezelSwipeListener {
                    override fun onSwipe(@BezelSwipeOverlay.SwipeEdge edge: Int): Boolean {
                        toolbarOverlay.setContents(
                            RedditPostActions.generateToolbar(
                                post,
                                mActivity!!,
                                false,
                                toolbarOverlay
                            )
                        )
                        toolbarOverlay.show(
                            if (edge == BezelSwipeOverlay.Companion.LEFT)
                                SideToolbarPosition.LEFT
                            else
                                SideToolbarPosition.RIGHT
                        )
                        return true
                    }

                    override fun onTap(): Boolean {
                        if (toolbarOverlay.isShown()) {
                            toolbarOverlay.hide()
                            return true
                        }

                        return false
                    }
                })

            outerFrame.addView(bezelOverlay)
            outerFrame.addView(toolbarOverlay)

            setLayoutMatchParent(bezelOverlay)
            setLayoutMatchParent(toolbarOverlay)
        }

        return outerFrame
    }

    override fun onDestroyView() {
        webView!!.stopLoading()
        webView!!.loadData("<html></html>", "text/plain", "UTF-8")
        webView!!.reload()
        webView!!.loadUrl("about:blank")
        outer!!.removeAllViews()
        webView!!.destroy()

        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(null)
        cookieManager.flush()

        super.onDestroyView()
    }

    fun onBackButtonPressed(): Boolean {
        if (webView!!.canGoBack()) {
            goingBack = true
            lastBackDepthAttempt = -1
            webView!!.goBack()
            return true
        }

        return false
    }

    override fun onPostSelected(post: RedditPreparedPost?) {
        (mActivity as PostSelectionListener).onPostSelected(post)
    }

    override fun onPostCommentsSelected(post: RedditPreparedPost?) {
        (mActivity as PostSelectionListener).onPostCommentsSelected(post)
    }

    fun getCurrentUrl(): UriString? {
        return if (currentUrl != null) currentUrl else mUrl
    }

    override fun onPause() {
        super.onPause()
        webView!!.onPause()
        webView!!.pauseTimers()
    }

    override fun onResume() {
        super.onResume()
        webView!!.resumeTimers()
        webView!!.onResume()
    }

    fun clearCache() {
        webView!!.clearBrowser()
    }

    companion object {
        fun newInstance(url: UriString?, post: RedditPost?): WebViewFragment {
            val f = WebViewFragment()

            val bundle = Bundle(1)
            bundle.putParcelable("url", url)
            if (post != null) {
                bundle.putParcelable("post", post)
            }
            f.setArguments(bundle)

            return f
        }

        fun newInstanceHtml(html: String?): WebViewFragment {
            val f = WebViewFragment()

            val bundle = Bundle(1)
            bundle.putString("html", html)
            f.setArguments(bundle)

            return f
        }
    }
}
