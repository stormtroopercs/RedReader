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

package org.quantumbadger.redreader.activities

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry

import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.common.FeatureFlagHandler
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.SharedPrefsWrapper

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue

/**
 * Tests for the predictive back migration (API 36 / Android 16).
 *
 * All `onBackPressed()` overrides have been removed from the app, so
 * any correct back behaviour observed here must be flowing through
 * [BaseActivity]'s [androidx.activity.OnBackPressedCallback].
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class BackNavigationUITest {

    private companion object {
        private const val TEST_SUBREDDIT_URL
            = "https://reddit.com/r/redreader_public_test"

        private const val PREF_BACK_AGAIN = "pref_behaviour_back_again"
        private const val PREF_TWOPANE = "pref_appearance_twopane"
        private const val PREF_POST_TAP_ACTION = "pref_behaviour_post_tap_action"

        private const val HTML_NO_HISTORY = "<html><body>no history</body></html>"
    }

    private lateinit var mContext: Context
    private lateinit var mPrefs: SharedPrefsWrapper
    private lateinit var mRawPrefs: android.content.SharedPreferences

    @Before
    fun setUp() {
        mContext = ApplicationProvider.getApplicationContext()
        mPrefs = General.getSharedPrefs(mContext)
        mRawPrefs = mContext.getSharedPreferences(
            mContext.packageName + "_preferences",
            Context.MODE_PRIVATE
        )

        // Stop the terms screen, the first-run login prompt and the changelog
        // dialog from covering the activities under test
        PrefsUtility.acceptRedditUserAgreement()

        val versionCode = mContext.packageManager
            .getPackageInfo(mContext.packageName, 0)
            .longVersionCode.toInt()

        if (!mRawPrefs.contains(FeatureFlagHandler.PREF_FIRST_RUN_MESSAGE_SHOWN)
            || mRawPrefs.getInt(FeatureFlagHandler.PREF_LAST_VERSION, 0)
            != versionCode
        ) {

            // Marking the first run as done skips MainActivity's call to
            // handleFirstInstall(), so the feature flags must be set here
            // instead. Without this, handleUpgrade() writes preferences during
            // onCreate(), which triggers a refresh before the layout exists.
            FeatureFlagHandler.handleFirstInstall(mPrefs)

            mPrefs.edit()
                .putString(FeatureFlagHandler.PREF_FIRST_RUN_MESSAGE_SHOWN, "true")
                .putInt(FeatureFlagHandler.PREF_LAST_VERSION, versionCode)
                .apply()

            settlePreferences()
        }

        setBackAgain(false)
        setTwoPane("auto")
        setPostTapAction("link")
    }

    private fun setBackAgain(value: Boolean) {

        if (mRawPrefs.getBoolean(PREF_BACK_AGAIN, false) != value) {
            mPrefs.edit().putBoolean(PREF_BACK_AGAIN, value).apply()
            settlePreferences()
        }
    }

    private fun setTwoPane(value: String) {

        if (value != mRawPrefs.getString(PREF_TWOPANE, "auto")) {
            mPrefs.edit().putString(PREF_TWOPANE, value).apply()
            settlePreferences()
        }
    }

    private fun setPostTapAction(value: String) {

        if (value != mRawPrefs.getString(PREF_POST_TAP_ACTION, "link")) {
            mPrefs.edit().putString(PREF_POST_TAP_ACTION, value).apply()
            settlePreferences()
        }
    }

    /**
     * `apply()` delivers its listener callbacks asynchronously on the main
     * thread. Activities refresh themselves when preferences change, so the
     * callbacks must be drained before an activity is launched -- otherwise they
     * arrive midway through `onCreate()`, before the activity has finished
     * building its layout.
     */
    private fun settlePreferences() {
        SystemClock.sleep(300)
        waitForIdle()
    }

    private fun intentFor(
        context: Context,
        activity: Class<out android.app.Activity>,
        url: String
    ): Intent {

        val intent = Intent(context, activity)
        intent.data = Uri.parse(url)
        return intent
    }

    /**
     * Whether the activity is currently intercepting back presses. When this is
     * false on Android 16, the system handles back itself (with predictive back
     * animations).
     */
    private fun hasEnabledCallbacks(scenario: ActivityScenario<*>): Boolean {

        val result = AtomicBoolean()

        scenario.onActivity { activity ->
            result.set(
                (activity as BaseActivity).onBackPressedDispatcher
                    .hasEnabledCallbacks()
            )
        }

        return result.get()
    }

    /**
     * Asserts whether the activity is intercepting back presses.
     *
     * Below API 36 the callback is always enabled, because it also runs the
     * double-press guard; `expectedOnApi36` therefore only applies where
     * the OS provides predictive back.
     */
    private fun assertIntercepts(
        message: String,
        expectedOnApi36: Boolean,
        scenario: ActivityScenario<*>
    ) {

        assertEquals(
            message,
            Build.VERSION.SDK_INT < 36 || expectedOnApi36,
            hasEnabledCallbacks(scenario)
        )
    }

    /**
     * Presses back, having first waited out the 300ms double-press guard. Below
     * API 36 the guard is active and Espresso's back press is fast enough to
     * fall inside its window.
     */
    private fun pressBackAfterGuardWindow() {
        SystemClock.sleep(400)
        pressBackUnconditionally()
    }

    /**
     * Polls until a view matching `matcher` is displayed, or the timeout
     * expires. Used in place of a fixed wait for content which is loaded over
     * the network.
     */
    private fun awaitView(matcher: Matcher<View>, timeoutSeconds: Int) {

        for (attempt in 0 until timeoutSeconds * 2) {

            try {
                onView(firstMatching(allOf(matcher, isDisplayed())))
                    .check(matches(isDisplayed()))
                return

            } catch (e: Throwable) {
                SystemClock.sleep(500)
            }
        }
    }

    private fun waitForIdle() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    /**
     * `finish()` is asynchronous, so an activity that is on its way out
     * briefly reports STARTED rather than DESTROYED.
     */
    private fun awaitDestroyed(scenario: ActivityScenario<*>): Lifecycle.State {

        repeat(100) {
            if (scenario.state == Lifecycle.State.DESTROYED) {
                return Lifecycle.State.DESTROYED
            }

            SystemClock.sleep(50)
        }

        return scenario.state
    }

    /**
     * Dispatches a back press straight through the activity's
     * [androidx.activity.OnBackPressedDispatcher], which is where the double-press guard
     * lives.
     *
     * Espresso's `pressBack()` takes the better part of a second to
     * complete, so it cannot be used to produce two presses inside the 300ms
     * guard window. The end-to-end tests in this class do use real input; this
     * helper exists only for the tests that need presses in quick succession.
     */
    private fun dispatchBack(scenario: ActivityScenario<*>) {
        scenario.onActivity { activity ->
            (activity as BaseActivity).onBackPressedDispatcher.onBackPressed()
        }
    }

    // ---------------------------------------------------------------------
    // Premise: predictive back really is active for this app on this device
    // ---------------------------------------------------------------------

    /**
     * The whole migration only matters if the platform has actually enabled
     * the ahead-of-time back dispatch for us. If this fails, every other test
     * in this class would be passing via the legacy key-event path.
     *
     * Registering a platform [OnBackInvokedCallback] and observing it
     * fire proves the ahead-of-time dispatch is live: when it is disabled, the
     * platform dispatcher never invokes registered callbacks and back arrives
     * as a `KEYCODE_BACK` key event instead.
     */
    @Test
    fun predictiveBackIsEnabledExactlyOnApi36AndAbove() {

        ActivityScenario.launch<HtmlViewActivity>(htmlIntent(mContext, HTML_NO_HISTORY)).use { scenario ->

            waitForIdle()

            val invoked = AtomicBoolean(false)
            val callbackRef = AtomicReference<OnBackInvokedCallback?>(null)

            scenario.onActivity { activity ->
                val callback = OnBackInvokedCallback { invoked.set(true) }
                callbackRef.set(callback)
                activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_OVERLAY,
                    callback
                )
            }

            waitForIdle()

            pressBackUnconditionally()
            waitForIdle()

            // This is exactly the condition BaseActivity.osHandlesBackAnimations()
            // tests, so this asserts that predicate is correct on this device.
            assertEquals(
                "Predictive back should be enabled precisely when the app "
                    + "targets API 36 and the device is API 36+",
                Build.VERSION.SDK_INT >= 36,
                invoked.get()
            )

            if (invoked.get()) {
                scenario.onActivity { activity ->
                    activity.onBackInvokedDispatcher
                        .unregisterOnBackInvokedCallback(callbackRef.get()!!)
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // PostListingActivity: "press back again to exit"
    // ---------------------------------------------------------------------

    @Test
    fun postListing_backAgainDisabled_doesNotIntercept() {

        setBackAgain(false)

        ActivityScenario.launch<PostListingActivity>(
            intentFor(mContext, PostListingActivity::class.java, TEST_SUBREDDIT_URL)
        ).use { scenario ->

            waitForIdle()

            // Nothing to intercept, so the system should own the back gesture
            // (and therefore animate it).
            assertIntercepts(
                "PostListingActivity should not intercept back when the "
                    + "'back again' pref is disabled",
                false,
                scenario
            )

            pressBackUnconditionally()
            waitForIdle()

            assertEquals(
                "A single back press should exit",
                Lifecycle.State.DESTROYED,
                awaitDestroyed(scenario)
            )
        }
    }

    @Test
    fun postListing_backAgainEnabled_requiresTwoPresses() {

        setBackAgain(true)

        ActivityScenario.launch<PostListingActivity>(
            intentFor(mContext, PostListingActivity::class.java, TEST_SUBREDDIT_URL)
        ).use { scenario ->

            waitForIdle()

            assertIntercepts(
                "PostListingActivity must intercept back to show the "
                    + "'press back again' toast",
                true,
                scenario
            )

            pressBackUnconditionally()
            waitForIdle()

            assertEquals(
                "The first back press should be consumed by the "
                    + "'press back again' prompt",
                Lifecycle.State.RESUMED,
                scenario.state
            )

            pressBackAfterGuardWindow()
            waitForIdle()

            assertEquals(
                "The second back press should exit",
                Lifecycle.State.DESTROYED,
                awaitDestroyed(scenario)
            )
        }
    }

    // ---------------------------------------------------------------------
    // The 300ms double-press guard, which only applies where the OS is not
    // providing predictive back animations
    // ---------------------------------------------------------------------

    /**
     * Below API 36 the guard swallows a second back press arriving within
     * 300ms of the first.
     */
    @Test
    fun postListing_rapidDoubleBack_isGuardedBelowApi36() {

        assumeTrue(
            "The double-press guard only applies where predictive back is "
                + "not in use",
            Build.VERSION.SDK_INT < 36
        )

        setBackAgain(true)

        ActivityScenario.launch<PostListingActivity>(
            intentFor(mContext, PostListingActivity::class.java, TEST_SUBREDDIT_URL)
        ).use { scenario ->

            waitForIdle()

            dispatchBack(scenario)
            waitForIdle()

            assertEquals(
                "The first back press should show the prompt",
                Lifecycle.State.RESUMED,
                scenario.state
            )

            // Immediately again, inside the 300ms guard window
            dispatchBack(scenario)
            waitForIdle()

            assertEquals(
                "A back press within 300ms should be swallowed by the guard",
                Lifecycle.State.RESUMED,
                scenario.state
            )

            // Once the guard window has passed, back should be honoured again
            SystemClock.sleep(500)

            dispatchBack(scenario)
            waitForIdle()

            assertEquals(
                "Back after the guard window should exit",
                Lifecycle.State.DESTROYED,
                awaitDestroyed(scenario)
            )
        }
    }

    /**
     * On API 36 the guard is deliberately skipped, as the predictive back
     * animation already protects against accidental presses.
     */
    @Test
    fun postListing_rapidDoubleBack_isNotGuardedOnApi36() {

        assumeTrue(
            "The double-press guard is skipped where predictive back is in use",
            Build.VERSION.SDK_INT >= 36
        )

        setBackAgain(true)

        ActivityScenario.launch<PostListingActivity>(
            intentFor(mContext, PostListingActivity::class.java, TEST_SUBREDDIT_URL)
        ).use { scenario ->

            waitForIdle()

            dispatchBack(scenario)
            waitForIdle()

            assertEquals(
                "The first back press should show the prompt",
                Lifecycle.State.RESUMED,
                scenario.state
            )

            // Immediately again: this must NOT be swallowed
            dispatchBack(scenario)
            waitForIdle()

            assertEquals(
                "A rapid second back press should exit on Android 16",
                Lifecycle.State.DESTROYED,
                awaitDestroyed(scenario)
            )
        }
    }

    /**
     * The prompt expires after 5 seconds, after which back should prompt again
     * rather than exiting.
     */
    @Test
    fun postListing_backAgainEnabled_promptIsNotConsumedByRapidPresses() {

        setBackAgain(true)

        ActivityScenario.launch<PostListingActivity>(
            intentFor(mContext, PostListingActivity::class.java, TEST_SUBREDDIT_URL)
        ).use { scenario ->

            waitForIdle()

            pressBackUnconditionally()
            waitForIdle()

            assertEquals(Lifecycle.State.RESUMED, scenario.state)

            // Let the 5 second window expire
            SystemClock.sleep(5500)

            pressBackUnconditionally()
            waitForIdle()

            assertEquals(
                "Back after the prompt expired should re-prompt, not exit",
                Lifecycle.State.RESUMED,
                scenario.state
            )
        }
    }

    // ---------------------------------------------------------------------
    // HtmlViewActivity / WebViewActivity: WebView history
    // ---------------------------------------------------------------------

    private fun htmlIntent(context: Context, html: String): Intent {
        val intent = Intent(context, HtmlViewActivity::class.java)
        intent.putExtra("html", html)
        intent.putExtra("title", "test")
        return intent
    }

    @Test
    fun htmlView_alwaysInterceptsBack() {

        ActivityScenario.launch<HtmlViewActivity>(htmlIntent(mContext, HTML_NO_HISTORY)).use { scenario ->

            waitForIdle()

            assertIntercepts(
                "The WebView activities must always intercept back, so they "
                    + "can navigate their history",
                true,
                scenario
            )
        }
    }

    /**
     * With no history to go back through, `onBackButtonPressed()` returns
     * false and the activity must still close. This exercises the re-dispatch
     * path in BaseActivity (disable the callback, dispatch again, fall through
     * to finishing the activity).
     */
    @Test
    fun htmlView_noHistory_backExits() {

        ActivityScenario.launch<HtmlViewActivity>(htmlIntent(mContext, HTML_NO_HISTORY)).use { scenario ->

            waitForIdle()

            pressBackUnconditionally()
            waitForIdle()

            assertEquals(
                "Back with no WebView history should exit",
                Lifecycle.State.DESTROYED,
                awaitDestroyed(scenario)
            )
        }
    }

    @Test
    fun htmlView_withHistory_backNavigatesHistoryThenExits() {

        ActivityScenario.launch<HtmlViewActivity>(htmlIntent(mContext, HTML_NO_HISTORY)).use { scenario ->

            waitForIdle()

            val webViewRef = AtomicReference<WebView?>(null)
            scenario.onActivity { activity -> webViewRef.set(findWebView(activity)) }
            assertNotNull("Could not find the WebView", webViewRef.get())

            // Let the initial page settle, then navigate to a second page so
            // that the WebView has history to go back through
            SystemClock.sleep(1000)

            scenario.onActivity { activity -> findWebView(activity)!!.loadDataWithBaseURL(
                "https://reddit.com/",
                "<html><body>second page</body></html>",
                "text/html; charset=utf-8",
                "UTF-8",
                null
            ) }

            assertNotNull(
                "WebView should have history to go back through",
                awaitCanGoBack(scenario)
            )

            pressBackUnconditionally()
            waitForIdle()

            assertEquals(
                "Back should navigate the WebView, not close the activity",
                Lifecycle.State.RESUMED,
                scenario.state
            )

            // The history entry should have been consumed
            val canStillGoBack = AtomicBoolean(true)
            scenario.onActivity { activity -> canStillGoBack.set(
                findWebView(activity)!!.canGoBack()
            ) }

            assertFalse(
                "The WebView should have navigated back",
                canStillGoBack.get()
            )

            pressBackAfterGuardWindow()
            waitForIdle()

            assertEquals(
                "Once history is exhausted, back should exit",
                Lifecycle.State.DESTROYED,
                awaitDestroyed(scenario)
            )
        }
    }

    /**
     * Polls until the WebView reports that it has history, returning it (or
     * null on timeout).
     */
    private fun awaitCanGoBack(scenario: ActivityScenario<*>): WebView? {

        val result = AtomicReference<WebView?>(null)

        repeat(100) {
            scenario.onActivity { activity ->
                val webView = findWebView(activity)
                if (webView != null && webView.canGoBack()) {
                    result.set(webView)
                }
            }

            if (result.get() != null) {
                return result.get()
            }

            SystemClock.sleep(100)
        }

        return null
    }

    private fun findWebView(activity: android.app.Activity): WebView? {
        return findWebView(activity.findViewById(android.R.id.content))
    }

    private fun findWebView(view: View): WebView? {

        if (view is WebView) {
            return view
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findWebView(view.getChildAt(i))?.let { return it }
            }
        }

        return null
    }

    /**
     * Matches only the first view satisfying the given matcher, so that a
     * listing full of posts does not produce an ambiguous match.
     */
    private fun firstMatching(matcher: Matcher<View>): Matcher<View> {

        return object : TypeSafeMatcher<View>() {

            private var found = false

            override fun describeTo(description: Description) {
                description.appendText("first view matching: ")
                matcher.describeTo(description)
            }

            override fun matchesSafely(view: View): Boolean {

                if (found || !matcher.matches(view)) {
                    return false
                }

                found = true
                return true
            }
        }
    }

    // ---------------------------------------------------------------------
    // Progress dialogs
    // ---------------------------------------------------------------------

    /**
     * The app's progress dialogs intercept back via
     * `setOnKeyListener(KEYCODE_BACK)`, which predictive back no longer
     * dispatches. They rely instead on their `OnCancelListener`, which
     * the platform triggers via `Dialog.onBackPressed() -> cancel()`.
     * This verifies that platform contract, which is what makes those dialogs
     * keep working unchanged on Android 16.
     */
    @Test
    fun progressDialog_backTriggersOnCancelListener() {

        ActivityScenario.launch<HtmlViewActivity>(htmlIntent(mContext, HTML_NO_HISTORY)).use { scenario ->

            waitForIdle()

            val cancelled = AtomicBoolean(false)
            val dialogRef = AtomicReference<Dialog?>(null)

            scenario.onActivity { activity ->
                @Suppress("DEPRECATION")
                val dialog
                    = ProgressDialog(activity)
                dialog.setTitle(R.string.comment_reply_submitting_title)
                dialog.setCancelable(true)
                dialog.setCanceledOnTouchOutside(false)
                dialog.setOnCancelListener { cancelled.set(true) }
                dialog.show()
                dialogRef.set(dialog)
            }

            waitForIdle()
            SystemClock.sleep(500)

            pressBackUnconditionally()
            waitForIdle()
            SystemClock.sleep(500)

            assertTrue(
                "Back should cancel the progress dialog (firing its "
                    + "OnCancelListener) on Android 16",
                cancelled.get()
            )

            assertFalse(
                "The dialog should no longer be showing",
                dialogRef.get()!!.isShowing
            )

            assertEquals(
                "Cancelling the dialog should not close the activity",
                Lifecycle.State.RESUMED,
                scenario.state
            )
        }
    }
}
