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

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.ColorUtils
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.common.FunctionOneArgNoReturn
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.General.isTablet
import org.quantumbadger.redreader.common.General.setLayoutMatchParent
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.PrefsUtility.AppearanceNavbarColour
import org.quantumbadger.redreader.common.SharedPrefsWrapper
import java.util.Locale
import kotlin.math.max

abstract class ViewsBaseActivity : BaseActivity() {
    private var mActionbarTitleTextView: Optional<TextView> = Optional.Companion.empty<TextView>()

    private var mContentListing: FrameLayout?=null
    private var mContentOverlay: FrameLayout?=null

    private var mActionbarBackIconView: ImageView?=null
    private var mActionbarTitleOuterView: View?=null

    protected open fun baseActivityIsToolbarActionBarEnabled(): Boolean {
        return true
    }

    protected open fun baseActivityIsToolbarSearchBarEnabled(): Boolean {
        return false
    }

    protected open fun baseActivityIsActionBarBackEnabled(): Boolean {
        return true
    }

    override fun setTitle(text: CharSequence?) {
        super.setTitle(text)
        mActionbarTitleTextView.apply(FunctionOneArgNoReturn { titleView: TextView? ->
            titleView!!.setText(
                text
            )
        })
    }

    override fun setTitle(res: Int) {
        setTitle(getText(res))
    }

    val supportActionBarOrThrow: ActionBar
        // Avoids IDE warnings about null pointers
        get() {
            val result: ActionBar = getSupportActionBar()!!

            if (result == null) {
                throw RuntimeException("Action bar is null")
            }

            return result
        }

