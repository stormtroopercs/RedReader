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
import android.view.ViewGroup
import android.widget.TableRow
import org.quantumbadger.redreader.activities.BaseActivity

class BodyElementTableRow(private val mElements: ArrayList<BodyElement>) :
    BodyElement(BlockType.TABLE_ROW) {
    override fun generateView(
        activity: BaseActivity,
        textColor: Int?,
        textSize: Float?,
        showLinkButtons: Boolean
    ): View {
        val result = TableRow(activity)

        for (element in mElements) {
            val view = element.generateView(
                activity,
                textColor,
                textSize,
                showLinkButtons
            )
            result.addView(view)

            val layoutParams = view.getLayoutParams() as TableRow.LayoutParams

            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT

            view.setLayoutParams(layoutParams)
        }

        return result
    }
}
