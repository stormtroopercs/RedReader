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

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.quantumbadger.redreader.compose.activity.ComposeBaseActivity
import org.quantumbadger.redreader.compose.ui.RedditTermsScreen

/**
 * Thin Compose wrapper around [RedditTermsScreen].
 * Keeps the legacy [launch] API so existing call sites still work.
 */
class RedditTermsActivity : ComposeBaseActivity() {

    companion object {
        private const val EXTRA_LAUNCH_MAIN = "launch_main"

        @JvmStatic
        fun launch(activity: AppCompatActivity, launchMainOnClose: Boolean) {
            val intent = Intent(activity, RedditTermsActivity::class.java)
            intent.putExtra(EXTRA_LAUNCH_MAIN, launchMainOnClose)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val launchMainOnClose = intent.getBooleanExtra(EXTRA_LAUNCH_MAIN, false)

        setContentCompose {
            RedditTermsScreen(launchMainOnClose = launchMainOnClose) {
                finish()
                if (launchMainOnClose) {
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }
        }
    }
}
