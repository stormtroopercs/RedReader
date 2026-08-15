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

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.quantumbadger.redreader.common.General.setLayoutMatchParent
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.common.RunnableOnce
import org.quantumbadger.redreader.common.UriString.Companion.from
import org.quantumbadger.redreader.reddit.api.RedditOAuth.completeLogin
import org.quantumbadger.redreader.common.General

class LinkDispatchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val backgroundView = View(this)

        backgroundView.setBackground(
            GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(-0x2cd0d1, -0x4ad9da)
            )
        )

        setContentView(backgroundView)

        setLayoutMatchParent(backgroundView)

        val intent = getIntent()

        if (intent == null) {
            Log.e(TAG, "Got null intent")
            finish()
            return
        }

        val data = intent.getData()

        if (data == null) {
            Log.e(TAG, "Got null intent data")
            finish()
            return
        }

        if (data.getScheme().equals("redreader", ignoreCase = true)) {
            completeLogin(this, data, RunnableOnce(Runnable { this.finish() }))
        } else {
            onLinkClicked(this, from(data), true, null, null, 0, true)
            finish()
        }
    }

    companion object {
        private const val TAG = "LinkDispatchActivity"
    }
}
