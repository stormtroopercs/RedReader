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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.activities.BugReportActivity.Companion.handleGlobalError
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.fragments.WebViewFragment
import java.io.ByteArrayOutputStream
import java.io.IOException

class HtmlViewActivity : ViewsBaseActivity() {
    private var webView: WebViewFragment? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        PrefsUtility.applyTheme(this)

        super.onCreate(savedInstanceState)

        val intent = getIntent()

        val html = intent.getStringExtra("html")
        val title = intent.getStringExtra("title")
        setTitle(title)

        if (html == null) {
            handleGlobalError(this, "No HTML")
        }

        webView = WebViewFragment.Companion.newInstanceHtml(html)

        setBaseActivityListing(View.inflate(this, R.layout.main_single, null))

        getSupportFragmentManager().beginTransaction()
            .add(R.id.main_single_frame, webView!!)
            .commit()
    }

    override fun baseActivityMustInterceptBack(): Boolean {
        // Always intercept, as the WebView may need to navigate back through
        // its history. See the equivalent method in WebViewActivity.
        return true
    }

    override fun baseActivityOnBackPressed(): Boolean {
        return webView!!.onBackButtonPressed()
    }

    companion object {
        fun showAsset(
            context: Context,
            filename: String
        ) {
            val html: String

            try {
                context.getAssets().open(filename).use { asset ->
                    val baos = ByteArrayOutputStream(16384)
                    val buf = ByteArray(8192)
                    var bytesRead: Int

                    while ((asset.read(buf).also { bytesRead = it }) > 0) {
                        baos.write(buf, 0, bytesRead)
                    }
                    html = baos.toString("UTF-8")
                }
            } catch (e: IOException) {
                handleGlobalError(context, e)
                return
            }

            val intent = Intent(context, HtmlViewActivity::class.java)
            intent.putExtra("html", html)
            context.startActivity(intent)
        }
    }
}
