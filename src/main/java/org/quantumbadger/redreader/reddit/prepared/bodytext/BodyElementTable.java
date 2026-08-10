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
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TableLayout
import org.quantumbadger.redreader.activities.BaseActivity

class BodyElementTable(private val mElements: ArrayList<BodyElement>) :
    BodyElement(BlockType.TABLE) {
    override fun generateView(
        activity: BaseActivity,
        textColor: Int?,
        textSize: Float?,
        showLinkButtons: Boolean
    ): View {
        val table = TableLayout(activity)

        for (element in mElements) {
            val view = element.generateView(
                activity,
                textColor,
                textSize,
                showLinkButtons
            )
            table.addView(view)
        }

        table.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE)
        table.setDividerDrawable(ColorDrawable(Color.GRAY))

        table.setLayoutParams(
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val scrollView = HorizontalScrollView(activity)

        scrollView.addView(table)

        return scrollView
    }
}
