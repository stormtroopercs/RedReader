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
import android.widget.LinearLayout
import android.widget.TextView
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.common.General.setLayoutMatchWidthWrapHeight
import org.quantumbadger.redreader.common.General

class BodyElementNumberedListElement(
    private val mListIndex: Int,
    private val mElements: ArrayList<BodyElement>
) : BodyElement(BlockType.LIST_ELEMENT) {
    override fun generateView(
        activity: BaseActivity,
        textColor: Int?,
        textSize: Float?,
        showLinkButtons: Boolean
    ): View {
        val outerLayout = LinearLayout(activity)
        val paddingPx = dpToPixels(activity, 6f)
        outerLayout.setPadding(paddingPx, 0, paddingPx, 0)

        val number = TextView(activity)
        number.setText(mListIndex.toString() + ".  ")
        if (textSize != null) {
            number.setTextSize(textSize)
        }

        outerLayout.addView(number)

        if (mElements.size == 1) {
            outerLayout.addView(
                mElements.get(0)!!
                    .generateView(
                        activity,
                        textColor,
                        textSize,
                        showLinkButtons
                    )
            )
        } else {
            outerLayout.addView(
                BodyElementVerticalSequence(mElements)
                    .generateView(
                        activity,
                        textColor,
                        textSize,
                        showLinkButtons
                    )
            )
        }

        setLayoutMatchWidthWrapHeight(outerLayout)

        return outerLayout
    }
}
