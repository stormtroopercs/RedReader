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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.common.General.setLayoutMatchWidthWrapHeight
import org.quantumbadger.redreader.common.General.setLayoutWidthHeight
import org.quantumbadger.redreader.common.Optional
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.RRThemeAttributes
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager
import org.quantumbadger.redreader.reddit.prepared.RedditRenderableInboxItem
import org.quantumbadger.redreader.common.General

class RedditInboxItemView(
    activity: BaseActivity,
    theme: RRThemeAttributes
) : LinearLayout(activity) {
    private val mDivider: View
    private val mHeader: TextView
    private val mBodyHolder: FrameLayout

    private val mTheme: RRThemeAttributes

    private val showLinkButtons: Boolean

    private var currentItem: RedditRenderableInboxItem?=null

    private val mActivity: BaseActivity?

    init {
        mActivity = activity
        mTheme = theme

        setOrientation(VERTICAL)

        mDivider = View(activity)
        mDivider.setBackgroundColor(Color.argb(128, 128, 128, 128)) // TODO better
        addView(mDivider)
        setLayoutWidthHeight(mDivider, LayoutParams.MATCH_PARENT, 1)

        val inner = LinearLayout(activity)
        inner.setOrientation(VERTICAL)

        mHeader = TextView(activity)
        mHeader.setTextSize(11.0f * theme.rrCommentHeaderFontScale)
        mHeader.setTextColor(theme.rrCommentHeaderCol)
        inner.addView(mHeader)
        setLayoutMatchWidthWrapHeight(mHeader)

        mBodyHolder = FrameLayout(activity)
        mBodyHolder.setPadding(0, dpToPixels(activity, 2f), 0, 0)
        inner.addView(mBodyHolder)
        setLayoutMatchWidthWrapHeight(mBodyHolder)

        val paddingPixels = dpToPixels(activity, 8.0f)
        inner.setPadding(
            paddingPixels + paddingPixels,
            paddingPixels,
            paddingPixels,
            paddingPixels
        )

        addView(inner)
        setLayoutMatchWidthWrapHeight(mBodyHolder)

        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS)

        showLinkButtons = PrefsUtility.pref_appearance_linkbuttons()

        setOnClickListener(OnClickListener { v: View? -> handleInboxClick(mActivity) })

        setOnLongClickListener(OnLongClickListener { v: View? ->
            handleInboxLongClick(mActivity)
            true
        })
    }

    fun reset(
        context: BaseActivity?,
        changeDataManager: RedditChangeDataManager?,
        theme: RRThemeAttributes?,
        item: RedditRenderableInboxItem,
        showDividerAtTop: Boolean
    ) {
        currentItem = item

        mDivider.setVisibility(if (showDividerAtTop) VISIBLE else GONE)
        mHeader.setText(
            item.getHeader(
                theme,
                changeDataManager,
                context,
                PrefsUtility.appearance_inbox_age_units(),
                null,
                null
            ).get()
        )

        mHeader.setContentDescription(
            item.getAccessibilityHeader(
                theme,
                changeDataManager,
                context,
                PrefsUtility.appearance_inbox_age_units(),
                null,
                null,
                false,
                Optional.Companion.empty<Int>()
            )
        )

        val body = item.getBody(
            context,
            mTheme.rrCommentBodyCol,
            13.0f * mTheme.rrCommentFontScale,
            showLinkButtons
        )

        mBodyHolder.removeAllViews()
        mBodyHolder.addView(body)
        setLayoutMatchWidthWrapHeight(body)
    }

    fun handleInboxClick(activity : BaseActivity) {
        if (currentItem != null) {
            currentItem!!.handleInboxClick(activity)
        }
    }

    fun handleInboxLongClick(activity : BaseActivity) {
        if (currentItem != null) {
            currentItem!!.handleInboxLongClick(activity)
        }
    }
}