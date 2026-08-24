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

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.General.checkThisIsUIThread
import org.quantumbadger.redreader.common.General.getSharedPrefs
import org.quantumbadger.redreader.common.GlobalExceptionHandler.Companion.handleLastCrash
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.SharedPrefsWrapper
import org.quantumbadger.redreader.common.TorCommon
import java.util.concurrent.atomic.AtomicInteger

abstract class BaseActivity : AppCompatActivity(),
    SharedPrefsWrapper.OnSharedPreferenceChangeListener {
    private var mSharedPreferences: SharedPrefsWrapper?=null

    private val mRequestIdGenerator = AtomicInteger(10000)

    private val mPermissionRequestCallbacks = HashMap<Int?, PermissionCallback?>()

    private val mActivityResultCallbacks = HashMap<Int?, ActivityResultCallback?>()

    interface PermissionCallback {
        fun onPermissionGranted()

        fun onPermissionDenied()
    }

    fun interface ActivityResultCallback {
        fun onActivityResult(resultCode: Int, data: Intent?)
    }

    private val mBackPressedCallback: OnBackPressedCallback=object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!osHandlesBackAnimations() && !General.onBackPressed()) {
                // Debounced: ignore rapid repeated presses
                return
            }

            if (baseActivityOnBackPressed()) {
                invalidateBackPressedCallback()
                return
            }

            // Not intercepting this press: disable this callback and
            // re-dispatch, so that the next handler runs (either the fragment
            // back stack, or the default behaviour of finishing the activity).
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()
            invalidateBackPressedCallback()
        }
    }

    fun closeAllExceptMain() {
        closingAll = true
        closeIfNecessary()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSharedPreferences = getSharedPrefs(this)

        if (baseActivityConfiguresEdgeToEdge()) {
            applyEdgeToEdge()
        }

        if (PrefsUtility.pref_appearance_android_status()
            == PrefsUtility.AppearanceStatusBarMode.ALWAYS_HIDE
        ) {
            hideStatusBar()
        }

        if (PrefsUtility.behaviour_block_screenshots()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        if (PrefsUtility.pref_behaviour_keep_screen_awake()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        mSharedPreferences!!.registerOnSharedPreferenceChangeListener(this)
        setOrientationFromPrefs()
        closeIfNecessary()

        onBackPressedDispatcher.addCallback(this, mBackPressedCallback)
        invalidateBackPressedCallback()

        handleLastCrash(this)
    }

    /**
     * Called when the user navigates back. Return true to consume the press,
     * or false for the default behaviour. Subclasses which override this must
     * also override [.baseActivityMustInterceptBack], and call
     * [.invalidateBackPressedCallback] whenever its result changes.
     */
    protected open fun baseActivityOnBackPressed(): Boolean {
        return false
    }

    /**
     * Whether [.baseActivityOnBackPressed] might currently consume a
     * back press. When the OS provides predictive back animations, back
     * presses are only intercepted while this returns true.
     */
    protected open fun baseActivityMustInterceptBack(): Boolean {
        return false
    }

    fun invalidateBackPressedCallback() {
        mBackPressedCallback.isEnabled =             !osHandlesBackAnimations() || baseActivityMustInterceptBack()
    }

    /**
     * Whether this class should configure the window for edge-to-edge display.
     * Subclasses which call enableEdgeToEdge() themselves should return false.
     */
    protected open fun baseActivityConfiguresEdgeToEdge(): Boolean {
        return true
    }

    /**
     * Lays the window out edge-to-edge on all API levels, with transparent
     * system bars and light bar icons (matching the app's pre-edge-to-edge
     * appearance). Bar backgrounds are drawn by the app -- see
     * ViewsBaseActivity.
     */
    private fun applyEdgeToEdge() {
        val window = getWindow()

        // The first getDecorView() call runs PhoneWindow.generateLayout(),
        // which reads the enforce*Contrast attributes back out of the theme.
        // The decor must therefore exist before the contrast flags below are
        // set, or they'd be silently reverted (leaving the system to draw a
        // theme-tinted scrim over the 3-button navigation bar).
        val controller = WindowCompat.getInsetsController(window, window.getDecorView())

        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT < 35) {
            // Deprecated and a no-op from SDK 35 onwards: the status bar is
            // always transparent once edge-to-edge is enforced
            window.setStatusBarColor(Color.TRANSPARENT)
        }

        // Deliberately called on every API level, even though it's deprecated
        // and draws nothing from SDK 35 onwards: it marks the nav bar colour
        // as app-specified, which stops DecorView deriving the nav button
        // appearance from the window background's luminance
        // (APPEARANCE_FORCE_LIGHT_NAVIGATION_BARS) and overriding the
        // appearance requested via the insets controller
        window.setNavigationBarColor(Color.TRANSPARENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // The app draws its own bar backgrounds, so the system shouldn't
            // add a contrast scrim of its own (e.g. for 3-button navigation)
            window.setStatusBarContrastEnforced(false)
            window.setNavigationBarContrastEnforced(false)
        }

        controller.setAppearanceLightStatusBars(false)
        controller.setAppearanceLightNavigationBars(false)
    }

    protected fun hideStatusBar() {
        val controller = WindowCompat.getInsetsController(
            getWindow(),
            getWindow().getDecorView()
        )

        controller.setSystemBarsBehavior(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        )
        controller.hide(WindowInsetsCompat.Type.statusBars())
    }

    protected fun showStatusBar() {
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
            .show(WindowInsetsCompat.Type.statusBars())
    }

    override fun onResume() {
        super.onResume()
        setOrientationFromPrefs()
        invalidateBackPressedCallback()
        closeIfNecessary()
        TorCommon.updateTorStatus()
    }


    override fun onDestroy() {
        super.onDestroy()
        mSharedPreferences!!.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun closeIfNecessary() {
        if (closingAll) {
            if (this is MainActivityCompose) {
                closingAll = false
            } else {
                finish()
            }
        }
    }

    fun requestPermissionWithCallback(
        permission: String,
        callback: PermissionCallback
    ) {
        checkThisIsUIThread()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                callback.onPermissionGranted()
            } else {
                val requestCode = mRequestIdGenerator.incrementAndGet()
                mPermissionRequestCallbacks.put(requestCode, callback)
                requestPermissions(arrayOf<String>(permission), requestCode)
            }
        } else {
            callback.onPermissionGranted()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val callback = mPermissionRequestCallbacks.remove(requestCode)

        if (callback == null) {
            return
        }

        if (permissions.size != 1) {
            throw RuntimeException("Unexpected permission result")
        }

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            callback.onPermissionGranted()
        } else {
            callback.onPermissionDenied()
        }
    }

    fun startActivityForResultWithCallback(
        intent: Intent,
        callback: ActivityResultCallback
    ) {
        val requestCode = mRequestIdGenerator.incrementAndGet()
        mActivityResultCallbacks.put(requestCode, callback)
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        val callback = mActivityResultCallbacks.remove(requestCode)

        if (callback == null) {
            return
        }

        callback.onActivityResult(resultCode, data)
    }

    private fun setOrientationFromPrefs() {
        val orientation = PrefsUtility.pref_behaviour_screen_orientation()

        if (orientation == PrefsUtility.ScreenOrientation.AUTO) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
        } else if (orientation == PrefsUtility.ScreenOrientation.PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        } else if (orientation == PrefsUtility.ScreenOrientation.LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        }
    }


    protected open fun onSharedPreferenceChangedInner(
        prefs: SharedPrefsWrapper,
        key: String
    ) {
        // Do nothing
    }

    override fun onSharedPreferenceChanged(
        prefs: SharedPrefsWrapper,
        key: String
    ) {
        onSharedPreferenceChangedInner(prefs, key)

        if (key == getString(R.string.pref_behaviour_screenorientation_key)) {
            setOrientationFromPrefs()
        }
    }

    companion object {
        private var closingAll = false

        /**
         * True if the OS animates back navigation itself (predictive back), which
         * is the default for apps targeting API 36+. In this case the back
         * callback is only enabled while the activity actually needs to intercept
         * back presses (so that the system animations run the rest of the time),
         * and the double-press guard is skipped, as the gesture animation already
         * guards against accidental presses.
         */
        private fun osHandlesBackAnimations(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
        }
    }
}
