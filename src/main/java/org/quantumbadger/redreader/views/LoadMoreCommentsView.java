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
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.activities.MoreCommentsListingActivity
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.common.General.quickToast
import org.quantumbadger.redreader.common.General.setLayoutWidthHeight
import org.quantumbadger.redreader.reddit.RedditCommentListItem
import org.quantumbadger.redreader.reddit.url.RedditURLParser
import org.quantumbadger.redreader.reddit.url.RedditURLParser.RedditURL

class LoadMoreCommentsView(
    context: Context,
    private val mCommentListingURL: RedditURL
) : LinearLayout(context) {
    private val mIndentView: IndentView
    private val mTitleView: TextView
    private var mItem: RedditCommentListItem? = null

    init {
        setOrientation(VERTICAL)

        val divider = View(context)
        addView(divider)

        setLayoutWidthHeight(divider, LayoutParams.MATCH_PARENT, 1)

        val layout = LinearLayout(context)
        layout.setOrientation(HORIZONTAL)
        layout.setLayoutDirection(LAYOUT_DIRECTION_LTR)

        addView(layout)
        val marginPx = dpToPixels(context, 8f)

        layout.setGravity(Gravity.CENTER_VERTICAL)

        mIndentView = IndentView(context)
        layout.addView(mIndentView)
        mIndentView.getLayoutParams().height = LayoutParams.MATCH_PARENT
        mIndentView.setLayoutParams(mIndentView.getLayoutParams())

        val icon: ImageView

        run {
            val appearance = context.obtainStyledAttributes(
                intArrayOf(
                    R.attr.rrIconForward,
                    R.attr.rrListItemBackgroundCol,
                    R.attr.rrListDividerCol
                )
            )
            icon = ImageView(context)
            icon.setImageDrawable(appearance.getDrawable(0))

            layout.setBackgroundColor(appearance.getColor(1, General.COLOR_INVALID))

            divider.setBackgroundColor(appearance.getColor(2, General.COLOR_INVALID))
            appearance.recycle()
        }

        icon.setScaleX(0.75f)
        icon.setScaleY(0.75f)

        layout.addView(icon)
        (icon.getLayoutParams() as LayoutParams).setMargins(
            marginPx,
            marginPx,
            marginPx,
            marginPx
        )

        val textLayout = LinearLayout(context)
        textLayout.setOrientation(VERTICAL)
        layout.addView(textLayout)
        (textLayout.getLayoutParams() as LayoutParams).setMargins(
            0,
            marginPx,
            marginPx,
            marginPx
        )

        mTitleView = TextView(context)
        mTitleView.setTextSize(13f)
        textLayout.addView(mTitleView)

        setOnClickListener(OnClickListener { v: View? ->
            if (mCommentListingURL.pathType()
                == RedditURLParser.POST_COMMENT_LISTING_URL
            ) {
                val listingUrl =
                    mCommentListingURL.asPostCommentListURL()

                val commentIds = ArrayList<String?>(16)
                for (url in mItem!!.asLoadMore()
                    .getMoreUrls(mCommentListingURL)) {
                    commentIds.add(url.commentId)
                }

                val intent =
                    Intent(context, MoreCommentsListingActivity::class.java)
                intent.putExtra("postId", listingUrl.postId)
                intent.putStringArrayListExtra("commentIds", commentIds)
                context.startActivity(intent)
            } else {
                quickToast(
                    context,
                    string.load_more_comments_failed_unknown_url_type
                )
            }
        })
    }

    fun reset(item: RedditCommentListItem) {
        mItem = item

        val count = item.asLoadMore().count

        mTitleView.setText(
            getResources().getQuantityString(
                R.plurals.load_more_comments_button_reply_count,
                count,
                count
            )
        )

        mIndentView.setIndentation(item.getIndent())
    }
}
