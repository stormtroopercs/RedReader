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

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.LinearLayout
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.common.General.setLayoutMatchWidthWrapHeight

class BodyElementQuote(private val mElements: ArrayList<BodyElement?>) :
    BodyElement(BlockType.QUOTE) {
    override fun generateView(
        activity: BaseActivity,
        textColor: Int?,
        textSize: Float?,
        showLinkButtons: Boolean
    ): View {
        val quoteLayout = LinearLayout(activity)

        val paddingPx = dpToPixels(activity, 6f)
        quoteLayout.setPadding(paddingPx, paddingPx, paddingPx, 0)

        val quoteBarWidth = dpToPixels(activity, 3f)

        val quoteIndent = View(activity)
        quoteLayout.addView(quoteIndent)
        quoteIndent.setBackgroundColor(Color.rgb(128, 128, 128))

        run {
            val quoteIndentLayoutParams = quoteIndent.getLayoutParams()
            quoteIndentLayoutParams.width = quoteBarWidth
            quoteIndentLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            (quoteIndentLayoutParams as MarginLayoutParams).rightMargin = quoteBarWidth
            quoteIndent.setLayoutParams(quoteIndentLayoutParams)
        }

        if (mElements.size == 1) {
            quoteLayout.addView(
                mElements.get(0)!!
                    .generateView(
                        activity,
                        textColor,
                        textSize,
                        showLinkButtons
                    )
            )
        } else {
            quoteLayout.addView(
                BodyElementVerticalSequence(mElements)
                    .generateView(
                        activity,
                        textColor,
                        textSize,
                        showLinkButtons
                    )
            )
        }

        setLayoutMatchWidthWrapHeight(quoteLayout)

        return quoteLayout
    }
}
