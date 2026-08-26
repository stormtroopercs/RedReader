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
 ******************************************************************************/
package org.quantumbadger.redreader.test.general

import android.app.Application
import android.view.WindowInsetsController
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.compose.prefs.ComposePrefsSingleton
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.PrefsUtility
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
class EdgeToEdgeInsetsTest {

    @Before
    fun initPrefs() {

        // Initialise PrefsUtility's static state directly: the full init()
        // path calls General.initAppConfig(), which doesn't work under
        // Robolectric
        val app = RuntimeEnvironment.getApplication() as Application

        try {
            val resField = PrefsUtility::class.java.getDeclaredField("mRes")
            resField.isAccessible = true
            resField.set(null, app.resources)

            val prefsField = PrefsUtility::class.java.getDeclaredField("sharedPrefs")
            prefsField.isAccessible = true
            prefsField.set(null, General.getSharedPrefs(app))

        } catch (e: ReflectiveOperationException) {
            throw RuntimeException(e)
        }

        // The Compose activity wrappers pull RedditAccountManager and
        // ComposePrefsSingleton from state wired in RedReader.onCreate, which
        // Robolectric doesn't run
        ComposePrefsSingleton.init(app)
        RedditAccountManager.setInstance(RedditAccountManager(app))
    }

    @Test
    fun testNavBarIconAppearanceForWhitePref() {

        val app = RuntimeEnvironment.getApplication() as Application

        General.getSharedPrefs(app)
            .edit()
            .putString("pref_appearance_navbar_color", "white")
            .apply()

        // A plain ComposeBaseActivity host (see EdgeToEdgeInsetsTestActivity):
        // the window's system-bar appearance is what's under test, not the
        // composed screen.
        val activity = Robolectric.buildActivity(EdgeToEdgeInsetsTestActivity::class.java)
            .setup()
            .get()

        val appearance = activity.window.insetsController!!.systemBarsAppearance

        val appearanceForceLightNavigationBars = 1 shl 9

        println("==== Final appearance bits: 0x" + Integer.toHexString(appearance) + " ====")

        Assert.assertEquals(
            "FORCE_LIGHT_NAVIGATION_BARS should be clear",
            0,
            appearance and appearanceForceLightNavigationBars
        )

        Assert.assertNotEquals(
            "LIGHT_NAVIGATION_BARS should be set for a white nav bar",
            0,
            appearance and WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        )
    }
}
