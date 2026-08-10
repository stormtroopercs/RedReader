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

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.ScrollView
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.common.ChangelogManager
import org.quantumbadger.redreader.common.PrefsUtility

class ChangelogActivity : ViewsBaseActivity() {
    override fun baseActivityNavigationBarColour(): Int {
        return Color.rgb(0x55, 0x55, 0x55)
    }

    protected override fun onCreate(savedInstanceState: Bundle?) {
        PrefsUtility.applySettingsTheme(this)

        super.onCreate(savedInstanceState)

        setTitle(R.string.title_changelog)

        val items = LinearLayout(this)
        items.setOrientation(LinearLayout.VERTICAL)

        ChangelogManager.generateViews(this, items, true)

        val sv = ScrollView(this)
        sv.addView(items)
        setBaseActivityListing(sv)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            android.R.id.home -> {
                finish()
                return true
            }

            else -> return false
        }
    }
}
