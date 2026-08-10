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
package org.quantumbadger.redreader.views.liststatus

import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.common.LinkHandler.onLinkClicked
import org.quantumbadger.redreader.reddit.url.PostCommentListingURL

class CommentSubThreadView(
    activity: AppCompatActivity,
    private val mUrl: PostCommentListingURL,
    messageRes: Int
) : StatusListItemView(activity) {
    init {
        val attr = activity.obtainStyledAttributes(
            intArrayOf(
                R.attr.rrCommentSpecificThreadHeaderBackCol,
                R.attr.rrCommentSpecificThreadHeaderTextCol
            )
        )

        val rrCommentSpecificThreadHeaderBackCol = attr.getColor(0, 0)
        val rrCommentSpecificThreadHeaderTextCol = attr.getColor(1, 0)

        attr.recycle()

        val textView = TextView(activity)
        textView.setText(messageRes)
        textView.setTextColor(rrCommentSpecificThreadHeaderTextCol)
        textView.setTextSize(15.0f)
        textView.setPadding(
            (15 * dpScale).toInt(),
            (10 * dpScale).toInt(),
            (10 * dpScale).toInt(),
            (4 * dpScale).toInt()
        )

        val messageView = TextView(activity)
        messageView.setText(string.comment_header_specific_thread_message)
        messageView.setTextColor(rrCommentSpecificThreadHeaderTextCol)
        messageView.setTextSize(12.0f)
        messageView.setPadding(
            (15 * dpScale).toInt(),
            0,
            (10 * dpScale).toInt(),
            (10 * dpScale).toInt()
        )

        val layout = LinearLayout(activity)
        layout.setOrientation(LinearLayout.VERTICAL)
        layout.addView(textView)
        layout.addView(messageView)

        setContents(layout)
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS)

        setBackgroundColor(rrCommentSpecificThreadHeaderBackCol)

        setOnClickListener(OnClickListener { v: View? ->
            val allComments = mUrl.commentId(null)
            onLinkClicked(activity, allComments.toUriString())
        })
    }
}
