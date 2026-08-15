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
import android.widget.FrameLayout
import org.quantumbadger.redreader.activities.BaseActivity
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.common.General

class BodyElementTableCell(private val mElements: ArrayList<BodyElement?>) :
    BodyElement(BlockType.TABLE_CELL) {
    override fun generateView(
        activity: BaseActivity,
        textColor: Int?,
        textSize: Float?,
        showLinkButtons: Boolean
    ): View {
        // Don't show link buttons inside tables

        val inner = BodyElementVerticalSequence(mElements)
            .generateView(activity, textColor, textSize, false)

        val padding = FrameLayout(activity)
        padding.addView(inner)

        val verticalPaddingPx = dpToPixels(activity, 2f)
        val horizontalPaddingPx = dpToPixels(activity, 5f)
        padding.setPadding(
            horizontalPaddingPx,
            verticalPaddingPx,
            horizontalPaddingPx,
            verticalPaddingPx
        )

        return padding
    }
}
