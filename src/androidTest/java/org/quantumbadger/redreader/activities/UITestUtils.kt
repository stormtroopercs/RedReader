package org.quantumbadger.redreader.activities

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import android.view.View
import org.hamcrest.Matcher
import org.hamcrest.core.IsAnything
import org.quantumbadger.redreader.R
import org.hamcrest.Matchers.allOf

object UITestUtils {

    fun handleFirstRunDialog() {

        try {
            onView(allOf(
                withId(R.id.terms_button_decline)
            )).perform(click())
        } catch (e: NoMatchingViewException) {
            // Ignore, the first run dialog has already been shown
            return
        }

        onView(allOf(
            withId(android.R.id.button1),
            withText("Log in now")
        )).perform(click())

        onView(allOf(
            withId(android.R.id.button3),
            withText("Close")
        )).perform(click())
    }

    fun waitForSeconds(seconds: Long): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            return IsAnything()
        }

        override fun getDescription(): String {
            return "Wait for $seconds seconds."
        }

        override fun perform(uiController: UiController, view: View) {
            uiController.loopMainThreadForAtLeast(seconds * 1000)
        }
    }
}
