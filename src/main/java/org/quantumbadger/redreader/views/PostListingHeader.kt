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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.quantumbadger.redreader.R
import org.quantumbadger.redreader.common.Fonts
import org.quantumbadger.redreader.common.General
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.UriString
import org.quantumbadger.redreader.reddit.SubredditDetails
import org.quantumbadger.redreader.reddit.things.RedditSubreddit
import org.quantumbadger.redreader.reddit.url.PostListingURL

class PostListingHeader(
    activity: AppCompatActivity,
    titleText: String?,
    subtitleText: String?,
    url: PostListingURL,
    subreddit: RedditSubreddit?
) : LinearLayout(activity) {
    init {
        val dpScale = activity.getResources().getDisplayMetrics().density

        setOrientation(VERTICAL)

        if (!PrefsUtility.pref_appearance_post_hide_subreddit_header()) {
            val greyHeader = LinearLayout(activity)
            greyHeader.setOrientation(VERTICAL)

            run {
                val appearance =                     activity.obtainStyledAttributes(intArrayOf(R.attr.rrPostListHeaderBackgroundCol))
                greyHeader.setBackgroundColor(appearance.getColor(0, General.COLOR_INVALID))
                appearance.recycle()
            }

            val sidesPadding = (15.0f * dpScale).toInt()
            val topPadding = (10.0f * dpScale).toInt()

            greyHeader.setPadding(sidesPadding, topPadding, sidesPadding, topPadding)

            val title = TextView(activity)
            title.setText(titleText)
            title.setTextSize(22.0f)
            title.setTypeface(Fonts.robotoLightOrAlternative)
            title.setTextColor(Color.WHITE)
            greyHeader.addView(title)

            val subtitle = TextView(activity)
            subtitle.setTextSize(14.0f)
            subtitle.setText(subtitleText)
            subtitle.setTextColor(Color.rgb(200, 200, 200))
            greyHeader.addView(subtitle)

            addView(greyHeader)
        }

        if (subreddit != null
            && !PrefsUtility.pref_appearance_hide_headertoolbar_postlist()
        ) {
            val buttons =                 inflate(activity, R.layout.subreddit_header_toolbar, this)
                    .findViewById<SubredditToolbar>(R.id.subreddit_toolbar_layout)

            buttons.bindSubreddit(
                SubredditDetails.Companion.newWithRuntimeException(subreddit),
                Optional.Companion.of<UriString?>(url.browserUrl())
            )
        }
    }
}
