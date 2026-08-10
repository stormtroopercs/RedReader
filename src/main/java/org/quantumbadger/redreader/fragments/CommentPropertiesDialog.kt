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
package org.quantumbadger.redreader.fragments

import android.content.Context
import android.os.Bundle
import android.widget.LinearLayout
import androidx.core.os.BundleCompat
import org.quantumbadger.redreader.R.string
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.reddit.kthings.RedditComment
import org.quantumbadger.redreader.reddit.kthings.RedditFieldEdited
import java.util.Objects

class CommentPropertiesDialog : PropertiesDialog() {
    override fun getTitle(context: Context): String {
        return context.getString(string.props_comment_title)
    }

    override fun prepare(
        context: BaseActivity,
        items: LinearLayout
    ) {
        val comment = Objects.requireNonNull<RedditComment>(
            BundleCompat.getParcelable<RedditComment?>(
                requireArguments(),
                "comment",
                RedditComment::class.java
            )
        )

        items.addView(propView(context, "ID", comment.name.value, true))

        // TODO nullability
        items.addView(
            propView(
                context,
                string.props_author,
                comment.author!!.decoded,
                false
            )
        )

        if (comment.author_flair_text != null
            && !comment.author_flair_text.decoded.isEmpty()
        ) {
            items.addView(
                propView(
                    context,
                    string.props_author_flair,
                    comment.author_flair_text.decoded,
                    false
                )
            )
        }

        items.addView(
            propView(
                context,
                string.props_created,
                comment.created_utc.value.format(),
                false
            )
        )

        if (comment.edited is RedditFieldEdited.Timestamp) {
            items.addView(
                propView(
                    context,
                    string.props_edited,
                    comment.edited
                        .value.value.format(),
                    false
                )
            )
        } else {
            items.addView(
                propView(
                    context,
                    string.props_edited,
                    string.props_never,
                    false
                )
            )
        }

        items.addView(
            propView(
                context,
                string.props_score,
                (comment.ups - comment.downs).toString(),
                false
            )
        )

        items.addView(
            propView(
                context,
                string.props_subreddit,
                comment.subreddit!!.decoded,
                false
            )
        )

        if (comment.body != null && !comment.body.decoded.isEmpty()) {
            items.addView(
                propView(
                    context,
                    string.props_body_markdown,
                    comment.body.decoded,
                    false
                )
            )

            if (comment.body_html != null) {
                items.addView(
                    propView(
                        context,
                        string.props_body_html,
                        comment.body_html.decoded,
                        false
                    )
                )
            }
        }
    }

    companion object {
        fun newInstance(comment: RedditComment?): CommentPropertiesDialog {
            val pp = CommentPropertiesDialog()

            val args = Bundle()
            args.putParcelable("comment", comment)
            pp.setArguments(args)

            return pp
        }
    }
}
