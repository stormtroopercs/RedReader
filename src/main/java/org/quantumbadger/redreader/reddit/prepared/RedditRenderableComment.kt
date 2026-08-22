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
package org.quantumbadger.redreader.reddit.prepared

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.view.View
import androidx.annotation.StringRes
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.BetterSSB
import org.quantumbadger.redreader.common.Constants.Reddit
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.PrefsUtility.AppearanceCommentHeaderItem
import org.quantumbadger.redreader.common.PrefsUtility.BehaviourCollapseStickyComments
import org.quantumbadger.redreader.common.PrefsUtility.CommentAgeMode
import org.quantumbadger.redreader.common.RRThemeAttributes
import org.quantumbadger.redreader.common.ScreenreaderPronunciation
import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.common.time.TimeFormatHelper.format
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import org.quantumbadger.redreader.reddit.things.RedditThingWithIdAndType
import java.util.Observable
import java.util.Observer
import kotlin.Any
import kotlin.Float
import kotlin.IllegalStateException
import kotlin.Int
import kotlin.String
import kotlin.or
import kotlin.toString

class RedditRenderableComment
    (
    val parsedComment: RedditParsedComment,
    private val mParentPostAuthor: String?,
    private val mMinimumCommentScore: Int?,
    private val mCurrentCanonicalUserName: String?,
    private val mShowScore: Boolean,
    private val mShowSubreddit: Boolean,
    private val mNeverAutoCollapse: Boolean
) : RedditRenderableCommentListItem, RedditThingWithIdAndType {
    private var isBlockedUser = false

    fun setBlockedUser(blocked: Boolean) {
        isBlockedUser = blocked
    }

    private fun computeScore(changeDataManager: RedditChangeDataManager): Int {
        val rawComment = parsedComment.rawComment

        var score = rawComment.ups - rawComment.downs

        if (true == rawComment.likes) {
            score--
        }
        if (false == rawComment.likes) {
            score++
        }

        if (changeDataManager.isUpvoted(idAndType)) {
            score++
        } else if (changeDataManager.isDownvoted(idAndType)) {
            score--
        }

        return score
    }

    override fun getHeader(
        theme: RRThemeAttributes,
        changeDataManager: RedditChangeDataManager,
        context: Context,
        commentAgeUnits: Int,
        postCreated: TimestampUTC?,
        parentCommentCreated: TimestampUTC?
    ): BetterSSB {
        val commentAgeMode = PrefsUtility.appearance_comment_age_mode()

        val sb = BetterSSB()

        val rawComment = parsedComment.rawComment

        val pointsCol: Int
        val score = computeScore(changeDataManager)

        if (changeDataManager.isUpvoted(idAndType)) {
            pointsCol = theme.rrPostSubtitleUpvoteCol
        } else if (changeDataManager.isDownvoted(idAndType)) {
            pointsCol = theme.rrPostSubtitleDownvoteCol
        } else {
            pointsCol = theme.rrCommentHeaderBoldCol
        }

        if (theme.shouldShow(AppearanceCommentHeaderItem.AUTHOR)) {
            var setBackgroundColour = false
            var backgroundColour = 0 // TODO color from theme

            if (rawComment.author!!.decoded.equals(mParentPostAuthor, ignoreCase = true)
                && rawComment.author.decoded != "[deleted]"
            ) {
                setBackgroundColour = true
                backgroundColour = Color.rgb(0, 126, 168)
            } else if ("moderator" == rawComment.distinguished) {
                setBackgroundColour = true
                backgroundColour = Color.rgb(0, 170, 0)
            } else if ("admin" == rawComment.distinguished) {
                setBackgroundColour = true
                backgroundColour = Color.rgb(170, 0, 0)
            } else if (rawComment.author.decoded.equals(
                    mCurrentCanonicalUserName, ignoreCase = true
                )
            ) {
                if (PrefsUtility.pref_appearance_highlight_own_username()) {
                    setBackgroundColour = true
                    backgroundColour = Color.rgb(0xEF, 0x6C, 0x00)
                }
            }

            if (setBackgroundColour) {
                sb.append(
                    " " + rawComment.author.decoded + " ",
                    (BetterSSB.Companion.BACKGROUND_COLOR
                            or BetterSSB.Companion.FOREGROUND_COLOR
                            or BetterSSB.Companion.BOLD),
                    Color.WHITE,
                    backgroundColour,
                    1f
                )
            } else {
                sb.append(
                    rawComment.author.decoded,
                    BetterSSB.Companion.FOREGROUND_COLOR or BetterSSB.Companion.BOLD,
                    theme.rrCommentHeaderAuthorCol,
                    0,
                    1f
                )
            }
            if (isBlockedUser) {
                sb.append(
                    " [" + context.getString(string.blocked_user_comment) + "]",
                    BetterSSB.Companion.FOREGROUND_COLOR, Color.RED, 0, 1f
                )
            }
        }

        val flair = parsedComment.flair

        if (theme.shouldShow(AppearanceCommentHeaderItem.FLAIR)
            && flair != null && !flair.isEmpty
        ) {
            if (theme.shouldShow(AppearanceCommentHeaderItem.AUTHOR)) {
                sb.append("  ", 0)
            }

            val flairStartIndex = sb.get().length

            sb.append(flair.get())

            val flairEndIndex = sb.get().length

            val observer = Observer { observable: Observable?, o: Any? ->
                sb.replace(
                    flairStartIndex,
                    flairEndIndex,
                    o as SpannableStringBuilder?
                )
            }

            flair.addObserver(observer)
        }

        if (theme.shouldShow(AppearanceCommentHeaderItem.AUTHOR)
            || theme.shouldShow(AppearanceCommentHeaderItem.FLAIR)
        ) {
            sb.append("   ", 0)
        }

        if (theme.shouldShow(AppearanceCommentHeaderItem.SCORE)
            && mShowScore
        ) {
            if (!rawComment.score_hidden) {
                sb.append(
                    score.toString(),
                    BetterSSB.Companion.FOREGROUND_COLOR or BetterSSB.Companion.BOLD,
                    pointsCol,
                    0,
                    1f
                )
            } else {
                sb.append(
                    "??",
                    BetterSSB.Companion.FOREGROUND_COLOR or BetterSSB.Companion.BOLD,
                    pointsCol,
                    0,
                    1f
                )
            }

            sb.append(
                BetterSSB.Companion.NBSP.toString() + context.getString(string.subtitle_points),
                0
            )

            if (!theme.shouldShow(AppearanceCommentHeaderItem.CONTROVERSIALITY)) {
                sb.append(" ", 0)
            }
        }

        if (theme.shouldShow(AppearanceCommentHeaderItem.CONTROVERSIALITY)) {
            if (rawComment.isControversial()) {
                sb.append(
                    context.getString(string.props_controversial_symbol),
                    BetterSSB.Companion.FOREGROUND_COLOR or BetterSSB.Companion.BOLD or BetterSSB.Companion.SUPERSCRIPT,
                    theme.rrCommentHeaderBoldCol,
                    0,
                    1f
                )
            }

            sb.append(" ", 0)
        }

        if (theme.shouldShow(AppearanceCommentHeaderItem.GOLD)) {
            if (rawComment.gilded > 0) {
                sb.append(" ", 0)

                sb.append(
                    (" "
                            + context.getString(string.gold)
                            + BetterSSB.Companion.NBSP + "x"
                            + rawComment.gilded
                            + " "),
                    BetterSSB.Companion.FOREGROUND_COLOR or BetterSSB.Companion.BACKGROUND_COLOR,
                    theme.rrGoldTextCol,
                    theme.rrGoldBackCol,
                    1f
                )

                sb.append("  ", 0)
            }
        }

        if (theme.shouldShow(AppearanceCommentHeaderItem.AGE)) {
            val formattedAge = formatAge(
                context,
                commentAgeMode,
                commentAgeUnits,
                rawComment.created_utc.value,
                postCreated,
                parentCommentCreated
            )

            sb.append(
                formattedAge,
                BetterSSB.Companion.FOREGROUND_COLOR or BetterSSB.Companion.BOLD,
                theme.rrCommentHeaderBoldCol,
                0,
                1f
            )

            if (rawComment.wasEdited()) {
                sb.append(
                    "*",
                    BetterSSB.Companion.FOREGROUND_COLOR or BetterSSB.Companion.BOLD,
                    theme.rrCommentHeaderBoldCol,
                    0,
                    1f
                )
            }

            sb.append(" ", 0)
        }

        if (theme.shouldShow(AppearanceCommentHeaderItem.SUBREDDIT)
            && mShowSubreddit
        ) {
            sb.append(context.getString(string.subtitle_to) + " ", 0)

            sb.append(
                parsedComment.rawComment.subreddit!!.decoded,  // TODO null
                BetterSSB.Companion.BOLD or BetterSSB.Companion.FOREGROUND_COLOR,
                theme.rrCommentHeaderBoldCol,
                0,
                1f
            )
        }

        return sb
    }

    override fun getAccessibilityHeader(
        theme: RRThemeAttributes,
        changeDataManager: RedditChangeDataManager,
        context: Context,
        commentAgeUnits: Int,
        postCreated: TimestampUTC?,
        parentCommentCreated: TimestampUTC?,
        collapsed: kotlin.Boolean,
        indentLevel: Optional<Int>
    ): String {
        val commentAgeMode = PrefsUtility.appearance_comment_age_mode()

        val accessibilityHeader = StringBuilder()

        val rawComment = parsedComment.rawComment

        val separator = " \n"

        val accessibilityConciseMode = PrefsUtility.pref_accessibility_concise_mode()

        if (indentLevel.isPresent
            && PrefsUtility.pref_accessibility_say_comment_indent_level()
        ) {
            val accessibilityLvl = indentLevel.get() + 1
            accessibilityHeader
                .append(
                    context.getString(
                        if (accessibilityConciseMode)
                            string.accessibility_comment_indent_level_concise
                        else
                            string.accessibility_comment_indent_level,
                        accessibilityLvl
                    )
                )
                .append(separator)
        }

        if (collapsed) {
            accessibilityHeader
                .append(
                    context.getString(
                        if (accessibilityConciseMode)
                            string.accessibility_subtitle_comment_collapsed_concise
                        else
                            string.accessibility_subtitle_comment_collapsed
                    )
                )
                .append(separator)
        }

        if (theme.shouldShow(AppearanceCommentHeaderItem.AUTHOR)) {
            @StringRes val authorString: Int

            val authorSubmitterModConcise =                 string.accessibility_subtitle_author_submitter_moderator_withperiod_concise

            val authorSubmitterMod =                 string.accessibility_subtitle_author_submitter_moderator_withperiod

            val authorModConcise =                 string.accessibility_subtitle_author_moderator_withperiod_concise_comment

            val authorMod = string.accessibility_subtitle_author_moderator_withperiod

            if (rawComment.author!!.decoded.equals(mParentPostAuthor, ignoreCase = true)
                && rawComment.author.decoded != "[deleted]"
            ) {
                if ("moderator" == rawComment.distinguished) {
                    authorString = if (accessibilityConciseMode)
                        authorSubmitterModConcise
                    else
                        authorSubmitterMod
                } else if ("admin" == rawComment.distinguished) {
                    authorString = if (accessibilityConciseMode)
                        string.accessibility_subtitle_author_submitter_admin_withperiod_concise
                    else
                        string.accessibility_subtitle_author_submitter_admin_withperiod
                } else {
                    authorString = if (accessibilityConciseMode)
                        string.accessibility_subtitle_author_submitter_withperiod_concise
                    else
                        string.accessibility_subtitle_author_submitter_withperiod
                }
            } else {
                if ("moderator" == rawComment.distinguished) {
                    authorString = if (accessibilityConciseMode)
                        authorModConcise
                    else
                        authorMod
                } else if ("admin" == rawComment.distinguished) {
                    authorString = if (accessibilityConciseMode)
                        string.accessibility_subtitle_author_admin_withperiod_concise_comment
                    else
                        string.accessibility_subtitle_author_admin_withperiod
                } else {
                    authorString = if (accessibilityConciseMode)
                        string.accessibility_subtitle_author_withperiod_concise_comment
                    else
                        string.accessibility_subtitle_author_withperiod
                }
            }

            accessibilityHeader
                .append(
                    context.getString(
                        authorString,
                        ScreenreaderPronunciation.getPronunciation(
                            context,
                            rawComment.author.decoded
                        )
                    )
                )
                .append(separator)
        }

        val flair = parsedComment.flair

        if (theme.shouldShow(AppearanceCommentHeaderItem.FLAIR)
            && flair != null && !flair.isEmpty
        ) {
            accessibilityHeader
                .append(
                    context.getString(
                        if (accessibilityConciseMode)
                            string.accessibility_subtitle_flair_withperiod_concise
                        else
                            string.accessibility_subtitle_flair_withperiod,
                        flair.get().toString() + General.LTR_OVERRIDE_MARK
                    )
                )
                .append(separator)
        }

        if (theme.shouldShow(AppearanceCommentHeaderItem.SCORE) && mShowScore) {
            if (rawComment.score_hidden) {
                accessibilityHeader
                    .append(
                        context.getString(
                            string.accessibility_subtitle_points_unknown_withperiod
                        )
                    )
                    .append(separator)
            } else {
                val score = computeScore(changeDataManager)

                accessibilityHeader
                    .append(
                        context.getResources().getQuantityString(
                            if (accessibilityConciseMode)
                                R.plurals.accessibility_subtitle_points_withperiod_concise_plural
                            else
                                R.plurals.accessibility_subtitle_points_withperiod_plural,
                            score,
                            score
                        )
                    )
                    .append(separator)
            }

            if (changeDataManager.isUpvoted(idAndType)) {
                accessibilityHeader
                    .append(
                        context.getString(
                            string.accessibility_subtitle_upvoted_withperiod
                        )
                    )
                    .append(separator)
            }

            if (changeDataManager.isDownvoted(idAndType)) {
                accessibilityHeader
                    .append(
                        context.getString(
                            string.accessibility_subtitle_downvoted_withperiod
                        )
                    )
                    .append(separator)
            }
        }

        if (theme.shouldShow(AppearanceCommentHeaderItem.CONTROVERSIALITY)) {
            if (rawComment.isControversial()) {
                accessibilityHeader.append(
                    context.getString(
                        if (accessibilityConciseMode)
                            string.accessibility_subtitle_controversiality_withperiod_concise
                        else
                            string.accessibility_subtitle_controversiality_withperiod
                    )
                )
                    .append(separator)
            }
        }

        if (theme.shouldShow(AppearanceCommentHeaderItem.GOLD)) {
            if (rawComment.gilded > 0) {
                accessibilityHeader
                    .append(
                        context.getString(
                            string.accessibility_subtitle_gold_withperiod,
                            rawComment.gilded
                        )
                    )
                    .append(separator)
            }
        }

        if (theme.shouldShow(AppearanceCommentHeaderItem.AGE)) {
            val formattedAge = formatAge(
                context,
                commentAgeMode,
                commentAgeUnits,
                rawComment.created_utc.value,
                postCreated,
                parentCommentCreated
            )

            accessibilityHeader
                .append(
                    context.getString(
                        string.accessibility_subtitle_age_withperiod,
                        formattedAge
                    )
                )
                .append(separator)

            if (rawComment.wasEdited()) {
                accessibilityHeader
                    .append(
                        context.getString(
                            string.accessibility_subtitle_edited_since_being_posted
                        )
                    )
                    .append(separator)
            }
        }

        if (theme.shouldShow(AppearanceCommentHeaderItem.SUBREDDIT)
            && mShowSubreddit
        ) {
            // TODO nullability

            accessibilityHeader
                .append(
                    context.getString(
                        if (accessibilityConciseMode)
                            string.accessibility_subtitle_subreddit_withperiod_concise
                        else
                            string.accessibility_subtitle_subreddit_withperiod,
                        ScreenreaderPronunciation.getPronunciation(
                            context,
                            parsedComment.rawComment.subreddit!!.decoded
                        )
                    )
                )
                .append(separator)
        }

        return accessibilityHeader.toString()
    }

    private fun formatAge(
        context: Context,
        commentAgeMode: CommentAgeMode,
        commentAgeUnits: Int,
        commentTime: TimestampUTC,
        postCreated: TimestampUTC?,
        parentCommentCreated: TimestampUTC?
    ): String {
        //In addition to enforcing the user's prefs, the lower mode cases also act as fallbacks
        when (commentAgeMode) {
            CommentAgeMode.RELATIVE_PARENT -> {
                if (parentCommentCreated != null) {
                    return format(
                        commentTime.elapsedPeriodSince(parentCommentCreated),
                        context,
                        string.time_after_reply,
                        commentAgeUnits
                    )
                }
                if (postCreated != null) {
                    return format(
                        commentTime.elapsedPeriodSince(postCreated),
                        context,
                        string.time_after,
                        commentAgeUnits
                    )
                }
                return format(
                    commentTime.elapsedPeriod(),
                    context,
                    string.time_ago,
                    commentAgeUnits
                )
            }

            CommentAgeMode.RELATIVE_POST -> {
                if (postCreated != null) {
                    return format(
                        commentTime.elapsedPeriodSince(postCreated),
                        context,
                        string.time_after,
                        commentAgeUnits
                    )
                }
                return format(
                    commentTime.elapsedPeriod(),
                    context,
                    string.time_ago,
                    commentAgeUnits
                )
            }

            CommentAgeMode.ABSOLUTE -> return format(
                commentTime.elapsedPeriod(),
                context,
                string.time_ago,
                commentAgeUnits
            )

            else -> throw IllegalStateException("Unexpected value: " + commentAgeMode)
        }
    }

    override fun getBody(
        activity: BaseActivity,
        textColor: Int,
        textSize: Float,
        showLinkButtons: Boolean
    ): View {
        return parsedComment.body
            .generateView(activity, textColor, textSize, showLinkButtons)
    }

    override val idAlone: String? get() = parsedComment.idAlone

    override val idAndType: RedditIdAndType? get() = parsedComment.idAndType

    private fun isScoreBelowThreshold(changeDataManager: RedditChangeDataManager): kotlin.Boolean {
        if (mMinimumCommentScore == null) {
            return false
        }

        if (parsedComment.rawComment.score_hidden) {
            return false
        }

        return (computeScore(changeDataManager) < mMinimumCommentScore)
    }

    fun isCollapsed(changeDataManager: RedditChangeDataManager): kotlin.Boolean {
        val collapsed = changeDataManager.isHidden(idAndType)

        if (collapsed != null) {
            return collapsed
        }

        //Always collapse blocked users
        if (isBlockedUser) {
            return true
        }

        if (mNeverAutoCollapse) {
            return false
        }

        val authorLowercase = StringUtils.asciiLowercase(
            parsedComment.rawComment.author!!.decoded.trim { it <= ' ' })

        if (authorLowercase == mCurrentCanonicalUserName) {
            return false
        }

        if (parsedComment.rawComment.stickied) {
            when (PrefsUtility.behaviour_collapse_sticky_comments()) {
                BehaviourCollapseStickyComments.ALWAYS -> return true

                BehaviourCollapseStickyComments.ONLY_BOTS -> if (Reddit.BOT_USERNAMES_LOWERCASE.contains(
                        authorLowercase
                    )
                ) {
                    return true
                }

                BehaviourCollapseStickyComments.NEVER -> {}
            }
        }

        return isScoreBelowThreshold(changeDataManager)
    }
}
