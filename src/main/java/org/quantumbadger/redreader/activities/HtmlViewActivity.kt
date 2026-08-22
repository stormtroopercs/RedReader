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

package org.quantumbadger.redreader.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import org.quantumbadger.redreader.compose.activity.ComposeBaseActivity
import org.quantumbadger.redreader.compose.ui.HtmlViewScreen
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Thin Compose wrapper around [HtmlViewScreen].
 * Keeps the legacy [showAsset] API so existing call sites still work.
 *
 * The loaded HTML can navigate to another page, in which case system back
 * must walk the WebView's own history before closing the screen. That is
 * handled here (synchronously, against the live WebView) rather than in a
 * Compose BackHandler: [ComposeBaseActivity] deliberately routes system back
 * through BaseActivity's OnBackPressedCallback instead of a Compose dispatcher.
 */
class HtmlViewActivity : ComposeBaseActivity() {

    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val html = intent.getStringExtra("html")
        val title = intent.getStringExtra("title") ?: "Document"

        if (html == null) {
            finish()
            return
        }

        setContentCompose {
            HtmlViewScreen(
                html = html,
                title = title,
                onNavigateBack = ::finish,
                onWebViewCreated = { webView = it }
            )
        }
    }

    /**
     * Consume a system back press to walk the WebView back while it has
     * history to go back through; otherwise return false so BaseActivity
     * performs its default (finish) behaviour.
     */
    override fun baseActivityOnBackPressed(): Boolean {
        val webView = webView ?: return false
        return if (webView.canGoBack()) {
            webView.goBack()
            true
        } else {
            false
        }
    }

    /**
     * The WebView's history can change at any time, and this activity can't
     * observe every change; on API levels below 36 the callback is always
     * enabled so [baseActivityOnBackPressed] is consulted on every press.
     * Report the current WebView history so the predictive-back path (API 36+)
     * keeps the callback enabled while there is history to walk.
     */
    override fun baseActivityMustInterceptBack(): Boolean {
        return webView?.canGoBack() == true
    }

    companion object {
        fun showAsset(context: Context, filename: String) {
            val html: String
            try {
                context.assets.open(filename).use { asset ->
                    val baos = ByteArrayOutputStream(16384)
                    val buf = ByteArray(8192)
                    var bytesRead: Int
                    while ((asset.read(buf).also { bytesRead = it }) > 0) {
                        baos.write(buf, 0, bytesRead)
                    }
                    html = baos.toString("UTF-8")
                }
            } catch (e: IOException) {
                return
            }

            val intent = Intent(context, HtmlViewActivity::class.java)
            intent.putExtra("html", html)
            context.startActivity(intent)
        }
    }
}
