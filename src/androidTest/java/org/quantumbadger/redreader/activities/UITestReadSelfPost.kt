package org.quantumbadger.redreader.activities

import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import android.widget.FrameLayout
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeTextIntoFocusedView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isFocused
import androidx.test.espresso.matcher.ViewMatchers.withChild
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsInstanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.quantumbadger.redreader.R
import org.hamcrest.Matchers.allOf

@LargeTest
@RunWith(AndroidJUnit4::class)
class UITestReadSelfPost {

    @get:Rule
    var mActivityTestRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun uITestReadSelfPost() {

        UITestUtils.handleFirstRunDialog()

        val mainMenu = onView(
            allOf(withId(R.id.scrollbar_recyclerview_recyclerview),
                childAtPosition(
                    withId(R.id.scrollbar_recyclerview_refreshlayout),
                    0
                ))
        )
        mainMenu.perform(actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(2, click()))

        val customSubredditTextBox
            = onView(allOf(withId(R.id.dialog_mainmenu_custom_value), isDisplayed()))

        customSubredditTextBox.perform(click())

        onView(allOf(
            withId(R.id.dialog_mainmenu_custom_value),
            isDisplayed(),
            isFocused()
        )).perform(typeTextIntoFocusedView("redreader_public_test"))

        onView(allOf(withId(android.R.id.button1), withText("Go")))
            .perform(click())

        val selfTextPost = onView(allOf(
            withId(R.id.reddit_post_layout_outer),
            withChild(allOf(
                withId(R.id.reddit_post_textLayout),
                withChild(allOf(
                    withId(R.id.reddit_post_title),
                    withText("Self text test post")
                ))
            )),
            isDisplayed()
        ))

        selfTextPost.perform(click())

        val textView = onView(
            allOf(withText("This is the self text"),
                childAtPosition(
                    childAtPosition(
                        IsInstanceOf.instanceOf(FrameLayout::class.java),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        textView.check(matches(isDisplayed()))

        val textView2 = onView(
            allOf(withId(R.id.empty_view_text), withText("No comments yet."),
                childAtPosition(
                    childAtPosition(
                        IsInstanceOf.instanceOf(FrameLayout::class.java),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        textView2.check(matches(isDisplayed()))

        val imageButton = onView(
            allOf(withContentDescription("Previous Parent Comment"),
                childAtPosition(
                    childAtPosition(
                        IsInstanceOf.instanceOf(FrameLayout::class.java),
                        0
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        imageButton.check(matches(isDisplayed()))

        val imageButton2 = onView(
            allOf(withContentDescription("Next Parent Comment"),
                childAtPosition(
                    childAtPosition(
                        IsInstanceOf.instanceOf(FrameLayout::class.java),
                        0
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        imageButton2.check(matches(isDisplayed()))

        val textView3 = onView(
            allOf(withContentDescription("Sort Comments"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.rr_actionbar_toolbar),
                        1
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        textView3.check(matches(isDisplayed()))

        val textView4 = onView(
            allOf(withContentDescription("Refresh Comments"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.rr_actionbar_toolbar),
                        1
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        textView4.check(matches(isDisplayed()))

        val imageView = onView(
            allOf(withId(R.id.actionbar_title_back_image),
                childAtPosition(
                    allOf(withId(R.id.actionbar_title_outer),
                        childAtPosition(
                            withId(R.id.rr_actionbar_toolbar),
                            0
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        imageView.check(matches(isDisplayed()))

        val textView5 = onView(
            allOf(withId(R.id.actionbar_title_text), withText("Self text test post"),
                childAtPosition(
                    allOf(withId(R.id.actionbar_title_outer),
                        childAtPosition(
                            withId(R.id.rr_actionbar_toolbar),
                            0
                        )
                    ),
                    1
                ),
                isDisplayed()
            )
        )
        textView5.check(matches(isDisplayed()))
    }

    private fun childAtPosition(parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
