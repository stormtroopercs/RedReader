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
package org.quantumbadger.redreader.views

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.TooltipCompat
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.PostSubmitActivity
import org.quantumbadger.redreader.common.AndroidCommon
import org.quantumbadger.redreader.common.General.findViewById
import org.quantumbadger.redreader.common.General.getSharedPrefs
import org.quantumbadger.redreader.common.General.showMustBeLoggedInDialog
import org.quantumbadger.redreader.common.LinkHandler.shareText
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.SharedPrefsWrapper
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.reddit.SubredditDetails
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager.ListenerContext
import org.quantumbadger.redreader.reddit.api.RedditSubredditSubscriptionManager.SubredditSubscriptionStateChangeListener
import org.quantumbadger.redreader.reddit.api.SubredditSubscriptionState
import java.util.Objects

class SubredditToolbar @JvmOverloads constructor(
    private val mContext: Context,
    attrs: AttributeSet?=null,
    defStyleAttr: Int = 0
) : LinearLayout(mContext, attrs, defStyleAttr), SubredditSubscriptionStateChangeListener,
    SharedPrefsWrapper.OnSharedPreferenceChangeListener {
    // Field can't be local because the listener gets put in a weak map, and we want to stop it
    // being garbage collected.
    private var mSubscriptionListenerContext: ListenerContext?=null

    private var mRunnableOnAttach: Runnable?=null
    private var mRunnableOnDetach: Runnable?=null
    private var mRunnableOnSubscriptionsChange: Runnable?=null
    private var mRunnableOnPinnedChange: Runnable?=null

    private var mSubredditDetails: Optional<SubredditDetails?> =         Optional.Companion.empty<SubredditDetails?>()
    private var mUrl: Optional<UriString?> = Optional.Companion.empty<UriString?>()

    private var mButtonInfo: ImageButton?=null

    fun bindSubreddit(
        subreddit: SubredditDetails,
        url: Optional<UriString?>
    ) {
        mSubredditDetails = Optional.Companion.of<SubredditDetails?>(subreddit)
        mUrl = url

        if (subreddit.hasSidebar()) {
            mButtonInfo!!.setVisibility(VISIBLE)
        } else {
            mButtonInfo!!.setVisibility(GONE)
        }

        mRunnableOnSubscriptionsChange!!.run()
        mRunnableOnPinnedChange!!.run()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        val activity = mContext as AppCompatActivity

        val sharedPreferences = getSharedPrefs(mContext)

        val currentUser: RedditAccount =             RedditAccountManager.Companion.getInstance(mContext).getDefaultAccount()

        val buttonSubscribe = findViewById<ImageButton>(R.id.subreddit_toolbar_button_subscribe)
        val buttonUnsubscribe = findViewById<ImageButton>(R.id.subreddit_toolbar_button_unsubscribe)
        val buttonSubscribeLoading = findViewById<FrameLayout>(R.id.subreddit_toolbar_button_subscribe_loading)

        val buttonPin = findViewById<ImageButton>(R.id.subreddit_toolbar_button_pin)
        val buttonUnpin = findViewById<ImageButton>(R.id.subreddit_toolbar_button_unpin)

        val buttonSubmit = findViewById<ImageButton>(R.id.subreddit_toolbar_button_submit)
        val buttonShare = findViewById<ImageButton>(R.id.subreddit_toolbar_button_share)
        mButtonInfo = findViewById<ImageButton>(R.id.subreddit_toolbar_button_info)

        for (i in 0..<getChildCount()) {
            val button = getChildAt(i)
            TooltipCompat.setTooltipText(button, button.getContentDescription())
        }

        buttonSubscribeLoading.addView(ButtonLoadingSpinnerView(mContext))

        val subscriptionManager: RedditSubredditSubscriptionManager=            RedditSubredditSubscriptionManager.Companion.getSingleton(
                mContext,
                currentUser
            )

        mRunnableOnSubscriptionsChange = Runnable {
            val subscriptionState = subscriptionManager.getSubscriptionState(
                mSubredditDetails.get().id
            )
            if (subscriptionState == SubredditSubscriptionState.SUBSCRIBED) {
                buttonSubscribe.setVisibility(GONE)
                buttonUnsubscribe.setVisibility(VISIBLE)
                buttonSubscribeLoading.setVisibility(GONE)
            } else if (subscriptionState == SubredditSubscriptionState.NOT_SUBSCRIBED) {
                buttonSubscribe.setVisibility(VISIBLE)
                buttonUnsubscribe.setVisibility(GONE)
                buttonSubscribeLoading.setVisibility(GONE)
            } else {
                buttonSubscribe.setVisibility(GONE)
                buttonUnsubscribe.setVisibility(GONE)
                buttonSubscribeLoading.setVisibility(VISIBLE)
            }
        }

        mRunnableOnPinnedChange = Runnable {
            val pinned = PrefsUtility.pref_pinned_subreddits_check(
                mSubredditDetails.get().id
            )
            if (pinned) {
                buttonPin.setVisibility(GONE)
                buttonUnpin.setVisibility(VISIBLE)
            } else {
                buttonPin.setVisibility(VISIBLE)
                buttonUnpin.setVisibility(GONE)
            }
        }

        mRunnableOnAttach = Runnable {
            mSubscriptionListenerContext = subscriptionManager.addListener(this)
            sharedPreferences.registerOnSharedPreferenceChangeListener(this)

            mRunnableOnSubscriptionsChange!!.run()
            mRunnableOnPinnedChange!!.run()
        }

        mRunnableOnDetach = Runnable {
            mSubscriptionListenerContext!!.removeListener()
            mSubscriptionListenerContext = null
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        if (currentUser.isAnonymous) {
            val mustBeLoggedInListener =                 OnClickListener { v: View? -> showMustBeLoggedInDialog(activity) }

            buttonSubscribe.setOnClickListener(mustBeLoggedInListener)
            buttonUnsubscribe.setOnClickListener(mustBeLoggedInListener)
            buttonSubmit.setOnClickListener(mustBeLoggedInListener)
        } else {
            buttonSubscribe.setOnClickListener(OnClickListener { v: View? ->
                subscriptionManager.subscribe(
                    mSubredditDetails.get().id,
                    activity
                )
            })

            buttonUnsubscribe.setOnClickListener(OnClickListener { v: View? ->
                subscriptionManager.unsubscribe(
                    mSubredditDetails.get().id,
                    activity
                )
            })

            buttonSubmit.setOnClickListener(OnClickListener { v: View? ->
                val intent = Intent(
                    activity,
                    PostSubmitActivity::class.java
                )
                intent.putExtra("subreddit", mSubredditDetails.get().id.toString())
                activity.startActivity(intent)
            })
        }

        buttonPin.setOnClickListener(OnClickListener { v: View? ->
            PrefsUtility.pref_pinned_subreddits_add(
                mContext,
                mSubredditDetails.get().id
            )
        })

        buttonUnpin.setOnClickListener(OnClickListener { v: View? ->
            PrefsUtility.pref_pinned_subreddits_remove(
                mContext,
                mSubredditDetails.get().id
            )
        })

        buttonShare.setOnClickListener(OnClickListener { v: View? ->
            shareText(
                activity,
                mSubredditDetails.get().id.toString(),
                mUrl.orElse(mSubredditDetails.get().url).value
            )
        })

        mButtonInfo!!.setOnClickListener(
            OnClickListener { v: View? -> mSubredditDetails.get().showSidebarActivity(activity) })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (mRunnableOnAttach != null) {
            mRunnableOnAttach!!.run()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        if (mRunnableOnDetach != null) {
            mRunnableOnDetach!!.run()
        }
    }

    override fun onSubredditSubscriptionListUpdated(
        subredditSubscriptionManager: RedditSubredditSubscriptionManager?
    ) {
        if (mRunnableOnSubscriptionsChange != null) {
            AndroidCommon.UI_THREAD_HANDLER.post(mRunnableOnSubscriptionsChange!!)
        }
    }

    override fun onSubredditSubscriptionAttempted(
        subredditSubscriptionManager: RedditSubredditSubscriptionManager?
    ) {
        if (mRunnableOnSubscriptionsChange != null) {
            AndroidCommon.UI_THREAD_HANDLER.post(mRunnableOnSubscriptionsChange!!)
        }
    }

    override fun onSubredditUnsubscriptionAttempted(
        subredditSubscriptionManager: RedditSubredditSubscriptionManager?
    ) {
        if (mRunnableOnSubscriptionsChange != null) {
            AndroidCommon.UI_THREAD_HANDLER.post(mRunnableOnSubscriptionsChange!!)
        }
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPrefsWrapper,
        key: String
    ) {
        if (mRunnableOnPinnedChange != null
            && key == mContext.getString(string.pref_pinned_subreddits_key)
        ) {
            mRunnableOnPinnedChange!!.run()
        }
    }
}
