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
import android.content.Intent
import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.apache.commons.text.StringEscapeUtils
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.account.RedditAccountManager
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.activities.CommentReplyActivity
import org.quantumbadger.redreader.activities.InboxListingActivity.InboxType
import org.quantumbadger.redreader.common.BetterSSB
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.General.mapIfNotNull
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.RRThemeAttributes
import org.quantumbadger.redreader.common.ScreenreaderPronunciation
import org.quantumbadger.redreader.common.StringUtils
import org.quantumbadger.redreader.common.time.TimeFormatHelper.format
import org.quantumbadger.redreader.common.time.TimestampUTC
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType
import org.quantumbadger.redreader.reddit.kthings.RedditMessage
import org.quantumbadger.redreader.reddit.kthings.UrlEncodedString
import org.quantumbadger.redreader.reddit.prepared.bodytext.BodyElement
import org.quantumbadger.redreader.reddit.prepared.html.HtmlReader

class RedditPreparedMessage(
    activity: AppCompatActivity,
    message: RedditMessage,
    inboxType: InboxType?
) : RedditRenderableInboxItem {
    val header: BetterSSB
    val body: BodyElement
    val idAndType: RedditIdAndType
    val src: RedditMessage
    val inboxType: InboxType?

    init {
        val applicationContext = activity.getApplicationContext()

        this.src = message
        this.inboxType = inboxType

        // TODO respect RRTheme
        val rrCommentHeaderBoldCol: Int
        val rrCommentHeaderAuthorCol: Int

        run {
            val appearance = activity.obtainStyledAttributes(
                intArrayOf(
                    R.attr.rrCommentHeaderBoldCol,
                    R.attr.rrCommentHeaderAuthorCol,
                )
            )
            rrCommentHeaderBoldCol = appearance.getColor(0, 255)
            rrCommentHeaderAuthorCol = appearance.getColor(1, 255)
            appearance.recycle()
        }

        body = HtmlReader.Companion.parse(message.body_html!!.decoded, activity)

        idAndType = message.idAndType

        val sb = BetterSSB()

        if (inboxType == InboxType.SENT) {
            sb.append(applicationContext.getString(string.subtitle_to) + " ", 0)

            if (src.dest == null) {
                sb.append(
                    "[" + applicationContext.getString(string.general_unknown) + "]",
                    BetterSSB.Companion.FOREGROUND_COLOR or BetterSSB.Companion.BOLD,
                    rrCommentHeaderAuthorCol,
                    0,
                    1f
                )
            } else {
                sb.append(
                    src.dest.decoded,
                    BetterSSB.Companion.FOREGROUND_COLOR or BetterSSB.Companion.BOLD,
                    rrCommentHeaderAuthorCol,
                    0,
                    1f
                )
            }
        } else {
            val author: String?=General.nullAlternative<String>(
                org.quantumbadger.redreader.common.General.mapIfNotNull<UrlEncodedString?, String>(
                    src.author,
                    UrlEncodedString::decoded
                )!!,
                org.quantumbadger.redreader.common.General.mapIfNotNull<UrlEncodedString?, String>(
                    src.subreddit_name_prefixed,
                    UrlEncodedString::decoded
                )!!,
                "[" + applicationContext.getString(string.general_unknown) + "]"
            )

            sb.append(
                author!!,
                BetterSSB.Companion.FOREGROUND_COLOR or BetterSSB.Companion.BOLD,
                rrCommentHeaderAuthorCol,
                0,
                1f
            )
        }

        sb.append("   ", 0)
        sb.append(
            format(
                src.created_utc.value.elapsedPeriod(),
                applicationContext,
                string.time_ago,
                PrefsUtility.appearance_inbox_age_units()
            ),
            BetterSSB.Companion.FOREGROUND_COLOR or BetterSSB.Companion.BOLD,
            rrCommentHeaderBoldCol,
            0,
            1f
        )

        header = sb
    }

    private fun openReplyActivity(activity: AppCompatActivity) {
        val intent = Intent(activity, CommentReplyActivity::class.java)
        intent.putExtra(CommentReplyActivity.Companion.PARENT_ID_AND_TYPE_KEY, idAndType)
        intent.putExtra(
            CommentReplyActivity.Companion.PARENT_MARKDOWN_KEY,
            mapIfNotNull<UrlEncodedString?, String>(src.body_html, UrlEncodedString::decoded)
        )
        intent.putExtra(
            CommentReplyActivity.Companion.PARENT_TYPE,
            CommentReplyActivity.Companion.PARENT_TYPE_MESSAGE
        )
        activity.startActivity(intent)
    }

    override fun handleInboxClick(activity: BaseActivity) {
        if (src.author == null) {
            return
        }

        val currentCanonicalUserName: String = RedditAccountManager.Companion.getInstance(activity)
            .getDefaultAccount().canonicalUsername

        if (StringUtils.asciiLowercase(src.author.decoded.trim { it <= ' ' })
            != currentCanonicalUserName
        ) {
            openReplyActivity(activity)
        }
    }

    override fun handleInboxLongClick(activity: BaseActivity) {
        handleInboxClick(activity)
    }

    override fun getHeader(
        theme: RRThemeAttributes,
        changeDataManager: RedditChangeDataManager,
        context: Context,
        commentAgeUnits: Int,
        postCreated: TimestampUTC?,
        parentCommentCreated: TimestampUTC?
    ): BetterSSB {
        return header
    }

    override fun getAccessibilityHeader(
        theme: RRThemeAttributes,
        changeDataManager: RedditChangeDataManager,
        context: Context,
        commentAgeUnits: Int,
        postCreated: TimestampUTC?,
        parentCommentCreated: TimestampUTC?,
        collapsed: Boolean,
        indentLevel: Optional<Int>
    ): String {
        val accessibilityHeader = StringBuilder()
        val separator = " \n"

        if (inboxType == InboxType.SENT && src.dest != null) {
            accessibilityHeader
                .append(
                    context.getString(
                        string.accessibility_subtitle_recipient_withperiod,
                        ScreenreaderPronunciation.getPronunciation(
                            context,
                            src.dest.decoded
                        )
                    )
                )
                .append(separator)
        } else if (src.author != null) {
            accessibilityHeader
                .append(
                    context.getString(
                        if (PrefsUtility.pref_accessibility_concise_mode())
                            string.accessibility_subtitle_author_withperiod_concise_post
                        else
                            string.accessibility_subtitle_author_withperiod,
                        ScreenreaderPronunciation.getPronunciation(
                            context,
                            src.author.decoded
                        )
                    )
                )
                .append(separator)
        }

        accessibilityHeader
            .append(
                context.getString(
                    string.accessibility_subtitle_age_withperiod,
                    format(
                        src.created_utc.value.elapsedPeriod(),
                        context,
                        string.time_ago,
                        PrefsUtility.appearance_inbox_age_units()
                    )
                )
            )
            .append(separator)

        return accessibilityHeader.toString()
    }

    override fun getBody(
        activity: BaseActivity,
        textColor: Int,
        textSize: Float,
        showLinkButtons: Boolean
    ): View {
        val subjectLayout = LinearLayout(activity)
        subjectLayout.setOrientation(LinearLayout.VERTICAL)

        val subjectText = TextView(activity)
        subjectText.setText(
            StringEscapeUtils.unescapeHtml4(
                if (src.subject != null)
                    src.subject.decoded
                else
                    "(no subject)"
            )
        )
        subjectText.setTextColor(textColor)
        subjectText.setTextSize(textSize)
        subjectText.setTypeface(null, Typeface.BOLD)

        subjectLayout.addView(subjectText)
        subjectLayout.addView(
            body.generateView(
                activity,
                textColor,
                textSize,
                showLinkButtons
            )
        )

        return subjectLayout
    }
}
