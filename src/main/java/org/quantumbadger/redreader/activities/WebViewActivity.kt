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

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.core.content.IntentCompat
import org.quantumbadger.redreader.compose.ui.WebViewScreen
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.UriString

/**
 * Thin Compose wrapper around [WebViewScreen].
 * Keeps the Activity so existing Intent-based call sites still work.
 *
 * Note: For complex WebView features (bezel menus, video fullscreen,
 * redirect loop detection), use the legacy WebViewFragment directly.
 */
class WebViewActivity : ViewsBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        PrefsUtility.applyTheme(this)
        super.onCreate(savedInstanceState)

        val intent = getIntent()
        val url = IntentCompat.getParcelableExtra<UriString?>(intent, "url", UriString::class.java)

        if (url == null) {
            finish()
            return
        }

        setContent {
            WebViewScreen(
                url = url.value,
                onNavigateBack = ::finish
            )
        }
    }
}
