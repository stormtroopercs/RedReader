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
package org.quantumbadger.redreader.reddit.prepared.bodytext

import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.views.LinkDetailsView
import org.quantumbadger.redreader.common.General

abstract class BodyElementBaseButton(
    private val mText: String,
    private val mSubtitle: String?, private val mIsLinkButton: Boolean
) : BodyElement(BlockType.BUTTON) {
    protected abstract fun generateOnClickListener(
        activity: BaseActivity,
        textColor: Int?,
        textSize: Float?,
        showLinkButtons: Boolean
    ): View.OnClickListener

    protected abstract fun generateOnLongClickListener(
        activity: BaseActivity,
        textColor: Int?,
        textSize: Float?,
        showLinkButtons: Boolean
    ): OnLongClickListener?

    override fun generateView(
        activity: BaseActivity,
        textColor: Int?,
        textSize: Float?,
        showLinkButtons: Boolean
    ): View {
        if (mIsLinkButton && !showLinkButtons) {
            // Don't show
            val result = View(activity)
            result.setVisibility(View.GONE)
            return result
        }

        val ldv = LinkDetailsView(
            activity,
            mText,
            mSubtitle
        )

        val linkMarginPx = dpToPixels(activity, 8f)

        val layoutParams = MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        layoutParams.setMargins(0, linkMarginPx, 0, linkMarginPx)
        ldv.setLayoutParams(layoutParams)

        ldv.setOnClickListener(
            generateOnClickListener(activity, textColor, textSize, showLinkButtons)
        )

        val longClickListener = generateOnLongClickListener(
            activity,
            textColor,
            textSize,
            showLinkButtons
        )

        if (longClickListener != null) {
            ldv.setOnLongClickListener(longClickListener)
        }

        return ldv
    }
}
