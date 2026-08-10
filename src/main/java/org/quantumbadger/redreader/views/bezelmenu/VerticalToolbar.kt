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
package org.quantumbadger.redreader.views.bezelmenu

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import org.quantumbadger.redreader.common.General.dpToPixels

class VerticalToolbar(context: Context) : FrameLayout(context) {
    private val buttons: LinearLayout

    init {
        setBackgroundColor(Color.argb(192, 0, 0, 0)) // TODO change color based on theme?

        setElevation(dpToPixels(context, 10f).toFloat())

        // TODO add light, vertical line on swipe side
        buttons = LinearLayout(context)
        buttons.setOrientation(LinearLayout.VERTICAL)

        val sv = ScrollView(context)
        sv.addView(buttons)
        addView(sv)
    }

    fun addItem(v: View?) {
        buttons.addView(v)
    }
}
