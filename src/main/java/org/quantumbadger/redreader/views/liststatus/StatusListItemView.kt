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
package org.quantumbadger.redreader.views.liststatus

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import org.quantumbadger.redreader.common.General.setLayoutMatchWidthWrapHeight

open class StatusListItemView(context: Context) : FrameLayout(context) {
    protected val dpScale: Float

    private var contents: View?=null

    init {
        dpScale = context.getResources().getDisplayMetrics().density // TODO xml?
    }

    fun setContents(contents: View) {
        if (this.contents != null) {
            removeView(this.contents)
        }
        this.contents = contents
        addView(contents)
        setLayoutMatchWidthWrapHeight(contents)
    }

    fun hideNoAnim() {
        setVisibility(GONE)
        removeAllViews()
        contents = null

        requestLayout()
    }
}
