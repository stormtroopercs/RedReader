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
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.quantumbadger.redreader.views

import android.annotation.SuppressLint
import android.text.Selection
import android.text.Spannable
import android.text.style.ClickableSpan
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import org.quantumbadger.redreader.common.PrefsUtility

class LinkifiedTextView(val activity: AppCompatActivity) : AppCompatTextView(activity) {
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val text = getText()

        if (text !is Spannable) {
            return false
        }

        if (!PrefsUtility.pref_appearance_link_text_clickable()) {
            return false
        }

        val buffer = text

        val action = event.getAction()

        if (action == MotionEvent.ACTION_UP ||
            action == MotionEvent.ACTION_DOWN
        ) {
            var x = event.getX().toInt()
            var y = event.getY().toInt()

            x -= getTotalPaddingLeft()
            y -= getTotalPaddingTop()

            x += getScrollX()
            y += getScrollY()

            val layout = getLayout()
            val line = layout.getLineForVertical(y)
            val off = layout.getOffsetForHorizontal(line, x.toFloat())

            val links = buffer.getSpans<ClickableSpan?>(off, off, ClickableSpan::class.java)

            if (links.size != 0) {
                if (action == MotionEvent.ACTION_UP) {
                    links[0]!!.onClick(this)
                } else if (action == MotionEvent.ACTION_DOWN) {
                    Selection.setSelection(
                        buffer,
                        buffer.getSpanStart(links[0]),
                        buffer.getSpanEnd(links[0])
                    )
                }

                return true
            } else {
                Selection.removeSelection(buffer)
            }
        }

        return false
    }
}