    protected fun configBackButton(isVisible: Boolean, listener: View.OnClickListener?) {
        mActionbarBackIconView!!.setImportantForAccessibility(
            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        )

        mActionbarTitleTextView.apply(
            FunctionOneArgNoReturn { titleView: TextView? ->
                titleView!!.setImportantForAccessibility(
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                )
            })

        if (isVisible) {
            mActionbarBackIconView!!.setVisibility(View.VISIBLE)
            mActionbarTitleOuterView!!.setOnClickListener(listener)
            mActionbarTitleOuterView!!.setClickable(true)
            mActionbarTitleOuterView!!.setContentDescription(getString(string.action_back))
            mActionbarTitleOuterView!!.setImportantForAccessibility(
                View.IMPORTANT_FOR_ACCESSIBILITY_YES
            )

            if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault())
                == View.LAYOUT_DIRECTION_RTL
            ) {
                mActionbarBackIconView!!.setImageResource(R.drawable.ic_action_forward_dark)
            }
        } else {
            mActionbarBackIconView!!.setVisibility(View.GONE)
            mActionbarTitleOuterView!!.setClickable(false)

            mActionbarTitleOuterView!!.setContentDescription(null)

            mActionbarTitleOuterView!!.setImportantForAccessibility(
                View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            )
        }
    }

    protected open fun baseActivityAllowToolbarHideOnScroll(): Boolean {
        // Disallow by default
        return false
    }

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (baseActivityIsToolbarActionBarEnabled()) {
            val outerView: View

            val isTablet = isTablet(this)

            val prefBottomToolbar = PrefsUtility.pref_appearance_bottom_toolbar()

            val prefHideOnScroll = PrefsUtility.pref_appearance_hide_toolbar_on_scroll()

            val layoutRes: Int

            if (prefHideOnScroll && !isTablet) {
                if (baseActivityAllowToolbarHideOnScroll()) {
                    layoutRes = R.layout.rr_actionbar_hide_on_scroll
                } else {
                    layoutRes = R.layout.rr_actionbar
                }
            } else if (prefBottomToolbar) {
                layoutRes = R.layout.rr_actionbar_reverse
            } else {
                layoutRes = R.layout.rr_actionbar
            }

            outerView = getLayoutInflater().inflate(layoutRes, null)

            val toolbar = outerView.findViewById<Toolbar>(R.id.rr_actionbar_toolbar)
            mContentListing = outerView.findViewById<FrameLayout>(R.id.rr_actionbar_content_listing)
            mContentOverlay = outerView.findViewById<FrameLayout>(R.id.rr_actionbar_content_overlay)

            super.setContentView(wrapWithSystemBarScrims(outerView))
            setSupportActionBar(toolbar)

            val supportActionBar = this.supportActionBarOrThrow

            if (baseActivityIsToolbarSearchBarEnabled()) {
                supportActionBar.setCustomView(R.layout.actionbar_search)
                setLayoutMatchParent(supportActionBar.getCustomView())
            } else {
                supportActionBar.setCustomView(R.layout.actionbar_title)
            }

            supportActionBar.setDisplayShowCustomEnabled(true)
            supportActionBar.setDisplayShowTitleEnabled(false)
            toolbar.setContentInsetsAbsolute(0, 0)

            mActionbarBackIconView =                 toolbar.findViewById<ImageView>(R.id.actionbar_title_back_image)
            mActionbarTitleOuterView = toolbar.findViewById<View>(R.id.actionbar_title_outer)

            if (baseActivityIsToolbarSearchBarEnabled()) {
                mActionbarTitleTextView = Optional.Companion.empty<TextView>()
            } else {
                mActionbarTitleTextView = Optional.Companion.of<TextView>(
                    toolbar.findViewById<TextView?>(R.id.actionbar_title_text)
                )
            }

            if (getTitle() != null) {
                // Update custom action bar text
                setTitle(getTitle())
            }

            configBackButton(
                baseActivityIsActionBarBackEnabled(),
                View.OnClickListener { v: View? -> onBackPressedDispatcher.onBackPressed() })
        } else {
            mContentListing = FrameLayout(this)
            mContentOverlay = FrameLayout(this)

            val outer = FrameLayout(this)
            outer.addView(mContentListing)
            outer.addView(mContentOverlay)

            super.setContentView(wrapWithSystemBarScrims(outer))
        }
    }

    /**
     * The colour drawn behind the navigation bar, replicating the old
     * window-level navigation bar colour.
     */
    protected open fun baseActivityNavigationBarColour(): Int {
        val navbarColour = PrefsUtility.appearance_navbar_colour()

        if (navbarColour == AppearanceNavbarColour.BLACK) {
            return Color.BLACK
        } else if (navbarColour == AppearanceNavbarColour.WHITE) {
            return Color.WHITE
        }

        val colour: Int
        run {
            val appearance = obtainStyledAttributes(
                intArrayOf(
                    androidx.appcompat.R.attr.colorPrimary,
                    androidx.appcompat.R.attr.colorPrimaryDark
                )
            )
            if (navbarColour == AppearanceNavbarColour.PRIMARY) {
                colour = appearance.getColor(0, General.COLOR_INVALID)
            } else {
                colour = appearance.getColor(1, General.COLOR_INVALID)
            }
            appearance.recycle()
        }

        return colour
    }

    private fun makeScrim(gravity: Int): View {
        val scrim = View(this)
        scrim.setLayoutParams(FrameLayout.LayoutParams(0, 0, gravity))
        return scrim
    }

    /**
     * The window is laid out edge-to-edge, so the activity content is inset by
     * the window insets here, and the system bar areas are painted to match
     * the app's pre-edge-to-edge appearance: the status bar in the theme's
     * colorPrimaryDark, and the navigation bar in the colour from
     * baseActivityNavigationBarColour().
     */
    // Window insets are physical coordinates, so the left/right scrims must
    // stay on their physical edges regardless of layout direction
    @SuppressLint("RtlHardcoded")
    private fun wrapWithSystemBarScrims(content: View): View {
        val statusBarColour: Int
        run {
            val appearance =                 obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.colorPrimaryDark))
            statusBarColour = appearance.getColor(0, General.COLOR_INVALID)
            appearance.recycle()
        }

        val navBarColour = baseActivityNavigationBarColour()

        // Report the effective nav bar colour to the system, even though the
        // visible pixels come from the scrim view below. From SDK 35 this
        // deprecated call draws nothing, but an opaque colour here keeps
        // SystemUI out of "transparent bar" mode, in which it ignores
        // the light/dark icon appearance requested below.
        getWindow().setNavigationBarColor(navBarColour)

        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
            .setAppearanceLightNavigationBars(
                ColorUtils.calculateLuminance(navBarColour) > 0.5
            )

        val root = FrameLayout(this)

        val scrimLeft = makeScrim(Gravity.LEFT)
        val scrimRight = makeScrim(Gravity.RIGHT)
        val scrimTop = makeScrim(Gravity.TOP)
        val scrimBottom = makeScrim(Gravity.BOTTOM)

        root.addView(content)

        // Side scrims first, so that the status/nav bar colours win in the
        // corners, as if the horizontal bars spanned the full screen width
        root.addView(scrimLeft)
        root.addView(scrimRight)
        root.addView(scrimTop)
        root.addView(scrimBottom)

        ViewCompat.setOnApplyWindowInsetsListener(
            root,
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                val bars = insets!!.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            or WindowInsetsCompat.Type.displayCutout()
                )
                val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

                val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom

                val contentParams = content.getLayoutParams() as FrameLayout.LayoutParams
                contentParams.setMargins(
                    bars.left,
                    bars.top,
                    bars.right,
                    max(bars.bottom, imeBottom)
                )
                content.setLayoutParams(contentParams)

                // Areas which are pure display cutout (no bar drawn over them)
                // are painted black, matching the old letterboxing behaviour
                scrimTop.setBackgroundColor(
                    if (insets.isVisible(WindowInsetsCompat.Type.statusBars()))
                        statusBarColour
                    else
                        Color.BLACK
                )
                scrimBottom.setBackgroundColor(
                    if (navBars.bottom > 0) navBarColour else Color.BLACK
                )
                scrimLeft.setBackgroundColor(
                    if (navBars.left > 0) navBarColour else Color.BLACK
                )
                scrimRight.setBackgroundColor(
                    if (navBars.right > 0) navBarColour else Color.BLACK
                )

                setScrimBounds(scrimTop, FrameLayout.LayoutParams.MATCH_PARENT, bars.top)
                setScrimBounds(scrimBottom, FrameLayout.LayoutParams.MATCH_PARENT, bars.bottom)
                setScrimBounds(scrimLeft, bars.left, FrameLayout.LayoutParams.MATCH_PARENT)
                setScrimBounds(scrimRight, bars.right, FrameLayout.LayoutParams.MATCH_PARENT)
                WindowInsetsCompat.CONSUMED
            })

        return root
    }

    fun setBaseActivityListing(view: View) {
        mContentListing!!.removeAllViews()
        mContentListing!!.addView(view)
    }

    fun clearBaseActivityListing() {
        mContentListing!!.removeAllViews()
    }

    fun setBaseActivityListing(layoutRes: Int) {
        mContentListing!!.removeAllViews()
        getLayoutInflater().inflate(layoutRes, mContentListing, true)
    }

    fun setBaseActivityOverlay(view: View) {
        mContentOverlay!!.removeAllViews()
        mContentOverlay!!.addView(view)
    }

    public override fun onSharedPreferenceChanged(
        prefs: SharedPrefsWrapper,
        key: String
    ) {
        super.onSharedPreferenceChanged(prefs, key)

        if (key.startsWith(getString(string.pref_menus_appbar_prefix))
            || key == getString(string.pref_menus_quick_account_switcher_key)
            || key == getString(string.pref_pinned_subreddits_key)
        ) {
            invalidateOptionsMenu()
        }
    }

    companion object {
        private fun setScrimBounds(
            scrim: View,
            width: Int,
            height: Int
        ) {
            val params = scrim.getLayoutParams() as FrameLayout.LayoutParams
            params.width = width
            params.height = height
            scrim.setLayoutParams(params)
        }
    }
}
