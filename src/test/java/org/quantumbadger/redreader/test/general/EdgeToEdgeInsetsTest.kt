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
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.FrameLayout
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.quantumbadger.redreader.activities.HtmlViewActivity
import org.quantumbadger.redreader.activities.PMSendActivity
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.compose.prefs.ComposePrefsSingleton
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.PrefsUtility
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h891dp")
class EdgeToEdgeInsetsTest {

    companion object {
        private const val STATUS_BAR_HEIGHT = 100
        private const val NAV_BAR_HEIGHT = 150

        private fun describe(view: View, depth: Int): String {

            val sb = StringBuilder()

            for (i in 0 until depth) {
                sb.append("  ")
            }

            sb.append(view.javaClass.simpleName)
            sb.append(" bounds=(").append(view.left).append(",").append(view.top)
                .append(",").append(view.right).append(",").append(view.bottom)
                .append(")")

            if (view.layoutParams is ViewGroup.MarginLayoutParams) {
                val lp = view.layoutParams as ViewGroup.MarginLayoutParams
                sb.append(" margins=(").append(lp.leftMargin).append(",").append(lp.topMargin)
                    .append(",").append(lp.rightMargin).append(",").append(lp.bottomMargin)
                    .append(")")
            }

            if (view.background is ColorDrawable) {
                sb.append(" bg=#").append(
                    Integer.toHexString((view.background as ColorDrawable).color)
                )
            }

            sb.append(" fitsSystemWindows=").append(view.fitsSystemWindows)
            sb.append(" padding=(").append(view.paddingLeft).append(",")
                .append(view.paddingTop).append(",").append(view.paddingRight)
                .append(",").append(view.paddingBottom).append(")")

            sb.append("\n")

            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    sb.append(describe(view.getChildAt(i), depth + 1))
                }
            }

            return sb.toString()
        }
    }

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

        val intent = Intent(app, HtmlViewActivity::class.java)
        intent.putExtra("html", "<p>test</p>")
        intent.putExtra("title", "test")

        val activity = Robolectric.buildActivity(HtmlViewActivity::class.java, intent)
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

    @Test
    fun testSystemBarScrimsAppliedOnInsetDispatch() {

        // PMSendActivity still uses the legacy ViewsBaseActivity scrim path
        // (the Compose activities — e.g. MainActivityCompose — handle their
        // insets via the Compose composition instead)
        val controller: ActivityController<PMSendActivity> =
            Robolectric.buildActivity(PMSendActivity::class.java).setup()

        val activity = controller.get()

        val decor = activity.window.decorView

        val insets = WindowInsetsCompat.Builder()
            .setInsets(
                WindowInsetsCompat.Type.statusBars(),
                Insets.of(0, STATUS_BAR_HEIGHT, 0, 0)
            )
            .setInsets(
                WindowInsetsCompat.Type.navigationBars(),
                Insets.of(0, 0, 0, NAV_BAR_HEIGHT)
            )
            .setVisible(WindowInsetsCompat.Type.statusBars(), true)
            .setVisible(WindowInsetsCompat.Type.navigationBars(), true)
            .build()

        decor.dispatchApplyWindowInsets(insets.toWindowInsets())

        val width = 1080
        val height = 2400

        decor.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        decor.layout(0, 0, width, height)

        println("==== Hierarchy after inset dispatch ====")
        println(describe(decor, 0))

        val content = decor.findViewById<ViewGroup>(android.R.id.content)
        Assert.assertNotNull(content)
        Assert.assertEquals(1, content.childCount)

        val root = content.getChildAt(0) as ViewGroup

        Assert.assertTrue(
            "Expected wrapper FrameLayout with content + 4 scrims, got " +
                root.javaClass + " with " + root.childCount + " children",
            root is FrameLayout && root.childCount == 5
        )

        val wrappedContent = root.getChildAt(0)
        val contentParams = wrappedContent.layoutParams as ViewGroup.MarginLayoutParams

        Assert.assertEquals(
            "Content top margin should equal status bar inset",
            STATUS_BAR_HEIGHT,
            contentParams.topMargin
        )

        Assert.assertEquals(
            "Content bottom margin should equal nav bar inset",
            NAV_BAR_HEIGHT,
            contentParams.bottomMargin
        )

        // Children 1-4: left, right, top, bottom scrims
        val scrimTop = root.getChildAt(3)
        val scrimBottom = root.getChildAt(4)

        Assert.assertEquals(
            "Top scrim height should equal status bar inset",
            STATUS_BAR_HEIGHT,
            scrimTop.layoutParams.height
        )

        Assert.assertEquals(
            "Bottom scrim height should equal nav bar inset",
            NAV_BAR_HEIGHT,
            scrimBottom.layoutParams.height
        )

        Assert.assertTrue(
            "Bottom scrim should have a solid colour background",
            scrimBottom.background is ColorDrawable
        )

        println("Bottom scrim colour: #" + Integer.toHexString(
            (scrimBottom.background as ColorDrawable).color
        ))
    }
}
