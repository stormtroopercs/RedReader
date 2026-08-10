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

import android.text.SpannableStringBuilder
import android.view.View
import android.widget.TextView.BufferType
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.AndroidCommon.runOnUiThread
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.views.LinkifiedTextView

class BodyElementTextSpanned(
    blockType: BlockType,
    private val mSpanned: SpannableStringBuilder
) : BodyElement(blockType), DynamicSpanned {
    private var mTextView: LinkifiedTextView?=null

    override fun addSpanDynamic(what: Any?, start: Int, end: Int, flags: Int) {
        runOnUiThread(Runnable {
            mSpanned.setSpan(what, start, end, flags)
            if (mTextView != null) {
                mTextView!!.setText(mSpanned)
            }
        })
    }

    override fun generateView(
        activity: BaseActivity,
        textColor: Int?,
        textSize: Float?,
        showLinkButtons: Boolean
    ): View? {
        mTextView = LinkifiedTextView(activity)

        if (textColor != null) {
            mTextView!!.setTextColor(textColor)
        }
        if (textSize != null) {
            mTextView!!.setTextSize(textSize)
        }

        mTextView!!.setText(mSpanned, BufferType.SPANNABLE)

        if (PrefsUtility.pref_accessibility_separate_body_text_lines()) {
            mTextView!!.setFocusable(true)
        }

        return mTextView
    }
}
