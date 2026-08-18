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
import android.view.View
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.BetterSSB
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.RRThemeAttributes
import org.quantumbadger.redreader.common.time.TimestampUTC

interface RedditRenderableCommentListItem {
    fun getHeader(
        theme: RRThemeAttributes,
        changeDataManager: RedditChangeDataManager,
        context: Context,
        commentAgeUnits: Int,
        postCreated: TimestampUTC?,
        parentCommentCreated: TimestampUTC?
    ): BetterSSB

    fun getAccessibilityHeader(
        theme: RRThemeAttributes,
        changeDataManager: RedditChangeDataManager,
        context: Context,
        commentAgeUnits: Int,
        postCreated: TimestampUTC?,
        parentCommentCreated: TimestampUTC?,
        collapsed: Boolean,
        indentLevel: Optional<Int>
    ): String

    fun getBody(
        activity: BaseActivity,
        textColor: Int?,
        textSize: Float?,
        showLinkButtons: Boolean
    ): View
}
