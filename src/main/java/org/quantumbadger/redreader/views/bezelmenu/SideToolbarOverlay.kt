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

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout

class SideToolbarOverlay(context: Context) : FrameLayout(context) {
    private var contents: View?=null
    private var shownPosition: SideToolbarPosition?=null

    enum class SideToolbarPosition {
        LEFT, RIGHT
    }

    fun setContents(contents: View) {
        this.contents = contents
        if (shownPosition != null) {
            show(shownPosition)
        }
    }

    @SuppressLint("RtlHardcoded")
    fun show(pos: SideToolbarPosition?) {
        removeAllViews()
        addView(contents)

        val layoutParams = contents!!.getLayoutParams()

        (layoutParams as LayoutParams).gravity = (if (pos == SideToolbarPosition.LEFT) Gravity.LEFT else Gravity.RIGHT)
        layoutParams.width = LayoutParams.WRAP_CONTENT
        layoutParams.height = LayoutParams.MATCH_PARENT

        contents!!.setLayoutParams(layoutParams)

        shownPosition = pos
    }

    fun hide() {
        shownPosition = null
        removeAllViews()
    }

    override fun isShown(): Boolean {
        return shownPosition != null
    }
}
