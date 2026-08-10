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
package org.quantumbadger.redreader.settings

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.FragmentTransaction
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.activities.ViewsBaseActivity
import org.quantumbadger.redreader.common.PrefsUtility

class SettingsActivity : ViewsBaseActivity() {
    private fun launchFragment(panel: String) {
        val bundle = Bundle()
        bundle.putString("panel", panel)

        getSupportFragmentManager()
            .beginTransaction()
            .setReorderingAllowed(false)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.single_fragment_container, SettingsFragment::class.java, bundle)
            .addToBackStack("Settings: " + panel)
            .commit()
    }

    override fun baseActivityNavigationBarColour(): Int {
        return Color.rgb(0x55, 0x55, 0x55)
    }

    protected override fun onCreate(savedInstanceState: Bundle?) {
        PrefsUtility.applySettingsTheme(this)

        super.onCreate(savedInstanceState)

        setBaseActivityListing(R.layout.single_fragment_layout)

        val bundle = Bundle()
        bundle.putString("panel", "root")

        getSupportFragmentManager()
            .beginTransaction()
            .setReorderingAllowed(false)
            .replace(R.id.single_fragment_container, SettingsFragment::class.java, bundle)
            .commit()
    }

    fun onPanelSelected(panel: String) {
        launchFragment(panel)
    }
}
