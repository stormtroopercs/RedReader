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
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.General.setLayoutMatchWidthWrapHeight
import org.quantumbadger.redreader.common.General

class BodyElementVerticalSequence(private val mElements: ArrayList<BodyElement>) : BodyElement(
    BlockType.VERTICAL_SEQUENCE
) {
    override fun generateView(
        activity: BaseActivity,
        textColor: Int?,
        textSize: Float?,
        showLinkButtons: Boolean
    ): View {
        val result = LinearLayout(activity)
        result.setOrientation(LinearLayout.VERTICAL)

        val dpScale = activity.getResources().getDisplayMetrics().density
        val paragraphSpacing = (dpScale * 6).toInt()

        var lastBlock: BlockType?=null

        for (element in mElements) {
            val view = element.generateView(
                activity,
                textColor,
                textSize,
                showLinkButtons
            )
            result.addView(view)

            val layoutParams = view.getLayoutParams() as LinearLayout.LayoutParams

            if (lastBlock != null) {
                if (!(element.type == BlockType.LIST_ELEMENT
                            && lastBlock == BlockType.LIST_ELEMENT)
                ) {
                    layoutParams.topMargin = paragraphSpacing
                }
            }

            view.setLayoutParams(layoutParams)

            lastBlock = element.type
        }

        setLayoutMatchWidthWrapHeight(result)

        return result
    }
}
