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
import org.quantumbadger.redreader.reddit.kthings.RedditFieldEdited
import org.quantumbadger.redreader.reddit.kthings.RedditPost
import java.util.Objects

class PostPropertiesDialog : PropertiesDialog() {
    override fun getTitle(context: Context): String {
        return context.getString(string.props_post_title)
    }

    override fun prepare(
        context: BaseActivity,
        items: LinearLayout
    ) {
        val post = Objects.requireNonNull<RedditPost>(
            BundleCompat.getParcelable<RedditPost?>(
                requireArguments(),
                "post",
                RedditPost::class.java
            )
        )

        // TODO nullability
        items.addView(
            propView(
                context,
                string.props_title,
                post.title!!.decoded.trim { it <= ' ' },
                true
            )
        )
        items.addView(
            propView(
                context,
                string.props_author,
                post.author!!.decoded,
                false
            )
        )
        items.addView(
            propView(
                context,
                string.props_url,
                post.url!!.decoded,
                false
            )
        )
        items.addView(
            propView(
                context,
                string.props_created,
                post.created_utc.value.format(),
                false
            )
        )

        if (post.edited is RedditFieldEdited.Timestamp) {
            items.addView(
                propView(
                    context,
                    string.props_edited,
                    post.edited
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
                string.props_subreddit,
                post.subreddit.decoded,
                false
            )
        )
        items.addView(
            propView(
                context,
                string.props_score,
                post.score.toString(),
                false
            )
        )
        items.addView(
            propView(
                context,
                string.props_num_comments,
                post.num_comments.toString(),
                false
            )
        )

        if (post.selftext != null && !post.selftext.decoded.isEmpty()) {
            items.addView(
                propView(
                    context,
                    string.props_self_markdown,
                    post.selftext.decoded,
                    false
                )
            )

            if (post.selftext_html != null) {
                items.addView(
                    propView(
                        context,
                        string.props_self_html,
                        post.selftext_html.decoded,
                        false
                    )
                )
            }
        }
    }

    companion object {
        fun newInstance(post: RedditPost?): PostPropertiesDialog {
            val pp = PostPropertiesDialog()

            val args = Bundle()
            args.putParcelable("post", post)
            pp.setArguments(args)

            return pp
        }
    }
}
