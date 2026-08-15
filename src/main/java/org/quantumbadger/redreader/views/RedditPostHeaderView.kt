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

import android.graphics.Color
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.TooltipCompat
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.Fonts
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.reddit.api.RedditPostActions
import org.quantumbadger.redreader.reddit.api.RedditPostActions.setupAccessibilityActions
import org.quantumbadger.redreader.reddit.api.RedditPostActions.showActionMenu
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager
import org.quantumbadger.redreader.reddit.prepared.RedditPreparedPost

class RedditPostHeaderView(
    activity: BaseActivity,
    post: RedditPreparedPost
) : LinearLayout(activity) {
    private val subtitle: TextView

    private val mChangeListenerAddTask: Runnable?
    private val mChangeListenerRemoveTask: Runnable?

    init {
        val dpScale = activity.getResources().getDisplayMetrics().density

        setOrientation(VERTICAL)

        val greyHeader = LinearLayout(activity)

        setupAccessibilityActions(
            AccessibilityActionManager(
                greyHeader,
                activity.getResources()
            ),
            post,
            activity,
            true
        )

        greyHeader.setOrientation(VERTICAL)

        val sidesPadding = (15.0f * dpScale).toInt()
        val topPadding = (10.0f * dpScale).toInt()

        greyHeader.setPadding(sidesPadding, topPadding, sidesPadding, topPadding)

        val titleFontScale = PrefsUtility.appearance_fontscale_post_header_titles()

        val title = TextView(activity)
        title.setTextSize(19.0f * titleFontScale)
        title.setTypeface(Fonts.robotoLightOrAlternative)
        title.setText(post.src.title)
        title.setContentDescription(post.buildAccessibilityTitle(activity, true))
        title.setTextColor(Color.WHITE)
        greyHeader.addView(title)

        val subtitleFontScale =             PrefsUtility.appearance_fontscale_post_header_subtitles()

        subtitle = TextView(activity)
        subtitle.setTextSize(13.0f * subtitleFontScale)
        subtitle.setText(post.buildSubtitle(activity, true))
        subtitle.setContentDescription(post.buildAccessibilitySubtitle(activity, true))

        subtitle.setTextColor(Color.rgb(200, 200, 200))
        greyHeader.addView(subtitle)

        run {
            val appearance =                 activity.obtainStyledAttributes(intArrayOf(R.attr.rrPostListHeaderBackgroundCol))
            greyHeader.setBackgroundColor(appearance.getColor(0, General.COLOR_INVALID))
            appearance.recycle()
        }

        greyHeader.setOnClickListener(OnClickListener { v: View? ->
            if (!post.isSelf) {
                onLinkClicked(
                    activity,
                    post.src.url,
                    false,
                    post.src.src
                )
            }
        })

        greyHeader.setOnLongClickListener(OnLongClickListener { v: View? ->
            showActionMenu(activity, post)
            true
        })

        addView(greyHeader)

        val currentUser: RedditAccount =             RedditAccountManager.Companion.getInstance(activity).getDefaultAccount()

        if (!currentUser.isAnonymous) {
            // A user is logged in

            val changeDataManager: RedditChangeDataManager=                RedditChangeDataManager.Companion.getInstance(currentUser)
            val changeListener: RedditChangeDataManager.Listener

            if (!PrefsUtility.pref_appearance_hide_headertoolbar_commentlist()) {
                val buttons =                     inflate(activity, R.layout.post_header_toolbar, this)
                        .findViewById<LinearLayout>(R.id.post_toolbar_layout)

                for (i in 0..<buttons.getChildCount()) {
                    val button = buttons.getChildAt(i) as ImageButton
                    TooltipCompat.setTooltipText(button, button.getContentDescription())
                }

                val buttonAddUpvote =                     buttons.findViewById<ImageButton>(R.id.post_toolbar_botton_add_upvote)
                val buttonRemoveUpvote =                     buttons.findViewById<ImageButton>(R.id.post_toolbar_botton_remove_upvote)
                val buttonAddDownvote =                     buttons.findViewById<ImageButton>(R.id.post_toolbar_botton_add_downvote)
                val buttonRemoveDownvote =                     buttons.findViewById<ImageButton>(R.id.post_toolbar_botton_remove_downvote)
                val buttonReply =                     buttons.findViewById<ImageButton>(R.id.post_toolbar_botton_reply)
                val buttonShare =                     buttons.findViewById<ImageButton>(R.id.post_toolbar_botton_share)
                val buttonMore =                     buttons.findViewById<ImageButton>(R.id.post_toolbar_botton_more)

                buttonAddUpvote.setOnClickListener(OnClickListener { v: View? ->
                    post.performAction(
                        activity,
                        RedditPostActions.Action.UPVOTE
                    )
                })
                buttonRemoveUpvote.setOnClickListener(OnClickListener { v: View? ->
                    post.performAction(
                        activity,
                        RedditPostActions.Action.UNVOTE
                    )
                })
                buttonAddDownvote.setOnClickListener(OnClickListener { v: View? ->
                    post.performAction(
                        activity,
                        RedditPostActions.Action.DOWNVOTE
                    )
                })
                buttonRemoveDownvote.setOnClickListener(OnClickListener { v: View? ->
                    post.performAction(
                        activity,
                        RedditPostActions.Action.UNVOTE
                    )
                })
                buttonReply.setOnClickListener(OnClickListener { v: View? ->
                    post.performAction(
                        activity,
                        RedditPostActions.Action.REPLY
                    )
                })
                buttonShare.setOnClickListener(OnClickListener { v: View? ->
                    post.performAction(
                        activity,
                        RedditPostActions.Action.SHARE
                    )
                })
                buttonMore.setOnClickListener(OnClickListener { v: View? ->
                    post.performAction(
                        activity,
                        RedditPostActions.Action.ACTION_MENU
                    )
                })

                changeListener =                     RedditChangeDataManager.Listener { thingIdAndType: RedditIdAndType? ->
                        subtitle.setText(post.buildSubtitle(activity, true))
                        subtitle.setContentDescription(
                            post.buildAccessibilitySubtitle(activity, true)
                        )

                        val isUpvoted = changeDataManager.isUpvoted(
                            post.src.getIdAndType()
                        )

                        val isDownvoted = changeDataManager.isDownvoted(
                            post.src.getIdAndType()
                        )
                        if (isUpvoted) {
                            buttonAddUpvote.setVisibility(GONE)
                            buttonRemoveUpvote.setVisibility(VISIBLE)
                            buttonAddDownvote.setVisibility(VISIBLE)
                            buttonRemoveDownvote.setVisibility(GONE)
                        } else if (isDownvoted) {
                            buttonAddUpvote.setVisibility(VISIBLE)
                            buttonRemoveUpvote.setVisibility(GONE)
                            buttonAddDownvote.setVisibility(GONE)
                            buttonRemoveDownvote.setVisibility(VISIBLE)
                        } else {
                            buttonAddUpvote.setVisibility(VISIBLE)
                            buttonRemoveUpvote.setVisibility(GONE)
                            buttonAddDownvote.setVisibility(VISIBLE)
                            buttonRemoveDownvote.setVisibility(GONE)
                        }
                    }
            } else {
                changeListener =                     RedditChangeDataManager.Listener { thingIdAndType: RedditIdAndType? ->
                        subtitle.setText(post.buildSubtitle(activity, true))
                        subtitle.setContentDescription(
                            post.buildAccessibilitySubtitle(activity, true)
                        )
                    }
            }

            mChangeListenerAddTask = Runnable {
                changeDataManager.addListener(post.src.getIdAndType(), changeListener)
                changeListener.onRedditDataChange(post.src.getIdAndType())
            }

            mChangeListenerRemoveTask = Runnable {
                changeDataManager.removeListener(
                    post.src.getIdAndType(),
                    changeListener
                )
            }
        } else {
            mChangeListenerAddTask = null
            mChangeListenerRemoveTask = null
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        if (mChangeListenerAddTask != null) {
            mChangeListenerAddTask.run()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        if (mChangeListenerRemoveTask != null) {
            mChangeListenerRemoveTask.run()
        }
    }
}
