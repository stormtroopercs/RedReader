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

import android.text.SpannableStringBuilder
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.account.RedditAccount
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.AndroidCommon.runOnUiThread
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.common.General.setLayoutMatchWidthWrapHeight
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.PrefsUtility.CommentAction
import org.quantumbadger.redreader.common.PrefsUtility.CommentFlingAction
import org.quantumbadger.redreader.common.RRThemeAttributes
import org.quantumbadger.redreader.fragments.CommentListingFragment
import org.quantumbadger.redreader.reddit.RedditCommentListItem
import org.quantumbadger.redreader.reddit.api.RedditAPICommentAction
import org.quantumbadger.redreader.reddit.api.RedditAPICommentAction.RedditCommentAction
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager
import java.util.Observable
import java.util.Observer
import org.quantumbadger.redreader.common.General

class RedditCommentView(
    context: BaseActivity,
    themeAttributes: RRThemeAttributes,
    listener: CommentListener,
    fragment: CommentListingFragment?
) : FlingableItemView(context), RedditChangeDataManager.Listener {
    private val mAccessibilityActionManager: AccessibilityActionManager

    var comment: RedditCommentListItem?=null
        private set

    private val mActivity: BaseActivity
    private val mChangeDataManager: RedditChangeDataManager
    private val mTheme: RRThemeAttributes

    private val mHeader: TextView
    private val mBodyHolder: FrameLayout

    private val mIndentView: IndentView
    private val mIndentedContent: LinearLayout

    private val mBodyFontScale: Float

    private val mShowLinkButtons: Boolean

    private var mHeaderText: CharSequence?=null

    private val mListener: CommentListener

    private val mFragment: CommentListingFragment?

    private var mLeftFlingAction: ActionDescriptionPair?=null
    private var mRightFlingAction: ActionDescriptionPair?=null

    override fun onSetItemFlingPosition(position: Float) {
        mIndentedContent.setTranslationX(position)
    }

    private class ActionDescriptionPair(
        val action: RedditCommentAction,
        val descriptionRes: Int
    )

    private fun chooseFlingAction(pref: CommentFlingAction): ActionDescriptionPair? {
        if (!comment!!.isComment) {
            return null
        }

        val comment = comment!!.asComment().parsedComment

        when (pref) {
            CommentFlingAction.UPVOTE -> if (mChangeDataManager.isUpvoted(comment.idAndType)) {
                return ActionDescriptionPair(
                    RedditCommentAction.UNVOTE,
                    string.action_vote_remove
                )
            } else {
                return ActionDescriptionPair(
                    RedditCommentAction.UPVOTE,
                    string.action_upvote
                )
            }

            CommentFlingAction.DOWNVOTE -> if (mChangeDataManager.isDownvoted(comment.idAndType)) {
                return ActionDescriptionPair(
                    RedditCommentAction.UNVOTE,
                    string.action_vote_remove
                )
            } else {
                return ActionDescriptionPair(
                    RedditCommentAction.DOWNVOTE,
                    string.action_downvote
                )
            }

            CommentFlingAction.SAVE -> if (mChangeDataManager.isSaved(comment.idAndType)) {
                return ActionDescriptionPair(
                    RedditCommentAction.UNSAVE,
                    string.action_unsave
                )
            } else {
                return ActionDescriptionPair(
                    RedditCommentAction.SAVE,
                    string.action_save
                )
            }

            CommentFlingAction.REPORT -> return ActionDescriptionPair(
                RedditCommentAction.REPORT,
                string.action_report
            )

            CommentFlingAction.REPLY -> return ActionDescriptionPair(
                RedditCommentAction.REPLY,
                string.action_reply
            )

            CommentFlingAction.CONTEXT -> return ActionDescriptionPair(
                RedditCommentAction.CONTEXT,
                string.action_comment_context
            )

            CommentFlingAction.GO_TO_COMMENT -> return ActionDescriptionPair(
                RedditCommentAction.GO_TO_COMMENT,
                string.action_comment_go_to
            )

            CommentFlingAction.COMMENT_LINKS -> return ActionDescriptionPair(
                RedditCommentAction.COMMENT_LINKS,
                string.action_comment_links
            )

            CommentFlingAction.SHARE -> return ActionDescriptionPair(
                RedditCommentAction.SHARE,
                string.action_share
            )

            CommentFlingAction.COPY_TEXT -> return ActionDescriptionPair(
                RedditCommentAction.COPY_TEXT,
                string.action_copy_text
            )

            CommentFlingAction.COPY_URL -> return ActionDescriptionPair(
                RedditCommentAction.COPY_URL,
                string.action_copy_link
            )

            CommentFlingAction.USER_PROFILE -> return ActionDescriptionPair(
                RedditCommentAction.USER_PROFILE,
                string.action_user_profile
            )

            CommentFlingAction.COLLAPSE -> {
                if (mFragment == null) {
                    return null
                }

                return ActionDescriptionPair(
                    RedditCommentAction.COLLAPSE,
                    string.action_collapse
                )
            }

            CommentFlingAction.ACTION_MENU -> {
                if (mFragment == null) {
                    return null
                }

                return ActionDescriptionPair(
                    RedditCommentAction.ACTION_MENU,
                    string.action_actionmenu_short
                )
            }

            CommentFlingAction.PROPERTIES -> return ActionDescriptionPair(
                RedditCommentAction.PROPERTIES,
                string.action_properties
            )

            CommentFlingAction.BACK -> return ActionDescriptionPair(
                RedditCommentAction.BACK,
                string.action_back
            )

            CommentFlingAction.DISABLED -> return null
        }

        return null
    }

    override val flingLeftText: String
        get() {
        val context = getContext()

        val pref =             PrefsUtility.pref_behaviour_fling_comment_left()

        mLeftFlingAction = chooseFlingAction(pref)

        if (mLeftFlingAction == null) {
            return "Disabled"
        }

        return context.getString(mLeftFlingAction!!.descriptionRes)
        }

    override val flingRightText: String
        get() {
        val context = getContext()

        val pref =             PrefsUtility.pref_behaviour_fling_comment_right()

        mRightFlingAction = chooseFlingAction(pref)

        if (mRightFlingAction == null) {
            return "Disabled"
        }

        return context.getString(mRightFlingAction!!.descriptionRes)
        }

    override fun allowFlingingLeft(): Boolean {
        return mLeftFlingAction != null
    }

    override fun allowFlingingRight(): Boolean {
        return mRightFlingAction != null
    }

    override fun onFlungLeft() {
        if (mLeftFlingAction == null || !comment!!.isComment) {
            return
        }

        RedditAPICommentAction.onActionMenuItemSelected(
            comment!!.asComment(),
            this,
            mActivity,
            mFragment,
            mLeftFlingAction!!.action,
            mChangeDataManager
        )
    }

    override fun onFlungRight() {
        if (mRightFlingAction == null || !comment!!.isComment) {
            return
        }

        RedditAPICommentAction.onActionMenuItemSelected(
            comment!!.asComment(),
            this,
            mActivity,
            mFragment,
            mRightFlingAction!!.action,
            mChangeDataManager
        )
    }

    interface CommentListener {
        fun onCommentClicked(view : RedditCommentView)

        fun onCommentLongClicked(view : RedditCommentView)
    }

    init {
        mAccessibilityActionManager = AccessibilityActionManager(
            this,
            context.getResources()
        )

        mActivity = context
        mTheme = themeAttributes
        mListener = listener
        mFragment = fragment

        mChangeDataManager = RedditChangeDataManager.Companion.getInstance(
            RedditAccountManager.Companion.getInstance(context).getDefaultAccount()
        )

        val rootView =             LayoutInflater.from(context).inflate(R.layout.reddit_comment, this, true)

        mIndentView = rootView.findViewById<IndentView>(R.id.view_reddit_comment_indentview)
        mHeader = rootView.findViewById<TextView>(R.id.view_reddit_comment_header)
        mBodyHolder = rootView.findViewById<FrameLayout>(R.id.view_reddit_comment_bodyholder)
        mIndentedContent =             rootView.findViewById<LinearLayout>(R.id.view_reddit_comment_indented_content)

        val minimumCommentHeight = PrefsUtility.pref_accessibility_min_comment_height()

        mIndentedContent.setMinimumHeight(dpToPixels(context, minimumCommentHeight.toFloat()))

        mBodyFontScale = PrefsUtility.appearance_fontscale_bodytext()
        val mHeaderFontScale = PrefsUtility.appearance_fontscale_comment_headers()

        mHeader.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            mHeader.getTextSize() * mHeaderFontScale
        )

        mShowLinkButtons = PrefsUtility.pref_appearance_linkbuttons()

        setOnClickListener(OnClickListener { view: View? -> mListener.onCommentClicked(this) })

        setOnLongClickListener(OnLongClickListener { v: View? ->
            mListener.onCommentLongClicked(this)
            true
        })
    }

    override fun onRedditDataChange(thingIdAndType: RedditIdAndType?) {
        reset(mActivity, this.comment!!, true)
    }

    @JvmOverloads
    fun reset(
        activity: BaseActivity,
        comment: RedditCommentListItem,
        updateOnly: Boolean = false
    ) {
        if (!updateOnly) {
            if (!comment.isComment) {
                throw RuntimeException("Not a comment")
            }

            if (this.comment !== comment) {
                if (this.comment != null) {
                    mChangeDataManager.removeListener(comment.asComment().idAndType, this)
                }

                mChangeDataManager.addListener(comment.asComment().idAndType, this)
            }

            this.comment = comment

            resetSwipeState()
        }

        mIndentView.setIndentation(comment.indent)

        val hideLinkButtons = comment.asComment()
            .parsedComment
            .rawComment.author!!.decoded.equals(
                "autowikibot", ignoreCase = true
            )

        mBodyHolder.removeAllViews()
        val commentBody = comment.asComment().getBody(
            activity,
            mTheme.rrCommentBodyCol,
            13.0f * mBodyFontScale,
            mShowLinkButtons && !hideLinkButtons
        )

        mBodyHolder.addView(commentBody)
        setLayoutMatchWidthWrapHeight(commentBody)

        (commentBody.getLayoutParams() as MarginLayoutParams).topMargin =             dpToPixels(activity, 1f)

        val renderableComment = comment.asComment()

        val ageUnits = PrefsUtility.appearance_comment_age_units()

        val post = mFragment?.post
        val postTimestamp = if (post != null)
            post.src.createdTimeUTC
        else
            null

        val parentCommentTimestamp = if (comment.parent != null)
            comment.parent.asComment().parsedComment.rawComment
                .created_utc.value
        else
            null

        val isCollapsed = comment.isCollapsed(mChangeDataManager)

        val header = renderableComment.getHeader(
            mTheme,
            mChangeDataManager,
            activity,
            ageUnits,
            postTimestamp,
            parentCommentTimestamp
        )

        val observer = Observer { observable: Observable?, o: Any? ->
            if (isCollapsed) {
                mHeaderText = "[ + ]  " + o
            } else {
                mHeaderText = o as SpannableStringBuilder?
            }
            runOnUiThread(Runnable { mHeader.setText(mHeaderText) })
        }

        header.addObserver(observer)

        mHeaderText = header.get()

        mHeader.setContentDescription(
            renderableComment.getAccessibilityHeader(
                mTheme,
                mChangeDataManager,
                activity,
                ageUnits,
                postTimestamp,
                parentCommentTimestamp,
                isCollapsed,
                Optional.Companion.of<Int>(comment.indent)
            )
        )

        if (isCollapsed) {
            setFlingingEnabled(false)
            mHeader.setText(
                "[ + ]  "
                        + mHeaderText
            ) // Note that this removes formatting (which is fine)
            mBodyHolder.setVisibility(GONE)
        } else {
            setFlingingEnabled(true)
            mHeader.setText(mHeaderText)
            mBodyHolder.setVisibility(VISIBLE)
        }

        setupAccessibilityActions()
    }

    private fun setupAccessibilityActions() {
        val defaultAccount: RedditAccount=            RedditAccountManager.Companion.getInstance(mActivity).getDefaultAccount()
        val isAuthenticated = defaultAccount.isNotAnonymous

        mAccessibilityActionManager.removeAllActions()

        if (!comment!!.isComment) {
            return
        }

        addAccessibilityActionFromDescriptionPair(
            chooseFlingAction(CommentFlingAction.COLLAPSE)
        )

        mAccessibilityActionManager.addAction(string.button_next_comment_parent, Runnable {
            mFragment!!.onNextParent()
        })

        mAccessibilityActionManager.addAction(string.button_prev_comment_parent, Runnable {
            mFragment!!.onPreviousParent()
        })

        if (isAuthenticated) {
            addAccessibilityActionFromDescriptionPair(
                chooseFlingAction(CommentFlingAction.REPLY)
            )
        }

        // TODO null
        if (comment!!.asComment().parsedComment.rawComment
                .author!!.decoded.equals(defaultAccount.username, ignoreCase = true)
        ) {
            addAccessibilityActionFromDescriptionPair(
                ActionDescriptionPair(
                    RedditCommentAction.EDIT,
                    string.action_edit
                )
            )

            addAccessibilityActionFromDescriptionPair(
                ActionDescriptionPair(
                    RedditCommentAction.DELETE,
                    string.action_delete
                )
            )
        }

        // #136: When "save" is implemented for comments, add an a11y action
        // here (behind an isAuthenticated guard).
        addAccessibilityActionFromDescriptionPair(
            chooseFlingAction(CommentFlingAction.USER_PROFILE)
        )

        if (isAuthenticated) {
            addAccessibilityActionFromDescriptionPair(
                chooseFlingAction(CommentFlingAction.REPORT)
            )
        }

        addAccessibilityActionFromDescriptionPair(
            chooseFlingAction(CommentFlingAction.SHARE)
        )

        if (isAuthenticated) {
            addAccessibilityActionFromDescriptionPair(
                chooseFlingAction(CommentFlingAction.DOWNVOTE)
            )

            addAccessibilityActionFromDescriptionPair(
                chooseFlingAction(CommentFlingAction.UPVOTE)
            )
        }

        mAccessibilityActionManager.setClickHint(
            getAccessibilityHintForActionPref(PrefsUtility.pref_behaviour_actions_comment_tap())
        )

        mAccessibilityActionManager.setLongClickHint(
            getAccessibilityHintForActionPref(
                PrefsUtility.pref_behaviour_actions_comment_longclick()
            )
        )
    }

    @StringRes
    private fun getAccessibilityHintForActionPref(
        pref: CommentAction
    ): Int? {
        when (pref) {
            CommentAction.COLLAPSE -> return string.action_collapse
            CommentAction.ACTION_MENU -> return string.action_actionmenu
            CommentAction.NOTHING -> return null
        }
    }

    private fun addAccessibilityActionFromDescriptionPair(
        pair: ActionDescriptionPair?
    ) {
        if (pair == null) {
            return
        }

        mAccessibilityActionManager.addAction(pair.descriptionRes, Runnable {
            RedditAPICommentAction.onActionMenuItemSelected(
                comment!!.asComment(),
                this,
                mActivity,
                mFragment,
                pair.action,
                mChangeDataManager
            )
        })
    }
}