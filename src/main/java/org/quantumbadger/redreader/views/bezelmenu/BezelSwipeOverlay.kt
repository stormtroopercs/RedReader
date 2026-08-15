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
import android.view.MotionEvent
import android.view.View
import androidx.annotation.IntDef
import org.quantumbadger.redreader.common.General.dpToPixels
import org.quantumbadger.redreader.common.PrefsUtility
import org.quantumbadger.redreader.common.General
import java.lang.annotation.Retention

class BezelSwipeOverlay(context: Context?, private val listener: BezelSwipeListener) :
    View(context) {
    @IntDef([LEFT, RIGHT])
    @Retention(AnnotationRetention.SOURCE)
    annotation class SwipeEdge

    private val mSwipeZonePixels: Int

    init {
        val swipeZoneDp = PrefsUtility.pref_behaviour_bezel_toolbar_swipezone_dp()

        mSwipeZonePixels = dpToPixels(getContext(), swipeZoneDp.toFloat())
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.getAction() and MotionEvent.ACTION_MASK

        if (action == MotionEvent.ACTION_DOWN) {
            if (event.getX() < mSwipeZonePixels) {
                return listener.onSwipe(LEFT)
            } else if (event.getX() > getWidth() - mSwipeZonePixels) {
                return listener.onSwipe(RIGHT)
            } else {
                return listener.onTap()
            }
        }

        return false
    }

    interface BezelSwipeListener {
        fun onSwipe(@SwipeEdge edge: Int): Boolean

        fun onTap(): Boolean
    }

    companion object {
        const val LEFT: Int = 0
        const val RIGHT: Int = 1
    }
}
