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
import org.quantumbadger.redreader.compose.activity.ComposeBaseActivity
import org.quantumbadger.redreader.compose.ui.HtmlViewScreen
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Thin Compose wrapper around [HtmlViewScreen].
 * Keeps the legacy [showAsset] API so existing call sites still work.
 */
class HtmlViewActivity : ComposeBaseActivity() {

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
                onNavigateBack = ::finish
            )
        }
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
